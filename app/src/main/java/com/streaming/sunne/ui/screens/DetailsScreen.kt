package com.streaming.sunne.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import coil.compose.rememberAsyncImagePainter
import com.streaming.sunne.data.model.MovieResponse
import com.streaming.sunne.ui.theme.*

// ─── DetailsScreen ─────────────────────────────────────────────────────────────
@Composable
fun DetailsScreen(
    movie: MovieResponse,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Full-bleed background
        Image(
            painter          = rememberAsyncImagePainter(movie.bannerUrl),
            contentDescription = null,
            modifier         = Modifier.fillMaxSize(),
            contentScale     = ContentScale.Crop
        )

        // Gradient overlays
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DarkBackground.copy(alpha = 0.98f),
                        DarkBackground.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
        )
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground.copy(alpha = 0.3f)))

        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(36.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.12f))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = White, modifier = Modifier.size(22.dp))
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 580.dp)
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            // Genre chip
            if (movie.genreNames().isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NetflixRed.copy(alpha = 0.2f))
                        .border(1.dp, NetflixRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text          = movie.genreNames().uppercase(),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = NetflixRed,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Title
            Text(
                text       = movie.title,
                fontSize   = 54.sp,
                fontWeight = FontWeight.Black,
                color      = White,
                lineHeight = 58.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Meta row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                movie.releaseYear?.let {
                    Text("$it", fontSize = 14.sp, color = LightGray)
                    MetaDot()
                }
                movie.formattedDuration()?.let {
                    Text(it, fontSize = 14.sp, color = LightGray)
                    MetaDot()
                }
                movie.rating?.let {
                    RatingTag(it)
                    MetaDot()
                }
                movie.videoFiles.firstOrNull()?.quality?.let {
                    QualityTag(it)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text     = movie.language,
                fontSize = 13.sp,
                color    = LightGray.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            movie.description?.let {
                Text(
                    text       = it,
                    fontSize   = 16.sp,
                    color      = White.copy(alpha = 0.85f),
                    lineHeight = 26.sp,
                    maxLines   = 5,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            // Director / Cast
            if (!movie.director.isNullOrBlank() || !movie.cast.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                movie.director?.takeIf { it.isNotBlank() }?.let {
                    MetaRow(label = "Director", value = it)
                }
                movie.cast?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    MetaRow(label = "Cast", value = it)
                }
            }

            movie.country?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                MetaRow(label = "Country", value = it)
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailsPlayButton(onClick = onPlayClick)
                AddToListButton()
                RateButton()
            }
        }
    }
}

// ─── Details Play Button ───────────────────────────────────────────────────────
@Composable
private fun DetailsPlayButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.06f else 1f, tween(150))
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(White)
            .clickable { onClick() }
            .padding(horizontal = 32.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = DarkBackground, modifier = Modifier.size(28.dp))
        Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
    }
}

@Composable
private fun AddToListButton() {
    Box(
        modifier = Modifier
            .size(50.dp).clip(CircleShape)
            .background(White.copy(alpha = 0.15f))
            .border(2.dp, White.copy(alpha = 0.4f), CircleShape)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "Add to List", tint = White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RateButton() {
    Box(
        modifier = Modifier
            .size(50.dp).clip(CircleShape)
            .background(White.copy(alpha = 0.15f))
            .border(2.dp, White.copy(alpha = 0.4f), CircleShape)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ThumbUp, "Rate", tint = White, modifier = Modifier.size(22.dp))
    }
}

// ─── Meta helpers ──────────────────────────────────────────────────────────────
@Composable
private fun MetaRow(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = 14.sp, color = LightGray.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 14.sp, color = LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetaDot() {
    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(LightGray.copy(alpha = 0.4f)))
}

@Composable
private fun RatingTag(rating: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, LightGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(rating, fontSize = 11.sp, color = LightGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QualityTag(quality: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(NetflixRed.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(quality, fontSize = 11.sp, color = NetflixRed, fontWeight = FontWeight.Bold)
    }
}


//package com.streaming.sunne.ui.screen
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.*
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.*
//import coil.compose.rememberAsyncImagePainter
//import com.streaming.sunne.data.model.Stream
//import com.streaming.sunne.ui.theme.*
//
//// ─── DetailsScreen ─────────────────────────────────────────────────────────────
//@Composable
//fun DetailsScreen(
//    stream: Stream,
//    onPlayClick: (Stream) -> Unit,
//    onBackClick: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground)
//    ) {
//        // Full-bleed background
//        Image(
//            painter = rememberAsyncImagePainter(stream.banner ?: stream.thumbnail),
//            contentDescription = null,
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
//
//        // Layered gradient overlays
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.horizontalGradient(
//                        colors = listOf(
//                            DarkBackground.copy(alpha = 0.98f),
//                            DarkBackground.copy(alpha = 0.6f),
//                            Color.Transparent
//                        )
//                    )
//                )
//        )
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(DarkBackground.copy(alpha = 0.3f))
//        )
//
//        // Back button (top left)
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(36.dp)
//                .size(44.dp)
//                .clip(CircleShape)
//                .background(White.copy(alpha = 0.12f))
//                .clickable { onBackClick() },
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                Icons.Default.ArrowBack,
//                contentDescription = "Back",
//                tint = White,
//                modifier = Modifier.size(22.dp)
//            )
//        }
//
//        // Main content
//        Column(
//            modifier = Modifier
//                .fillMaxHeight()
//                .widthIn(max = 580.dp)
//                .align(Alignment.CenterStart)
//                .padding(start = 56.dp)
//                .verticalScroll(rememberScrollState()),
//            verticalArrangement = Arrangement.Center
//        ) {
//            // Category chip
//            Box(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(4.dp))
//                    .background(NetflixRed.copy(alpha = 0.2f))
//                    .border(1.dp, NetflixRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
//                    .padding(horizontal = 10.dp, vertical = 4.dp)
//            ) {
//                Text(
//                    text = stream.category.uppercase(),
//                    fontSize = 11.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = NetflixRed,
//                    letterSpacing = 2.sp
//                )
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Title
//            Text(
//                text = stream.title,
//                fontSize = 54.sp,
//                fontWeight = FontWeight.Black,
//                color = White,
//                lineHeight = 58.sp,
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis
//            )
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Meta info row
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(14.dp)
//            ) {
//                stream.rating?.let {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Icon(
//                            Icons.Default.Star,
//                            contentDescription = null,
//                            tint = Color(0xFFFFD700),
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Text(
//                            "$it / 10",
//                            fontSize = 14.sp,
//                            color = Color(0xFFFFD700),
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//                    MetaDot()
//                }
//                stream.release_year?.let {
//                    Text("$it", fontSize = 14.sp, color = LightGray)
//                    MetaDot()
//                }
//                stream.duration?.let {
//                    Text(it, fontSize = 14.sp, color = LightGray)
//                    MetaDot()
//                }
//                stream.quality?.let {
//                    QualityTag(it)
//                }
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Language
//            stream.language?.let {
//                Text(
//                    text = it,
//                    fontSize = 13.sp,
//                    color = LightGray.copy(alpha = 0.6f)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Description
//            stream.description?.let {
//                Text(
//                    text = it,
//                    fontSize = 16.sp,
//                    color = White.copy(alpha = 0.85f),
//                    lineHeight = 26.sp,
//                    maxLines = 5,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//
//            // Director / Cast
//            if (!stream.director.isNullOrBlank() || !stream.cast.isNullOrBlank()) {
//                Spacer(modifier = Modifier.height(24.dp))
//                Divider(color = White.copy(alpha = 0.1f))
//                Spacer(modifier = Modifier.height(16.dp))
//
//                stream.director?.takeIf { it.isNotBlank() }?.let {
//                    MetaRow(label = "Director", value = it)
//                }
//                stream.cast?.takeIf { it.isNotBlank() }?.let {
//                    Spacer(modifier = Modifier.height(8.dp))
//                    MetaRow(label = "Cast", value = it)
//                }
//            }
//
//            Spacer(modifier = Modifier.height(36.dp))
//
//            // Action buttons
//            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//                // Play
//                DetailsPlayButton(onClick = { onPlayClick(stream) })
//                // Add to list
//                AddToListButton()
//                // Rate
//                RateButton()
//            }
//        }
//    }
//}
//
//// ─── Details Play Button ───────────────────────────────────────────────────────
//@Composable
//private fun DetailsPlayButton(onClick: () -> Unit) {
//    var isFocused by remember { mutableStateOf(false) }
//    val scale by animateFloatAsState(
//        targetValue = if (isFocused) 1.06f else 1f,
//        animationSpec = tween(150)
//    )
//    Row(
//        modifier = Modifier
//            .scale(scale)
//            .clip(RoundedCornerShape(6.dp))
//            .background(White)
//            .clickable { onClick() }
//            .padding(horizontal = 32.dp, vertical = 14.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(10.dp)
//    ) {
//        Icon(
//            Icons.Default.PlayArrow,
//            contentDescription = null,
//            tint = DarkBackground,
//            modifier = Modifier.size(28.dp)
//        )
//        Text(
//            "Play",
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold,
//            color = DarkBackground
//        )
//    }
//}
//
//// ─── Add to List Button ────────────────────────────────────────────────────────
//@Composable
//private fun AddToListButton() {
//    Box(
//        modifier = Modifier
//            .size(50.dp)
//            .clip(CircleShape)
//            .background(White.copy(alpha = 0.15f))
//            .border(2.dp, White.copy(alpha = 0.4f), CircleShape)
//            .clickable { },
//        contentAlignment = Alignment.Center
//    ) {
//        Icon(Icons.Default.Add, contentDescription = "Add to List", tint = White,
//            modifier = Modifier.size(24.dp))
//    }
//}
//
//// ─── Rate Button ───────────────────────────────────────────────────────────────
//@Composable
//private fun RateButton() {
//    Box(
//        modifier = Modifier
//            .size(50.dp)
//            .clip(CircleShape)
//            .background(White.copy(alpha = 0.15f))
//            .border(2.dp, White.copy(alpha = 0.4f), CircleShape)
//            .clickable { },
//        contentAlignment = Alignment.Center
//    ) {
//        Icon(Icons.Default.ThumbUp, contentDescription = "Rate", tint = White,
//            modifier = Modifier.size(22.dp))
//    }
//}
//
//// ─── Meta Helpers ──────────────────────────────────────────────────────────────
//@Composable
//private fun MetaRow(label: String, value: String) {
//    Row {
//        Text(
//            "$label: ",
//            fontSize = 14.sp,
//            color = LightGray.copy(alpha = 0.5f),
//            fontWeight = FontWeight.Bold
//        )
//        Text(
//            value,
//            fontSize = 14.sp,
//            color = LightGray,
//            maxLines = 2,
//            overflow = TextOverflow.Ellipsis
//        )
//    }
//}
//
//@Composable
//private fun MetaDot() {
//    Box(
//        modifier = Modifier
//            .size(4.dp)
//            .clip(CircleShape)
//            .background(LightGray.copy(alpha = 0.4f))
//    )
//}
//
//@Composable
//private fun QualityTag(quality: String) {
//    Box(
//        modifier = Modifier
//            .clip(RoundedCornerShape(3.dp))
//            .border(1.dp, LightGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
//            .padding(horizontal = 6.dp, vertical = 2.dp)
//    ) {
//        Text(
//            quality,
//            fontSize = 11.sp,
//            color = LightGray,
//            fontWeight = FontWeight.Bold
//        )
//    }
//}