package com.streaming.sunne.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.White
import kotlinx.coroutines.delay

/**
 * Splash Screen - Shows Sunne logo with fade-in animation
 * Automatically navigates to Home after 2 seconds
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    // Animation for fade-in effect
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    // Navigate to home after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Name
            Text(
                text = "SUNNE",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = NetflixRed,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "Your Streaming Experience",
                fontSize = 18.sp,
                color = White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
