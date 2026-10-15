package com.streaming.sunne.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.Composable
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

/**
 * Rotates phones to landscape while a player is on screen and hands rotation back to the
 * user afterwards. TVs are already landscape, so this is a no-op there.
 */
@Composable
fun LandscapeWhileVisible() {
    val context = LocalContext.current
    val isTV = (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    DisposableEffect(context) {
        val activity = context.findActivity()
        if (activity != null && !isTV) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (activity != null && !isTV) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
