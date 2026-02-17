package com.streaming.sunne.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.streaming.sunne.data.model.MovieResponse
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.HomeUiState
import com.streaming.sunne.viewmodel.MovieViewModel

// ══════════════════════════════════════════════════════════════════════════════
//  DETAILS SCREEN — landscape (TV/tablet) vs portrait (phone)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun DetailsScreen(
    movie: MovieResponse,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val ad = rememberAdaptive()
    if (ad.isPhone) DetailsPortrait(movie, onPlayClick, onBackClick, ad)
    else            DetailsLandscape(movie, onPlayClick, onBackClick, ad)
}

// ── TV / Tablet — side-by-side poster + info ──────────────────────────────────
@Composable
private fun DetailsLandscape(
    movie: MovieResponse, onPlayClick: () -> Unit, onBackClick: () -> Unit, ad: Adaptive
) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Full bleed backdrop
        Image(
            painter            = rememberAsyncImagePainter(movie.bannerUrl),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                listOf(DarkBackground.copy(0.97f), DarkBackground.copy(0.55f), Color.Transparent)
            )
        ))
        Box(Modifier.fillMaxSize().background(DarkBackground.copy(0.3f)))

        // Back button
        BackBtn(onBackClick, Modifier.align(Alignment.TopStart).padding(ad.pad))

        // Info column
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (ad.isTV) 560.dp else 480.dp)
                .align(Alignment.CenterStart)
                .padding(start = ad.pad, end = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            DetailsInfo(movie, onPlayClick, ad)
        }
    }
}

// ── Phone — stacked image + scrollable info ───────────────────────────────────
@Composable
private fun DetailsPortrait(
    movie: MovieResponse, onPlayClick: () -> Unit, onBackClick: () -> Unit, ad: Adaptive
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            AsyncImage(movie.bannerUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, DarkBackground))
            ))
            BackBtn(onBackClick, Modifier.align(Alignment.TopStart).padding(14.dp))
        }
        Column(Modifier.padding(ad.pad)) {
            DetailsInfo(movie, onPlayClick, ad)
        }
    }
}

@Composable
private fun DetailsInfo(movie: MovieResponse, onPlayClick: () -> Unit, ad: Adaptive) {
    // Genre chip
    if (movie.genreNames().isNotEmpty()) {
        Box(
            Modifier.clip(RoundedCornerShape(4.dp))
                .background(NetflixRed.copy(0.18f))
                .border(1.dp, NetflixRed.copy(0.45f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(movie.genreNames().uppercase(), fontSize = sp(11), fontWeight = FontWeight.Bold,
                color = NetflixRed, letterSpacing = sp(2))
        }
        Spacer(Modifier.height(14.dp))
    }

    // Title
    Text(movie.title, fontSize = ad.heroTitle, fontWeight = FontWeight.Black, color = White,
        lineHeight = ad.heroTitle * 1.1f, maxLines = 2, overflow = TextOverflow.Ellipsis)

    Spacer(Modifier.height(12.dp))

    // Meta row
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        movie.releaseYear?.let { Text("$it", fontSize = ad.caption, color = LightGray) }
        movie.formattedDuration()?.let {
            MiniDot(); Text(it, fontSize = ad.caption, color = LightGray)
        }
        movie.rating?.let {
            MiniDot()
            Box(Modifier.clip(RoundedCornerShape(3.dp))
                .border(1.dp, LightGray.copy(0.5f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text(it, fontSize = sp(11), color = LightGray, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Text(movie.language, fontSize = sp(12), color = LightGray.copy(0.45f))

    movie.description?.let {
        Spacer(Modifier.height(14.dp))
        Text(it, fontSize = ad.body, color = White.copy(0.82f),
            lineHeight = ad.body * 1.55f, maxLines = if (ad.isTV) 5 else 4, overflow = TextOverflow.Ellipsis)
    }

    if (!movie.director.isNullOrBlank() || !movie.cast.isNullOrBlank()) {
        Spacer(Modifier.height(16.dp))
        Divider(color = White.copy(0.08f))
        Spacer(Modifier.height(12.dp))
        movie.director?.let { LabelRow("Director", it) }
        movie.cast?.let { Spacer(Modifier.height(6.dp)); LabelRow("Cast", it) }
    }
    movie.country?.let { Spacer(Modifier.height(6.dp)); LabelRow("Country", it) }

    Spacer(Modifier.height(26.dp))

    // Action buttons
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        // Play
        Row(
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(White)
                .clickable { onPlayClick() }
                .padding(horizontal = if (ad.isPhone) 20.dp else 28.dp,
                    vertical = if (ad.isPhone) 10.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.Black,
                modifier = Modifier.size(if (ad.isPhone) 20.dp else 26.dp))
            Text("Play", fontSize = if (ad.isPhone) sp(14) else sp(17),
                fontWeight = FontWeight.Bold, color = Color.Black)
        }
        // Add
        IconCircleBtn(Icons.Default.Add, if (ad.isPhone) 38.dp else 48.dp)
        // Rate
        IconCircleBtn(Icons.Default.ThumbUp, if (ad.isPhone) 38.dp else 48.dp)
    }
}

@Composable
private fun LabelRow(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = sp(12), color = LightGray.copy(0.45f), fontWeight = FontWeight.Bold)
        Text(value, fontSize = sp(12), color = LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IconCircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(White.copy(0.12f)).border(1.5.dp, White.copy(0.3f), CircleShape).clickable {},
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = White, modifier = Modifier.size(size * 0.45f)) }
}

@Composable
private fun MiniDot() = Box(Modifier.size(3.dp).clip(CircleShape).background(LightGray.copy(0.4f)))

@Composable
fun BackBtn(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape)
            .background(Color.Black.copy(0.55f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(20.dp)) }
}

// ══════════════════════════════════════════════════════════════════════════════
//  LIVE SCREEN — side panel (TV/tablet) vs stacked (phone)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun LiveScreen(
    viewModel: MovieViewModel,
    onChannelPlay: (MovieResponse) -> Unit,
    onBackClick: () -> Unit
) {
    val homeState  = viewModel.homeState.collectAsState().value
    val ad         = rememberAdaptive()
    var preview by remember { mutableStateOf<MovieResponse?>(null) }

    val movies = when (val s = homeState) {
        is HomeUiState.Success -> s.categories.flatMap { it.movies }.distinctBy { it.id }
        else -> emptyList()
    }
    LaunchedEffect(movies) { if (preview == null) preview = movies.firstOrNull() }

    Box(Modifier.fillMaxSize().background(DarkBackground)) {
        if (!ad.isPhone) {
            // TV / Tablet — side-by-side
            Row(Modifier.fillMaxSize()) {
                LiveChannelPanel(movies, preview, ad, onBackClick,
                    onHover  = { preview = it },
                    onSelect = { onChannelPlay(it) }
                )
                LivePreviewPanel(preview, ad, onPlay = { preview?.let { onChannelPlay(it) } })
            }
        } else {
            // Phone — vertical
            Column(Modifier.fillMaxSize()) {
                LivePhoneHeader(preview, onBackClick, onPlay = { preview?.let { onChannelPlay(it) } })
                LazyColumn(
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(movies) { i, m ->
                        ChannelRow(m, i + 1, m.id == preview?.id, ad,
                            onHover = { preview = m }, onSelect = { onChannelPlay(m) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePhoneHeader(movie: MovieResponse?, onBackClick: () -> Unit, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        AsyncImage(movie?.bannerUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(DarkBackground.copy(0.6f)))
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, DarkBackground), startY = 80f)
        ))

        Row(
            Modifier.align(Alignment.TopStart).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackBtn(onBackClick)
            Text("LIVE TV", fontSize = sp(16), fontWeight = FontWeight.Black,
                color = White, letterSpacing = sp(2))
            LiveDot()
        }

        if (movie != null) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(movie.title, fontSize = sp(20), fontWeight = FontWeight.Black, color = White, maxLines = 1)
                    movie.genreNames().let { if (it.isNotEmpty()) Text(it, fontSize = sp(12), color = LightGray) }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(NetflixRed)
                        .clickable { onPlay() }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(16.dp))
                        Text("Watch", fontSize = sp(13), fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChannelPanel(
    movies: List<MovieResponse>, selected: MovieResponse?,
    ad: Adaptive, onBackClick: () -> Unit,
    onHover: (MovieResponse) -> Unit, onSelect: (MovieResponse) -> Unit
) {
    Column(
        modifier = Modifier.width(if (ad.isTV) 400.dp else 300.dp)
            .fillMaxHeight().background(Color(0xFF0D0D0D))
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BackBtn(onBackClick)
                Column {
                    Text("SUNE", fontSize = sp(20), fontWeight = FontWeight.Black, color = NetflixRed, letterSpacing = sp(3))
                    Text("Live TV", fontSize = sp(12), color = LightGray)
                }
            }
            LiveDot()
        }
        Divider(color = White.copy(0.07f))
        Text("${movies.size} CHANNELS", fontSize = sp(10), fontWeight = FontWeight.Bold,
            color = LightGray.copy(0.4f), letterSpacing = sp(2),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(movies) { i, m ->
                ChannelRow(m, i + 1, m.id == selected?.id, ad,
                    onHover = { onHover(m) }, onSelect = { onSelect(m) })
            }
        }
    }
}

@Composable
private fun ChannelRow(
    movie: MovieResponse, idx: Int, isSelected: Boolean, ad: Adaptive,
    onHover: () -> Unit, onSelect: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) { if (focused) onHover() }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(when { isSelected -> NetflixRed.copy(0.16f); focused -> White.copy(0.05f); else -> Color.Transparent })
            .border(if (isSelected) 1.dp else 0.dp,
                if (isSelected) NetflixRed.copy(0.45f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .onFocusChanged { focused = it.isFocused }
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(String.format("%02d", idx), fontSize = sp(11), fontWeight = FontWeight.Bold,
            color = if (isSelected) NetflixRed else LightGray.copy(0.35f),
            modifier = Modifier.width(22.dp))
        val thumbW = if (ad.isTV) 62.dp else 50.dp
        val thumbH = if (ad.isTV) 40.dp else 33.dp
        Box(Modifier.size(thumbW, thumbH).clip(RoundedCornerShape(5.dp)).background(CardBackground)) {
            AsyncImage(movie.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (isSelected) {
                Box(Modifier.fillMaxSize().background(NetflixRed.copy(0.4f)), Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(movie.title, fontSize = if (ad.isTV) sp(14) else sp(13),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) White else White.copy(0.82f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(movie.genreNames().ifEmpty { "Movie" }, fontSize = sp(11),
                color = if (isSelected) NetflixRed.copy(0.75f) else LightGray.copy(0.45f))
        }
        Box(Modifier.size(7.dp).clip(CircleShape)
            .background(if (isSelected) NetflixRed else LightGray.copy(0.25f)))
    }
}

@Composable
private fun LivePreviewPanel(movie: MovieResponse?, ad: Adaptive, onPlay: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBackground)) {
        if (movie == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Select a channel", fontSize = sp(18), color = LightGray.copy(0.5f))
            }; return@Box
        }
        AsyncImage(movie.bannerUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(DarkBackground.copy(0.75f), Color.Transparent), 0f, 200f)
        ))
        Box(Modifier.fillMaxSize().background(DarkBackground.copy(0.42f)))

        Column(
            modifier = Modifier.align(Alignment.Center).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).background(NetflixRed)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(White))
                Text("LIVE", fontSize = sp(12), fontWeight = FontWeight.Black, color = White, letterSpacing = sp(2))
            }
            Text(movie.title, fontSize = ad.heroTitle * 0.88f, fontWeight = FontWeight.Black,
                color = White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (movie.genreNames().isNotEmpty())
                Text(movie.genreNames().uppercase(), fontSize = sp(12), color = NetflixRed,
                    fontWeight = FontWeight.Bold, letterSpacing = sp(2))
            movie.description?.let {
                Text(it, fontSize = ad.body, color = White.copy(0.68f), textAlign = TextAlign.Center,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 420.dp))
            }
            Spacer(Modifier.height(6.dp))
            var focused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (focused) 1.05f else 1f, spring())
            Row(
                modifier = Modifier.scale(scale)
                    .clip(RoundedCornerShape(8.dp)).background(NetflixRed)
                    .clickable { onPlay() }.onFocusChanged { focused = it.isFocused }
                    .padding(horizontal = 36.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(26.dp))
                Text("Watch Now", fontSize = sp(16), fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

@Composable
fun LiveDot() {
    val inf = rememberInfiniteTransition(label = "live")
    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "a")
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(NetflixRed.copy(0.15f))
            .border(1.dp, NetflixRed.copy(0.38f), RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(NetflixRed.copy(a)))
        Text("LIVE", fontSize = sp(11), fontWeight = FontWeight.Bold, color = NetflixRed, letterSpacing = sp(1))
    }
}

private operator fun androidx.compose.ui.unit.TextUnit.times(f: Float) =
    TextUnit(this.value * f, TextUnitType.Sp)
private operator fun androidx.compose.ui.unit.TextUnit.times(f: Double) =
    TextUnit(this.value * f.toFloat(), TextUnitType.Sp)
private operator fun Dp.times(f: Float): Dp = (this.value * f).dp
