package com.streaming.sunne.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.streaming.sunne.data.model.MovieResponse
import com.streaming.sunne.viewmodel.Playable
import com.streaming.sunne.viewmodel.UpNext

/**
 * Movie player. Stream URL, resume position, quality and progress are handled by
 * [SunnePlayer]; when the movie ends the user gets an Up Next suggestion.
 */
@Composable
fun PlayerScreen(
    movie: MovieResponse,
    onBackPress: () -> Unit,
    onUpNext: (UpNext) -> Unit
) {
    val playable = remember(movie.id) { Playable.Movie(movie) }
    SunnePlayer(playable = playable, onBack = onBackPress, onUpNext = onUpNext)
}
