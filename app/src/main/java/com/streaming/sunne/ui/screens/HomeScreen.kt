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
import androidx.compose.runtime.saveable.rememberSaveable
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.CONTINUE_WATCHING
import com.streaming.sunne.data.model.CatalogSource
import com.streaming.sunne.data.model.ContentItem
import com.streaming.sunne.data.model.ContentRow
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
    onItemClick:   (ContentItem) -> Unit,
    onPlayMovie:   (MovieResponse) -> Unit,
    onLiveClick:   () -> Unit,
    onSearchClick: () -> Unit,
    onMyListClick: () -> Unit,
    onSeeAll:      (CatalogSource) -> Unit,
    onLogout:      () -> Unit
) {
    val state    by viewModel.homeState.collectAsState()
    val user     by authViewModel.currentUser.collectAsState()
    val opening  by viewModel.opening.collectAsState()
    val openError by viewModel.openError.collectAsState()
    val adaptive  = rememberAdaptive()
    var activeNav        by rememberSaveable { mutableStateOf("Home") }
    var showProfileMenu  by remember { mutableStateOf(false) }
    val cardFocus = rememberCardFocus()

    val navItems = listOf("Home", "Movies", "Series", "My List", "Live")
    val selectNav: (String) -> Unit = { item ->
        when (item) {
            "Live"    -> onLiveClick()
            "My List" -> onMyListClick()
            else      -> activeNav = item
        }
    }

    LaunchedEffect(Unit) { viewModel.ensureHomeLoaded() }

    // Keep Continue Watching fresh after returning from a player
    OnResumeEffect(Unit, skipFirst = true) { viewModel.refreshContinueWatching() }

    LaunchedEffect(openError) {
        if (openError != null) { kotlinx.coroutines.delay(3_500); viewModel.clearOpenError() }
    }

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
                    onNavSelected = selectNav,
                    onSearchClick = onSearchClick,
                    onProfileClick = { showProfileMenu = true }
                )
            }

            // Scrollable body
            if (activeNav == "Movies" || activeNav == "Series") {
                if (adaptive.useBottomNav) {
                    PhoneTopBar(adaptive, user?.username ?: "A", onSearchClick) { showProfileMenu = true }
                }
                CatalogContent(
                    source      = if (activeNav == "Movies") CatalogSource.Movies("Movies") else CatalogSource.Series("Series"),
                    onItemClick = onItemClick,
                    modifier    = Modifier.weight(1f),
                    bottomInset = if (adaptive.useBottomNav) 64.dp else 0.dp
                )
            } else when (val s = state) {
                is HomeUiState.Loading -> HomeLoadingScreen()
                is HomeUiState.Error   -> HomeErrorScreen(s.message, viewModel::retry)
                is HomeUiState.Success -> {
                    val categories = s.categories
                    val hero = categories
                        .filter { it.label != CONTINUE_WATCHING }
                        .flatMap { it.items }
                        .firstNotNullOfOrNull { (it as? ContentItem.Movie)?.data }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (hero != null) {
                            HeroBanner(
                                movie         = hero,
                                adaptive      = adaptive,
                                username      = user?.username ?: "A",
                                showPhoneBar  = adaptive.useBottomNav,
                                onPlayClick   = { onPlayMovie(hero) },
                                onInfoClick   = { onItemClick(ContentItem.Movie(hero)) },
                                onSearchClick = onSearchClick,
                                onProfileClick = { showProfileMenu = true },
                                requestInitialFocus = adaptive.isTV && cardFocus.lastKey == null
                            )
                        } else if (adaptive.useBottomNav) {
                            PhoneTopBar(adaptive, user?.username ?: "A", onSearchClick) { showProfileMenu = true }
                        }

                        Spacer(Modifier.height(adaptive.rowGap))

                        if (categories.isEmpty()) {
                            Text(
                                "Nothing here yet.",
                                fontSize = sp(15), color = LightGray,
                                modifier = Modifier.padding(horizontal = adaptive.pad, vertical = 32.dp)
                            )
                        }

                        categories.forEach { cat ->
                            CategoryRow(cat, adaptive, onItemClick, onSeeAll, cardFocus)
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
                onSelect  = selectNav
            )
        }

        if (opening != null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(48.dp))
            }
        }

        openError?.let { message ->
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (adaptive.useBottomNav) 76.dp else 32.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(message, fontSize = sp(13), color = White)
            }
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
                    onLogout()
                }
            )
        }
    }
}

@Composable
private fun PhoneTopBar(adaptive: Adaptive, username: String, onSearchClick: () -> Unit, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = adaptive.pad, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("SUNE", fontSize = adaptive.logo, fontWeight = FontWeight.Black,
            color = NetflixRed, letterSpacing = sp(4), modifier = Modifier.weight(1f))
        SearchIconButton(onSearchClick)
        Spacer(Modifier.width(8.dp))
        Avatar(
            letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
            size    = 32.dp,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun SearchIconButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(38.dp).clip(CircleShape)
            .background(if (focused) White.copy(alpha = 0.2f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Search, "Search", tint = White, modifier = Modifier.size(24.dp))
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
    onSearchClick: () -> Unit,
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

        SearchIconButton(onSearchClick)
        Spacer(Modifier.width(16.dp))
        Avatar(
            letter  = username.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
            size    = 38.dp,
            onClick = onProfileClick
        )
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
        "Series" to Icons.Default.VideoLibrary,
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
    onInfoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    requestInitialFocus: Boolean = false
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
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("SUNE", fontSize = adaptive.logo, fontWeight = FontWeight.Black,
                    color = NetflixRed, letterSpacing = sp(4), modifier = Modifier.weight(1f))
                SearchIconButton(onSearchClick)
                Spacer(Modifier.width(8.dp))
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
                HeroPlayButton(onClick = onPlayClick, adaptive = adaptive, requestInitialFocus = requestInitialFocus)
                HeroInfoButton(onClick = onInfoClick, adaptive = adaptive)
            }
        }
    }
}

@Composable
private fun HeroPlayButton(onClick: () -> Unit, adaptive: Adaptive, requestInitialFocus: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, spring())
    val requester = remember { FocusRequester() }
    if (requestInitialFocus) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }
    Row(
        modifier = Modifier
            .focusRequester(requester)
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
private fun HeroInfoButton(onClick: () -> Unit, adaptive: Adaptive) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(White.copy(alpha = 0.18f))
            .clickable { onClick() }
            .padding(
                horizontal = if (adaptive.isPhone) 16.dp else 22.dp,
                vertical   = if (adaptive.isPhone) 9.dp  else 12.dp
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Info, null, tint = White,
            modifier = Modifier.size(if (adaptive.isPhone) 16.dp else 20.dp))
        Text("Info", fontSize = if (adaptive.isPhone) sp(14) else sp(16),
            fontWeight = FontWeight.SemiBold, color = White)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category Row + Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryRow(
    category: ContentRow,
    adaptive: Adaptive,
    onItemClick: (ContentItem) -> Unit,
    onSeeAll: (CatalogSource) -> Unit = {},
    focus: CardFocus? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = adaptive.pad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                category.label,
                fontSize   = adaptive.sectionTitle,
                fontWeight = FontWeight.Bold,
                color      = White,
                modifier   = Modifier.weight(1f)
            )
            category.source?.let { source ->
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onSeeAll(source) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("See all", fontSize = sp(13), color = LightGray, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight, null, tint = LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding        = PaddingValues(horizontal = adaptive.pad),
            horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap)
        ) {
            items(category.items, key = { "${category.label}/${it.stableKey}" }) { item ->
                ContentCard(
                    item     = item,
                    adaptive = adaptive,
                    focusKey = "${category.label}/${item.stableKey}",
                    focus    = focus,
                    onClick  = { onItemClick(item) }
                )
            }
        }
    }
}

/** Identifies a card across recompositions so focus can return to it on TV. */
val ContentItem.stableKey: String
    get() = when (this) {
        is ContentItem.Movie          -> "m${data.id}"
        is ContentItem.Series         -> "s${data.id}"
        is ContentItem.ContinueSeries -> "cs${data.seriesId}"
        is ContentItem.LiveStream     -> "l${data.id}"
    }

/**
 * Remembers which card had focus so it can be focused again when the user comes back
 * (TV remotes otherwise lose their place). [restoreKey] survives navigation.
 */
class CardFocus(initial: String?) {
    var lastKey: String? = initial
        private set
    /** Set only when restored from saved state, and cleared after the first restore. */
    private var pending: String? = initial

    fun onFocused(key: String) { lastKey = key }

    fun consumeRestore(key: String): Boolean {
        if (pending != key) return false
        pending = null
        return true
    }

    fun shouldRestore(key: String): Boolean = pending == key

    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<CardFocus, String>(
            save = { it.lastKey.orEmpty() },
            restore = { CardFocus(it.ifEmpty { null }) }
        )
    }
}

@Composable
fun rememberCardFocus(): CardFocus = rememberSaveable(saver = CardFocus.Saver) { CardFocus(null) }

@Composable
fun ContentCard(
    item: ContentItem,
    adaptive: Adaptive,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusKey: String? = null,
    focus: CardFocus? = null
) {
    when (item) {
        is ContentItem.Movie -> PosterCard(
            title    = item.data.title,
            imageUrl = item.data.thumbnailUrl,
            subtitle = item.data.formattedDuration(),
            badge    = if (item.data.isTrending && item.progress == null) "HOT" else null,
            progress = item.progress,
            adaptive = adaptive,
            modifier = modifier,
            focusKey = focusKey,
            focus    = focus,
            onClick  = onClick
        )
        is ContentItem.Series -> PosterCard(
            title    = item.data.title,
            imageUrl = item.data.thumbnailUrl,
            subtitle = item.data.seasons.takeIf { it.isNotEmpty() }?.let { item.data.formattedSeasonCount() } ?: "Series",
            badge    = "SERIES",
            adaptive = adaptive,
            modifier = modifier,
            focusKey = focusKey,
            focus    = focus,
            onClick  = onClick
        )
        is ContentItem.ContinueSeries -> PosterCard(
            title    = item.data.seriesTitle,
            imageUrl = item.data.seriesBackdrop ?: item.data.seriesPoster,
            subtitle = item.data.resumeEpisodeTitle?.let { "${item.data.episodeLabel} · $it" } ?: item.data.episodeLabel,
            progress = item.data.watchPercentage / 100f,
            adaptive = adaptive,
            modifier = modifier,
            focusKey = focusKey,
            focus    = focus,
            onClick  = onClick
        )
        is ContentItem.LiveStream -> PosterCard(
            title      = item.data.title,
            imageUrl   = item.data.thumbnailUrl,
            subtitle   = "${item.data.formattedViewerCount()} watching",
            badge      = "LIVE",
            badgeColor = NetflixRed,
            adaptive   = adaptive,
            modifier   = modifier,
            focusKey   = focusKey,
            focus      = focus,
            onClick    = onClick
        )
    }
}

@Composable
private fun PosterCard(
    title: String,
    imageUrl: String?,
    subtitle: String?,
    adaptive: Adaptive,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = Color(0xFFE55300),
    progress: Float? = null,
    focusKey: String? = null,
    focus: CardFocus? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.08f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val focusRequester = remember { FocusRequester() }
    if (focusKey != null && focus != null && focus.shouldRestore(focusKey)) {
        LaunchedEffect(focusKey) {
            withFrameNanos { }
            if (focus.consumeRestore(focusKey)) runCatching { focusRequester.requestFocus() }
        }
    }
    Box(
        modifier = modifier
            .width(adaptive.cardW)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused && focusKey != null) focus?.onFocused(focusKey) }
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
    ) {
        // Thumbnail
        AsyncImage(
            model              = imageUrl,
            contentDescription = title,
            modifier           = Modifier.fillMaxWidth().height(adaptive.cardH).background(CardBackground),
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
        ) {
            progress?.let { WatchProgressBar(it) }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(title, fontSize = adaptive.caption, fontWeight = FontWeight.SemiBold,
                    color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(it, fontSize = sp(10), color = LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        badge?.let {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    .clip(RoundedCornerShape(4.dp)).background(badgeColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(it, fontSize = sp(9), fontWeight = FontWeight.Black, color = White)
            }
        }
    }
}

/** Thin red bar showing how much of a title has been watched (0..1). */
@Composable
fun WatchProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(3.dp).background(White.copy(alpha = 0.2f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(NetflixRed))
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
