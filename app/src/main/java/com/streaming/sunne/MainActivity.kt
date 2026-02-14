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
import com.streaming.sunne.ui.screen.*
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.SunneTheme
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.MovieViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise token storage
        TokenManager.init(applicationContext)

        setContent {
            SunneTheme {
                val systemUiController = rememberSystemUiController()
                systemUiController.setSystemBarsColor(color = DarkBackground)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
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

    // Determine start destination based on stored token
    val startDestination = if (TokenManager.isLoggedIn) Screen.Home.route
    else Screen.Splash.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    val dest = if (TokenManager.isLoggedIn) Screen.Home.route
                    else Screen.Login.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = movieViewModel,
                onStreamClick = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Details.route)
                },
                onLiveClick = {
                    navController.navigate(Screen.Live.route)
                }
            )
        }

        // ── Live TV ───────────────────────────────────────────────────────────
        composable(Screen.Live.route) {
            LiveScreen(
                viewModel = movieViewModel,
                onChannelPlay = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Player.route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Details ───────────────────────────────────────────────────────────
        composable(Screen.Details.route) {
            val movie = movieViewModel.selectedMovie.value
            if (movie != null) {
                DetailsScreen(
                    stream = movie.toStream(),           // adapter below
                    onPlayClick = {
                        navController.navigate(Screen.Player.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // ── Player ────────────────────────────────────────────────────────────
        composable(Screen.Player.route) {
            val movie = movieViewModel.selectedMovie.value
            if (movie != null) {
                PlayerScreen(
                    movie = movie,
                    viewModel = movieViewModel,
                    onBackPress = { navController.popBackStack() }
                )
            }
        }
    }
}

// ─── Route constants ───────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Splash   : Screen("splash")
    object Login    : Screen("login")
    object Register : Screen("register")
    object Home     : Screen("home")
    object Live     : Screen("live")
    object Details  : Screen("details")
    object Player   : Screen("player")
}

// ─── Adapter: MovieResponse → legacy Stream model ──────────────────────────────
// The redesigned screens (HomeScreen, LiveScreen, DetailsScreen) still accept
// the old Stream type. This extension converts MovieResponse so both work.

fun com.streaming.sunne.data.model.MovieResponse.toStream() =
    com.streaming.sunne.data.model.Stream(
        id          = id,
        title       = title,
        thumbnail   = posterUrl ?: backdropUrl ?: "",
        url         = videoUrl  ?: "",
        category    = genres.firstOrNull()?.name ?: "Movies",
        description = description,
        banner      = backdropUrl ?: posterUrl,
        duration    = formattedDuration(),
        rating      = null,               // API rating is a String ("PG-13") not numeric
        release_year = releaseYear,
        director    = director,
        cast        = cast,
        language    = language,
        quality     = videoFiles.firstOrNull()?.quality ?: "HD",
        is_featured = isFeatured,
        is_live     = false,
        view_count  = viewCount
    )


//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material.MaterialTheme
//import androidx.compose.material.Surface
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.google.accompanist.systemuicontroller.rememberSystemUiController
//import com.streaming.sunne.ui.screen.*
//import com.streaming.sunne.ui.theme.DarkBackground
//import com.streaming.sunne.ui.theme.SunneTheme
//import com.streaming.sunne.viewmodel.StreamViewModel
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            SunneTheme {
//                val systemUiController = rememberSystemUiController()
//                systemUiController.setSystemBarsColor(color = DarkBackground)
//
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colors.background
//                ) {
//                    SuneApp()
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun SuneApp() {
//    val navController = rememberNavController()
//    val viewModel: StreamViewModel = viewModel()
//
//    NavHost(
//        navController = navController,
//        startDestination = Screen.Splash.route
//    ) {
//        // ── Splash ────────────────────────────────────────────────────────────
//        composable(Screen.Splash.route) {
//            SplashScreen(
//                onNavigateToHome = {
//                    navController.navigate(Screen.Home.route) {
//                        popUpTo(Screen.Splash.route) { inclusive = true }
//                    }
//                }
//            )
//        }
//
//        // ── Home ──────────────────────────────────────────────────────────────
//        composable(Screen.Home.route) {
//            HomeScreen(
//                viewModel = viewModel,
//                onStreamClick = { stream ->
//                    viewModel.selectStream(stream)
//                    navController.navigate(Screen.Details.route)
//                },
//                onLiveClick = {
//                    navController.navigate(Screen.Live.route)
//                }
//            )
//        }
//
//        // ── Live TV ───────────────────────────────────────────────────────────
//        composable(Screen.Live.route) {
//            LiveScreen(
//                viewModel = viewModel,
//                onChannelPlay = { stream ->
//                    viewModel.selectStream(stream)
//                    navController.navigate(Screen.Player.route)
//                },
//                onBackClick = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        // ── Details ───────────────────────────────────────────────────────────
//        composable(Screen.Details.route) {
//            val stream = viewModel.selectedStream.value
//            if (stream != null) {
//                DetailsScreen(
//                    stream = stream,
//                    onPlayClick = {
//                        navController.navigate(Screen.Player.route)
//                    },
//                    onBackClick = {
//                        navController.popBackStack()
//                    }
//                )
//            }
//        }
//
//        // ── Player ────────────────────────────────────────────────────────────
//        composable(Screen.Player.route) {
//            val stream = viewModel.selectedStream.value
//            if (stream != null) {
//                PlayerScreen(
//                    stream = stream,
//                    onBackPress = {
//                        navController.popBackStack()
//                    }
//                )
//            }
//        }
//    }
//}
//
//// ─── Navigation Routes ─────────────────────────────────────────────────────────
//sealed class Screen(val route: String) {
//    object Splash  : Screen("splash")
//    object Home    : Screen("home")
//    object Live    : Screen("live")
//    object Details : Screen("details")
//    object Player  : Screen("player")
//}