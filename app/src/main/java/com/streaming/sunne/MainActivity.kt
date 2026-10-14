package com.streaming.sunne

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.streaming.sunne.data.model.ContentItem
import com.streaming.sunne.data.network.SessionEvents
import com.streaming.sunne.data.network.TokenManager
import com.streaming.sunne.ui.screens.*
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.SunneTheme
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.LiveStreamViewModel
import com.streaming.sunne.viewmodel.MovieViewModel
import com.streaming.sunne.viewmodel.UpNext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(applicationContext)
        setContent {
            SunneTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect { systemUiController.setSystemBarsColor(color = DarkBackground) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    SuneApp()
                }
            }
        }
    }
}

@Composable
fun SuneApp() {
    val navController  = rememberNavController()
    val authViewModel  : AuthViewModel  = viewModel()
    val movieViewModel : MovieViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (TokenManager.isLoggedIn) authViewModel.loadCurrentUser()
    }

    LaunchedEffect(Unit) {
        SessionEvents.expired.collect {
            authViewModel.onSessionExpired()
            movieViewModel.reset()
            navController.resetTo(Screen.Login.route)
        }
    }

    val selectedMovie   by movieViewModel.selectedMovie.collectAsState()
    val selectedSeries  by movieViewModel.selectedSeries.collectAsState()
    val selectedEpisode by movieViewModel.selectedEpisode.collectAsState()
    val selectedStream  by movieViewModel.selectedLiveStream.collectAsState()

    val start = if (TokenManager.isLoggedIn) Screen.Home.route else Screen.Splash.route

    NavHost(navController = navController, startDestination = start) {

        composable(Screen.Splash.route) {
            SplashScreen(onNavigateToHome = {
                val dest = if (TokenManager.isLoggedIn) Screen.Home.route else Screen.Login.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel          = authViewModel,
                onLoginSuccess     = { navController.resetTo(Screen.Home.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel          = authViewModel,
                onRegisterSuccess  = { navController.resetTo(Screen.Home.route) },
                onNavigateToLogin  = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel     = movieViewModel,
                authViewModel = authViewModel,
                onItemClick   = { item ->
                    when (item) {
                        is ContentItem.Movie -> {
                            movieViewModel.selectMovie(item.data)
                            navController.navigate(Screen.Details.route)
                        }
                        is ContentItem.Series -> {
                            movieViewModel.selectSeries(item.data)
                            navController.navigate(Screen.SeriesDetail.route)
                        }
                        is ContentItem.ContinueSeries -> movieViewModel.resumeSeries(
                            item      = item.data,
                            onPlay    = { navController.navigate(Screen.EpisodePlayer.route) },
                            onDetails = { navController.navigate(Screen.SeriesDetail.route) }
                        )
                        is ContentItem.LiveStream -> {
                            movieViewModel.selectLiveStream(item.data)
                            navController.navigate(Screen.LivePlayer.route)
                        }
                    }
                },
                onPlayMovie   = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Player.route)
                },
                onLiveClick   = { navController.navigate(Screen.LiveStreamList.route) },
                onLogout      = {
                    authViewModel.logout()
                    movieViewModel.reset()
                    navController.resetTo(Screen.Login.route)
                }
            )
        }

        composable(Screen.Details.route) { entry ->
            RequireSelection(selectedMovie, navController, entry) { movie ->
                DetailsScreen(
                    movie       = movie,
                    onPlayClick = { navController.navigate(Screen.Player.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Player.route) { entry ->
            RequireSelection(selectedMovie, navController, entry) { movie ->
                PlayerScreen(
                    movie       = movie,
                    onBackPress = { navController.popBackStack() },
                    onUpNext    = { next -> handleUpNext(next, movieViewModel, navController, Screen.Player.route) }
                )
            }
        }

        composable(Screen.SeriesDetail.route) { entry ->
            RequireSelection(selectedSeries, navController, entry) { series ->
                SeriesDetailScreen(
                    initial        = series,
                    onEpisodeClick = { fullSeries, episode ->
                        movieViewModel.selectSeries(fullSeries)
                        movieViewModel.selectEpisode(episode)
                        navController.navigate(Screen.EpisodePlayer.route)
                    },
                    onBackClick    = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.EpisodePlayer.route) { entry ->
            RequireSelection(selectedEpisode, navController, entry) { episode ->
                EpisodePlayerScreen(
                    episode     = episode,
                    seriesTitle = selectedSeries?.title,
                    onBackPress = { navController.popBackStack() },
                    onUpNext    = { next -> handleUpNext(next, movieViewModel, navController, Screen.EpisodePlayer.route) }
                )
            }
        }

        composable(Screen.LiveStreamList.route) {
            val liveViewModel: LiveStreamViewModel = viewModel()
            LiveStreamListScreen(
                viewModel     = liveViewModel,
                onStreamClick = { stream ->
                    movieViewModel.selectLiveStream(stream)
                    navController.navigate(Screen.LivePlayer.route)
                },
                onBackClick   = { navController.popBackStack() }
            )
        }

        composable(Screen.LivePlayer.route) { entry ->
            RequireSelection(selectedStream, navController, entry) { stream ->
                val liveViewModel: LiveStreamViewModel = viewModel()
                LivePlayerScreen(
                    stream      = stream,
                    viewModel   = liveViewModel,
                    onBackPress = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Selections live in [MovieViewModel]; if one is gone (e.g. after a reset) leave the screen. */
@Composable
private fun <T : Any> RequireSelection(
    value: T?,
    navController: NavHostController,
    entry: NavBackStackEntry,
    content: @Composable (T) -> Unit
) {
    if (value != null) {
        content(value)
    } else {
        LaunchedEffect(Unit) {
            if (navController.currentBackStackEntry?.id == entry.id) navController.popBackStack()
        }
    }
}

private fun handleUpNext(
    next: UpNext,
    movieViewModel: MovieViewModel,
    navController: NavHostController,
    currentRoute: String
) {
    when (next) {
        is UpNext.Movie -> {
            movieViewModel.selectMovie(next.movie)
            navController.navigate(Screen.Player.route) {
                popUpTo(currentRoute) { inclusive = true }
            }
        }
        is UpNext.Episode -> {
            movieViewModel.selectEpisode(next.episode)
            if (currentRoute != Screen.EpisodePlayer.route) {
                navController.navigate(Screen.EpisodePlayer.route) {
                    popUpTo(currentRoute) { inclusive = true }
                }
            }
        }
        is UpNext.Series -> movieViewModel.openSeries(next.seriesId) {
            navController.navigate(Screen.SeriesDetail.route) {
                popUpTo(currentRoute) { inclusive = true }
            }
        }
    }
}

/** Navigates to [route] and drops the entire back stack. */
private fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object Details        : Screen("details")
    object Player         : Screen("player")
    object SeriesDetail   : Screen("series-detail")
    object EpisodePlayer  : Screen("episode-player")
    object LiveStreamList : Screen("live-stream-list")
    object LivePlayer     : Screen("live-player")
}
