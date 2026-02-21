package com.streaming.sunne.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.*
import com.streaming.sunne.ui.adaptive.rememberAdaptive
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel:    SearchViewModel,
    onMovieClick: (MovieResponse) -> Unit,
    onSeriesClick: (SeriesResponse) -> Unit,
    onBackClick:  () -> Unit
) {
    val query         by viewModel.query.collectAsState()
    val movies        by viewModel.movieResults.collectAsState()
    val series        by viewModel.seriesResults.collectAsState()
    val genres        by viewModel.genres.collectAsState()
    val isSearching   by viewModel.isSearching.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val ad             = rememberAdaptive()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {

        // ── Search bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ad.pad, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack, "Back",
                tint     = White,
                modifier = Modifier.size(24.dp).clickable { onBackClick() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A2A))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = LightGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isBlank()) {
                            Text("Search movies, series…", fontSize = sp(15), color = LightGray.copy(0.5f))
                        }
                        BasicTextField(
                            value         = query,
                            onValueChange = viewModel::setQuery,
                            singleLine    = true,
                            cursorBrush   = SolidColor(NetflixRed),
                            textStyle     = TextStyle(color = White, fontSize = sp(15)),
                            modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                    if (query.isNotBlank()) {
                        Icon(
                            Icons.Default.Close, "Clear",
                            tint     = LightGray,
                            modifier = Modifier.size(18.dp).clickable { viewModel.clear() }
                        )
                    }
                }
            }
        }

        // ── Genre filter chips ────────────────────────────────────────────────
        if (genres.isNotEmpty()) {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = ad.pad),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    GenreChip(
                        label    = "All",
                        selected = selectedGenre == null,
                        onClick  = { viewModel.selectGenre(null) }
                    )
                }
                items(genres) { genre ->
                    GenreChip(
                        label    = genre.name,
                        selected = selectedGenre?.id == genre.id,
                        onClick  = { viewModel.selectGenre(genre) }
                    )
                }
            }
        }

        // ── Results ────────────────────────────────────────────────────────────
        when {
            isSearching -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = NetflixRed)
                }
            }

            query.isBlank() && selectedGenre == null -> {
                // Empty state — show browse hint
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, tint = LightGray.copy(0.4f),
                            modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Search for movies & series", fontSize = sp(16), color = LightGray.copy(0.6f))
                        Spacer(Modifier.height(6.dp))
                        Text("or browse by genre above", fontSize = sp(13), color = LightGray.copy(0.4f))
                    }
                }
            }

            movies.isEmpty() && series.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = LightGray.copy(0.4f),
                            modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No results for \"$query\"", fontSize = sp(15), color = LightGray.copy(0.7f))
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(if (ad.isPhone) 2 else if (ad.isTablet) 3 else 5),
                    contentPadding        = PaddingValues(horizontal = ad.pad, vertical = 8.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxSize()
                ) {
                    if (movies.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Movies (${movies.size})")
                        }
                        items(movies) { movie ->
                            SearchMovieCard(movie = movie, onClick = { onMovieClick(movie) })
                        }
                    }
                    if (series.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Series (${series.size})")
                        }
                        items(series) { s ->
                            SearchSeriesCard(series = s, onClick = { onSeriesClick(s) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) NetflixRed else Color(0xFF2A2A2A))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = sp(13), color = if (selected) White else LightGray,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        fontSize   = sp(16),
        fontWeight = FontWeight.Bold,
        color      = White,
        modifier   = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun SearchMovieCard(movie: MovieResponse, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier           = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            contentScale       = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(0.75f))
                ))
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        ) {
            Text(movie.title, fontSize = sp(12), fontWeight = FontWeight.SemiBold,
                color = White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            movie.releaseYear?.let {
                Text("$it", fontSize = sp(10), color = LightGray)
            }
        }
    }
}

@Composable
private fun SearchSeriesCard(series: SeriesResponse, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = series.thumbnailUrl,
            contentDescription = series.title,
            modifier           = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            contentScale       = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(0.8f))
                ))
        )
        // TV badge
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp)
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A73E8))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text("TV", fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        ) {
            Text(series.title, fontSize = sp(12), fontWeight = FontWeight.SemiBold,
                color = White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sc = series.seasons.size
            if (sc > 0) Text("$sc Season${if (sc > 1) "s" else ""}", fontSize = sp(10), color = LightGray)
        }
    }
}

private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Int)   = sp(value.toFloat())

