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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.MovieCategory
import com.streaming.sunne.data.model.MovieResponse
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.HomeUiState
import com.streaming.sunne.viewmodel.MovieViewModel

private val navItems = listOf("Home", "TV Shows", "Movies", "My List", "Live")

// ─── HomeScreen ────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    viewModel: MovieViewModel,
    onMovieClick: (MovieResponse) -> Unit,
    onLiveClick: () -> Unit
) {
    val homeState by viewModel.homeState.collectAsState()
    var selectedNav by remember { mutableStateOf("Home") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (val state = homeState) {
            is HomeUiState.Loading -> FullScreenLoader()
            is HomeUiState.Error   -> ErrorContent(
                message = state.message,
                onRetry = viewModel::retry
            )
            is HomeUiState.Success -> {
                val categories    = state.categories
                val featuredMovie = categories.firstOrNull()?.movies?.firstOrNull()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (featuredMovie != null) {
                        HeroBanner(
                            movie       = featuredMovie,
                            navItems    = navItems,
                            selectedNav = selectedNav,
                            onNavSelected = { item ->
                                selectedNav = item
                                if (item == "Live") onLiveClick()
                            },
                            onPlayClick = { onMovieClick(featuredMovie) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    categories.forEach { category ->
                        ContentRow(category = category, onMovieClick = onMovieClick)
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// ─── Hero Banner ───────────────────────────────────────────────────────────────
@Composable
private fun HeroBanner(
    movie: MovieResponse,
    navItems: List<String>,
    selectedNav: String,
    onNavSelected: (String) -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
    ) {
        AsyncImage(
            model             = movie.bannerUrl,
            contentDescription = null,
            modifier          = Modifier.fillMaxSize(),
            contentScale      = ContentScale.Crop
        )

        // Left + bottom gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.95f),
                            DarkBackground.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors  = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.3f), DarkBackground),
                        startY  = 200f
                    )
                )
        )

        TopNavBar(
            navItems      = navItems,
            selectedNav   = selectedNav,
            onNavSelected = onNavSelected
        )

        // Hero content
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp, top = 80.dp)
                .widthIn(max = 500.dp)
        ) {
            Text(
                text          = "✦  FEATURED",
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Bold,
                color         = NetflixRed,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text       = movie.title,
                fontSize   = 52.sp,
                fontWeight = FontWeight.Black,
                color      = White,
                lineHeight = 56.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Meta row
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                movie.releaseYear?.let {
                    Text("$it", fontSize = 14.sp, color = LightGray)
                    MetaDivider()
                }
                movie.formattedDuration()?.let {
                    Text(it, fontSize = 14.sp, color = LightGray)
                    MetaDivider()
                }
                movie.rating?.let {
                    QualityBadge(it)          // e.g. "PG-13"
                }
                if (movie.genreNames().isNotEmpty()) {
                    MetaDivider()
                    Text(
                        text     = movie.genreNames(),
                        fontSize = 13.sp,
                        color    = LightGray.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            movie.description?.let {
                Text(
                    text       = it,
                    fontSize   = 15.sp,
                    color      = White.copy(alpha = 0.8f),
                    maxLines   = 3,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayButton(onClick = onPlayClick)
                MoreInfoButton()
            }
        }
    }
}

// ─── Top Nav Bar ───────────────────────────────────────────────────────────────
@Composable
private fun TopNavBar(
    navItems: List<String>,
    selectedNav: String,
    onNavSelected: (String) -> Unit
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp, vertical = 28.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        Text(
            text          = "SUNNE",
            fontSize      = 36.sp,
            fontWeight    = FontWeight.Black,
            color         = NetflixRed,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.width(56.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                NavItem(
                    label      = item,
                    isSelected = item == selectedNav,
                    isLive     = item == "Live",
                    onClick    = { onNavSelected(item) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search",
                tint = White, modifier = Modifier.size(24.dp).clickable {})
            Icon(Icons.Default.Notifications, contentDescription = "Notifications",
                tint = White, modifier = Modifier.size(24.dp).clickable {})
            Box(
                modifier        = Modifier.size(36.dp).clip(CircleShape).background(NetflixRed),
                contentAlignment = Alignment.Center
            ) {
                Text("A", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

// ─── Nav Item ──────────────────────────────────────────────────────────────────
@Composable
private fun NavItem(
    label: String,
    isSelected: Boolean,
    isLive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLive) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NetflixRed))
            }
            Text(
                text       = label,
                fontSize   = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color      = if (isSelected) White else White.copy(alpha = 0.7f)
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(20.dp).height(2.dp).background(NetflixRed, RoundedCornerShape(1.dp)))
        }
    }
}

// ─── Play Button ───────────────────────────────────────────────────────────────
@Composable
private fun PlayButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.06f else 1f, tween(150))
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(White)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 28.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = DarkBackground, modifier = Modifier.size(26.dp))
        Text("Play", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
    }
}

// ─── More Info Button ──────────────────────────────────────────────────────────
@Composable
private fun MoreInfoButton() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(White.copy(alpha = 0.25f))
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Info, null, tint = White, modifier = Modifier.size(22.dp))
        Text("More Info", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = White)
    }
}

// ─── Content Row (Carousel) ────────────────────────────────────────────────────
@Composable
fun ContentRow(
    category: MovieCategory,
    onMovieClick: (MovieResponse) -> Unit
) {
    Column {
        Row(
            modifier              = Modifier.padding(horizontal = 56.dp).fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category.label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text("See all ›", fontSize = 14.sp, color = NetflixRed, modifier = Modifier.clickable { })
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(category.movies) { movie ->
                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
            }
        }
    }
}

// ─── Movie Card ────────────────────────────────────────────────────────────────
@Composable
private fun MovieCard(
    movie: MovieResponse,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (isFocused) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .width(220.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width  = if (isFocused) 2.5.dp else 0.dp,
                color  = if (isFocused) NetflixRed else Color.Transparent,
                shape  = RoundedCornerShape(8.dp)
            )
    ) {
        AsyncImage(
            model              = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier           = Modifier.fillMaxWidth().height(130.dp),
            contentScale       = ContentScale.Crop
        )

        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopStart)
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.7f))
                ))
        )

        // Info bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(CardBackground)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text      = movie.title,
                fontSize  = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color     = White,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
            movie.formattedDuration()?.let {
                Text(it, fontSize = 11.sp, color = LightGray)
            }
        }

        // Trending badge
        if (movie.isTrending) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF6B00))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("TRENDING", fontSize = 9.sp, fontWeight = FontWeight.Black, color = White)
            }
        }
    }
}

// ─── Shared helpers ────────────────────────────────────────────────────────────
@Composable
private fun QualityBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, LightGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 11.sp, color = LightGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetaDivider() {
    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(LightGray.copy(alpha = 0.5f)))
}

@Composable
private fun FullScreenLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Loading Sune...", fontSize = 18.sp, color = LightGray)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oops!", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NetflixRed)
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 16.sp, color = LightGray)
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NetflixRed)
                    .clickable { onRetry() }
                    .padding(horizontal = 36.dp, vertical = 14.dp)
            ) {
                Text("Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}
