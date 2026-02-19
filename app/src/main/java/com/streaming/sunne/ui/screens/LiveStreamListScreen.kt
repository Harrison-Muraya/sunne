package com.streaming.sunne.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.LiveStreamPublicResponse
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.LiveStreamViewModel
import com.streaming.sunne.viewmodel.UiState

@Composable
fun LiveStreamListScreen(
    viewModel: LiveStreamViewModel,
    onStreamClick: (LiveStreamPublicResponse) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.streams.collectAsState()
    val ad = rememberAdaptive()

    LaunchedEffect(Unit) {
        viewModel.loadStreams()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {

        when (val s = state) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(52.dp))
                }
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = NetflixRed, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(s.message, fontSize = sp(14), color = LightGray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(NetflixRed)
                            .clickable { viewModel.loadStreams() }
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        Text("Retry", fontSize = sp(15), fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }

            is UiState.Success -> {
                val liveStreams = s.data.filter { it.isLive }

                if (liveStreams.isEmpty()) {
                    // No live streams
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LiveTv, null, tint = LightGray, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(20.dp))
                        Text("No Live Streams", fontSize = sp(24), fontWeight = FontWeight.Bold, color = White)
                        Spacer(Modifier.height(8.dp))
                        Text("Check back later for live content", fontSize = sp(14), color = LightGray)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = ad.pad, vertical = if (ad.isTV) 28.dp else 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape)
                                        .background(Color.Black.copy(0.4f))
                                        .clickable { onBackClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LiveStreamDot()
                                        Text("LIVE TV", fontSize = if (ad.isTV) sp(28) else sp(20),
                                            fontWeight = FontWeight.Black, color = White)
                                    }
                                    Text("${liveStreams.size} streams active", fontSize = sp(12), color = LightGray)
                                }
                            }
                        }

                        // Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (ad.isTV) 3 else if (ad.isTablet) 2 else 1),
                            contentPadding = PaddingValues(horizontal = ad.pad, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(if (ad.isTV) 20.dp else 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (ad.isTV) 20.dp else 16.dp)
                        ) {
                            items(liveStreams) { stream ->
                                LiveStreamCard(stream, ad) { onStreamClick(stream) }
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun LiveStreamCard(stream: LiveStreamPublicResponse, ad: Adaptive, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, spring())

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .background(CardBackground)
            .clickable { onClick() }
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (ad.isTV) 180.dp else if (ad.isTablet) 140.dp else 200.dp)
            ) {
                if (stream.thumbnailUrl != null) {
                    AsyncImage(
                        stream.thumbnailUrl, null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), Alignment.Center) {
                        Icon(Icons.Default.LiveTv, null, tint = LightGray, modifier = Modifier.size(48.dp))
                    }
                }

                // Live badge
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NetflixRed)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val inf = rememberInfiniteTransition(label = "dot")
                    val alpha by inf.animateFloat(0.5f, 1f,
                        infiniteRepeatable(tween(700), RepeatMode.Reverse), "a")
                    Box(Modifier.size(7.dp).clip(CircleShape).background(White.copy(alpha)))
                    Text("LIVE", fontSize = sp(11), fontWeight = FontWeight.Black, color = White)
                }

                // Viewer count
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(0.7f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, tint = White, modifier = Modifier.size(14.dp))
                    Text(stream.formattedViewerCount(), fontSize = sp(11), color = White)
                }
            }

            // Info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    stream.title,
                    fontSize = if (ad.isTV) sp(16) else sp(14),
                    fontWeight = FontWeight.Bold,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                stream.description?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        fontSize = sp(12),
                        color = LightGray.copy(0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStreamDot() {
    val inf = rememberInfiniteTransition(label = "live")
    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "a")
    Box(Modifier.size(8.dp).clip(CircleShape).background(NetflixRed.copy(a)))
}

private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Int) = sp(value.toFloat())

//package com.streaming.sunne.ui.screens
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import com.streaming.sunne.data.model.LiveStreamPublicResponse
//import com.streaming.sunne.ui.adaptive.*
//import com.streaming.sunne.ui.theme.*
//import com.streaming.sunne.viewmodel.LiveStreamViewModel
//import com.streaming.sunne.viewmodel.UiState
//
//@Composable
//fun LiveStreamListScreen(
//    viewModel: LiveStreamViewModel,
//    onStreamClick: (LiveStreamPublicResponse) -> Unit,
//    onBackClick: () -> Unit
//) {
//    val state by viewModel.streams.collectAsState()
//    val ad = rememberAdaptive()
//
//    LaunchedEffect(Unit) {
//        viewModel.loadStreams()
//    }
//
//    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
//
//        when (val s = state) {
//            is UiState.Loading -> {
//                Box(Modifier.fillMaxSize(), Alignment.Center) {
//                    CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(52.dp))
//                }
//            }
//
//            is UiState.Error -> {
//                Column(
//                    modifier = Modifier.fillMaxSize(),
//                    verticalArrangement = Arrangement.Center,
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Icon(Icons.Default.ErrorOutline, null, tint = NetflixRed, modifier = Modifier.size(64.dp))
//                    Spacer(Modifier.height(16.dp))
//                    Text(s.message, fontSize = sp(14), color = LightGray, textAlign = TextAlign.Center)
//                    Spacer(Modifier.height(24.dp))
//                    Box(
//                        Modifier.clip(RoundedCornerShape(8.dp)).background(NetflixRed)
//                            .clickable { viewModel.loadStreams() }
//                            .padding(horizontal = 32.dp, vertical = 12.dp)
//                    ) {
//                        Text("Retry", fontSize = sp(15), fontWeight = FontWeight.Bold, color = White)
//                    }
//                }
//            }
//
//            is UiState.Success -> {
//                val liveStreams = s.data.filter { it.isLive }
//
//                if (liveStreams.isEmpty()) {
//                    // No live streams
//                    Column(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Icon(Icons.Default.LiveTv, null, tint = LightGray, modifier = Modifier.size(80.dp))
//                        Spacer(Modifier.height(20.dp))
//                        Text("No Live Streams", fontSize = sp(24), fontWeight = FontWeight.Bold, color = White)
//                        Spacer(Modifier.height(8.dp))
//                        Text("Check back later for live content", fontSize = sp(14), color = LightGray)
//                    }
//                } else {
//                    Column(modifier = Modifier.fillMaxSize()) {
//                        // Header
//                        Row(
//                            modifier = Modifier.fillMaxWidth()
//                                .padding(horizontal = ad.pad, vertical = if (ad.isTV) 28.dp else 20.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                                Box(
//                                    Modifier.size(40.dp).clip(CircleShape)
//                                        .background(Color.Black.copy(0.4f))
//                                        .clickable { onBackClick() },
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(20.dp))
//                                }
//                                Column {
//                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                        LiveDot()
//                                        Text("LIVE TV", fontSize = if (ad.isTV) sp(28) else sp(20),
//                                            fontWeight = FontWeight.Black, color = White)
//                                    }
//                                    Text("${liveStreams.size} streams active", fontSize = sp(12), color = LightGray)
//                                }
//                            }
//                        }
//
//                        // Grid
//                        LazyVerticalGrid(
//                            columns = GridCells.Fixed(if (ad.isTV) 3 else if (ad.isTablet) 2 else 1),
//                            contentPadding = PaddingValues(horizontal = ad.pad, vertical = 12.dp),
//                            verticalArrangement = Arrangement.spacedBy(if (ad.isTV) 20.dp else 16.dp),
//                            horizontalArrangement = Arrangement.spacedBy(if (ad.isTV) 20.dp else 16.dp)
//                        ) {
//                            items(liveStreams) { stream ->
//                                LiveStreamCard(stream, ad) { onStreamClick(stream) }
//                            }
//                        }
//                    }
//                }
//            }
//
//            else -> {}
//        }
//    }
//}
//
//@Composable
//private fun LiveStreamCard(stream: LiveStreamPublicResponse, ad: Adaptive, onClick: () -> Unit) {
//    var focused by remember { mutableStateOf(false) }
//    val scale by animateFloatAsState(if (focused) 1.05f else 1f, spring())
//
//    Box(
//        modifier = Modifier
//            .scale(scale)
//            .clip(RoundedCornerShape(10.dp))
//            .border(
//                width = if (focused) 2.dp else 0.dp,
//                color = if (focused) NetflixRed else Color.Transparent,
//                shape = RoundedCornerShape(10.dp)
//            )
//            .background(CardBackground)
//            .clickable { onClick() }
//    ) {
//        Column {
//            // Thumbnail
//            Box(
//                modifier = Modifier.fillMaxWidth()
//                    .height(if (ad.isTV) 180.dp else if (ad.isTablet) 140.dp else 200.dp)
//            ) {
//                if (stream.thumbnailUrl != null) {
//                    AsyncImage(
//                        stream.thumbnailUrl, null,
//                        Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                } else {
//                    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), Alignment.Center) {
//                        Icon(Icons.Default.LiveTv, null, tint = LightGray, modifier = Modifier.size(48.dp))
//                    }
//                }
//
//                // Live badge
//                Row(
//                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
//                        .clip(RoundedCornerShape(20.dp))
//                        .background(NetflixRed)
//                        .padding(horizontal = 10.dp, vertical = 5.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(5.dp)
//                ) {
//                    val inf = rememberInfiniteTransition(label = "dot")
//                    val alpha by inf.animateFloat(0.5f, 1f,
//                        infiniteRepeatable(tween(700), RepeatMode.Reverse), "a")
//                    Box(Modifier.size(7.dp).clip(CircleShape).background(White.copy(alpha)))
//                    Text("LIVE", fontSize = sp(11), fontWeight = FontWeight.Black, color = White)
//                }
//
//                // Viewer count
//                Row(
//                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
//                        .clip(RoundedCornerShape(20.dp))
//                        .background(Color.Black.copy(0.7f))
//                        .padding(horizontal = 10.dp, vertical = 5.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(5.dp)
//                ) {
//                    Icon(Icons.Default.Visibility, null, tint = White, modifier = Modifier.size(14.dp))
//                    Text(stream.formattedViewerCount(), fontSize = sp(11), color = White)
//                }
//            }
//
//            // Info
//            Column(modifier = Modifier.padding(12.dp)) {
//                Text(
//                    stream.title,
//                    fontSize = if (ad.isTV) sp(16) else sp(14),
//                    fontWeight = FontWeight.Bold,
//                    color = White,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                stream.description?.let {
//                    Spacer(Modifier.height(4.dp))
//                    Text(
//                        it,
//                        fontSize = sp(12),
//                        color = LightGray.copy(0.7f),
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun LiveDot() {
//    val inf = rememberInfiniteTransition(label = "live")
//    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "a")
//    Box(Modifier.size(8.dp).clip(CircleShape).background(NetflixRed.copy(a)))
//}
//
//private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
//private fun sp(value: Int) = sp(value.toFloat())
