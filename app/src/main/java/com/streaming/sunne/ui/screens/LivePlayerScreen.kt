package com.streaming.sunne.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.streaming.sunne.data.model.LiveStreamPublicResponse
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.White
import com.streaming.sunne.viewmodel.LiveStreamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val VIEWER_POLL_MS = 15_000L

@Composable
fun LivePlayerScreen(
    stream: LiveStreamPublicResponse,
    viewModel: LiveStreamViewModel,
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    val url = stream.streamUrl
    val viewerCount by viewModel.viewerCount.collectAsState()

    var attempt by remember { mutableStateOf(0) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler { onBackPress() }

    LaunchedEffect(stream.id) { viewModel.joinStream(stream.id) }

    LaunchedEffect(stream.id) {
        while (isActive) {
            viewModel.refreshViewerCount(stream.id)
            delay(VIEWER_POLL_MS)
        }
    }

    DisposableEffect(stream.id) {
        onDispose { viewModel.leaveStream(stream.id) }
    }

    Box(Modifier.fillMaxSize().background(DarkBackground)) {
        if (url.isNullOrBlank() || !stream.isLive) {
            PlayerErrorPanel(
                title   = stream.title,
                message = if (stream.isLive) "This stream isn't available right now." else "This stream has ended.",
                onRetry = onBackPress,
                onBack  = null
            )
        } else {
            val player = remember(url, attempt) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                        if (state == Player.STATE_READY) errorMessage = null
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        isBuffering = false
                        errorMessage = playbackErrorMessage(error)
                    }
                }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                    player.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && errorMessage == null) {
                CircularProgressIndicator(color = NetflixRed, modifier = Modifier.align(Alignment.Center).size(56.dp))
            }

            errorMessage?.let { message ->
                PlayerErrorPanel(
                    title   = stream.title,
                    message = message,
                    onRetry = {
                        errorMessage = null
                        isBuffering = true
                        attempt++
                    },
                    onBack  = onBackPress
                )
            }
        }

        if (errorMessage == null) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(end = 160.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerBackButton(onBackPress)
                LiveDot()
                Spacer(Modifier.width(10.dp))
                Text(
                    stream.title,
                    color = White,
                    fontSize = TextUnit(15f, TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Visibility, null, tint = White, modifier = Modifier.size(16.dp))
                Text(
                    formatViewers(viewerCount ?: stream.viewerCount),
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    color = White
                )
            }
        }
    }
}

private fun formatViewers(count: Int): String = when {
    count < 1_000     -> "$count"
    count < 1_000_000 -> "${count / 1_000}K"
    else              -> "${count / 1_000_000}M"
}
