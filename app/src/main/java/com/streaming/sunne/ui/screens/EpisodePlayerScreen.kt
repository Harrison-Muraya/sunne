package com.streaming.sunne.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.streaming.sunne.data.model.EpisodeResponse
import com.streaming.sunne.repository.EpisodeWatchRepository
import com.streaming.sunne.repository.PlayNextRepository
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.viewmodel.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun EpisodePlayerScreen(
    episode: EpisodeResponse,
    viewModel: MovieViewModel,
    onBackPress: () -> Unit,
    onAutoPlayNext: (EpisodeResponse) -> Unit
) {
    val context = LocalContext.current
    val watchRepo = remember { EpisodeWatchRepository() }
    val playNextRepo = remember { PlayNextRepository() }

    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // ✅ State flag set from the listener, reacted to in a LaunchedEffect below
    var episodeEnded by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val url = episode.videoUrl
                ?: episode.videoFiles.firstOrNull()?.filePath
                ?: return@apply

            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        // ✅ Just flip the flag — no @Composable call here
                        episodeEnded = true
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    // ✅ React to episode ending here, in a proper composable context
    LaunchedEffect(episodeEnded) {
        if (!episodeEnded) return@LaunchedEffect

        playNextRepo.getNextAfterEpisode(episode.id)
            .onSuccess { result ->
                val type = result["type"] as? String
                when (type) {
                    "episode" -> {
                        val nextEp = parseEpisodeFromMap(result["episode"] as? Map<String, Any>)
                        if (nextEp != null) onAutoPlayNext(nextEp) else onBackPress()
                    }
                    "series_recommendation" -> onBackPress()
                    else -> onBackPress()
                }
            }
            .onFailure { onBackPress() }
    }

    // Report progress every 30 seconds
    LaunchedEffect(player, episode.id) {
        while (isActive) {
            if (isPlaying) {
                currentPosition = player.currentPosition / 1000
                duration = (player.duration / 1000).coerceAtLeast(1)
                val percentage = ((currentPosition.toFloat() / duration) * 100).coerceIn(0f, 100f)

                watchRepo.updateProgress(
                    episodeId  = episode.id,
                    position   = currentPosition.toInt(),
                    percentage = percentage,
                    completed  = percentage >= 90f
                )
            }
            delay(30_000)
        }
    }

    // Report final position on exit
    DisposableEffect(player, episode.id) {
        onDispose {
            val pos = (player.currentPosition / 1000).toInt()
            val dur = (player.duration / 1000).coerceAtLeast(1)
            val pct = ((pos.toFloat() / dur) * 100).coerceIn(0f, 100f)

            kotlinx.coroutines.GlobalScope.launch {
                watchRepo.updateProgress(episode.id, pos, pct, pct >= 90f)
            }
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
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
    }
}

/** Quick parser for episode from Play Next API response */
private fun parseEpisodeFromMap(data: Map<String, Any>?): EpisodeResponse? {
    if (data == null) return null
    return try {
        EpisodeResponse(
            id            = (data["id"] as? Number)?.toInt() ?: return null,
            seasonId      = (data["season_id"] as? Number)?.toInt() ?: return null,
            episodeNumber = (data["episode_number"] as? Number)?.toInt() ?: return null,
            title         = data["title"] as? String ?: "",
            description   = data["description"] as? String,
            duration      = (data["duration"] as? Number)?.toInt(),
            thumbnailUrl  = data["thumbnail_url"] as? String,
            videoUrl      = data["video_url"] as? String,
            status        = data["status"] as? String ?: "ready",
            viewCount     = (data["view_count"] as? Number)?.toInt() ?: 0,
            createdAt     = data["created_at"] as? String ?: "",
            updatedAt     = data["updated_at"] as? String ?: "",
            videoFiles    = emptyList()
        )
    } catch (e: Exception) {
        null
    }
}

