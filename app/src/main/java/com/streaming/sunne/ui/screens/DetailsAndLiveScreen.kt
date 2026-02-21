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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.streaming.sunne.data.model.*
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.MovieViewModel

// ══════════════════════════════════════════════════════════════════════════════
//  DETAILS SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun DetailsScreen(
    movie:       MovieResponse,
    viewModel:   MovieViewModel,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (MovieResponse) -> Unit = {}
) {
    val ad = rememberAdaptive()

    // Ancillary data
    val myRating     by viewModel.myRating.collectAsState()
    val avgRating    by viewModel.avgRating.collectAsState()
    val similarMovies by viewModel.similarMovies.collectAsState()
    val becauseYouWatched by viewModel.becauseYouWatched.collectAsState()

    if (ad.isPhone) DetailsPortrait(
        movie, viewModel, myRating, avgRating, similarMovies, becauseYouWatched,
        onPlayClick, onBackClick, onMovieClick, ad
    )
    else DetailsLandscape(
        movie, viewModel, myRating, avgRating, similarMovies, becauseYouWatched,
        onPlayClick, onBackClick, onMovieClick, ad
    )
}

// ── TV / Tablet ───────────────────────────────────────────────────────────────
@Composable
private fun DetailsLandscape(
    movie: MovieResponse,
    viewModel: MovieViewModel,
    myRating: MovieRatingResponse?,
    avgRating: AverageRatingResponse?,
    similar: List<SimilarMovieResponse>,
    becauseYouWatched: List<RecommendationResponse>,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (MovieResponse) -> Unit,
    ad: Adaptive
) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Image(
            painter            = rememberAsyncImagePainter(movie.bannerUrl),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(DarkBackground.copy(0.97f), DarkBackground.copy(0.55f), Color.Transparent))
        ))
        Box(Modifier.fillMaxSize().background(DarkBackground.copy(0.3f)))

        BackBtn(onBackClick, Modifier.align(Alignment.TopStart).padding(ad.pad))

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (ad.isTV) 580.dp else 500.dp)
                .align(Alignment.CenterStart)
                .padding(start = ad.pad, end = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            DetailsInfo(movie, viewModel, myRating, avgRating, similar, becauseYouWatched, onPlayClick, onMovieClick, ad)
        }
    }
}

// ── Phone ─────────────────────────────────────────────────────────────────────
@Composable
private fun DetailsPortrait(
    movie: MovieResponse,
    viewModel: MovieViewModel,
    myRating: MovieRatingResponse?,
    avgRating: AverageRatingResponse?,
    similar: List<SimilarMovieResponse>,
    becauseYouWatched: List<RecommendationResponse>,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (MovieResponse) -> Unit,
    ad: Adaptive
) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Backdrop
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                AsyncImage(
                    model              = movie.bannerUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, DarkBackground))
                ))
            }
            // Info below
            Column(modifier = Modifier.padding(horizontal = ad.pad)) {
                DetailsInfo(movie, viewModel, myRating, avgRating, similar, becauseYouWatched, onPlayClick, onMovieClick, ad)
            }
        }
        BackBtn(onBackClick, Modifier.align(Alignment.TopStart).padding(ad.pad))
    }
}

// ── Shared info block ─────────────────────────────────────────────────────────
@Composable
private fun DetailsInfo(
    movie: MovieResponse,
    viewModel: MovieViewModel,
    myRating: MovieRatingResponse?,
    avgRating: AverageRatingResponse?,
    similar: List<SimilarMovieResponse>,
    becauseYouWatched: List<RecommendationResponse>,
    onPlayClick: () -> Unit,
    onMovieClick: (MovieResponse) -> Unit,
    ad: Adaptive
) {
    var showRatingDialog by remember { mutableStateOf(false) }

    // Title
    Text(
        movie.title,
        fontSize   = if (ad.isTV) sp(38) else sp(26),
        fontWeight = FontWeight.Black,
        color      = White,
        lineHeight  = if (ad.isTV) sp(44) else sp(32)
    )

    Spacer(Modifier.height(10.dp))

    // Meta row
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        movie.releaseYear?.let { Text("$it", fontSize = sp(13), color = LightGray) }
        movie.formattedDuration()?.let {
            MetaDot()
            Text(it, fontSize = sp(13), color = LightGray)
        }
        movie.rating?.let {
            MetaDot()
            RatingChip(it, ad)
        }
    }

    Spacer(Modifier.height(6.dp))

    // Genres
    if (movie.genres.isNotEmpty()) {
        Text(movie.genreNames(), fontSize = sp(13), color = LightGray.copy(0.8f))
    }

    // Average star rating
    avgRating?.let { avg ->
        if ((avg.totalRatings) > 0) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StarRow(rating = avg.averageRating?.toFloat() ?: 0f, size = 16.dp)
                Text(
                    text = "${"%.1f".format(avg.averageRating ?: 0.0)} (${avg.totalRatings})",
                    fontSize = sp(12), color = LightGray
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Description
    movie.description?.let {
        Text(it, fontSize = sp(14), color = LightGray, lineHeight = sp(21))
        Spacer(Modifier.height(16.dp))
    }

    // Director / Cast
    movie.director?.let {
        InfoLine("Director", it)
        Spacer(Modifier.height(6.dp))
    }
    movie.cast?.let {
        InfoLine("Cast", it)
        Spacer(Modifier.height(16.dp))
    }

    // CTA buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Play
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp)).background(White)
                .clickable { onPlayClick() }
                .padding(horizontal = 28.dp, vertical = 13.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(22.dp))
            Text("Play", fontSize = sp(16), fontWeight = FontWeight.Bold, color = Color.Black)
        }
        // Rate
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (myRating != null) NetflixRed.copy(0.2f) else White.copy(0.15f))
                .border(1.dp, if (myRating != null) NetflixRed else White.copy(0.3f), RoundedCornerShape(8.dp))
                .clickable { showRatingDialog = true }
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                if (myRating != null) Icons.Default.Star else Icons.Default.StarOutline,
                null, tint = if (myRating != null) NetflixRed else White, modifier = Modifier.size(20.dp)
            )
            Text(
                if (myRating != null) "Rated ${myRating.rating}★" else "Rate",
                fontSize = sp(15), color = if (myRating != null) NetflixRed else White
            )
        }
    }

    // Similar Movies row
    if (similar.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        Text("More Like This", fontSize = sp(17), fontWeight = FontWeight.Bold, color = White)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(similar) { sim ->
                SimilarCard(sim)
            }
        }
    }

    // Because You Watched row
    if (becauseYouWatched.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        Text("Because You Watched This", fontSize = sp(17), fontWeight = FontWeight.Bold, color = White)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(becauseYouWatched) { rec ->
                RecommendationCard(rec)
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // Rating dialog
    if (showRatingDialog) {
        RatingDialog(
            currentRating = myRating?.rating,
            onSubmit      = { stars, review ->
                viewModel.rateMovie(movie.id, stars, review)
                showRatingDialog = false
            },
            onDelete      = if (myRating != null) {
                { viewModel.deleteMyRating(movie.id); showRatingDialog = false }
            } else null,
            onDismiss     = { showRatingDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rating dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RatingDialog(
    currentRating: Int?,
    onSubmit:  (Int, String?) -> Unit,
    onDelete:  (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var selectedStars by remember { mutableStateOf(currentRating ?: 0) }
    var review        by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor  = Color(0xFF1E1E1E),
        contentColor     = White,
        title = {
            Text("Rate This Movie", fontWeight = FontWeight.Bold, color = White)
        },
        text = {
            Column {
                // Star selector
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "$star stars",
                            tint     = if (star <= selectedStars) Color(0xFFFFC107) else LightGray,
                            modifier = Modifier.size(36.dp).clickable { selectedStars = star }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Review text field
                OutlinedTextField(
                    value          = review,
                    onValueChange  = { review = it },
                    placeholder    = { Text("Add a review (optional)", color = LightGray) },
                    colors         = TextFieldDefaults.outlinedTextFieldColors(
                        textColor             = White,
                        focusedBorderColor    = NetflixRed,
                        unfocusedBorderColor  = LightGray.copy(0.3f),
                        cursorColor           = NetflixRed
                    ),
                    modifier       = Modifier.fillMaxWidth(),
                    maxLines       = 3,
                    singleLine     = false
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Remove", color = NetflixRed)
                    }
                }
                TextButton(
                    onClick  = { if (selectedStars > 0) onSubmit(selectedStars, review.ifBlank { null }) },
                    enabled  = selectedStars > 0
                ) {
                    Text("Submit", color = NetflixRed)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LightGray)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Similar / Recommendation Cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SimilarCard(sim: SimilarMovieResponse) {
    Box(
        modifier = Modifier.width(120.dp).clip(RoundedCornerShape(6.dp))
    ) {
        AsyncImage(
            model              = sim.posterUrl,
            contentDescription = sim.title,
            modifier           = Modifier.fillMaxWidth().height(160.dp),
            contentScale       = ContentScale.Crop
        )
        Box(Modifier.fillMaxWidth().height(160.dp).background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))
        ))
        Text(
            sim.title,
            fontSize = sp(11), color = White, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
        )
    }
}

@Composable
private fun RecommendationCard(rec: RecommendationResponse) {
    Box(
        modifier = Modifier.width(130.dp).clip(RoundedCornerShape(6.dp))
    ) {
        AsyncImage(
            model              = rec.posterUrl,
            contentDescription = rec.title,
            modifier           = Modifier.fillMaxWidth().height(170.dp),
            contentScale       = ContentScale.Crop
        )
        Box(Modifier.fillMaxWidth().height(170.dp).background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))
        ))
        Column(Modifier.align(Alignment.BottomStart).padding(6.dp)) {
            Text(rec.title, fontSize = sp(11), color = White, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(rec.reason, fontSize = sp(9), color = LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Star Row (read-only display)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StarRow(rating: Float, size: Dp = 14.dp) {
    Row {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = null,
                tint     = Color(0xFFFFC107),
                modifier = Modifier.size(size)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", fontSize = sp(13), color = LightGray, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = sp(13), color = LightGray.copy(0.85f))
    }
}

@Composable
private fun MetaDot() {
    Box(Modifier.size(4.dp).clip(CircleShape).background(LightGray.copy(0.4f)))
}

@Composable
private fun RatingChip(label: String, adaptive: Adaptive) {
    Box(
        Modifier.clip(RoundedCornerShape(3.dp))
            .border(1.dp, LightGray.copy(0.5f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = adaptive.caption, color = LightGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BackBtn(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(0.55f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(20.dp))
    }
}

private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Int)   = sp(value.toFloat())

// ──────────────────────────────────────────────────────────────────────────────
//  LEGACY LiveScreen  (kept to satisfy MainActivity's Screen.Live route)
//  Real live content goes through LiveStreamListScreen → LivePlayerScreen
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun LiveScreen(
    viewModel:     MovieViewModel,
    onChannelPlay: (MovieResponse) -> Unit,
    onBackClick:   () -> Unit
) {
    // Redirect users to the proper LiveStreamListScreen by just calling back
    LaunchedEffect(Unit) { onBackClick() }
    Box(Modifier.fillMaxSize().background(DarkBackground), Alignment.Center) {
        CircularProgressIndicator(color = NetflixRed)
    }
}
