package com.streaming.sune.ui.screen


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.White

/**
 * Error Screen - Displayed when app is offline or network request fails
 */
@Composable
fun ErrorScreen(
    errorMessage: String = "Unable to connect to server",
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Error icon (using text as placeholder)
            Text(
                text = "⚠️",
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error title
            Text(
                text = "Oops! Something went wrong",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            Text(
                text = errorMessage,
                fontSize = 16.sp,
                color = White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Retry button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NetflixRed)
                    .clickable { onRetry() }
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Retry",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help text
            Text(
                text = "Please check your internet connection",
                fontSize = 14.sp,
                color = White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}