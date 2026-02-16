package com.streaming.sunne.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.streaming.sunne.data.model.MovieResponse
import com.streaming.sunne.repository.WatchRepository
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.MovieViewModel
import com.streaming.sunne.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    movie: MovieResponse,
    viewModel: MovieViewModel,
    quality: String = "1080p",
    onBackPress: () -> Unit
) {
    val context     = LocalContext.current
    val streamState by viewModel.streamUrl.collectAsState()
    val scope       = rememberCoroutineScope()
    val watchRepo   = remember { WatchRepository() }

    LaunchedEffect(movie.id) {
        viewModel.loadStreamUrl(movie.id, quality)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = streamState) {
            is UiState.Loading, UiState.Idle -> PlayerLoader(title = movie.title)

            is UiState.Success -> {
                ActualPlayer(
                    streamUrl  = state.data,
                    movie      = movie,
                    onReady    = {
                        // Increment view count once when playback starts
                        viewModel.trackView(movie.id)
                    },
                    onProgress = { positionMs, durationMs ->
                        // Called every 30 s — report to /api/v1/watch/progress
                        if (durationMs > 0) {
                            val positionSec = (positionMs / 1000).toInt()
                            val pct = (positionMs.toFloat() / durationMs * 100f).coerceIn(0f, 100f)
                            val completed = pct >= 90f
                            scope.launch {
                                watchRepo.updateProgress(movie.id, positionSec, pct, completed)
                            }
                        }
                    },
                    onEnded = { onBackPress() }
                )
            }

            is UiState.Error -> PlayerError(
                message = state.message,
                onRetry = { viewModel.loadStreamUrl(movie.id, quality) },
                onBack  = onBackPress
            )
        }

        // Back button — always on top
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onBackPress() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(22.dp))
        }
    }
}

// ─── ExoPlayer composable ──────────────────────────────────────────────────────

@Composable
private fun ActualPlayer(
    streamUrl: String,
    movie: MovieResponse,
    onReady: () -> Unit,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY    -> { isBuffering = false; onReady() }
                        Player.STATE_BUFFERING -> isBuffering = true
                        Player.STATE_ENDED    -> onEnded()
                        else -> Unit
                    }
                }
            })
        }
    }

    // Report progress every 30 seconds
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            delay(30_000)
            if (exoPlayer.isPlaying) {
                onProgress(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            // Save final position when leaving
            onProgress(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
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

        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(movie.title, fontSize = 16.sp, color = LightGray)
                }
            }
        }
    }
}

// ─── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun PlayerLoader(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Loading stream…", fontSize = 16.sp, color = LightGray)
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 14.sp, color = LightGray.copy(alpha = 0.6f))
        }
    }
}

// ─── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Playback Error", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NetflixRed)
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 15.sp, color = LightGray)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)).background(NetflixRed)
                        .clickable { onRetry() }.padding(horizontal = 28.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = White, modifier = Modifier.size(20.dp))
                    Text("Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)).background(White.copy(alpha = 0.15f))
                        .clickable { onBack() }.padding(horizontal = 28.dp, vertical = 13.dp)
                ) {
                    Text("Go Back", fontSize = 16.sp, color = White)
                }
            }
        }
    }
}


//-------------second player screen-------------------
//package com.streaming.sunne.ui.screens
//
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.CircularProgressIndicator
//import androidx.compose.material.Icon
//import androidx.compose.material.Text
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import com.google.android.exoplayer2.ExoPlayer
//import com.google.android.exoplayer2.MediaItem
//import com.google.android.exoplayer2.Player
//import com.google.android.exoplayer2.ui.StyledPlayerView
//import com.streaming.sunne.data.model.MovieResponse
//import com.streaming.sunne.ui.theme.*
//import com.streaming.sunne.viewmodel.MovieViewModel
//import com.streaming.sunne.viewmodel.UiState
//
///**
// * Player Screen
// *
// * Flow:
// * 1. ViewModel calls [MovieViewModel.loadStreamUrl] which hits GET /api/v1/movies/{id}/stream
// * 2. On success → ExoPlayer loads the URL
// * 3. On failure → error state with retry button
// *
// * View count is incremented via POST /api/v1/movies/{id}/view when playback starts.
// */
//@Composable
//fun PlayerScreen(
//    movie: MovieResponse,
//    viewModel: MovieViewModel,
//    quality: String = "1080p",
//    onBackPress: () -> Unit
//) {
//    val context = LocalContext.current
//    val streamState by viewModel.streamUrl.collectAsState()
//
//    // Request the stream URL as soon as this screen is shown
//    LaunchedEffect(movie.id) {
//        viewModel.loadStreamUrl(movie.id, quality)
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//    ) {
//        when (val state = streamState) {
//            is UiState.Loading, UiState.Idle -> {
//                // Spinner while fetching stream URL
//                PlayerLoader(title = movie.title)
//            }
//
//            is UiState.Success -> {
//                val streamUrl = state.data
//                ActualPlayer(
//                    streamUrl = streamUrl,
//                    movie = movie,
//                    onReady = { viewModel.trackView(movie.id) },
//                    onEnded = { onBackPress() }
//                )
//            }
//
//            is UiState.Error -> {
//                PlayerError(
//                    message = state.message,
//                    onRetry = { viewModel.loadStreamUrl(movie.id, quality) },
//                    onBack = onBackPress
//                )
//            }
//        }
//
//        // Back button (always visible)
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(20.dp)
//                .size(42.dp)
//                .clip(CircleShape)
//                .background(Color.Black.copy(alpha = 0.55f))
//                .clickable { onBackPress() },
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                Icons.Default.ArrowBack,
//                contentDescription = "Back",
//                tint = White,
//                modifier = Modifier.size(22.dp)
//            )
//        }
//    }
//}
//
//// ─── ExoPlayer composable ──────────────────────────────────────────────────────
//
//@Composable
//private fun ActualPlayer(
//    streamUrl: String,
//    movie: MovieResponse,
//    onReady: () -> Unit,
//    onEnded: () -> Unit
//) {
//    val context = LocalContext.current
//    var isBuffering by remember { mutableStateOf(true) }
//    var hasError by remember { mutableStateOf(false) }
//
//    val exoPlayer = remember(streamUrl) {
//        ExoPlayer.Builder(context).build().apply {
//            setMediaItem(MediaItem.fromUri(streamUrl))
//            prepare()
//            playWhenReady = true
//
//            addListener(object : Player.Listener {
//                override fun onPlaybackStateChanged(state: Int) {
//                    when (state) {
//                        Player.STATE_READY    -> { isBuffering = false; onReady() }
//                        Player.STATE_BUFFERING -> isBuffering = true
//                        Player.STATE_ENDED    -> onEnded()
//                        else -> Unit
//                    }
//                }
//                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
//                    hasError = true
//                    isBuffering = false
//                }
//            })
//        }
//    }
//
//    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
//
//    Box(Modifier.fillMaxSize()) {
//        AndroidView(
//            factory = { ctx ->
//                StyledPlayerView(ctx).apply {
//                    player = exoPlayer
//                    useController = true
//                    layoutParams = FrameLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                }
//            },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        // Buffering overlay
//        if (isBuffering && !hasError) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.45f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(52.dp))
//                    Spacer(Modifier.height(12.dp))
//                    Text(movie.title, fontSize = 16.sp, color = LightGray)
//                }
//            }
//        }
//    }
//}
//
//// ─── Loading state ─────────────────────────────────────────────────────────────
//
//@Composable
//private fun PlayerLoader(title: String) {
//    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(56.dp))
//            Spacer(Modifier.height(16.dp))
//            Text("Loading stream…", fontSize = 16.sp, color = LightGray)
//            Spacer(Modifier.height(6.dp))
//            Text(title, fontSize = 14.sp, color = LightGray.copy(alpha = 0.6f))
//        }
//    }
//}
//
//// ─── Error state ───────────────────────────────────────────────────────────────
//
//@Composable
//private fun PlayerError(
//    message: String,
//    onRetry: () -> Unit,
//    onBack: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text("Playback Error", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NetflixRed)
//            Spacer(Modifier.height(12.dp))
//            Text(message, fontSize = 15.sp, color = LightGray)
//            Spacer(Modifier.height(32.dp))
//            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//                // Retry
//                Row(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(NetflixRed)
//                        .clickable { onRetry() }
//                        .padding(horizontal = 28.dp, vertical = 13.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Icon(Icons.Default.Refresh, null, tint = White, modifier = Modifier.size(20.dp))
//                    Text("Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
//                }
//                // Back
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(White.copy(alpha = 0.15f))
//                        .clickable { onBack() }
//                        .padding(horizontal = 28.dp, vertical = 13.dp)
//                ) {
//                    Text("Go Back", fontSize = 16.sp, color = White)
//                }
//            }
//        }
//    }
//}
//------------------- first player screen ----------------------------------
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.CircularProgressIndicator
//import androidx.compose.material.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import com.google.android.exoplayer2.ExoPlayer
//import com.google.android.exoplayer2.MediaItem
//import com.google.android.exoplayer2.Player
//import com.google.android.exoplayer2.ui.StyledPlayerView
//import com.streaming.sunne.data.model.Stream
//import com.streaming.sunne.ui.theme.DarkBackground
//import com.streaming.sunne.ui.theme.NetflixRed
//import com.streaming.sunne.ui.theme.White
//
//
///**
// * Player Screen - Full-screen video playback using ExoPlayer
// */
//@Composable
//fun PlayerScreen(
//    stream: Stream,
//    onBackPress: () -> Unit
//) {
//    val context = LocalContext.current
//    var isLoading by remember { mutableStateOf(true) }
//    var hasError by remember { mutableStateOf(false) }
//
//    // Create ExoPlayer instance
//    val exoPlayer = remember {
//        ExoPlayer.Builder(context).build().apply {
//            // Set media item
//            val mediaItem = MediaItem.fromUri(stream.url)
//            setMediaItem(mediaItem)
//
//            // Prepare and play
//            prepare()
//            playWhenReady = true
//
//            // Add listener for loading state
//            addListener(object : Player.Listener {
//                override fun onPlaybackStateChanged(playbackState: Int) {
//                    when (playbackState) {
//                        Player.STATE_READY -> {
//                            isLoading = false
//                            hasError = false
//                        }
//                        Player.STATE_BUFFERING -> {
//                            isLoading = true
//                        }
//                        Player.STATE_ENDED -> {
//                            onBackPress()
//                        }
//                        Player.STATE_IDLE -> {
//                            // Do nothing
//                        }
//                    }
//                }
//
//                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
//                    hasError = true
//                    isLoading = false
//                }
//            })
//        }
//    }
//
//    // Release player when composable leaves composition
//    DisposableEffect(Unit) {
//        onDispose {
//            exoPlayer.release()
//        }
//    }
//
//    // Handle back press
//    BackHandler {
//        exoPlayer.release()
//        onBackPress()
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground)
//    ) {
//        // ExoPlayer view
//        AndroidView(
//            factory = { ctx ->
//                StyledPlayerView(ctx).apply {
//                    player = exoPlayer
//                    useController = true
//                    layoutParams = FrameLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                }
//            },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        // Loading indicator
//        if (isLoading && !hasError) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    CircularProgressIndicator(
//                        color = NetflixRed,
//                        modifier = Modifier.size(60.dp)
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Text(
//                        text = "Loading video...",
//                        fontSize = 18.sp,
//                        color = White.copy(alpha = 0.7f)
//                    )
//                }
//            }
//        }
//
//        // Error message
//        if (hasError) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "Failed to load video",
//                        fontSize = 24.sp,
//                        color = NetflixRed
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = "Please check your connection and try again",
//                        fontSize = 16.sp,
//                        color = White.copy(alpha = 0.7f)
//                    )
//                }
//            }
//        }
//    }
//}
//
///**
// * Simple back handler composable
// */
//@Composable
//fun BackHandler(onBack: () -> Unit) {
//    androidx.activity.compose.BackHandler(onBack = onBack)
//}