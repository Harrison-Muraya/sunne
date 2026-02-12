package com.streaming.sunne


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.streaming.sunne.ui.screen.*
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.SunneTheme
import com.streaming.sunne.viewmodel.StreamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val navController = rememberNavController()
    val viewModel: StreamViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onStreamClick = { stream ->
                    viewModel.selectStream(stream)
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
                viewModel = viewModel,
                onChannelPlay = { stream ->
                    viewModel.selectStream(stream)
                    navController.navigate(Screen.Player.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Details ───────────────────────────────────────────────────────────
        composable(Screen.Details.route) {
            val stream = viewModel.selectedStream.value
            if (stream != null) {
                DetailsScreen(
                    stream = stream,
                    onPlayClick = {
                        navController.navigate(Screen.Player.route)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // ── Player ────────────────────────────────────────────────────────────
        composable(Screen.Player.route) {
            val stream = viewModel.selectedStream.value
            if (stream != null) {
                PlayerScreen(
                    stream = stream,
                    onBackPress = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

// ─── Navigation Routes ─────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Splash  : Screen("splash")
    object Home    : Screen("home")
    object Live    : Screen("live")
    object Details : Screen("details")
    object Player  : Screen("player")
}