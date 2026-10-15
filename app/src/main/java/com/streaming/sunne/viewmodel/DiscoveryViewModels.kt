package com.streaming.sunne.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streaming.sunne.data.model.*
import com.streaming.sunne.repository.DiscoveryRepository
import com.streaming.sunne.repository.MyListRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 350L
private const val CATALOG_PAGE_SIZE = 30

// ═══════════════════════════════════════════════════════════════════════════════
//  SEARCH
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {
    private val repo = DiscoveryRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Idle until the user types; Success with an empty list means "no results". */
    private val _results = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Idle)
    val results: StateFlow<UiState<List<ContentItem>>> = _results.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _query.map { it.trim() }.distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MS).collect(::runSearch)
        }
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun retry() = runSearch(_query.value.trim())

    private fun runSearch(q: String) {
        searchJob?.cancel()
        if (q.isEmpty()) { _results.value = UiState.Idle; return }
        searchJob = viewModelScope.launch {
            _results.value = UiState.Loading
            repo.search(q)
                .onSuccess { _results.value = UiState.Success(it) }
                .onFailure { _results.value = UiState.Error(friendlyMessage(it)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MY LIST
// ═══════════════════════════════════════════════════════════════════════════════

class MyListViewModel : ViewModel() {
    private val repo = MyListRepository()

    private val _items = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val items: StateFlow<UiState<List<ContentItem>>> = _items.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (_items.value !is UiState.Success) _items.value = UiState.Loading
            repo.items()
                .onSuccess { list -> _items.value = UiState.Success(list.mapNotNull { it.toContentItem() }) }
                .onFailure { if (_items.value !is UiState.Success) _items.value = UiState.Error(friendlyMessage(it)) }
        }
    }
}

/** Add/remove toggle for one title, used on the movie and series detail pages. */
class MyListToggleViewModel : ViewModel() {
    private val repo = MyListRepository()

    private val _inList = MutableStateFlow(false)
    val inList: StateFlow<Boolean> = _inList.asStateFlow()

    private var type = ""
    private var id = 0

    fun bind(contentType: String, contentId: Int) {
        if (type == contentType && id == contentId) return
        type = contentType
        id = contentId
        viewModelScope.launch { repo.contains(type, id).onSuccess { _inList.value = it } }
    }

    fun toggle() {
        val wasIn = _inList.value
        _inList.value = !wasIn
        viewModelScope.launch {
            val result = if (wasIn) repo.remove(type, id) else repo.add(type, id)
            if (result.isFailure) _inList.value = wasIn
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CATALOG (Browse tabs and "See all")
// ═══════════════════════════════════════════════════════════════════════════════

data class CatalogState(
    val items: List<ContentItem> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null
)

class CatalogViewModel : ViewModel() {
    private val repo = DiscoveryRepository()

    private val _genres = MutableStateFlow<List<GenreResponse>>(emptyList())
    val genres: StateFlow<List<GenreResponse>> = _genres.asStateFlow()

    private val _source = MutableStateFlow<CatalogSource?>(null)
    val source: StateFlow<CatalogSource?> = _source.asStateFlow()

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        viewModelScope.launch { repo.genres().onSuccess { _genres.value = it } }
    }

    fun show(source: CatalogSource) {
        if (_source.value == source) return
        _source.value = source
        loadJob?.cancel()
        nextPage = 1
        _state.value = CatalogState()
        loadMore()
    }

    fun selectGenre(genre: GenreResponse?) {
        val current = _source.value ?: return
        show(
            when (current) {
                is CatalogSource.Movies -> current.copy(genreId = genre?.id)
                is CatalogSource.Series -> current.copy(genreId = genre?.id)
            }
        )
    }

    fun loadMore() {
        val source = _source.value ?: return
        val s = _state.value
        if (s.loading || s.endReached) return
        _state.value = s.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            repo.catalogPage(source, nextPage, CATALOG_PAGE_SIZE)
                .onSuccess { page ->
                    nextPage++
                    _state.value = _state.value.copy(
                        items = _state.value.items + page,
                        loading = false,
                        endReached = page.size < CATALOG_PAGE_SIZE
                    )
                }
                .onFailure { _state.value = _state.value.copy(loading = false, error = friendlyMessage(it)) }
        }
    }
}
