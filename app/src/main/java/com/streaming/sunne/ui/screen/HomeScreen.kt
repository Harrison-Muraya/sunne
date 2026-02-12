@file:JvmName("HomeScreenKt")

package com.streaming.sunne.ui.screen

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
import com.streaming.sunne.data.model.Stream
import com.streaming.sunne.data.model.StreamCategory
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.StreamUiState
import com.streaming.sunne.viewmodel.StreamViewModel

// ─── Navigation items ──────────────────────────────────────────────────────────
private val navItems = listOf("Home", "TV Shows", "Movies", "My List", "Live")

// ─── HomeScreen ────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    viewModel: StreamViewModel,
    onStreamClick: (Stream) -> Unit,
    onLiveClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNav by remember { mutableStateOf("Home") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (uiState) {
            is StreamUiState.Loading -> FullScreenLoader()
            is StreamUiState.Error  -> ErrorContent(
                message = (uiState as StreamUiState.Error).message,
                onRetry = viewModel::retry
            )
            is StreamUiState.Success -> {
                val categories = (uiState as StreamUiState.Success).categories
                val featuredStream = categories.firstOrNull()?.streams?.firstOrNull()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero banner (first featured stream)
                    if (featuredStream != null) {
                        HeroBanner(
                            stream = featuredStream,
                            navItems = navItems,
                            selectedNav = selectedNav,
                            onNavSelected = { item ->
                                selectedNav = item
                                if (item == "Live") onLiveClick()
                            },
                            onPlayClick = { onStreamClick(featuredStream) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Content carousels
                    categories.forEach { category ->
                        ContentRow(
                            category = category,
                            onStreamClick = onStreamClick
                        )
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
    stream: Stream,
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
        // Background banner image
        AsyncImage(
            model = stream.banner ?: stream.thumbnail,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark vignette overlay - left and bottom
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
                        colors = listOf(
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.3f),
                            DarkBackground
                        ),
                        startY = 200f
                    )
                )
        )

        // Top navigation bar
        TopNavBar(
            navItems = navItems,
            selectedNav = selectedNav,
            onNavSelected = onNavSelected
        )

        // Hero content
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp, top = 80.dp)
                .widthIn(max = 500.dp)
        ) {
            // Welcome tag
            Text(
                text = "✦  FEATURED",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NetflixRed,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = stream.title,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = White,
                lineHeight = 56.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Meta info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stream.rating?.let {
                    Text(
                        text = "★ $it",
                        fontSize = 14.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    MetaDivider()
                }
                stream.release_year?.let {
                    Text(text = "$it", fontSize = 14.sp, color = LightGray)
                    MetaDivider()
                }
                Text(
                    text = stream.duration ?: "HD",
                    fontSize = 14.sp,
                    color = LightGray
                )
                MetaDivider()
                QualityBadge(stream.quality ?: "HD")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            stream.description?.let {
                Text(
                    text = it,
                    fontSize = 15.sp,
                    color = White.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Play button
                PlayButton(onClick = onPlayClick)

                // More Info button
                MoreInfoButton()
            }
        }
    }
}

// ─── Top Navigation Bar ────────────────────────────────────────────────────────
@Composable
private fun TopNavBar(
    navItems: List<String>,
    selectedNav: String,
    onNavSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Text(
            text = "SUNE",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = NetflixRed,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.width(56.dp))

        // Nav items
        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                NavItem(
                    label = item,
                    isSelected = item == selectedNav,
                    isLive = item == "Live",
                    onClick = { onNavSelected(item) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search",
                tint = White, modifier = Modifier.size(24.dp).clickable {})
            Icon(Icons.Default.Notifications, contentDescription = "Notifications",
                tint = White, modifier = Modifier.size(24.dp).clickable {})
            // Profile avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NetflixRed),
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
    var isFocused by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLive) {
                // Live indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NetflixRed)
                )
            }
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) White else White.copy(alpha = 0.7f)
            )
        }

        // Selected underline
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(NetflixRed, RoundedCornerShape(1.dp))
            )
        }
    }
}

// ─── Play Button ───────────────────────────────────────────────────────────────
@Composable
private fun PlayButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(150)
    )
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(White)
            .clickable { onClick() }
            .padding(horizontal = 28.dp, vertical = 13.dp)
            .onFocusChanged { isFocused = it.isFocused },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null,
            tint = DarkBackground, modifier = Modifier.size(26.dp))
        Text(
            text = "Play",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBackground
        )
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Info, contentDescription = null,
            tint = White, modifier = Modifier.size(22.dp))
        Text(
            text = "More Info",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = White
        )
    }
}

// ─── Content Row (Carousel) ────────────────────────────────────────────────────
@Composable
fun ContentRow(
    category: StreamCategory,
    onStreamClick: (Stream) -> Unit
) {
    Column {
        // Section header
        Row(
            modifier = Modifier
                .padding(horizontal = 56.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.category,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Text(
                text = "See all ›",
                fontSize = 14.sp,
                color = NetflixRed,
                modifier = Modifier.clickable { }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(category.streams) { stream ->
                StreamCard(stream = stream, onClick = { onStreamClick(stream) })
            }
        }
    }
}

// ─── Stream Card ───────────────────────────────────────────────────────────────
@Composable
private fun StreamCard(
    stream: Stream,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
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
                width = if (isFocused) 2.5.dp else 0.dp,
                color = if (isFocused) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // Thumbnail
        AsyncImage(
            model = stream.thumbnail,
            contentDescription = stream.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentScale = ContentScale.Crop
        )

        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.7f))
                    )
                )
        )

        // Title at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(CardBackground)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = stream.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            stream.duration?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = LightGray
                )
            }
        }

        // Live badge
        if (stream.is_live == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NetflixRed)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = White)
            }
        }
    }
}

// ─── Quality Badge ─────────────────────────────────────────────────────────────
@Composable
private fun QualityBadge(quality: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, LightGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(quality, fontSize = 11.sp, color = LightGray, fontWeight = FontWeight.Bold)
    }
}

// ─── Meta Divider ──────────────────────────────────────────────────────────────
@Composable
private fun MetaDivider() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(LightGray.copy(alpha = 0.5f))
    )
}

// ─── Full Screen Loader ────────────────────────────────────────────────────────
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

// ─── Error Content ─────────────────────────────────────────────────────────────
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