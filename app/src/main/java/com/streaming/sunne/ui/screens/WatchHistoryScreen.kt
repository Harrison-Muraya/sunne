package com.streaming.sunne.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.streaming.sunne.data.model.WatchHistoryResponse
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.UiState
import com.streaming.sunne.viewmodel.WatchHistoryViewModel

@Composable
fun WatchHistoryScreen(
    viewModel:   WatchHistoryViewModel,
    onMovieClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val state         by viewModel.history.collectAsState()
    val completedOnly by viewModel.showCompletedOnly.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = White,
                    modifier = Modifier.size(24.dp).clickable { onBackClick() })
                Text("Watch History", fontSize = sp(20), fontWeight = FontWeight.Bold, color = White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Filter toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (completedOnly) NetflixRed else Color(0xFF2A2A2A))
                        .clickable { viewModel.toggleFilter() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (completedOnly) "Completed" else "All",
                        fontSize = sp(12), color = White, fontWeight = FontWeight.SemiBold
                    )
                }
                // Clear all
                Icon(Icons.Default.DeleteSweep, "Clear all", tint = LightGray,
                    modifier = Modifier.size(24.dp).clickable { showClearDialog = true })
            }
        }

        when (val s = state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = NetflixRed)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = NetflixRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(s.message, color = LightGray, fontSize = sp(14))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.load() },
                        colors  = ButtonDefaults.buttonColors(backgroundColor = NetflixRed)
                    ) { Text("Retry", color = White) }
                }
            }
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, tint = LightGray.copy(0.4f),
                                modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (completedOnly) "No completed movies yet" else "No watch history yet",
                                fontSize = sp(16), color = LightGray.copy(0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.data, key = { it.id }) { item ->
                            HistoryItem(
                                item      = item,
                                onClick   = { onMovieClick(item.movieId) },
                                onDelete  = { viewModel.deleteItem(item.movieId) }
                            )
                        }
                    }
                }
            }
            is UiState.Idle -> Unit
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            backgroundColor  = Color(0xFF1E1E1E),
            contentColor     = White,
            title   = { Text("Clear History", fontWeight = FontWeight.Bold) },
            text    = { Text("Remove all watch history? This cannot be undone.", color = LightGray) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAll(); showClearDialog = false }) {
                    Text("Clear All", color = NetflixRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = LightGray)
                }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    item:     WatchHistoryResponse,
    onClick:  () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail placeholder (movie_id shown since no direct image here)
        Box(
            modifier = Modifier.size(width = 80.dp, height = 55.dp)
                .clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Movie, null, tint = LightGray.copy(0.4f), modifier = Modifier.size(28.dp))
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text("Movie #${item.movieId}", fontSize = sp(14), fontWeight = FontWeight.SemiBold, color = White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            // Progress bar
            LinearProgressIndicator(
                progress          = (item.watchPercentage / 100f).coerceIn(0f, 1f),
                color             = if (item.completed) Color(0xFF4CAF50) else NetflixRed,
                backgroundColor   = White.copy(0.1f),
                modifier          = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${"%.0f".format(item.watchPercentage)}% watched",
                    fontSize = sp(11), color = LightGray
                )
                if (item.completed) {
                    Text("✓ Completed", fontSize = sp(11), color = Color(0xFF4CAF50))
                }
            }
        }

        // Delete button
        Icon(Icons.Default.Close, "Remove", tint = LightGray.copy(0.6f),
            modifier = Modifier.size(20.dp).clickable { onDelete() })
    }
}

private fun sp(value: Int) = androidx.compose.ui.unit.TextUnit(value.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
