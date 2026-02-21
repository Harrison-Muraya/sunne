package com.streaming.sunne.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.*
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.HomeUiState
import com.streaming.sunne.viewmodel.MovieViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    viewModel:          MovieViewModel,
    authViewModel:      AuthViewModel,
    onMovieClick:       (MovieResponse) -> Unit,
    onSeriesClick:      (SeriesResponse) -> Unit,
    onLiveStreamClick:  (LiveStreamPublicResponse) -> Unit,
    onSearchClick:      () -> Unit,
    onLiveTabClick:     () -> Unit,
    onLogout:           () -> Unit,
    onWatchHistoryClick: () -> Unit,
    onProfileClick:     () -> Unit
) {
    val state   by viewModel.homeState.collectAsState()
    val user    by authViewModel.currentUser.collectAsState()
    val adaptive = rememberAdaptive()

    var activeNav       by remember { mutableStateOf("Home") }
    var showProfileMenu by remember { mutableStateOf(false) }

    val navItems = listOf("Home", "Movies", "Series", "Live")

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // TV top nav — pinned outside scroll
            if (!adaptive.useBottomNav) {
                TvTopBar(
                    navItems       = navItems,
                    activeNav      = activeNav,
                    adaptive       = adaptive,
                    username       = user?.username ?: "A",
                    onNavSelected  = { item ->
                        activeNav = item
                        when (item) {
                            "Live"   -> onLiveTabClick()
                            "Movies" -> { /* stay on home, movies shown in rows */ }
                            "Series" -> { /* stay on home, series shown in rows */ }
                        }
                    },
                    onProfileClick = { showProfileMenu = true },
                    onSearchClick  = onSearchClick
                )
            }

            when (val s = state) {
                is HomeUiState.Loading -> HomeLoadingScreen()
                is HomeUiState.Error   -> HomeErrorScreen(s.message, viewModel::retry)
                is HomeUiState.Success -> {
                    val categories = s.categories

                    // Hero: first Movie item across all rows
                    val heroMovie: MovieResponse? = categories
                        .flatMap { it.items }
                        .filterIsInstance<ContentItem.Movie>()
                        .firstOrNull()?.data

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (heroMovie != null) {
                            HeroBanner(
                                movie          = heroMovie,
                                adaptive       = adaptive,
                                username       = user?.username ?: "A",
                                showPhoneBar   = adaptive.useBottomNav,
                                onPlayClick    = { onMovieClick(heroMovie) },
                                onProfileClick = { showProfileMenu = true },
                                onSearchClick  = onSearchClick
                            )
                        }

                        Spacer(Modifier.height(adaptive.rowGap))

                        categories.forEach { row ->
                            ContentRowSection(
                                row             = row,
                                adaptive        = adaptive,
                                onMovieClick    = onMovieClick,
                                onSeriesClick   = onSeriesClick,
                                onLiveClick     = onLiveStreamClick
                            )
                            Spacer(Modifier.height(adaptive.rowGap))
                        }

                        if (adaptive.useBottomNav) Spacer(Modifier.height(64.dp))
                    }
                }
            }
        }

        // Phone / Tablet bottom nav
        if (adaptive.useBottomNav) {
            PhoneBottomNav(
                modifier  = Modifier.align(Alignment.BottomCenter),
                navItems  = navItems,
                activeNav = activeNav,
                onSelect  = { item ->
                    activeNav = item
                    when (item) {
                        "Live"   -> onLiveTabClick()
                        "Search" -> onSearchClick()
                    }
                }
            )
        }

        // Profile dropdown
        AnimatedVisibility(
            visible = showProfileMenu,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            ProfileMenuOverlay(
                username            = user?.username ?: "",
                email               = user?.email ?: "",
                isAdmin             = user?.isAdmin ?: false,
                onDismiss           = { showProfileMenu = false },
                onLogout            = {
                    showProfileMenu = false
                    authViewModel.logout()
                    onLogout()
                },
                onWatchHistory      = {
                    showProfileMenu = false
                    onWatchHistoryClick()
                },
                onProfile           = {
                    showProfileMenu = false
                    onProfileClick()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Content Row — handles Movie / Series / LiveStream cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ContentRowSection(
    row:           ContentRow,
    adaptive:      Adaptive,
    onMovieClick:  (MovieResponse) -> Unit,
    onSeriesClick: (SeriesResponse) -> Unit,
    onLiveClick:   (LiveStreamPublicResponse) -> Unit
) {
    Column {
        // Row label
        Text(
            text       = row.label,
            fontSize   = adaptive.sectionTitle,
            fontWeight = FontWeight.Bold,
            color      = White,
            modifier   = Modifier.padding(horizontal = adaptive.pad, vertical = 6.dp)
        )

        LazyRow(
            contentPadding       = PaddingValues(horizontal = adaptive.pad),
            horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap)
        ) {
            items(row.items) { item ->
                when (item) {
                    is ContentItem.Movie -> {
                        MovieCard(
                            movie    = item.data,
                            adaptive = adaptive,
                            onClick  = { onMovieClick(item.data) }
                        )
                    }
                    is ContentItem.Series -> {
                        SeriesCard(
                            series   = item.data,
                            adaptive = adaptive,
                            onClick  = { onSeriesClick(item.data) }
                        )
                    }
                    is ContentItem.LiveStream -> {
                        LiveStreamCard(
                            stream   = item.data,
                            adaptive = adaptive,
                            onClick  = { onLiveClick(item.data) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Movie Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MovieCard(
    movie: MovieResponse,
    adaptive: Adaptive,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(adaptive.cardW)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier           = Modifier.fillMaxWidth().height(adaptive.cardH),
            contentScale       = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(adaptive.cardH)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, DarkBackground.copy(alpha = 0.75f))
                ))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(CardBackground)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(movie.title, fontSize = adaptive.caption, fontWeight = FontWeight.SemiBold,
                color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            movie.formattedDuration()?.let {
                Text(it, fontSize = sp(10), color = LightGray)
            }
        }
        if (movie.isTrending) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFFE55300))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("HOT", fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Series Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SeriesCard(
    series: SeriesResponse,
    adaptive: Adaptive,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(adaptive.cardW)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = series.thumbnailUrl,
            contentDescription = series.title,
            modifier           = Modifier.fillMaxWidth().height(adaptive.cardH),
            contentScale       = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(adaptive.cardH)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, DarkBackground.copy(alpha = 0.8f))
                ))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(CardBackground)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(series.title, fontSize = adaptive.caption, fontWeight = FontWeight.SemiBold,
                color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val seasonCount = series.seasons.size
            if (seasonCount > 0) {
                Text("$seasonCount Season${if (seasonCount > 1) "s" else ""}",
                    fontSize = sp(10), color = LightGray)
            }
        }
        // TV badge
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A73E8))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("TV", fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
        }
        if (series.isTrending) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFFE55300))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("HOT", fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Live Stream Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LiveStreamCard(
    stream: LiveStreamPublicResponse,
    adaptive: Adaptive,
    onClick: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition()
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue   = 0.6f,
        targetValue    = 1.0f,
        animationSpec  = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .width(adaptive.cardW)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model              = stream.thumbnailUrl,
            contentDescription = stream.title,
            modifier           = Modifier.fillMaxWidth().height(adaptive.cardH),
            contentScale       = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(adaptive.cardH)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                ))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(stream.title, fontSize = adaptive.caption, fontWeight = FontWeight.SemiBold,
                color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (stream.viewerCount > 0) {
                Text("${stream.viewerCount} watching", fontSize = sp(10), color = LightGray)
            }
        }
        // LIVE badge
        if (stream.isLive) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Red.copy(alpha = pulseAlpha))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("● LIVE", fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TV Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TvTopBar(
    navItems: List<String>,
    activeNav: String,
    adaptive: Adaptive,
    username: String,
    onNavSelected: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(
                listOf(DarkBackground, DarkBackground.copy(alpha = 0f))
            ))
            .padding(horizontal = adaptive.pad, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "SUNE",
            fontSize      = adaptive.logo,
            fontWeight    = FontWeight.Black,
            color         = NetflixRed,
            letterSpacing = sp(4)
        )

        Spacer(Modifier.width(52.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                TvNavItem(
                    label      = item,
                    isSelected = item == activeNav,
                    isLive     = item == "Live",
                    onClick    = { onNavSelected(item) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, "Search",
                tint     = White,
                modifier = Modifier.size(24.dp).clickable { onSearchClick() }
            )
            Avatar(
                letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                size    = 38.dp,
                onClick = onProfileClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TV Nav Item
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TvNavItem(label: String, isSelected: Boolean, isLive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isLive) {
                Box(
                    Modifier.size(7.dp).clip(CircleShape).background(Color.Red)
                )
            }
            Text(
                text       = label,
                fontSize   = sp(15),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color      = if (isSelected) White else LightGray
            )
        }
        if (isSelected) {
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier.width(20.dp).height(2.dp).clip(RoundedCornerShape(1.dp))
                    .background(NetflixRed)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Phone Bottom Nav
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PhoneBottomNav(
    modifier: Modifier,
    navItems: List<String>,
    activeNav: String,
    onSelect: (String) -> Unit
) {
    val icons = mapOf(
        "Home"   to Icons.Default.Home,
        "Movies" to Icons.Default.Movie,
        "Series" to Icons.Default.Tv,
        "Live"   to Icons.Default.LiveTv,
        "Search" to Icons.Default.Search
    )
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .background(DarkBackground.copy(alpha = 0.97f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        val allItems = navItems + "Search"
        allItems.forEach { item ->
            val icon    = icons[item] ?: Icons.Default.Circle
            val selected = item == activeNav
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.clickable { onSelect(item) }.padding(horizontal = 8.dp)
            ) {
                Icon(
                    icon, item,
                    tint     = if (selected) NetflixRed else LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    item,
                    fontSize = sp(10),
                    color    = if (selected) NetflixRed else LightGray
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero Banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroBanner(
    movie: MovieResponse,
    adaptive: Adaptive,
    username: String,
    showPhoneBar: Boolean,
    onPlayClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (adaptive.isTV) 560.dp else if (adaptive.isPhone) 420.dp else 480.dp)
    ) {
        AsyncImage(
            model              = movie.bannerUrl,
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )
        // Gradient overlays
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent, DarkBackground))
        ))
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(DarkBackground.copy(0.5f), Color.Transparent))
        ))

        // Phone top bar
        if (showPhoneBar) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("SUNE", fontSize = sp(26), fontWeight = FontWeight.Black, color = NetflixRed,
                    letterSpacing = sp(3))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, "Search", tint = White, modifier = Modifier.size(22.dp).clickable { onSearchClick() })
                    Avatar(
                        letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        size    = 34.dp,
                        onClick = onProfileClick
                    )
                }
            }
        }

        // Hero info at bottom
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = adaptive.pad, end = 60.dp, bottom = 28.dp)
        ) {
            Text(
                movie.title,
                fontSize   = if (adaptive.isTV) sp(42) else sp(28),
                fontWeight = FontWeight.Black,
                color      = White,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                movie.releaseYear?.let { Text("$it", fontSize = sp(13), color = LightGray) }
                if (!movie.genreNames().isBlank()) {
                    Text("·", fontSize = sp(13), color = LightGray)
                    Text(movie.genreNames(), fontSize = sp(13), color = LightGray, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(14.dp))
            // Play button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(White)
                    .clickable { onPlayClick() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                Text("Play", fontSize = sp(15), fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Profile Menu Overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileMenuOverlay(
    username:       String,
    email:          String,
    isAdmin:        Boolean,
    onDismiss:      () -> Unit,
    onLogout:       () -> Unit,
    onWatchHistory: () -> Unit,
    onProfile:      () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 20.dp)
                .width(220.dp)
                .clickable { /* absorb */ },
            backgroundColor = Color(0xFF1C1C1C),
            shape           = RoundedCornerShape(14.dp),
            elevation       = 20.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Avatar(
                        letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        size    = 40.dp,
                        onClick = {}
                    )
                    Column {
                        Text(username, fontSize = sp(14), fontWeight = FontWeight.Bold, color = White)
                        Text(email, fontSize = sp(11), color = LightGray, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        if (isAdmin) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(3.dp))
                                    .background(NetflixRed.copy(0.25f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("ADMIN", fontSize = sp(9), color = NetflixRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = White.copy(alpha = 0.08f))

                MenuRow(Icons.Default.Person, "My Profile", onClick = onProfile)
                MenuRow(Icons.Default.History, "Watch History", onClick = onWatchHistory)
                MenuRow(Icons.Default.Settings, "Settings") { onDismiss() }

                Divider(color = White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                MenuRow(Icons.Default.Logout, "Sign Out", tint = NetflixRed, onClick = onLogout)
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, fontSize = sp(14), color = tint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable atoms
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Avatar(letter: String, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(NetflixRed).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            fontSize   = sp(size.value * 0.38f),
            fontWeight = FontWeight.Bold,
            color      = White
        )
    }
}

@Composable
private fun HomeLoadingScreen() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(14.dp))
            Text("Loading Sune...", fontSize = sp(16), color = LightGray)
        }
    }
}

@Composable
private fun HomeErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, tint = NetflixRed, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = sp(14), color = LightGray,
                modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(NetflixRed)
                    .clickable { onRetry() }.padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text("Retry", fontSize = sp(15), fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

// Legacy CategoryRow removed — use ContentRowSection instead.
// Kept here for compiler compatibility if any screen still imports it:
@Deprecated("Use ContentRowSection inside HomeScreen")
@Composable
fun CategoryRow(
    cat: MovieCategory,
    adaptive: Adaptive,
    onMovieClick: (MovieResponse) -> Unit
) {
    Column {
        Text(
            cat.label,
            fontSize   = adaptive.sectionTitle,
            fontWeight = FontWeight.Bold,
            color      = White,
            modifier   = Modifier.padding(horizontal = adaptive.pad, vertical = 6.dp)
        )
        LazyRow(
            contentPadding        = PaddingValues(horizontal = adaptive.pad),
            horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap)
        ) {
            items(cat.movies) { movie ->
                MovieCard(movie = movie, adaptive = adaptive, onClick = { onMovieClick(movie) })
            }
        }
    }
}

private operator fun TextUnit.times(f: Float) = sp(this.value * f)
private operator fun TextUnit.times(f: Double) = sp(this.value * f.toFloat())