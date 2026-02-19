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
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.MovieCategory
import com.streaming.sunne.data.model.MovieResponse
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
    viewModel:     MovieViewModel,
    authViewModel: AuthViewModel,
    onMovieClick:  (MovieResponse) -> Unit,
    onLiveClick:   () -> Unit,
    onLogout:      () -> Unit
) {
    val state    by viewModel.homeState.collectAsState()
    val user     by authViewModel.currentUser.collectAsState()
    val adaptive  = rememberAdaptive()
    var activeNav        by remember { mutableStateOf("Home") }
    var showProfileMenu  by remember { mutableStateOf(false) }

    val navItems = listOf("Home", "Movies", "Live")

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {

        // ── Main scrollable content ───────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {

            // TV top nav lives outside the scroll so it stays pinned
            if (!adaptive.useBottomNav) {
                TvTopBar(
                    navItems      = navItems,
                    activeNav     = activeNav,
                    adaptive      = adaptive,
                    username      = user?.username ?: "A",
                    onNavSelected = { item ->
                        activeNav = item
                        if (item == "Live") onLiveClick()
                    },
                    onProfileClick = { showProfileMenu = true }
                )
            }

            // Scrollable body
            when (val s = state) {
                is HomeUiState.Loading -> HomeLoadingScreen()
                is HomeUiState.Error   -> HomeErrorScreen(s.message, viewModel::retry)
                is HomeUiState.Success -> {
                    val categories = s.categories
                    val hero = categories.firstOrNull()?.items?.firstOrNull()?.let { item ->
                        when (item) {
                            is com.streaming.sunne.data.model.ContentItem.Movie -> item.data
                            else -> null
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Hero banner
                        if (hero != null) {
                            HeroBanner(
                                movie         = hero,
                                adaptive      = adaptive,
                                username      = user?.username ?: "A",
                                showPhoneBar  = adaptive.useBottomNav,
                                onPlayClick   = { onMovieClick(hero) },
                                onProfileClick = { showProfileMenu = true }
                            )
                        }

                        Spacer(Modifier.height(adaptive.rowGap))

                        // Content rows
                        categories.forEach { cat ->
                            CategoryRow(cat, adaptive, onMovieClick)
                            Spacer(Modifier.height(adaptive.rowGap))
                        }

                        // Bottom padding for nav bar
                        if (adaptive.useBottomNav) Spacer(Modifier.height(64.dp))
                    }
                }
            }
        }

        // ── Phone / Tablet bottom nav ─────────────────────────────────────
        if (adaptive.useBottomNav) {
            PhoneBottomNav(
                modifier  = Modifier.align(Alignment.BottomCenter),
                navItems  = navItems,
                activeNav = activeNav,
                onSelect  = { item ->
                    activeNav = item
                    if (item == "Live") onLiveClick()
                }
            )
        }

        // ── Profile dropdown ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showProfileMenu,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            ProfileMenuOverlay(
                username  = user?.username ?: "",
                email     = user?.email ?: "",
                isAdmin   = user?.isAdmin ?: false,
                onDismiss = { showProfileMenu = false },
                onLogout  = {
                    showProfileMenu = false
                    authViewModel.logout()
                    onLogout()
                }
            )
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
    onProfileClick: () -> Unit
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
        // Logo
        Text(
            "SUNE",
            fontSize      = adaptive.logo,
            fontWeight    = FontWeight.Black,
            color         = NetflixRed,
            letterSpacing = sp(4)
        )

        Spacer(Modifier.width(52.dp))

        // Nav items
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

        // Right icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, "Search",
                tint     = White,
                modifier = Modifier.size(24.dp).clickable {}
            )
            Avatar(
                letter  = username.first().uppercaseChar().toString(),
                size    = 38.dp,
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun TvNavItem(label: String, isSelected: Boolean, isLive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (isLive) {
                val inf = rememberInfiniteTransition(label = "dot")
                val a by inf.animateFloat(0.5f, 1f,
                    infiniteRepeatable(tween(700), RepeatMode.Reverse), "a")
                Box(Modifier.size(7.dp).clip(CircleShape).background(NetflixRed.copy(alpha = a)))
            }
            Text(
                label,
                fontSize   = sp(16),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color      = if (isSelected) White else White.copy(alpha = 0.65f)
            )
        }
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(20.dp).height(2.dp).background(NetflixRed, RoundedCornerShape(1.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Phone bottom nav
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PhoneBottomNav(
    modifier: Modifier,
    navItems: List<String>,
    activeNav: String,
    onSelect: (String) -> Unit
) {
    val iconMap = mapOf(
        "Home"   to Icons.Default.Home,
        "Movies" to Icons.Default.Movie,
        "Live"   to Icons.Default.LiveTv
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF080808))
            .border(BorderStroke(0.5.dp, White.copy(alpha = 0.08f)),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val selected = item == activeNav
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(item) },
                verticalArrangement = Arrangement.Center
            ) {
                val icon = iconMap[item] ?: Icons.Default.Home
                val tint = if (selected) White else LightGray.copy(alpha = 0.45f)

                if (item == "Live" && !selected) {
                    val inf = rememberInfiniteTransition(label = "live")
                    val a by inf.animateFloat(0.4f, 0.9f,
                        infiniteRepeatable(tween(800), RepeatMode.Reverse), "a")
                    Icon(icon, item, tint = NetflixRed.copy(alpha = a), modifier = Modifier.size(22.dp))
                } else {
                    Icon(icon, item, tint = tint, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.height(3.dp))
                Text(
                    item,
                    fontSize   = sp(10),
                    color      = if (selected) White else LightGray.copy(alpha = 0.45f),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                // Active indicator dot
                if (selected) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(NetflixRed))
                }
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
    onProfileClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(adaptive.bannerH)) {
        AsyncImage(
            model              = movie.bannerUrl,
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )
        // Horizontal gradient (left overlay for text)
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                listOf(
                    DarkBackground.copy(alpha = if (adaptive.isTV) 0.95f else 0.88f),
                    DarkBackground.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        ))
        // Bottom fade to background
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Transparent, DarkBackground),
                startY = adaptive.bannerH.value * 0.55f
            )
        ))

        // Phone mini-topbar (logo + avatar)
        if (showPhoneBar) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = adaptive.pad, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("SUNE", fontSize = adaptive.logo, fontWeight = FontWeight.Black,
                    color = NetflixRed, letterSpacing = sp(4))
                Avatar(
                    letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                    size    = 32.dp,
                    onClick = onProfileClick
                )
            }
        }

        // Hero text content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = adaptive.pad, bottom = if (adaptive.isTV) 48.dp else 24.dp)
                .fillMaxWidth(adaptive.heroTextFraction)
        ) {
            // Featured label
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(NetflixRed))
                Text("FEATURED", fontSize = sp(11), fontWeight = FontWeight.Bold,
                    color = NetflixRed, letterSpacing = sp(3))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                movie.title,
                fontSize   = adaptive.heroTitle,
                fontWeight = FontWeight.Black,
                color      = White,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = adaptive.heroTitle * 1.12f
            )

            Spacer(Modifier.height(8.dp))

            // Meta
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movie.releaseYear?.let { Text("$it", fontSize = adaptive.caption, color = LightGray) }
                movie.formattedDuration()?.let {
                    MetaDivider()
                    Text(it, fontSize = adaptive.caption, color = LightGray)
                }
                movie.rating?.let {
                    MetaDivider()
                    RatingChip(it, adaptive)
                }
            }

            // Description — skip on phone to save space
            if (!adaptive.isPhone) {
                Spacer(Modifier.height(10.dp))
                movie.description?.let {
                    Text(
                        it,
                        fontSize   = adaptive.body,
                        color      = White.copy(alpha = 0.78f),
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = adaptive.body * 1.5f
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroPlayButton(onClick = onPlayClick, adaptive = adaptive)
                if (!adaptive.isPhone) HeroInfoButton(adaptive)
            }
        }
    }
}

@Composable
private fun HeroPlayButton(onClick: () -> Unit, adaptive: Adaptive) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, spring())
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(White)
            .clickable { onClick() }
            .onFocusChanged { focused = it.isFocused }
            .padding(
                horizontal = if (adaptive.isPhone) 18.dp else 26.dp,
                vertical   = if (adaptive.isPhone) 9.dp  else 12.dp
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = Color.Black,
            modifier = Modifier.size(if (adaptive.isPhone) 18.dp else 24.dp))
        Text("Play", fontSize = if (adaptive.isPhone) sp(14) else sp(16),
            fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun HeroInfoButton(adaptive: Adaptive) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(White.copy(alpha = 0.18f))
            .clickable {}
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Info, null, tint = White, modifier = Modifier.size(20.dp))
        Text("Info", fontSize = sp(16), fontWeight = FontWeight.SemiBold, color = White)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category Row + Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryRow(category: com.streaming.sunne.data.model.ContentRow, adaptive: Adaptive, onMovieClick: (MovieResponse) -> Unit) {
    Column {
        Row(
            modifier              = Modifier.padding(horizontal = adaptive.pad).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(category.label, fontSize = adaptive.sectionTitle, fontWeight = FontWeight.Bold, color = White)
            Text("See all ›", fontSize = sp(13), color = NetflixRed, modifier = Modifier.clickable {})
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding        = PaddingValues(horizontal = adaptive.pad),
            horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap)
        ) {
            items(category.items) { item ->
                when (item) {
                    is com.streaming.sunne.data.model.ContentItem.Movie -> {
                        MovieCard(item.data, adaptive) { onMovieClick(item.data) }
                    }
                    is com.streaming.sunne.data.model.ContentItem.Series -> {
                        // For now, show series as cards too (skip or implement SeriesCard later)
                        // MovieCard works because Series has similar properties
                    }
                    is com.streaming.sunne.data.model.ContentItem.LiveStream -> {
                        // Skip live streams in regular rows or implement LiveCard
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieCard(movie: MovieResponse, adaptive: Adaptive, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.08f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Box(
        modifier = Modifier
            .width(adaptive.cardW)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { focused = it.isFocused }
    ) {
        // Thumbnail
        AsyncImage(
            model              = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier           = Modifier.fillMaxWidth().height(adaptive.cardH),
            contentScale       = ContentScale.Crop
        )
        // Bottom gradient
        Box(
            modifier = Modifier.fillMaxWidth().height(adaptive.cardH)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, DarkBackground.copy(alpha = 0.75f))
                ))
        )
        // Info strip
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
        // HOT badge
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
//  Profile Menu Overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileMenuOverlay(
    username: String,
    email: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onDismiss() }
    ) {
        // The card — stops click propagation
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

                // User info header
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
                                Modifier.clip(RoundedCornerShape(3.dp)).background(NetflixRed.copy(0.25f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("ADMIN", fontSize = sp(9), color = NetflixRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = White.copy(alpha = 0.08f))

                MenuRow(Icons.Default.Person, "My Profile") { onDismiss() }
                MenuRow(Icons.Default.History, "Watch History") { onDismiss() }
                MenuRow(Icons.Default.Settings, "Settings") { onDismiss() }

                Divider(
                    color    = White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Logout — highlighted in red
                MenuRow(
                    icon    = Icons.Default.Logout,
                    label   = "Sign Out",
                    tint    = NetflixRed,
                    onClick = onLogout
                )
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
private fun MetaDivider() {
    Box(Modifier.size(3.dp).clip(CircleShape).background(LightGray.copy(alpha = 0.4f)))
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
            Text(message, fontSize = sp(14), color = LightGray, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(24.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NetflixRed)
                .clickable { onRetry() }.padding(horizontal = 32.dp, vertical = 12.dp)) {
                Text("Retry", fontSize = sp(15), fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

private operator fun androidx.compose.ui.unit.TextUnit.times(f: Float) =
    sp(this.value * f)
private operator fun androidx.compose.ui.unit.TextUnit.times(f: Double) =
    sp(this.value * f.toFloat())


//package com.streaming.sunne.ui.screens
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.*
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.*
//import androidx.compose.ui.focus.*
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.*
//import coil.compose.AsyncImage
//import com.streaming.sunne.data.model.MovieCategory
//import com.streaming.sunne.data.model.MovieResponse
//import com.streaming.sunne.ui.adaptive.*
//import com.streaming.sunne.ui.theme.*
//import com.streaming.sunne.viewmodel.AuthViewModel
//import com.streaming.sunne.viewmodel.HomeUiState
//import com.streaming.sunne.viewmodel.MovieViewModel
//
//// ─────────────────────────────────────────────────────────────────────────────
////  HomeScreen
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//fun HomeScreen(
//    viewModel:     MovieViewModel,
//    authViewModel: AuthViewModel,
//    onMovieClick:  (MovieResponse) -> Unit,
//    onLiveClick:   () -> Unit,
//    onLogout:      () -> Unit
//) {
//    val state    by viewModel.homeState.collectAsState()
//    val user     by authViewModel.currentUser.collectAsState()
//    val adaptive  = rememberAdaptive()
//    var activeNav        by remember { mutableStateOf("Home") }
//    var showProfileMenu  by remember { mutableStateOf(false) }
//
//    val navItems = listOf("Home", "Movies", "Live")
//
//    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
//
//        // ── Main scrollable content ───────────────────────────────────────
//        Column(modifier = Modifier.fillMaxSize()) {
//
//            // TV top nav lives outside the scroll so it stays pinned
//            if (!adaptive.useBottomNav) {
//                TvTopBar(
//                    navItems      = navItems,
//                    activeNav     = activeNav,
//                    adaptive      = adaptive,
//                    username      = user?.username ?: "A",
//                    onNavSelected = { item ->
//                        activeNav = item
//                        if (item == "Live") onLiveClick()
//                    },
//                    onProfileClick = { showProfileMenu = true }
//                )
//            }
//
//            // Scrollable body
//            when (val s = state) {
//                is HomeUiState.Loading -> HomeLoadingScreen()
//                is HomeUiState.Error   -> HomeErrorScreen(s.message, viewModel::retry)
//                is HomeUiState.Success -> {
//                    val categories    = s.categories
//                    val hero          = categories.firstOrNull()?.movies?.firstOrNull()
//
//                    Column(
//                        modifier = Modifier
//                            .weight(1f)
//                            .verticalScroll(rememberScrollState())
//                    ) {
//                        // Hero banner
//                        if (hero != null) {
//                            HeroBanner(
//                                movie         = hero,
//                                adaptive      = adaptive,
//                                username      = user?.username ?: "A",
//                                showPhoneBar  = adaptive.useBottomNav,
//                                onPlayClick   = { onMovieClick(hero) },
//                                onProfileClick = { showProfileMenu = true }
//                            )
//                        }
//
//                        Spacer(Modifier.height(adaptive.rowGap))
//
//                        // Content rows
//                        categories.forEach { cat ->
//                            CategoryRow(cat, adaptive, onMovieClick)
//                            Spacer(Modifier.height(adaptive.rowGap))
//                        }
//
//                        // Bottom padding for nav bar
//                        if (adaptive.useBottomNav) Spacer(Modifier.height(64.dp))
//                    }
//                }
//            }
//        }
//
//        // ── Phone / Tablet bottom nav ─────────────────────────────────────
//        if (adaptive.useBottomNav) {
//            PhoneBottomNav(
//                modifier  = Modifier.align(Alignment.BottomCenter),
//                navItems  = navItems,
//                activeNav = activeNav,
//                onSelect  = { item ->
//                    activeNav = item
//                    if (item == "Live") onLiveClick()
//                }
//            )
//        }
//
//        // ── Profile dropdown ──────────────────────────────────────────────
//        AnimatedVisibility(
//            visible = showProfileMenu,
//            enter   = fadeIn(),
//            exit    = fadeOut()
//        ) {
//            ProfileMenuOverlay(
//                username  = user?.username ?: "",
//                email     = user?.email ?: "",
//                isAdmin   = user?.isAdmin ?: false,
//                onDismiss = { showProfileMenu = false },
//                onLogout  = {
//                    showProfileMenu = false
//                    authViewModel.logout()
//                    onLogout()
//                }
//            )
//        }
//    }
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
////  TV Top Bar
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//private fun TvTopBar(
//    navItems: List<String>,
//    activeNav: String,
//    adaptive: Adaptive,
//    username: String,
//    onNavSelected: (String) -> Unit,
//    onProfileClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Brush.verticalGradient(
//                listOf(DarkBackground, DarkBackground.copy(alpha = 0f))
//            ))
//            .padding(horizontal = adaptive.pad, vertical = 24.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // Logo
//        Text(
//            "SUNNE",
//            fontSize      = adaptive.logo,
//            fontWeight    = FontWeight.Black,
//            color         = NetflixRed,
//            letterSpacing = sp(4)
//        )
//
//        Spacer(Modifier.width(52.dp))
//
//        // Nav items
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(36.dp),
//            verticalAlignment     = Alignment.CenterVertically
//        ) {
//            navItems.forEach { item ->
//                TvNavItem(
//                    label      = item,
//                    isSelected = item == activeNav,
//                    isLive     = item == "Live",
//                    onClick    = { onNavSelected(item) }
//                )
//            }
//        }
//
//        Spacer(Modifier.weight(1f))
//
//        // Right icons
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(20.dp),
//            verticalAlignment     = Alignment.CenterVertically
//        ) {
//            Icon(
//                Icons.Default.Search, "Search",
//                tint     = White,
//                modifier = Modifier.size(24.dp).clickable {}
//            )
//            Avatar(
//                letter  = username.first().uppercaseChar().toString(),
//                size    = 38.dp,
//                onClick = onProfileClick
//            )
//        }
//    }
//}
//
//@Composable
//private fun TvNavItem(label: String, isSelected: Boolean, isLive: Boolean, onClick: () -> Unit) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier            = Modifier.clickable { onClick() }
//    ) {
//        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
//            if (isLive) {
//                val inf = rememberInfiniteTransition(label = "dot")
//                val a by inf.animateFloat(0.5f, 1f,
//                    infiniteRepeatable(tween(700), RepeatMode.Reverse), "a")
//                Box(Modifier.size(7.dp).clip(CircleShape).background(NetflixRed.copy(alpha = a)))
//            }
//            Text(
//                label,
//                fontSize   = sp(16),
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
//                color      = if (isSelected) White else White.copy(alpha = 0.65f)
//            )
//        }
//        if (isSelected) {
//            Spacer(Modifier.height(4.dp))
//            Box(Modifier.width(20.dp).height(2.dp).background(NetflixRed, RoundedCornerShape(1.dp)))
//        }
//    }
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
////  Phone bottom nav
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//private fun PhoneBottomNav(
//    modifier: Modifier,
//    navItems: List<String>,
//    activeNav: String,
//    onSelect: (String) -> Unit
//) {
//    val iconMap = mapOf(
//        "Home"   to Icons.Default.Home,
//        "Movies" to Icons.Default.Movie,
//        "Live"   to Icons.Default.LiveTv
//    )
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(60.dp)
//            .background(Color(0xFF080808))
//            .border(BorderStroke(0.5.dp, White.copy(alpha = 0.08f)),
//                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
//        horizontalArrangement = Arrangement.SpaceAround,
//        verticalAlignment     = Alignment.CenterVertically
//    ) {
//        navItems.forEach { item ->
//            val selected = item == activeNav
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier            = Modifier
//                    .weight(1f)
//                    .fillMaxHeight()
//                    .clickable { onSelect(item) },
//                verticalArrangement = Arrangement.Center
//            ) {
//                val icon = iconMap[item] ?: Icons.Default.Home
//                val tint = if (selected) White else LightGray.copy(alpha = 0.45f)
//
//                if (item == "Live" && !selected) {
//                    val inf = rememberInfiniteTransition(label = "live")
//                    val a by inf.animateFloat(0.4f, 0.9f,
//                        infiniteRepeatable(tween(800), RepeatMode.Reverse), "a")
//                    Icon(icon, item, tint = NetflixRed.copy(alpha = a), modifier = Modifier.size(22.dp))
//                } else {
//                    Icon(icon, item, tint = tint, modifier = Modifier.size(22.dp))
//                }
//
//                Spacer(Modifier.height(3.dp))
//                Text(
//                    item,
//                    fontSize   = sp(10),
//                    color      = if (selected) White else LightGray.copy(alpha = 0.45f),
//                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
//                )
//                // Active indicator dot
//                if (selected) {
//                    Spacer(Modifier.height(2.dp))
//                    Box(Modifier.size(4.dp).clip(CircleShape).background(NetflixRed))
//                }
//            }
//        }
//    }
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
////  Hero Banner
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//private fun HeroBanner(
//    movie: MovieResponse,
//    adaptive: Adaptive,
//    username: String,
//    showPhoneBar: Boolean,
//    onPlayClick: () -> Unit,
//    onProfileClick: () -> Unit
//) {
//    Box(modifier = Modifier.fillMaxWidth().height(adaptive.bannerH)) {
//        AsyncImage(
//            model              = movie.bannerUrl,
//            contentDescription = null,
//            modifier           = Modifier.fillMaxSize(),
//            contentScale       = ContentScale.Crop
//        )
//        // Horizontal gradient (left overlay for text)
//        Box(Modifier.fillMaxSize().background(
//            Brush.horizontalGradient(
//                listOf(
//                    DarkBackground.copy(alpha = if (adaptive.isTV) 0.95f else 0.88f),
//                    DarkBackground.copy(alpha = 0.3f),
//                    Color.Transparent
//                )
//            )
//        ))
//        // Bottom fade to background
//        Box(Modifier.fillMaxSize().background(
//            Brush.verticalGradient(
//                listOf(Color.Transparent, DarkBackground),
//                startY = adaptive.bannerH.value * 0.55f
//            )
//        ))
//
//        // Phone mini-topbar (logo + avatar)
//        if (showPhoneBar) {
//            Row(
//                modifier = Modifier.fillMaxWidth()
//                    .padding(horizontal = adaptive.pad, vertical = 14.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment     = Alignment.CenterVertically
//            ) {
//                Text("SUNNE", fontSize = adaptive.logo, fontWeight = FontWeight.Black,
//                    color = NetflixRed, letterSpacing = sp(4))
//                Avatar(
//                    letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
//                    size    = 32.dp,
//                    onClick = onProfileClick
//                )
//            }
//        }
//
//        // Hero text content
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(start = adaptive.pad, bottom = if (adaptive.isTV) 48.dp else 24.dp)
//                .fillMaxWidth(adaptive.heroTextFraction)
//        ) {
//            // Featured label
//            Row(
//                verticalAlignment     = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(6.dp)
//            ) {
//                Box(Modifier.size(6.dp).clip(CircleShape).background(NetflixRed))
//                Text("FEATURED", fontSize = sp(11), fontWeight = FontWeight.Bold,
//                    color = NetflixRed, letterSpacing = sp(3))
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            Text(
//                movie.title,
//                fontSize   = adaptive.heroTitle,
//                fontWeight = FontWeight.Black,
//                color      = White,
//                maxLines   = 2,
//                overflow   = TextOverflow.Ellipsis,
//                lineHeight = adaptive.heroTitle * 1.12f
//            )
//
//            Spacer(Modifier.height(8.dp))
//
//            // Meta
//            Row(
//                verticalAlignment     = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                movie.releaseYear?.let { Text("$it", fontSize = adaptive.caption, color = LightGray) }
//                movie.formattedDuration()?.let {
//                    MetaDivider()
//                    Text(it, fontSize = adaptive.caption, color = LightGray)
//                }
//                movie.rating?.let {
//                    MetaDivider()
//                    RatingChip(it, adaptive)
//                }
//            }
//
//            // Description — skip on phone to save space
//            if (!adaptive.isPhone) {
//                Spacer(Modifier.height(10.dp))
//                movie.description?.let {
//                    Text(
//                        it,
//                        fontSize   = adaptive.body,
//                        color      = White.copy(alpha = 0.78f),
//                        maxLines   = 2,
//                        overflow   = TextOverflow.Ellipsis,
//                        lineHeight = adaptive.body * 1.5f
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(16.dp))
//
//            // Buttons
//            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//                HeroPlayButton(onClick = onPlayClick, adaptive = adaptive)
//                if (!adaptive.isPhone) HeroInfoButton(adaptive)
//            }
//        }
//    }
//}
//
//@Composable
//private fun HeroPlayButton(onClick: () -> Unit, adaptive: Adaptive) {
//    var focused by remember { mutableStateOf(false) }
//    val scale by animateFloatAsState(if (focused) 1.05f else 1f, spring())
//    Row(
//        modifier = Modifier
//            .scale(scale)
//            .clip(RoundedCornerShape(6.dp))
//            .background(White)
//            .clickable { onClick() }
//            .onFocusChanged { focused = it.isFocused }
//            .padding(
//                horizontal = if (adaptive.isPhone) 18.dp else 26.dp,
//                vertical   = if (adaptive.isPhone) 9.dp  else 12.dp
//            ),
//        verticalAlignment     = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(6.dp)
//    ) {
//        Icon(Icons.Default.PlayArrow, null, tint = Color.Black,
//            modifier = Modifier.size(if (adaptive.isPhone) 18.dp else 24.dp))
//        Text("Play", fontSize = if (adaptive.isPhone) sp(14) else sp(16),
//            fontWeight = FontWeight.Bold, color = Color.Black)
//    }
//}
//
//@Composable
//private fun HeroInfoButton(adaptive: Adaptive) {
//    Row(
//        modifier = Modifier
//            .clip(RoundedCornerShape(6.dp))
//            .background(White.copy(alpha = 0.18f))
//            .clickable {}
//            .padding(horizontal = 22.dp, vertical = 12.dp),
//        verticalAlignment     = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(6.dp)
//    ) {
//        Icon(Icons.Default.Info, null, tint = White, modifier = Modifier.size(20.dp))
//        Text("Info", fontSize = sp(16), fontWeight = FontWeight.SemiBold, color = White)
//    }
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
////  Category Row + Card
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//fun CategoryRow(category: MovieCategory, adaptive: Adaptive, onMovieClick: (MovieResponse) -> Unit) {
//    Column {
//        Row(
//            modifier              = Modifier.padding(horizontal = adaptive.pad).fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment     = Alignment.CenterVertically
//        ) {
//            Text(category.label, fontSize = adaptive.sectionTitle, fontWeight = FontWeight.Bold, color = White)
//            Text("See all ›", fontSize = sp(13), color = NetflixRed, modifier = Modifier.clickable {})
//        }
//        Spacer(Modifier.height(10.dp))
//        LazyRow(
//            contentPadding        = PaddingValues(horizontal = adaptive.pad),
//            horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap)
//        ) {
//            items(category.movies) { movie ->
//                MovieCard(movie, adaptive) { onMovieClick(movie) }
//            }
//        }
//    }
//}
//
//@Composable
//private fun MovieCard(movie: MovieResponse, adaptive: Adaptive, onClick: () -> Unit) {
//    var focused by remember { mutableStateOf(false) }
//    val scale by animateFloatAsState(
//        if (focused) 1.08f else 1f,
//        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
//    )
//    Box(
//        modifier = Modifier
//            .width(adaptive.cardW)
//            .scale(scale)
//            .clip(RoundedCornerShape(8.dp))
//            .border(
//                width = if (focused) 2.dp else 0.dp,
//                color = if (focused) NetflixRed else Color.Transparent,
//                shape = RoundedCornerShape(8.dp)
//            )
//            .clickable { onClick() }
//            .onFocusChanged { focused = it.isFocused }
//    ) {
//        // Thumbnail
//        AsyncImage(
//            model              = movie.thumbnailUrl,
//            contentDescription = movie.title,
//            modifier           = Modifier.fillMaxWidth().height(adaptive.cardH),
//            contentScale       = ContentScale.Crop
//        )
//        // Bottom gradient
//        Box(
//            modifier = Modifier.fillMaxWidth().height(adaptive.cardH)
//                .background(Brush.verticalGradient(
//                    listOf(Color.Transparent, DarkBackground.copy(alpha = 0.75f))
//                ))
//        )
//        // Info strip
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .fillMaxWidth()
//                .background(CardBackground)
//                .padding(horizontal = 8.dp, vertical = 5.dp)
//        ) {
//            Text(movie.title, fontSize = adaptive.caption, fontWeight = FontWeight.SemiBold,
//                color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
//            movie.formattedDuration()?.let {
//                Text(it, fontSize = sp(10), color = LightGray)
//            }
//        }
//        // HOT badge
//        if (movie.isTrending) {
//            Box(
//                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
//                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFFE55300))
//                    .padding(horizontal = 6.dp, vertical = 2.dp)
//            ) {
//                Text("HOT", fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
//            }
//        }
//    }
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
////  Profile Menu Overlay
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//private fun ProfileMenuOverlay(
//    username: String,
//    email: String,
//    isAdmin: Boolean,
//    onDismiss: () -> Unit,
//    onLogout: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black.copy(alpha = 0.55f))
//            .clickable { onDismiss() }
//    ) {
//        // The card — stops click propagation
//        Card(
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .padding(top = 70.dp, end = 20.dp)
//                .width(220.dp)
//                .clickable { /* absorb */ },
//            backgroundColor = Color(0xFF1C1C1C),
//            shape           = RoundedCornerShape(14.dp),
//            elevation       = 20.dp
//        ) {
//            Column(modifier = Modifier.padding(vertical = 8.dp)) {
//
//                // User info header
//                Row(
//                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
//                    verticalAlignment     = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    Avatar(
//                        letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
//                        size    = 40.dp,
//                        onClick = {}
//                    )
//                    Column {
//                        Text(username, fontSize = sp(14), fontWeight = FontWeight.Bold, color = White)
//                        Text(email, fontSize = sp(11), color = LightGray, maxLines = 1,
//                            overflow = TextOverflow.Ellipsis)
//                        if (isAdmin) {
//                            Spacer(Modifier.height(2.dp))
//                            Box(
//                                Modifier.clip(RoundedCornerShape(3.dp)).background(NetflixRed.copy(0.25f))
//                                    .padding(horizontal = 5.dp, vertical = 1.dp)
//                            ) {
//                                Text("ADMIN", fontSize = sp(9), color = NetflixRed, fontWeight = FontWeight.Bold)
//                            }
//                        }
//                    }
//                }
//
//                Divider(color = White.copy(alpha = 0.08f))
//
//                MenuRow(Icons.Default.Person, "My Profile") { onDismiss() }
//                MenuRow(Icons.Default.History, "Watch History") { onDismiss() }
//                MenuRow(Icons.Default.Settings, "Settings") { onDismiss() }
//
//                Divider(
//                    color    = White.copy(alpha = 0.08f),
//                    modifier = Modifier.padding(vertical = 4.dp)
//                )
//
//                // Logout — highlighted in red
//                MenuRow(
//                    icon    = Icons.Default.Logout,
//                    label   = "Sign Out",
//                    tint    = NetflixRed,
//                    onClick = onLogout
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun MenuRow(
//    icon: androidx.compose.ui.graphics.vector.ImageVector,
//    label: String,
//    tint: Color = White,
//    onClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//            .padding(horizontal = 16.dp, vertical = 11.dp),
//        verticalAlignment     = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
//        Text(label, fontSize = sp(14), color = tint)
//    }
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
////  Reusable atoms
//// ─────────────────────────────────────────────────────────────────────────────
//@Composable
//fun Avatar(letter: String, size: Dp, onClick: () -> Unit) {
//    Box(
//        modifier = Modifier.size(size).clip(CircleShape)
//            .background(NetflixRed).clickable { onClick() },
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            letter,
//            fontSize   = sp(size.value * 0.38f),
//            fontWeight = FontWeight.Bold,
//            color      = White
//        )
//    }
//}
//
//@Composable
//private fun MetaDivider() {
//    Box(Modifier.size(3.dp).clip(CircleShape).background(LightGray.copy(alpha = 0.4f)))
//}
//
//@Composable
//private fun RatingChip(label: String, adaptive: Adaptive) {
//    Box(
//        Modifier.clip(RoundedCornerShape(3.dp))
//            .border(1.dp, LightGray.copy(0.5f), RoundedCornerShape(3.dp))
//            .padding(horizontal = 5.dp, vertical = 2.dp)
//    ) {
//        Text(label, fontSize = adaptive.caption, color = LightGray, fontWeight = FontWeight.Bold)
//    }
//}
//
//@Composable
//private fun HomeLoadingScreen() {
//    Box(Modifier.fillMaxSize(), Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(50.dp))
//            Spacer(Modifier.height(14.dp))
//            Text("Loading Sunne...", fontSize = sp(16), color = LightGray)
//        }
//    }
//}
//
//@Composable
//private fun HomeErrorScreen(message: String, onRetry: () -> Unit) {
//    Box(Modifier.fillMaxSize(), Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Icon(Icons.Default.ErrorOutline, null, tint = NetflixRed, modifier = Modifier.size(52.dp))
//            Spacer(Modifier.height(12.dp))
//            Text(message, fontSize = sp(14), color = LightGray, modifier = Modifier.padding(horizontal = 32.dp))
//            Spacer(Modifier.height(24.dp))
//            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NetflixRed)
//                .clickable { onRetry() }.padding(horizontal = 32.dp, vertical = 12.dp)) {
//                Text("Retry", fontSize = sp(15), fontWeight = FontWeight.Bold, color = White)
//            }
//        }
//    }
//}
//
//private operator fun androidx.compose.ui.unit.TextUnit.times(f: Float) =
//    sp(this.value * f)
//private operator fun androidx.compose.ui.unit.TextUnit.times(f: Double) =
//    sp(this.value * f.toFloat())

//----------------------------------------------------
//package com.streaming.sunne.ui.screens
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.*
//import androidx.compose.ui.focus.*
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.*
//import coil.compose.AsyncImage
//import com.streaming.sunne.data.model.MovieCategory
//import com.streaming.sunne.data.model.MovieResponse
//import com.streaming.sunne.ui.theme.*
//import com.streaming.sunne.viewmodel.HomeUiState
//import com.streaming.sunne.viewmodel.MovieViewModel
//
//private val navItems = listOf("Home", "TV Shows", "Movies", "My List", "Live")
//
//// ─── HomeScreen ────────────────────────────────────────────────────────────────
//@Composable
//fun HomeScreen(
//    viewModel: MovieViewModel,
//    onMovieClick: (MovieResponse) -> Unit,
//    onLiveClick: () -> Unit
//) {
//    val homeState by viewModel.homeState.collectAsState()
//    var selectedNav by remember { mutableStateOf("Home") }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground)
//    ) {
//        when (val state = homeState) {
//            is HomeUiState.Loading -> FullScreenLoader()
//            is HomeUiState.Error   -> ErrorContent(
//                message = state.message,
//                onRetry = viewModel::retry
//            )
//            is HomeUiState.Success -> {
//                val categories    = state.categories
//                val featuredMovie = categories.firstOrNull()?.movies?.firstOrNull()
//
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .verticalScroll(rememberScrollState())
//                ) {
//                    if (featuredMovie != null) {
//                        HeroBanner(
//                            movie       = featuredMovie,
//                            navItems    = navItems,
//                            selectedNav = selectedNav,
//                            onNavSelected = { item ->
//                                selectedNav = item
//                                if (item == "Live") onLiveClick()
//                            },
//                            onPlayClick = { onMovieClick(featuredMovie) }
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    categories.forEach { category ->
//                        ContentRow(category = category, onMovieClick = onMovieClick)
//                        Spacer(modifier = Modifier.height(32.dp))
//                    }
//
//                    Spacer(modifier = Modifier.height(40.dp))
//                }
//            }
//        }
//    }
//}
//
//// ─── Hero Banner ───────────────────────────────────────────────────────────────
//@Composable
//private fun HeroBanner(
//    movie: MovieResponse,
//    navItems: List<String>,
//    selectedNav: String,
//    onNavSelected: (String) -> Unit,
//    onPlayClick: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(560.dp)
//    ) {
//        AsyncImage(
//            model             = movie.bannerUrl,
//            contentDescription = null,
//            modifier          = Modifier.fillMaxSize(),
//            contentScale      = ContentScale.Crop
//        )
//
//        // Left + bottom gradient overlays
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.horizontalGradient(
//                        colors = listOf(
//                            DarkBackground.copy(alpha = 0.95f),
//                            DarkBackground.copy(alpha = 0.4f),
//                            Color.Transparent
//                        )
//                    )
//                )
//        )
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors  = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.3f), DarkBackground),
//                        startY  = 200f
//                    )
//                )
//        )
//
//        TopNavBar(
//            navItems      = navItems,
//            selectedNav   = selectedNav,
//            onNavSelected = onNavSelected
//        )
//
//        // Hero content
//        Column(
//            modifier = Modifier
//                .align(Alignment.CenterStart)
//                .padding(start = 56.dp, top = 80.dp)
//                .widthIn(max = 500.dp)
//        ) {
//            Text(
//                text          = "✦  FEATURED",
//                fontSize      = 13.sp,
//                fontWeight    = FontWeight.Bold,
//                color         = NetflixRed,
//                letterSpacing = 3.sp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text       = movie.title,
//                fontSize   = 52.sp,
//                fontWeight = FontWeight.Black,
//                color      = White,
//                lineHeight = 56.sp,
//                maxLines   = 2,
//                overflow   = TextOverflow.Ellipsis
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Meta row
//            Row(
//                verticalAlignment   = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                movie.releaseYear?.let {
//                    Text("$it", fontSize = 14.sp, color = LightGray)
//                    MetaDivider()
//                }
//                movie.formattedDuration()?.let {
//                    Text(it, fontSize = 14.sp, color = LightGray)
//                    MetaDivider()
//                }
//                movie.rating?.let {
//                    QualityBadge(it)          // e.g. "PG-13"
//                }
//                if (movie.genreNames().isNotEmpty()) {
//                    MetaDivider()
//                    Text(
//                        text     = movie.genreNames(),
//                        fontSize = 13.sp,
//                        color    = LightGray.copy(alpha = 0.8f),
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            movie.description?.let {
//                Text(
//                    text       = it,
//                    fontSize   = 15.sp,
//                    color      = White.copy(alpha = 0.8f),
//                    maxLines   = 3,
//                    overflow   = TextOverflow.Ellipsis,
//                    lineHeight = 22.sp
//                )
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//                PlayButton(onClick = onPlayClick)
//                MoreInfoButton()
//            }
//        }
//    }
//}
//
//// ─── Top Nav Bar ───────────────────────────────────────────────────────────────
//@Composable
//private fun TopNavBar(
//    navItems: List<String>,
//    selectedNav: String,
//    onNavSelected: (String) -> Unit
//) {
//    Row(
//        modifier            = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 56.dp, vertical = 28.dp),
//        verticalAlignment   = Alignment.CenterVertically
//    ) {
//        Text(
//            text          = "SUNNE",
//            fontSize      = 36.sp,
//            fontWeight    = FontWeight.Black,
//            color         = NetflixRed,
//            letterSpacing = 4.sp
//        )
//
//        Spacer(modifier = Modifier.width(56.dp))
//
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(36.dp),
//            verticalAlignment     = Alignment.CenterVertically
//        ) {
//            navItems.forEach { item ->
//                NavItem(
//                    label      = item,
//                    isSelected = item == selectedNav,
//                    isLive     = item == "Live",
//                    onClick    = { onNavSelected(item) }
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.weight(1f))
//
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(20.dp),
//            verticalAlignment     = Alignment.CenterVertically
//        ) {
//            Icon(Icons.Default.Search, contentDescription = "Search",
//                tint = White, modifier = Modifier.size(24.dp).clickable {})
//            Icon(Icons.Default.Notifications, contentDescription = "Notifications",
//                tint = White, modifier = Modifier.size(24.dp).clickable {})
//            Box(
//                modifier        = Modifier.size(36.dp).clip(CircleShape).background(NetflixRed),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("A", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
//            }
//        }
//    }
//}
//
//// ─── Nav Item ──────────────────────────────────────────────────────────────────
//@Composable
//private fun NavItem(
//    label: String,
//    isSelected: Boolean,
//    isLive: Boolean = false,
//    onClick: () -> Unit
//) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier            = Modifier.clickable { onClick() }
//    ) {
//        Row(
//            verticalAlignment     = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(6.dp)
//        ) {
//            if (isLive) {
//                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NetflixRed))
//            }
//            Text(
//                text       = label,
//                fontSize   = 16.sp,
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
//                color      = if (isSelected) White else White.copy(alpha = 0.7f)
//            )
//        }
//        if (isSelected) {
//            Spacer(modifier = Modifier.height(4.dp))
//            Box(modifier = Modifier.width(20.dp).height(2.dp).background(NetflixRed, RoundedCornerShape(1.dp)))
//        }
//    }
//}
//
//// ─── Play Button ───────────────────────────────────────────────────────────────
//@Composable
//private fun PlayButton(onClick: () -> Unit) {
//    var isFocused by remember { mutableStateOf(false) }
//    val scale by animateFloatAsState(if (isFocused) 1.06f else 1f, tween(150))
//    Row(
//        modifier = Modifier
//            .scale(scale)
//            .clip(RoundedCornerShape(6.dp))
//            .background(White)
//            .clickable { onClick() }
//            .onFocusChanged { isFocused = it.isFocused }
//            .padding(horizontal = 28.dp, vertical = 13.dp),
//        verticalAlignment     = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Icon(Icons.Default.PlayArrow, null, tint = DarkBackground, modifier = Modifier.size(26.dp))
//        Text("Play", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
//    }
//}
//
//// ─── More Info Button ──────────────────────────────────────────────────────────
//@Composable
//private fun MoreInfoButton() {
//    Row(
//        modifier = Modifier
//            .clip(RoundedCornerShape(6.dp))
//            .background(White.copy(alpha = 0.25f))
//            .clickable { }
//            .padding(horizontal = 24.dp, vertical = 13.dp),
//        verticalAlignment     = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Icon(Icons.Default.Info, null, tint = White, modifier = Modifier.size(22.dp))
//        Text("More Info", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = White)
//    }
//}
//
//// ─── Content Row (Carousel) ────────────────────────────────────────────────────
//@Composable
//fun ContentRow(
//    category: MovieCategory,
//    onMovieClick: (MovieResponse) -> Unit
//) {
//    Column {
//        Row(
//            modifier              = Modifier.padding(horizontal = 56.dp).fillMaxWidth(),
//            verticalAlignment     = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(category.label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
//            Text("See all ›", fontSize = 14.sp, color = NetflixRed, modifier = Modifier.clickable { })
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        LazyRow(
//            contentPadding        = PaddingValues(horizontal = 56.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            items(category.movies) { movie ->
//                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
//            }
//        }
//    }
//}
//
//// ─── Movie Card ────────────────────────────────────────────────────────────────
//@Composable
//private fun MovieCard(
//    movie: MovieResponse,
//    onClick: () -> Unit
//) {
//    var isFocused by remember { mutableStateOf(false) }
//    val scale by animateFloatAsState(
//        targetValue  = if (isFocused) 1.1f else 1f,
//        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
//    )
//
//    Box(
//        modifier = Modifier
//            .width(220.dp)
//            .scale(scale)
//            .clip(RoundedCornerShape(8.dp))
//            .clickable { onClick() }
//            .onFocusChanged { isFocused = it.isFocused }
//            .border(
//                width  = if (isFocused) 2.5.dp else 0.dp,
//                color  = if (isFocused) NetflixRed else Color.Transparent,
//                shape  = RoundedCornerShape(8.dp)
//            )
//    ) {
//        AsyncImage(
//            model              = movie.thumbnailUrl,
//            contentDescription = movie.title,
//            modifier           = Modifier.fillMaxWidth().height(130.dp),
//            contentScale       = ContentScale.Crop
//        )
//
//        // Bottom gradient
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(130.dp)
//                .align(Alignment.TopStart)
//                .background(Brush.verticalGradient(
//                    colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.7f))
//                ))
//        )
//
//        // Info bar
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .background(CardBackground)
//                .fillMaxWidth()
//                .padding(horizontal = 10.dp, vertical = 8.dp)
//        ) {
//            Text(
//                text      = movie.title,
//                fontSize  = 13.sp,
//                fontWeight = FontWeight.SemiBold,
//                color     = White,
//                maxLines  = 1,
//                overflow  = TextOverflow.Ellipsis
//            )
//            movie.formattedDuration()?.let {
//                Text(it, fontSize = 11.sp, color = LightGray)
//            }
//        }
//
//        // Trending badge
//        if (movie.isTrending) {
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopStart)
//                    .padding(8.dp)
//                    .clip(RoundedCornerShape(4.dp))
//                    .background(Color(0xFFFF6B00))
//                    .padding(horizontal = 8.dp, vertical = 3.dp)
//            ) {
//                Text("TRENDING", fontSize = 9.sp, fontWeight = FontWeight.Black, color = White)
//            }
//        }
//    }
//}
//
//// ─── Shared helpers ────────────────────────────────────────────────────────────
//@Composable
//private fun QualityBadge(label: String) {
//    Box(
//        modifier = Modifier
//            .clip(RoundedCornerShape(3.dp))
//            .border(1.dp, LightGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
//            .padding(horizontal = 6.dp, vertical = 2.dp)
//    ) {
//        Text(label, fontSize = 11.sp, color = LightGray, fontWeight = FontWeight.Bold)
//    }
//}
//
//@Composable
//private fun MetaDivider() {
//    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(LightGray.copy(alpha = 0.5f)))
//}
//
//@Composable
//private fun FullScreenLoader() {
//    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(56.dp))
//            Spacer(Modifier.height(16.dp))
//            Text("Loading Sunne...", fontSize = 18.sp, color = LightGray)
//        }
//    }
//}
//
//@Composable
//private fun ErrorContent(message: String, onRetry: () -> Unit) {
//    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text("Oops!", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NetflixRed)
//            Spacer(Modifier.height(12.dp))
//            Text(message, fontSize = 16.sp, color = LightGray)
//            Spacer(Modifier.height(28.dp))
//            Box(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(NetflixRed)
//                    .clickable { onRetry() }
//                    .padding(horizontal = 36.dp, vertical = 14.dp)
//            ) {
//                Text("Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
//            }
//        }
//    }
//}
