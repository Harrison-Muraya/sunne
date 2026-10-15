package com.streaming.sunne.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.*
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.streaming.sunne.viewmodel.MyListToggleViewModel
import com.streaming.sunne.viewmodel.SeriesViewModel
import com.streaming.sunne.viewmodel.UiState

@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    initial: SeriesResponse?,
    onEpisodeClick: (SeriesResponse, EpisodeResponse) -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel: SeriesViewModel = viewModel(key = "series-$seriesId")
    val state    by viewModel.seriesDetail.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val listVm: MyListToggleViewModel = viewModel(key = "my-list/series/$seriesId")
    val inList   by listVm.inList.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId, initial)
        listVm.bind("series", seriesId)
    }

    // Refresh episode progress when coming back from the player
    OnResumeEffect(seriesId, skipFirst = true) { viewModel.refreshProgress(seriesId) }

    val series = (state as? UiState.Success)?.data ?: initial?.takeIf { it.id == seriesId }
    if (series == null) {
        when (val s = state) {
            is UiState.Error -> ErrorScreen(s.message) { viewModel.loadSeries(seriesId) }
            else -> Box(Modifier.fillMaxSize().background(DarkBackground), Alignment.Center) {
                CircularProgressIndicator(color = NetflixRed)
            }
        }
        return
    }
    SeriesDetailContent(
        series         = series,
        progress       = progress,
        isLoading      = state is UiState.Loading,
        error          = (state as? UiState.Error)?.message,
        inList         = inList,
        onToggleList   = listVm::toggle,
        onRetry        = { viewModel.loadSeries(seriesId) },
        onEpisodeClick = { onEpisodeClick(series, it) },
        onBackClick    = onBackClick
    )
}

/** Episode to start from: resume the last unfinished one, else the next unwatched, else S1E1. */
private fun resumeTarget(
    series: SeriesResponse,
    progress: Map<Int, EpisodeProgressItem>
): Pair<EpisodeResponse, String>? {
    val ordered = series.seasons.sortedBy { it.seasonNumber }.flatMap { season ->
        season.episodes.filter { it.status == "ready" }.sortedBy { it.episodeNumber }.map { season to it }
    }
    if (ordered.isEmpty()) return null
    fun label(verb: String, i: Int) = "$verb S${ordered[i].first.seasonNumber}:E${ordered[i].second.episodeNumber}"

    val lastWatched = ordered.indexOfLast { progress.containsKey(it.second.id) }
    if (lastWatched == -1) return ordered[0].second to label("Play", 0)
    if (progress[ordered[lastWatched].second.id]?.completed == false) {
        return ordered[lastWatched].second to label("Resume", lastWatched)
    }
    val next = lastWatched + 1
    return if (next < ordered.size) ordered[next].second to label("Play", next)
    else ordered[0].second to "Watch again"
}

@Composable
private fun SeriesDetailContent(
    series: SeriesResponse,
    progress: Map<Int, EpisodeProgressItem>,
    isLoading: Boolean,
    error: String?,
    inList: Boolean,
    onToggleList: () -> Unit,
    onRetry: () -> Unit,
    onEpisodeClick: (EpisodeResponse) -> Unit,
    onBackClick: () -> Unit
) {
    val ad = rememberAdaptive()
    val playFocus = remember { FocusRequester() }
    if (ad.isTV) {
        LaunchedEffect(series.id) {
            withFrameNanos { }
            runCatching { playFocus.requestFocus() }
        }
    }
    var expandedSeasons by remember { mutableStateOf(setOf(1)) } // First season expanded by default
    val target = remember(series, progress) { resumeTarget(series, progress) }
    LaunchedEffect(target) {
        target?.first?.let { ep ->
            series.seasons.firstOrNull { s -> s.episodes.any { it.id == ep.id } }
                ?.let { expandedSeasons = expandedSeasons + it.seasonNumber }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // Hero banner
            item {
                Box(modifier = Modifier.fillMaxWidth().height(if (ad.isTV) 480.dp else 320.dp)) {
                    AsyncImage(
                        model = series.bannerUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, DarkBackground.copy(0.3f), DarkBackground)
                        )
                    ))

                    // Back button
                    SeriesBackBtn(onBackClick, Modifier.align(Alignment.TopStart).padding(ad.pad))

                    // Series info overlay
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart)
                            .padding(start = ad.pad, bottom = ad.pad, end = ad.pad)
                            .fillMaxWidth(if (ad.isTV) 0.6f else 0.9f)
                    ) {
                        if (series.isTrending) {
                            Box(
                                Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE55300))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("TRENDING", fontSize = sp(10), fontWeight = FontWeight.Black, color = White)
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            series.title,
                            fontSize = if (ad.isTV) sp(38) else sp(28),
                            fontWeight = FontWeight.Black,
                            color = White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            series.releaseYear?.let {
                                Text("$it", fontSize = sp(13), color = LightGray)
                                MiniDot()
                            }
                            Text(series.formattedSeasonCount(), fontSize = sp(13), color = LightGray)
                            MiniDot()
                            Text("${series.totalEpisodeCount()} Episodes", fontSize = sp(13), color = LightGray)
                            series.rating?.let {
                                MiniDot()
                                RatingChip(it)
                            }
                        }

                        if (series.genreNames().isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(series.genreNames(), fontSize = sp(12), color = LightGray.copy(0.75f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            target?.let { (episode, label) ->
                                Row(
                                    modifier = Modifier.focusRequester(playFocus)
                                        .clip(RoundedCornerShape(6.dp)).background(White)
                                        .clickable { onEpisodeClick(episode) }
                                        .padding(horizontal = 24.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                    Text(label, fontSize = sp(15), fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            IconCircleBtn(
                                icon     = if (inList) Icons.Default.Check else Icons.Default.Add,
                                size     = if (ad.isPhone) 38.dp else 44.dp,
                                selected = inList,
                                onClick  = onToggleList
                            )
                        }
                    }
                }
            }

            if (isLoading && series.seasons.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = NetflixRed)
                    }
                }
            }

            error?.let { message ->
                item {
                    Column(Modifier.fillMaxWidth().padding(ad.pad), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message, fontSize = sp(14), color = LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text("Retry", fontSize = sp(15), fontWeight = FontWeight.Bold, color = NetflixRed,
                            modifier = Modifier.clickable { onRetry() }.padding(8.dp))
                    }
                }
            }

            // Description
            item {
                Column(modifier = Modifier.padding(horizontal = ad.pad, vertical = 20.dp)) {
                    series.description?.let {
                        Text(
                            it,
                            fontSize = ad.body,
                            color = White.copy(0.85f),
                            lineHeight = ad.body * 1.5f,
                            maxLines = if (ad.isTV) 4 else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // Meta info
                    if (!series.director.isNullOrBlank() || !series.cast.isNullOrBlank()) {
                        Divider(color = White.copy(0.08f))
                        Spacer(Modifier.height(12.dp))
                        series.director?.let {
                            MetaRow("Director", it)
                            Spacer(Modifier.height(6.dp))
                        }
                        series.cast?.let { MetaRow("Cast", it) }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            // Seasons & Episodes
            items(series.seasons.sortedBy { it.seasonNumber }) { season ->
                SeasonSection(
                    season = season,
                    progress = progress,
                    isExpanded = expandedSeasons.contains(season.seasonNumber),
                    adaptive = ad,
                    onToggle = {
                        expandedSeasons = if (expandedSeasons.contains(season.seasonNumber)) {
                            expandedSeasons - season.seasonNumber
                        } else {
                            expandedSeasons + season.seasonNumber
                        }
                    },
                    onEpisodeClick = onEpisodeClick
                )
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SeasonSection(
    season: SeasonResponse,
    progress: Map<Int, EpisodeProgressItem>,
    isExpanded: Boolean,
    adaptive: Adaptive,
    onToggle: () -> Unit,
    onEpisodeClick: (EpisodeResponse) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Season header
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = adaptive.pad, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = NetflixRed,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        season.displayTitle(),
                        fontSize = if (adaptive.isTV) sp(20) else sp(18),
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Text(
                        "${season.episodes.size} Episodes",
                        fontSize = sp(12),
                        color = LightGray
                    )
                }
            }
        }

        // Episodes
        AnimatedVisibility(visible = isExpanded) {
            Column {
                season.episodes.sortedBy { it.episodeNumber }.forEach { episode ->
                    EpisodeRow(episode, progress[episode.id], adaptive, onEpisodeClick)
                }
            }
        }

        Divider(color = White.copy(0.05f), modifier = Modifier.padding(horizontal = adaptive.pad))
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeResponse,
    progress: EpisodeProgressItem?,
    adaptive: Adaptive,
    onClick: (EpisodeResponse) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, spring())

    Row(
        modifier = Modifier.fillMaxWidth()
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick(episode) }
            .background(if (focused) White.copy(0.05f) else Color.Transparent)
            .padding(horizontal = adaptive.pad, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode number badge
        Box(
            modifier = Modifier.size(if (adaptive.isTV) 56.dp else 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            if (episode.thumbnailUrl != null) {
                AsyncImage(episode.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(DarkBackground.copy(0.4f)), Alignment.Center) {
                    Text(
                        "${episode.episodeNumber}",
                        fontSize = if (adaptive.isTV) sp(20) else sp(16),
                        fontWeight = FontWeight.Black,
                        color = White
                    )
                }
            } else {
                Text(
                    "${episode.episodeNumber}",
                    fontSize = if (adaptive.isTV) sp(20) else sp(16),
                    fontWeight = FontWeight.Black,
                    color = White
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                episode.title,
                fontSize = if (adaptive.isTV) sp(16) else sp(14),
                fontWeight = FontWeight.SemiBold,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            episode.description?.let {
                Text(
                    it,
                    fontSize = sp(12),
                    color = LightGray.copy(0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = sp(16)
                )
            }
            progress?.let {
                Spacer(Modifier.height(6.dp))
                WatchProgressBar(if (it.completed) 1f else it.watchPercentage / 100f)
            }
        }

        // Duration
        episode.formattedDuration()?.let {
            Text(
                it,
                fontSize = sp(12),
                color = LightGray.copy(0.5f),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        // Play icon
        Icon(
            Icons.Default.PlayCircleOutline,
            null,
            tint = if (focused) NetflixRed else LightGray.copy(0.5f),
            modifier = Modifier.size(24.dp).align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = sp(12), color = LightGray.copy(0.45f), fontWeight = FontWeight.Bold)
        Text(value, fontSize = sp(12), color = LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RatingChip(label: String) {
    Box(
        Modifier.clip(RoundedCornerShape(3.dp))
            .border(1.dp, LightGray.copy(0.5f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = sp(11), color = LightGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniDot() = Box(Modifier.size(3.dp).clip(CircleShape).background(LightGray.copy(0.4f)))

@Composable
private fun SeriesBackBtn(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape)
            .background(Color.Black.copy(0.5f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(20.dp))
    }
}

private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Int) = sp(value.toFloat())
private operator fun androidx.compose.ui.unit.TextUnit.times(f: Float) = sp(this.value * f)
