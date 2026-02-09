package com.streaming.sunne.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.Stream
import com.streaming.sunne.data.model.StreamCategory
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.StreamUiState
import com.streaming.sunne.viewmodel.StreamViewModel

/**
 * Home Screen - Displays stream categories in horizontal carousels
 */
@Composable
fun HomeScreen(
    viewModel: StreamViewModel,
    onStreamClick: (Stream) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (uiState) {
            is StreamUiState.Loading -> {
                LoadingScreen()
            }
            is StreamUiState.Success -> {
                val categories = (uiState as StreamUiState.Success).categories
                StreamCategoryList(
                    categories = categories,
                    onStreamClick = onStreamClick
                )
            }
            is StreamUiState.Error -> {
                ErrorContent(
                    message = (uiState as StreamUiState.Error).message,
                    onRetry = { viewModel.retry() }
                )
            }
        }
    }
}

@Composable
private fun StreamCategoryList(
    categories: List<StreamCategory>,
    onStreamClick: (Stream) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUNNE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NetflixRed
            )

            Text(
                text = "Browse",
                fontSize = 18.sp,
                color = White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Category carousels
        categories.forEach { category ->
            StreamCarousel(
                category = category,
                onStreamClick = onStreamClick
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StreamCarousel(
    category: StreamCategory,
    onStreamClick: (Stream) -> Unit
) {
    Column {
        // Category title
        Text(
            text = category.category,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = White,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal scrolling list of streams
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.streams) { stream ->
                StreamCard(
                    stream = stream,
                    onClick = { onStreamClick(stream) }
                )
            }
        }
    }
}

@Composable
private fun StreamCard(
    stream: Stream,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() }
            .scale(if (isFocused) 1.1f else 1f)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
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
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = stream.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = NetflixRed,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading streams...",
                fontSize = 18.sp,
                color = White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Oops!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NetflixRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NetflixRed)
                    .clickable { onRetry() }
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Retry",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }
    }
}