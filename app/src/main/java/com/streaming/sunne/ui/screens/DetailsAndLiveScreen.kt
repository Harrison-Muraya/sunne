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
import androidx.compose.material.icons.outlined.ThumbUp
import coil.compose.rememberAsyncImagePainter
import com.streaming.sunne.data.model.MovieResponse
import com.streaming.sunne.data.model.WatchHistoryResponse
import com.streaming.sunne.repository.WatchRepository
import kotlinx.coroutines.launch
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.MovieViewModel
import com.streaming.sunne.viewmodel.MyListToggleViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// ══════════════════════════════════════════════════════════════════════════════
//  DETAILS SCREEN — landscape (TV/tablet) vs portrait (phone)
// ══════════════════════════════════════════════════════════════════════════════
/** Per-user state shown on the details page: resume position and like. */
private data class DetailsUserState(
    val progress: WatchHistoryResponse? = null,
    val liked: Boolean = false,
    val onToggleLike: () -> Unit = {},
    val inList: Boolean = false,
    val onToggleList: () -> Unit = {}
) {
    val resumable: Boolean
        get() = progress != null && !progress.completed && progress.lastPosition >= 10 && progress.watchPercentage < 95f
}

@Composable
fun DetailsScreen(
    movie: MovieResponse,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val ad        = rememberAdaptive()
    val scope     = rememberCoroutineScope()
    val watchRepo = remember { WatchRepository() }
    var progress  by remember(movie.id) { mutableStateOf<WatchHistoryResponse?>(null) }
    var liked     by remember(movie.id) { mutableStateOf(false) }
    val listVm: MyListToggleViewModel = viewModel(key = "my-list/movie/${movie.id}")
    val inList    by listVm.inList.collectAsState()

    LaunchedEffect(movie.id) {
        listVm.bind("movie", movie.id)
        liked = (watchRepo.getMyRating(movie.id).getOrNull()?.rating ?: 0) >= 4
    }
    OnResumeEffect(movie.id) {
        scope.launch { progress = watchRepo.getProgress(movie.id).getOrNull() }
    }

    val user = DetailsUserState(
        progress = progress,
        liked    = liked,
        onToggleLike = {
            val wasLiked = liked
            liked = !wasLiked
            scope.launch {
                val result = if (wasLiked) watchRepo.deleteMyRating(movie.id)
                             else watchRepo.rateMovie(movie.id, 5)
                if (result.isFailure) liked = wasLiked
            }
        },
        inList       = inList,
        onToggleList = listVm::toggle
    )

    if (ad.isPhone) DetailsPortrait(movie, user, onPlayClick, onBackClick, ad)
    else            DetailsLandscape(movie, user, onPlayClick, onBackClick, ad)
}

// ── TV / Tablet — side-by-side poster + info ──────────────────────────────────
@Composable
private fun DetailsLandscape(
    movie: MovieResponse, user: DetailsUserState, onPlayClick: () -> Unit, onBackClick: () -> Unit, ad: Adaptive
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
            DetailsInfo(movie, user, onPlayClick, ad)
        }
    }
}

// ── Phone — stacked image + scrollable info ───────────────────────────────────
@Composable
private fun DetailsPortrait(
    movie: MovieResponse, user: DetailsUserState, onPlayClick: () -> Unit, onBackClick: () -> Unit, ad: Adaptive
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
            DetailsInfo(movie, user, onPlayClick, ad)
        }
    }
}

@Composable
private fun DetailsInfo(movie: MovieResponse, user: DetailsUserState, onPlayClick: () -> Unit, ad: Adaptive) {
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

    user.progress?.takeIf { user.resumable }?.let { p ->
        WatchProgressBar(p.watchPercentage / 100f, Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(6.dp))
        val remainingMin = movie.duration?.let { ((it - p.lastPosition).coerceAtLeast(0) / 60) }
        Text(
            remainingMin?.let { "${it}m left" } ?: "Resume from ${formatClock(p.lastPosition * 1000L)}",
            fontSize = sp(12), color = LightGray
        )
        Spacer(Modifier.height(12.dp))
    }

    val playFocus = remember { FocusRequester() }
    if (ad.isTV) {
        LaunchedEffect(movie.id) {
            withFrameNanos { }
            runCatching { playFocus.requestFocus() }
        }
    }

    // Action buttons
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        // Play / Resume
        Row(
            modifier = Modifier.focusRequester(playFocus).clip(RoundedCornerShape(6.dp)).background(White)
                .clickable { onPlayClick() }
                .padding(horizontal = if (ad.isPhone) 20.dp else 28.dp,
                    vertical = if (ad.isPhone) 10.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.Black,
                modifier = Modifier.size(if (ad.isPhone) 20.dp else 26.dp))
            Text(if (user.resumable) "Resume" else "Play", fontSize = if (ad.isPhone) sp(14) else sp(17),
                fontWeight = FontWeight.Bold, color = Color.Black)
        }
        IconCircleBtn(
            icon     = if (user.inList) Icons.Default.Check else Icons.Default.Add,
            size     = if (ad.isPhone) 38.dp else 48.dp,
            selected = user.inList,
            onClick  = user.onToggleList
        )
        // Like
        IconCircleBtn(
            icon     = if (user.liked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
            size     = if (ad.isPhone) 38.dp else 48.dp,
            selected = user.liked,
            onClick  = user.onToggleLike
        )
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
fun IconCircleBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Dp,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(if (selected) NetflixRed.copy(0.25f) else White.copy(0.12f))
            .border(1.5.dp, if (selected) NetflixRed else White.copy(0.3f), CircleShape)
            .clickable { onClick() },
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
