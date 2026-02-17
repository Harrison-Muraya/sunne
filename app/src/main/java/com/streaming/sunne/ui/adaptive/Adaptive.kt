package com.streaming.sunne.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

enum class ScreenType { PHONE, TABLET, TV }

data class Adaptive(
    val screen: ScreenType,

    // Fonts
    val heroTitle: TextUnit,
    val sectionTitle: TextUnit,
    val body: TextUnit,
    val caption: TextUnit,
    val logo: TextUnit,

    // Spacing & sizing
    val pad: Dp,           // outer horizontal padding
    val cardW: Dp,
    val cardH: Dp,
    val bannerH: Dp,
    val rowGap: Dp,
    val cardGap: Dp,

    // Layout flags
    val useBottomNav: Boolean,
    val heroTextFraction: Float,  // fraction of screen width hero text occupies
) {
    val isTV get()     = screen == ScreenType.TV
    val isPhone get()  = screen == ScreenType.PHONE
    val isTablet get() = screen == ScreenType.TABLET
}

fun sp(value: Float) = TextUnit(value, TextUnitType.Sp)
fun sp(value: Int)   = TextUnit(value.toFloat(), TextUnitType.Sp)

@Composable
fun rememberAdaptive(): Adaptive {
    val cfg = LocalConfiguration.current
    val w   = cfg.screenWidthDp

    return remember(w) {
        when {
            w >= 960 -> Adaptive(          // TV / large landscape
                screen           = ScreenType.TV,
                heroTitle        = sp(50),
                sectionTitle     = sp(22),
                body             = sp(15),
                caption          = sp(12),
                logo             = sp(36),
                pad              = 56.dp,
                cardW            = 220.dp,
                cardH            = 130.dp,
                bannerH          = 560.dp,
                rowGap           = 32.dp,
                cardGap          = 12.dp,
                useBottomNav     = false,
                heroTextFraction = 0.55f,
            )
            w >= 600 -> Adaptive(          // Tablet / landscape phone
                screen           = ScreenType.TABLET,
                heroTitle        = sp(34),
                sectionTitle     = sp(18),
                body             = sp(14),
                caption          = sp(12),
                logo             = sp(28),
                pad              = 28.dp,
                cardW            = 175.dp,
                cardH            = 108.dp,
                bannerH          = 360.dp,
                rowGap           = 24.dp,
                cardGap          = 10.dp,
                useBottomNav     = true,
                heroTextFraction = 0.68f,
            )
            else -> Adaptive(              // Phone portrait
                screen           = ScreenType.PHONE,
                heroTitle        = sp(26),
                sectionTitle     = sp(16),
                body             = sp(13),
                caption          = sp(11),
                logo             = sp(22),
                pad              = 16.dp,
                cardW            = 138.dp,
                cardH            = 88.dp,
                bannerH          = 290.dp,
                rowGap           = 20.dp,
                cardGap          = 8.dp,
                useBottomNav     = true,
                heroTextFraction = 0.95f,
            )
        }
    }
}
