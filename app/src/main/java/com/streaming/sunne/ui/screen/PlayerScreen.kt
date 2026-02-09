package com.streaming.sunne.ui.screen

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.streaming.sunne.data.model.Stream
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.White


/**
 * Player Screen - Full-screen video playback using ExoPlayer
 */
@Composable
fun PlayerScreen(
    stream: Stream,
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Set media item
            val mediaItem = MediaItem.fromUri(stream.url)
            setMediaItem(mediaItem)

            // Prepare and play
            prepare()
            playWhenReady = true

            // Add listener for loading state
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isLoading = false
                            hasError = false
                        }
                        Player.STATE_BUFFERING -> {
                            isLoading = true
                        }
                        Player.STATE_ENDED -> {
                            onBackPress()
                        }
                        Player.STATE_IDLE -> {
                            // Do nothing
                        }
                    }
                }

                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                    hasError = true
                    isLoading = false
                }
            })
        }
    }

    // Release player when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Handle back press
    BackHandler {
        exoPlayer.release()
        onBackPress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ExoPlayer view
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading && !hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading video...",
                        fontSize = 18.sp,
                        color = White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Error message
        if (hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to load video",
                        fontSize = 24.sp,
                        color = NetflixRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please check your connection and try again",
                        fontSize = 16.sp,
                        color = White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Simple back handler composable
 */
@Composable
fun BackHandler(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
}