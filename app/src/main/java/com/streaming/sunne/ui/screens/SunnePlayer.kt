package com.streaming.sunne.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val PROGRESS_SAVE_INTERVAL_S = 15
private const val UP_NEXT_PREFETCH_MS = 30_000L
private const val UP_NEXT_COUNTDOWN_S = 10

/**
 * Full-screen player shared by movies and episodes. Drives a [PlaybackViewModel]:
 * loading, errors with retry, resume, quality switching, progress saving and Up Next.
 */
@Composable
fun SunnePlayer(
    playable: Playable,
    onBack: () -> Unit,
    onUpNext: (UpNext) -> Unit
) {
    val context   = LocalContext.current
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<PlaybackViewModel>(key = playable.key)
    val state     by viewModel.state.collectAsState()
    val upNext    by viewModel.upNext.collectAsState()
    var ended     by remember(playable.key) { mutableStateOf(false) }
    var holdMode  by remember { mutableStateOf(HoldMode.Auto) }
    var pictureFit by remember { mutableStateOf(PictureFit.Fit) }
    PlaybackOrientation(holdMode)

    LaunchedEffect(playable.key) {
        viewModel.start(playable, PlaybackPrefs.defaultQuality(context))
    }

    // Movie/episode finished and nothing to suggest — leave the player.
    LaunchedEffect(ended, upNext) {
        if (ended && upNext == UpNextState.None) onBack()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            is PlaybackState.Loading -> PlayerStatus(title = playable.title, message = "Loading…")

            is PlaybackState.Error -> PlayerErrorPanel(
                message = s.message,
                onRetry = { viewModel.retry(0) },
                onBack  = onBack
            )

            is PlaybackState.Ready -> VideoSurface(
                source          = s.source,
                title           = playable.title,
                qualities       = playable.qualities,
                holdMode        = holdMode,
                onHoldMode      = { holdMode = it },
                pictureFit      = pictureFit,
                onPictureFit    = { pictureFit = it },
                onBack          = onBack,
                onStarted       = viewModel::onPlaybackStarted,
                onProgress      = viewModel::saveProgress,
                onNearEnd       = viewModel::loadUpNext,
                onEnded         = { viewModel.loadUpNext(); ended = true },
                onQualityChange = viewModel::changeQuality,
                onRetry         = viewModel::retry
            )
        }

        if (state !is PlaybackState.Ready) {
            PlayerBackButton(onBack, Modifier.align(Alignment.TopStart))
        }

        if (ended) {
            when (val next = upNext) {
                is UpNextState.Ready -> UpNextOverlay(
                    next     = next.next,
                    onPlay   = { onUpNext(next.next) },
                    onCancel = onBack
                )
                UpNextState.Loading, UpNextState.Idle ->
                    PlayerStatus(title = playable.title, message = "Finding what's next…")
                UpNextState.None -> Unit
            }
        }
    }
}

@Composable
private fun VideoSurface(
    source: PlaybackSource,
    title: String,
    qualities: List<String>,
    holdMode: HoldMode,
    onHoldMode: (HoldMode) -> Unit,
    pictureFit: PictureFit,
    onPictureFit: (PictureFit) -> Unit,
    onBack: () -> Unit,
    onStarted: () -> Unit,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onNearEnd: () -> Unit,
    onEnded: () -> Unit,
    onQualityChange: (quality: String, positionMs: Long) -> Unit,
    onRetry: (positionMs: Long) -> Unit
) {
    val context      = LocalContext.current
    var isBuffering  by remember(source) { mutableStateOf(true) }
    var errorMessage by remember(source) { mutableStateOf<String?>(null) }
    var showResume   by remember(source) { mutableStateOf(source.resumed) }

    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnEnded    by rememberUpdatedState(onEnded)
    val currentOnStarted  by rememberUpdatedState(onStarted)

    val player = remember(source) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(source.url))
            if (source.startPositionMs > 0) seekTo(source.startPositionMs)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY     -> { isBuffering = false; errorMessage = null; currentOnStarted() }
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_ENDED     -> { isBuffering = false; currentOnEnded() }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && player.playbackState == Player.STATE_READY) {
                    currentOnProgress(player.currentPosition, player.duration)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = playbackErrorMessage(error)
            }
        }
        player.addListener(listener)
        onDispose {
            currentOnProgress(player.currentPosition, player.duration)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        var ticks = 0
        var nearEndSignalled = false
        while (isActive) {
            delay(1_000)
            if (!player.isPlaying) continue
            ticks++
            if (ticks % PROGRESS_SAVE_INTERVAL_S == 0) {
                currentOnProgress(player.currentPosition, player.duration)
            }
            val duration = player.duration
            if (!nearEndSignalled && duration > 0 && duration - player.currentPosition < UP_NEXT_PREFETCH_MS) {
                nearEndSignalled = true
                onNearEnd()
            }
        }
    }

    LaunchedEffect(showResume) {
        if (showResume) { delay(8_000); showResume = false }
    }

    Box(Modifier.fillMaxSize()) {
        NetflixPlayer(
            player         = player,
            title          = title,
            onBack         = onBack,
            holdMode       = holdMode,
            onHoldMode     = onHoldMode,
            pictureFit     = pictureFit,
            onPictureFit   = onPictureFit,
            qualities      = qualities,
            currentQuality = source.quality,
            onQuality      = { q -> if (q != source.quality) onQualityChange(q, player.currentPosition) }
        )

        if (isBuffering && errorMessage == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(52.dp))
            }
        }

        AnimatedVisibility(
            visible  = showResume,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 96.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { player.seekTo(0); showResume = false }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Resumed at ${formatClock(source.startPositionMs)}", fontSize = 13.sp, color = LightGray)
                Icon(Icons.Default.Replay, null, tint = White, modifier = Modifier.size(16.dp))
                Text("Start over", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }

        errorMessage?.let { message ->
            PlayerErrorPanel(
                message = message,
                onRetry = { onRetry(player.currentPosition) },
                onBack  = null,
                title   = title
            )
        }
    }
}

@Composable
internal fun QualityButton(
    current: String?,
    qualities: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (Boolean) -> Unit = {}
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { open = true; onMenu(true) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Settings, "Quality", tint = White, modifier = Modifier.size(16.dp))
            Text(current ?: "Auto", fontSize = 13.sp, color = White, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false; onMenu(false) }) {
            qualities.forEach { q ->
                DropdownMenuItem(onClick = { open = false; onMenu(false); onSelect(q) }) {
                    Text(
                        q,
                        fontWeight = if (q == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (q == current) NetflixRed else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextOverlay(next: UpNext, onPlay: () -> Unit, onCancel: () -> Unit) {
    var secondsLeft by remember(next) { mutableStateOf(UP_NEXT_COUNTDOWN_S) }
    val currentOnPlay by rememberUpdatedState(onPlay)

    LaunchedEffect(next) {
        while (secondsLeft > 0) { delay(1_000); secondsLeft-- }
        currentOnPlay()
    }

    val (heading, title, imageUrl, reason) = when (next) {
        is UpNext.Movie   -> UpNextCopy("Up next", next.movie.title, next.movie.bannerUrl, next.reason)
        is UpNext.Episode -> UpNextCopy("Next episode", next.label, next.episode.thumbnailUrl, null)
        is UpNext.Series  -> UpNextCopy("You might like", next.title, next.posterUrl, next.reason)
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(32.dp).widthIn(max = 720.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(240.dp).height(135.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Column(Modifier.weight(1f)) {
                Text(heading.uppercase(), fontSize = 12.sp, color = NetflixRed,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Text(title, fontSize = 22.sp, color = White, fontWeight = FontWeight.Black,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                reason?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 13.sp, color = LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerActionButton(
                        label   = if (next is UpNext.Series) "Open in $secondsLeft" else "Play in $secondsLeft",
                        primary = true,
                        onClick = onPlay
                    )
                    PlayerActionButton(label = "Back", primary = false, onClick = onCancel)
                }
            }
        }
    }
}

private data class UpNextCopy(val heading: String, val title: String, val imageUrl: String?, val reason: String?)

@Composable
private fun PlayerStatus(title: String, message: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, fontSize = 16.sp, color = LightGray)
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 14.sp, color = LightGray.copy(alpha = 0.6f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun PlayerErrorPanel(message: String, onRetry: () -> Unit, onBack: (() -> Unit)?, title: String? = null) {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBackground.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("Playback problem", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NetflixRed)
            title?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, fontSize = 14.sp, color = LightGray.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 15.sp, color = LightGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayerActionButton(label = "Retry", primary = true, onClick = onRetry, icon = true)
                onBack?.let { PlayerActionButton(label = "Go back", primary = false, onClick = it) }
            }
        }
    }
}

@Composable
private fun PlayerActionButton(label: String, primary: Boolean, onClick: () -> Unit, icon: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) White else White.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (primary) {
            Icon(if (icon) Icons.Default.Refresh else Icons.Default.PlayArrow, null,
                tint = Color.Black, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = if (primary) Color.Black else White)
    }
}

@Composable
fun PlayerBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(20.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(22.dp))
    }
}

internal fun playbackErrorMessage(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Lost connection to the server. Check your internet and retry."
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "This video isn't available on the server right now."
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
        "This device can't play this video format."
    else -> "Playback failed (${error.errorCodeName})."
}

fun formatClock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
