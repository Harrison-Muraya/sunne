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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.streaming.sunne.data.network.TokenManager
import com.streaming.sunne.ui.screens.*
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.SunneTheme
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.MovieViewModel
import com.streaming.sunne.viewmodel.LiveStreamViewModel


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

    // Load current user on startup if already logged in
    LaunchedEffect(Unit) {
        if (TokenManager.isLoggedIn) authViewModel.loadCurrentUser()
    }

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
                onLoginSuccess     = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel          = authViewModel,
                onRegisterSuccess  = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin  = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel     = movieViewModel,
                authViewModel = authViewModel,       // ← passes user info + logout
                onMovieClick  = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Details.route)
                },
                onLiveClick   = { navController.navigate(Screen.Live.route) },
                onLogout      = {
                    // Clear everything and go to Login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Live.route) {
            LiveScreen(
                viewModel      = movieViewModel,
                onChannelPlay  = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Player.route)
                },
                onBackClick    = { navController.popBackStack() }
            )
        }

        composable(Screen.Details.route) {
            val movie = movieViewModel.selectedMovie.value
            if (movie != null) {
                DetailsScreen(
                    movie       = movie,
                    onPlayClick = { navController.navigate(Screen.Player.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Player.route) {
            val movie = movieViewModel.selectedMovie.value
            if (movie != null) {
                PlayerScreen(
                    movie       = movie,
                    viewModel   = movieViewModel,
                    onBackPress = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.SeriesDetail.route) {
            val series = movieViewModel.selectedSeries.value
            if (series != null) {
                SeriesDetailScreen(
                    series = series,
                    onEpisodeClick = { episode ->
                        movieViewModel.selectEpisode(episode)
                        navController.navigate(Screen.EpisodePlayer.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.EpisodePlayer.route) {
            val episode = movieViewModel.selectedEpisode.value
            if (episode != null) {
                EpisodePlayerScreen(
                    episode = episode,
                    viewModel = movieViewModel,
                    onBackPress = { navController.popBackStack() },
                    onAutoPlayNext = { nextEpisode ->
                        movieViewModel.selectEpisode(nextEpisode)
                        // Stay on same screen, player reloads
                    }
                )
            }
        }

        composable(Screen.LiveStreamList.route) {
            val liveViewModel: LiveStreamViewModel = viewModel()
            LiveStreamListScreen(
                viewModel = liveViewModel,
                onStreamClick = { stream ->
                    movieViewModel.selectLiveStream(stream)
                    navController.navigate(Screen.LivePlayer.route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.LivePlayer.route) {
            val stream = movieViewModel.selectedLiveStream.value
            if (stream != null) {
                LivePlayerScreen(
                    stream = stream,
                    viewModel = LiveStreamViewModel(),
                    onBackPress = { navController.popBackStack() }
                )
            }
        }
    }
}

sealed class Screen(val route: String) {
    object SeriesDetail : Screen("series-detail")
    object EpisodePlayer : Screen("episode-player")
    object LiveStreamList : Screen("live-stream-list")
    object LivePlayer : Screen("live-player")
    object Splash   : Screen("splash")
    object Login    : Screen("login")
    object Register : Screen("register")
    object Home     : Screen("home")
    object Live     : Screen("live")
    object Details  : Screen("details")
    object Player   : Screen("player")
}
