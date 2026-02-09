package com.streaming.sunne.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val DarkColorPalette = darkColors(
    primary = NetflixRed,
    primaryVariant = Color(0xFFB00710),
    secondary = AccentGray,
    background = DarkBackground,
    surface = CardBackground,
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = White,
    error = ErrorRed
)

@Composable
fun SuneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = DarkColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}