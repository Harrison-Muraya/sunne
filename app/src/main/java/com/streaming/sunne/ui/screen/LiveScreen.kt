package com.streaming.sunne.ui.screen

import androidx.compose.animation.*
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
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.StreamUiState
import com.streaming.sunne.viewmodel.StreamViewModel

// ─── LiveScreen ────────────────────────────────────────────────────────────────
@Composable
fun LiveScreen(
    viewModel: StreamViewModel,
    onChannelPlay: (Stream) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedChannel by remember { mutableStateOf<Stream?>(null) }
    var previewChannel by remember { mutableStateOf<Stream?>(null) }

    // Extract live streams
    val liveStreams = when (uiState) {
        is StreamUiState.Success -> {
            val all = (uiState as StreamUiState.Success).categories
                .flatMap { it.streams }
            // Prioritise streams flagged is_live, fallback to all
            all.filter { it.is_live == true }.ifEmpty { all }
        }
        else -> emptyList()
    }

    // Initialise preview with first channel
    LaunchedEffect(liveStreams) {
        if (previewChannel == null && liveStreams.isNotEmpty()) {
            previewChannel = liveStreams.first()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ── Left: Channel List ──────────────────────────────────────────
            ChannelListPanel(
                streams = liveStreams,
                selectedChannel = selectedChannel ?: previewChannel,
                onChannelHover = { previewChannel = it },
                onChannelSelect = { stream ->
                    selectedChannel = stream
                    onChannelPlay(stream)
                },
                onBackClick = onBackClick
            )

            // ── Right: Preview Panel ────────────────────────────────────────
            ChannelPreviewPanel(
                stream = previewChannel,
                onPlayClick = {
                    previewChannel?.let { onChannelPlay(it) }
                }
            )
        }
    }
}

// ─── Channel List Panel ────────────────────────────────────────────────────────
@Composable
private fun ChannelListPanel(
    streams: List<Stream>,
    selectedChannel: Stream?,
    onChannelHover: (Stream) -> Unit,
    onChannelSelect: (Stream) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(420.dp)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkBackground.copy(alpha = 0.97f)
                    )
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.1f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "SUNE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = NetflixRed,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "Live TV",
                        fontSize = 13.sp,
                        color = LightGray
                    )
                }
            }

            // Live indicator
            LivePulseBadge()
        }

        // Divider
        Divider(color = White.copy(alpha = 0.08f), thickness = 1.dp)

        // Channel count
        Text(
            text = "${streams.size} CHANNELS AVAILABLE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LightGray.copy(alpha = 0.6f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )

        // Channel list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(streams) { index, stream ->
                ChannelListItem(
                    stream = stream,
                    index = index + 1,
                    isSelected = stream.id == selectedChannel?.id,
                    onHover = { onChannelHover(stream) },
                    onSelect = { onChannelSelect(stream) }
                )
            }
        }
    }
}

// ─── Channel List Item ─────────────────────────────────────────────────────────
@Composable
private fun ChannelListItem(
    stream: Stream,
    index: Int,
    isSelected: Boolean,
    onHover: () -> Unit,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgAlpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isFocused  -> 0.6f
            else       -> 0f
        },
        animationSpec = tween(150)
    )

    LaunchedEffect(isFocused) { if (isFocused) onHover() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) NetflixRed.copy(alpha = 0.18f)
                else White.copy(alpha = bgAlpha * 0.06f)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) NetflixRed.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Channel number
        Text(
            text = String.format("%02d", index),
            fontSize = 13.sp,
            color = if (isSelected) NetflixRed else LightGray.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )

        // Thumbnail
        Box(
            modifier = Modifier
                .size(64.dp, 42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CardBackground)
        ) {
            AsyncImage(
                model = stream.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NetflixRed.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) White else White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stream.category,
                fontSize = 12.sp,
                color = if (isSelected) NetflixRed.copy(alpha = 0.8f) else LightGray.copy(alpha = 0.6f)
            )
        }

        // Live dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isSelected) NetflixRed else LightGray.copy(alpha = 0.3f))
        )
    }
}

// ─── Channel Preview Panel ─────────────────────────────────────────────────────
@Composable
private fun ChannelPreviewPanel(
    stream: Stream?,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (stream == null) {
            // Empty state
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a channel", fontSize = 20.sp, color = LightGray)
            }
            return@Box
        }

        // Full background image
        AsyncImage(
            model = stream.banner ?: stream.thumbnail,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 300f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.45f))
        )

        // Preview content at center
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LIVE badge large
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NetflixRed)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(White)
                    )
                    Text(
                        text = "LIVE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = White,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Channel title
            Text(
                text = stream.title,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Category
            Text(
                text = stream.category.uppercase(),
                fontSize = 13.sp,
                color = NetflixRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            // Description
            stream.description?.let {
                Text(
                    text = it,
                    fontSize = 15.sp,
                    color = White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 460.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Watch Now button
            WatchNowButton(onClick = onPlayClick)
        }
    }
}

// ─── Watch Now Button ──────────────────────────────────────────────────────────
@Composable
private fun WatchNowButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(NetflixRed)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 40.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = White,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Watch Now",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
    }
}

// ─── Live Pulse Badge ──────────────────────────────────────────────────────────
@Composable
private fun LivePulseBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NetflixRed.copy(alpha = 0.15f))
            .border(1.dp, NetflixRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(NetflixRed.copy(alpha = alpha))
        )
        Text(
            "LIVE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NetflixRed,
            letterSpacing = 1.sp
        )
    }
}