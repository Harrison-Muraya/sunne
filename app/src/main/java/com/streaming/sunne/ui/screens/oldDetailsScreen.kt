//package com.streaming.sunne.ui.screens
//
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.Icon
//import androidx.compose.material.Text
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import coil.compose.rememberAsyncImagePainter
//import com.streaming.sunne.data.model.Stream
//import com.streaming.sunne.ui.theme.*
//
///**
// * Details Screen - Shows detailed information about a selected stream
// * with a prominent Play button
// */
//@Composable
//fun dDetailsScreen(
//    stream: Stream,
//    onPlayClick: (Stream) -> Unit,
//    onBackClick: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground)
//    ) {
//        // Background banner with gradient overlay
//        Box(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            // Banner image
//            Image(
//                painter = rememberAsyncImagePainter(
//                    model = stream.banner ?: stream.thumbnail
//                ),
//                contentDescription = null,
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop
//            )
//
//            // Gradient overlay for readability
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(
//                                Color.Transparent,
//                                DarkBackground.copy(alpha = 0.7f),
//                                DarkBackground
//                            ),
//                            startY = 0f,
//                            endY = 1000f
//                        )
//                    )
//            )
//        }
//
//        // Content
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(48.dp),
//            verticalArrangement = Arrangement.Bottom
//        ) {
//            // Title
//            Text(
//                text = stream.title,
//                fontSize = 48.sp,
//                fontWeight = FontWeight.Bold,
//                color = White,
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Duration and category
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//                stream.duration?.let {
//                    Text(
//                        text = it,
//                        fontSize = 16.sp,
//                        color = AccentGray
//                    )
//                }
//
//                Text(
//                    text = stream.category,
//                    fontSize = 16.sp,
//                    color = AccentGray
//                )
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Description
//            stream.description?.let {
//                Text(
//                    text = it,
//                    fontSize = 18.sp,
//                    color = White.copy(alpha = 0.9f),
//                    maxLines = 4,
//                    overflow = TextOverflow.Ellipsis,
//                    lineHeight = 28.sp,
//                    modifier = Modifier.widthIn(max = 700.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(40.dp))
//
//            // Action buttons
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//                // Play button
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(NetflixRed)
//                        .clickable { onPlayClick(stream) }
//                        .padding(horizontal = 40.dp, vertical = 16.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.PlayArrow,
//                            contentDescription = "Play",
//                            tint = White,
//                            modifier = Modifier.size(32.dp)
//                        )
//                        Text(
//                            text = "Play",
//                            fontSize = 20.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = White
//                        )
//                    }
//                }
//
//                // Back button
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(CardBackground.copy(alpha = 0.8f))
//                        .clickable { onBackClick() }
//                        .padding(horizontal = 32.dp, vertical = 16.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = "Back",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = White
//                    )
//                }
//            }
//        }
//    }
//}