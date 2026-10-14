package com.streaming.sunne.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.streaming.sunne.data.model.EpisodeResponse
import com.streaming.sunne.viewmodel.Playable
import com.streaming.sunne.viewmodel.UpNext

/**
 * Episode player. Each episode gets its own player and ViewModel (keyed by episode id),
 * so autoplaying the next episode swaps the media cleanly.
 */
@Composable
fun EpisodePlayerScreen(
    episode: EpisodeResponse,
    seriesTitle: String?,
    onBackPress: () -> Unit,
    onUpNext: (UpNext) -> Unit
) {
    val playable = remember(episode.id) { Playable.Episode(episode, seriesTitle) }
    SunnePlayer(playable = playable, onBack = onBackPress, onUpNext = onUpNext)
}
