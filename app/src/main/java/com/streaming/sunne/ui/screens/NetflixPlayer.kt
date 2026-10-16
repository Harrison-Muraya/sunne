package com.streaming.sunne.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val SKIP_MS = 5_000L

/** How the picture sits in the screen. */
enum class PictureFit(val label: String, val resizeMode: Int) {
    Fit("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Full("Full", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    Stretch("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
}

internal fun seekBy(positionMs: Long, durationMs: Long, deltaMs: Long): Long {
    val next = positionMs + deltaMs
    return if (durationMs > 0) next.coerceIn(0, durationMs) else next.coerceAtLeast(0)
}

/**
 * Netflix-style surface: tap pauses, double-tap either side skips 5 seconds,
 * and the viewer picks portrait/landscape plus how the picture fits.
 */
@Composable
fun NetflixPlayer(
    player: ExoPlayer,
    title: String,
    onBack: () -> Unit,
    holdMode: HoldMode,
    onHoldMode: (HoldMode) -> Unit,
    pictureFit: PictureFit,
    onPictureFit: (PictureFit) -> Unit,
    allowSeek: Boolean = true,
    qualities: List<String> = emptyList(),
    currentQuality: String? = null,
    onQuality: ((String) -> Unit)? = null,
    topStart: @Composable RowScope.() -> Unit = {},
    topEnd: @Composable RowScope.() -> Unit = {},
) {
    var showControls by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(player.isPlaying) }
    var position by remember { mutableStateOf(player.currentPosition) }
    var duration by remember { mutableStateOf(player.duration.coerceAtLeast(0)) }
    var scrubbing by remember { mutableStateOf(false) }
    var skipFlash by remember { mutableStateOf<SkipFlash?>(null) }
    var fitMenu by remember { mutableStateOf(false) }
    var qualityMenu by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (isActive) {
            delay(250)
            playing = player.isPlaying
            if (!scrubbing) {
                position = player.currentPosition.coerceAtLeast(0)
                duration = player.duration.coerceAtLeast(0)
            }
        }
    }

    LaunchedEffect(showControls, playing, fitMenu, qualityMenu) {
        if (showControls && playing && !fitMenu && !qualityMenu) {
            delay(3_000)
            showControls = false
        }
    }

    LaunchedEffect(skipFlash) {
        if (skipFlash != null) {
            delay(700)
            skipFlash = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    keepScreenOn = true
                    resizeMode = pictureFit.resizeMode
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = {
                it.player = player
                it.resizeMode = pictureFit.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(player, allowSeek) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (!allowSeek) return@detectTapGestures
                            val forward = offset.x >= size.width / 2
                            val delta = if (forward) SKIP_MS else -SKIP_MS
                            val target = seekBy(player.currentPosition, player.duration, delta)
                            player.seekTo(target)
                            position = target
                            skipFlash = SkipFlash(forward = forward, label = if (forward) "+5" else "−5")
                        },
                        onTap = {
                            if (player.isPlaying) player.pause() else player.play()
                            playing = player.isPlaying
                            showControls = true
                        }
                    )
                }
        )

        skipFlash?.let { flash ->
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (flash.forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (flash.forward) Icons.Default.Forward5 else Icons.Default.Replay5,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(flash.label, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        if (duration > 0 && !showControls) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((position.toFloat() / duration.toFloat()).coerceIn(0f, 1f))
                        .background(NetflixRed)
                )
            }
        }

        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.75f), Color.Transparent)))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 8.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerBackButton(onBack)
                    topStart()
                    Text(
                        title,
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 220.dp).padding(end = 8.dp)
                    )
                    if (qualities.size > 1 && onQuality != null) {
                        QualityButton(
                            current = currentQuality,
                            qualities = qualities,
                            onSelect = onQuality,
                            onMenu = { qualityMenu = it },
                            modifier = Modifier
                        )
                    }
                    FitButton(pictureFit, fitMenu, { fitMenu = it }, onPictureFit)
                    HoldButton(holdMode) {
                        val next = HoldMode.entries[(holdMode.ordinal + 1) % HoldMode.entries.size]
                        onHoldMode(next)
                    }
                    topEnd()
                }

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            if (player.isPlaying) player.pause() else player.play()
                            playing = player.isPlaying
                            showControls = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                if (allowSeek && duration > 0) {
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Slider(
                            value = position.coerceIn(0, duration).toFloat(),
                            onValueChange = {
                                scrubbing = true
                                position = it.toLong()
                            },
                            onValueChangeFinished = {
                                player.seekTo(position)
                                scrubbing = false
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = NetflixRed,
                                activeTrackColor = NetflixRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatClock(position), color = White, fontSize = 12.sp)
                            Text(formatClock(duration), color = White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class SkipFlash(val forward: Boolean, val label: String)

@Composable
private fun FitButton(
    current: PictureFit,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    onSelect: (PictureFit) -> Unit
) {
    Box {
        PlayerChip(onClick = { onOpen(true) }) {
            Icon(
                when (current) {
                    PictureFit.Full -> Icons.Default.Fullscreen
                    PictureFit.Stretch -> Icons.Default.AspectRatio
                    PictureFit.Fit -> Icons.Default.AspectRatio
                },
                contentDescription = "Picture fit",
                tint = White,
                modifier = Modifier.size(16.dp)
            )
            Text(current.label, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { onOpen(false) }) {
            PictureFit.entries.forEach { fit ->
                DropdownMenuItem(onClick = { onOpen(false); onSelect(fit) }) {
                    Text(
                        fit.label,
                        fontWeight = if (fit == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (fit == current) NetflixRed else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldButton(mode: HoldMode, onClick: () -> Unit) {
    PlayerChip(onClick = onClick) {
        Icon(
            when (mode) {
                HoldMode.Upright -> Icons.Default.StayCurrentPortrait
                HoldMode.Landscape -> Icons.Default.StayCurrentLandscape
                HoldMode.Auto -> Icons.Default.ScreenRotation
            },
            contentDescription = "Hold ${mode.label}",
            tint = White,
            modifier = Modifier.size(16.dp)
        )
        Text(mode.label, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerChip(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}
