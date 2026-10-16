package com.streaming.sunne.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Runs [onResume] each time this screen becomes visible again (e.g. back from the player).
 * With [skipFirst], the initial resume when the screen first opens is ignored.
 */
@Composable
fun OnResumeEffect(key: Any?, skipFirst: Boolean = false, onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner, key) {
        var first = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!(skipFirst && first)) currentOnResume()
                first = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** How the viewer is holding the phone while a video or live stream is open. */
enum class HoldMode(val label: String, val orientation: Int) {
    Auto("Auto", ActivityInfo.SCREEN_ORIENTATION_FULL_USER),
    Upright("Upright", ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT),
    Landscape("Landscape", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
}

/**
 * Follows [mode] while a player is on screen and restores free rotation afterwards.
 * TVs stay landscape.
 */
@Composable
fun PlaybackOrientation(mode: HoldMode) {
    val context = LocalContext.current
    val isTV = (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val activity = context.findActivity()
    val window = activity?.window
    SideEffect { window?.let { hideSystemBars(it) } }
    DisposableEffect(context, mode) {
        window?.let { hideSystemBars(it) }
        if (activity != null && !isTV) activity.requestedOrientation = mode.orientation
        onDispose {
            window?.let { showSystemBars(it) }
            if (activity != null && !isTV) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private fun hideSystemBars(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

private fun showSystemBars(window: Window) {
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    WindowCompat.setDecorFitsSystemWindows(window, true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
