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
import com.streaming.sunne.viewmodel.*


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
    val navController   = rememberNavController()
    val authViewModel   : AuthViewModel   = viewModel()
    val movieViewModel  : MovieViewModel  = viewModel()
    val searchViewModel : SearchViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (TokenManager.isLoggedIn) authViewModel.loadCurrentUser()
    }

    val start = if (TokenManager.isLoggedIn) Screen.Home.route else Screen.Splash.route

    NavHost(navController = navController, startDestination = start) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(onNavigateToHome = {
                val dest = if (TokenManager.isLoggedIn) Screen.Home.route else Screen.Login.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel            = authViewModel,
                onLoginSuccess       = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel         = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel     = movieViewModel,
                authViewModel = authViewModel,
                onMovieClick  = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Details.route)
                },
                onSeriesClick = { series ->
                    movieViewModel.selectSeries(series)
                    navController.navigate(Screen.SeriesDetail.route)
                },
                onLiveStreamClick = { stream ->
                    movieViewModel.selectLiveStream(stream)
                    navController.navigate(Screen.LivePlayer.route)
                },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onLiveTabClick = { navController.navigate(Screen.LiveStreamList.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onWatchHistoryClick = { navController.navigate(Screen.WatchHistory.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        // ── Search ────────────────────────────────────────────────────────────
        composable(Screen.Search.route) {
            SearchScreen(
                viewModel    = searchViewModel,
                onMovieClick = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Details.route)
                },
                onSeriesClick = { series ->
                    movieViewModel.selectSeries(series)
                    navController.navigate(Screen.SeriesDetail.route)
                },
                onBackClick  = { navController.popBackStack() }
            )
        }

        // ── Movie Detail + Player ─────────────────────────────────────────────
        composable(Screen.Details.route) {
            val movie = movieViewModel.selectedMovie.value
            if (movie != null) {
                DetailsScreen(
                    movie        = movie,
                    viewModel    = movieViewModel,
                    onPlayClick  = { navController.navigate(Screen.Player.route) },
                    onBackClick  = { navController.popBackStack() },
                    onMovieClick = { related ->
                        movieViewModel.selectMovie(related)
                        // Replace to avoid deep back stacks
                        navController.navigate(Screen.Details.route)
                    }
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

        // ── Series ────────────────────────────────────────────────────────────
        composable(Screen.SeriesDetail.route) {
            val series by movieViewModel.selectedSeries.collectAsState()
            if (series != null) {
                SeriesDetailScreen(
                    series         = series!!,
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
                    episode        = episode,
                    viewModel      = movieViewModel,
                    onBackPress    = { navController.popBackStack() },
                    onAutoPlayNext = { nextEpisode ->
                        movieViewModel.selectEpisode(nextEpisode)
                        // Stay on EpisodePlayer — recompose with new episode
                    }
                )
            }
        }

        // ── Live Streaming ────────────────────────────────────────────────────
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

        composable(Screen.LivePlayer.route) {
            val stream = movieViewModel.selectedLiveStream.value
            if (stream != null) {
                LivePlayerScreen(
                    stream      = stream,
                    viewModel   = LiveStreamViewModel(),
                    onBackPress = { navController.popBackStack() }
                )
            }
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                authViewModel    = authViewModel,
                profileViewModel = profileViewModel,
                onBackClick      = { navController.popBackStack() },
                onLogout         = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Watch History ─────────────────────────────────────────────────────
        composable(Screen.WatchHistory.route) {
            val historyViewModel: WatchHistoryViewModel = viewModel()
            WatchHistoryScreen(
                viewModel    = historyViewModel,
                onMovieClick = { movieId ->
                    // We only have id here; navigate to details after fetching
                    navController.popBackStack()
                },
                onBackClick  = { navController.popBackStack() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Screen route definitions
// ─────────────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object Search         : Screen("search")
    object Details        : Screen("details")
    object Player         : Screen("player")
    object SeriesDetail   : Screen("series-detail")
    object EpisodePlayer  : Screen("episode-player")
    object LiveStreamList : Screen("live-stream-list")
    object LivePlayer     : Screen("live-player")
    object Profile        : Screen("profile")
    object WatchHistory   : Screen("watch-history")
    // Legacy — kept for any remaining usages
    object Live           : Screen("live")
}