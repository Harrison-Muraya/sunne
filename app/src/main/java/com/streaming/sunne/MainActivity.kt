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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.streaming.sunne.ui.screen.*
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.SunneTheme
import com.streaming.sunne.viewmodel.StreamViewModel

/**
 * Main Activity - Entry point of the app
 * Sets up navigation and theme
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SunneTheme {
                // Set status bar color
                val systemUiController = rememberSystemUiController()
                systemUiController.setSystemBarsColor(
                    color = DarkBackground
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    SunneApp()
                }
            }
        }
    }
}

/**
 * Main app composable with navigation
 */
@Composable
fun SunneApp() {
    val navController = rememberNavController()
    val viewModel: StreamViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onStreamClick = { stream ->
                    viewModel.selectStream(stream)
                    navController.navigate(Screen.Details.route)
                }
            )
        }

        // Details Screen
        composable(Screen.Details.route) {
            val selectedStream = viewModel.selectedStream.value
            if (selectedStream != null) {
                DetailsScreen(
                    stream = selectedStream,
                    onPlayClick = { stream ->
                        viewModel.selectStream(stream)
                        navController.navigate(Screen.Player.route)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Player Screen
        composable(Screen.Player.route) {
            val selectedStream = viewModel.selectedStream.value
            if (selectedStream != null) {
                PlayerScreen(
                    stream = selectedStream,
                    onBackPress = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Details : Screen("details")
    object Player : Screen("player")
}