package com.streaming.sunne.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.streaming.sunne.data.model.LiveStreamPublicResponse
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.White
import com.streaming.sunne.viewmodel.LiveStreamViewModel
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LivePlayerScreen(
    stream: LiveStreamPublicResponse,
    viewModel: LiveStreamViewModel,
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    var viewerCount by remember { mutableStateOf(stream.viewerCount) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            stream.streamUrl?.let { url ->
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
        }
    }

    // Join stream on entry
    LaunchedEffect(stream.id) {
        viewModel.joinStream(stream.id)
    }

    // Poll viewer count every 10 seconds
    LaunchedEffect(stream.id) {
        while (isActive) {
            delay(10_000)
            // Optionally re-fetch stream to get updated viewer count
            // For now just keep the initial count
        }
    }

    // Leave stream on exit
    DisposableEffect(stream.id) {
        onDispose {
            viewModel.leaveStream(stream.id)
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // ExoPlayer
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top overlay row ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back button + LIVE badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .clickable { onBackPress() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pulsing LIVE badge
                val inf = rememberInfiniteTransition(label = "dot")
                val pulseAlpha by inf.animateFloat(
                    initialValue = 0.5f,
                    targetValue  = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(700),
                        RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Row(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(0.7f),
                            androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                NetflixRed.copy(alpha = pulseAlpha),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text("LIVE", fontSize = sp(12), fontWeight = FontWeight.Black, color = White)
                }
            }

            // Right: Viewer count
            Row(
                modifier = Modifier
                    .background(
                        Color.Black.copy(0.7f),
                        androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Visibility, null, tint = White, modifier = Modifier.size(16.dp))
                Text(stream.formattedViewerCount(), fontSize = sp(12), color = White)
            }
        }

        // ── Stream title at bottom ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(0.8f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                stream.title,
                fontSize   = sp(18),
                fontWeight = FontWeight.Bold,
                color      = White
            )
            stream.description?.let {
                Text(it, fontSize = sp(13), color = White.copy(0.7f), maxLines = 2)
            }
        }
    }
}

private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Int) = sp(value.toFloat())