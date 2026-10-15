package com.streaming.sunne.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streaming.sunne.data.model.CatalogSource
import com.streaming.sunne.data.model.ContentItem
import com.streaming.sunne.data.model.GenreResponse
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.CatalogViewModel
import com.streaming.sunne.viewmodel.MyListViewModel
import com.streaming.sunne.viewmodel.SearchViewModel
import com.streaming.sunne.viewmodel.UiState

// ═══════════════════════════════════════════════════════════════════════════════
//  CATALOG — Browse tabs and "See all"
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CatalogContent(
    source: CatalogSource,
    onItemClick: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    val ad = rememberAdaptive()
    val kind = if (source is CatalogSource.Movies) "movies" else "series"
    val vm: CatalogViewModel = viewModel(key = "catalog/$kind/${source.title}")
    val state  by vm.state.collectAsState()
    val genres by vm.genres.collectAsState()
    val current by vm.source.collectAsState()
    val cardFocus = rememberCardFocus()

    LaunchedEffect(source) { if (vm.source.value == null) vm.show(source) }

    val selectedGenre = when (val c = current) {
        is CatalogSource.Movies -> c.genreId
        is CatalogSource.Series -> c.genreId
        null -> null
    }

    Column(modifier) {
        if (genres.isNotEmpty()) {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = ad.pad, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { GenreChip("All", selectedGenre == null) { vm.selectGenre(null) } }
                items(genres, key = { it.id }) { g ->
                    GenreChip(g.name, selectedGenre == g.id) { vm.selectGenre(g) }
                }
            }
        }

        when {
            state.items.isEmpty() && state.loading -> CenteredSpinner()
            state.items.isEmpty() && state.error != null -> CenteredMessage(state.error!!, "Retry", vm::loadMore)
            state.items.isEmpty() && state.endReached -> CenteredMessage(
                if (kind == "movies") "No movies here yet." else "No series here yet."
            )
            else -> ContentGrid(
                items       = state.items,
                adaptive    = ad,
                focus       = cardFocus,
                bottomInset = bottomInset,
                onItemClick = onItemClick,
                footer = {
                    when {
                        state.loading -> Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(28.dp))
                        }
                        state.error != null -> Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            TextAction("Couldn't load more. Retry", vm::loadMore)
                        }
                    }
                },
                onNearEnd = vm::loadMore
            )
        }
    }
}

@Composable
fun CatalogScreen(source: CatalogSource, onItemClick: (ContentItem) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        ScreenHeader(source.title, onBack)
        CatalogContent(source, onItemClick, Modifier.weight(1f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SEARCH
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SearchScreen(onItemClick: (ContentItem) -> Unit, onBack: () -> Unit) {
    val ad = rememberAdaptive()
    val vm: SearchViewModel = viewModel()
    val query   by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val cardFocus = rememberCardFocus()
    val inputFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { if (query.isEmpty()) runCatching { inputFocus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ad.pad, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackBtn(onBack)
            Spacer(Modifier.width(12.dp))
            TextField(
                value         = query,
                onValueChange = vm::onQueryChange,
                modifier      = Modifier.weight(1f).focusRequester(inputFocus),
                placeholder   = { Text("Search movies and series", color = LightGray) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = LightGray) },
                trailingIcon  = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Default.Close, "Clear", tint = LightGray)
                        }
                    }
                },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape           = RoundedCornerShape(8.dp),
                colors          = TextFieldDefaults.textFieldColors(
                    textColor               = White,
                    backgroundColor         = CardBackground,
                    cursorColor             = NetflixRed,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        when (val r = results) {
            is UiState.Idle    -> CenteredMessage("Find a movie or series by title.")
            is UiState.Loading -> CenteredSpinner()
            is UiState.Error   -> CenteredMessage(r.message, "Retry", vm::retry)
            is UiState.Success ->
                if (r.data.isEmpty()) CenteredMessage("No results for \"${query.trim()}\".")
                else ContentGrid(r.data, ad, cardFocus, onItemClick = onItemClick)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MY LIST
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun MyListScreen(onItemClick: (ContentItem) -> Unit, onBack: () -> Unit) {
    val ad = rememberAdaptive()
    val vm: MyListViewModel = viewModel()
    val state by vm.items.collectAsState()
    val cardFocus = rememberCardFocus()

    // Reload on every return so titles removed on a detail page disappear here too
    OnResumeEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        ScreenHeader("My List", onBack)
        when (val s = state) {
            is UiState.Idle, is UiState.Loading -> CenteredSpinner()
            is UiState.Error   -> CenteredMessage(s.message, "Retry", vm::load)
            is UiState.Success ->
                if (s.data.isEmpty()) CenteredMessage("Titles you add with + will show up here.")
                else ContentGrid(s.data, ad, cardFocus, onItemClick = onItemClick)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SHARED PIECES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ContentGrid(
    items: List<ContentItem>,
    adaptive: Adaptive,
    focus: CardFocus,
    bottomInset: Dp = 0.dp,
    onItemClick: (ContentItem) -> Unit,
    footer: (@Composable () -> Unit)? = null,
    onNearEnd: (() -> Unit)? = null
) {
    val gridState = rememberLazyGridState()
    if (onNearEnd != null) {
        val nearEnd by remember(items.size) {
            derivedStateOf {
                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= items.size - 6
            }
        }
        LaunchedEffect(nearEnd, items.size) { if (nearEnd) onNearEnd() }
    }

    LazyVerticalGrid(
        state                 = gridState,
        columns               = GridCells.Adaptive(adaptive.cardW),
        contentPadding        = PaddingValues(start = adaptive.pad, end = adaptive.pad, top = 8.dp, bottom = 24.dp + bottomInset),
        horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap),
        verticalArrangement   = Arrangement.spacedBy(adaptive.cardGap + 6.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.stableKey }) { item ->
            ContentCard(
                item     = item,
                adaptive = adaptive,
                focusKey = item.stableKey,
                focus    = focus,
                onClick  = { onItemClick(item) }
            )
        }
        if (footer != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { footer() }
        }
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) White else if (focused) White.copy(0.25f) else White.copy(0.1f))
            .border(1.dp, if (focused) NetflixRed else Color.Transparent, RoundedCornerShape(16.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = sp(13), fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.Black else White)
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    val ad = rememberAdaptive()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ad.pad, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackBtn(onBack)
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = ad.sectionTitle, fontWeight = FontWeight.Bold, color = White)
    }
}

@Composable
private fun CenteredSpinner() {
    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = NetflixRed) }
}

@Composable
private fun CenteredMessage(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, fontSize = sp(15), color = LightGray)
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                TextAction(action, onAction)
            }
        }
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    Text(label, fontSize = sp(15), fontWeight = FontWeight.Bold, color = NetflixRed,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onClick() }.padding(8.dp))
}
