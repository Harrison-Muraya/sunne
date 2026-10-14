package com.streaming.sunne.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streaming.sunne.data.model.*
import com.streaming.sunne.data.network.TokenManager
import com.streaming.sunne.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
//  UI STATE
// ═══════════════════════════════════════════════════════════════════════════════

sealed class UiState<out T> {
    object Idle    : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val categories: List<ContentRow>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

// ═══════════════════════════════════════════════════════════════════════════════
//  AUTH VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    private val _authState = MutableStateFlow<UiState<UserResponse>>(UiState.Idle)
    val authState: StateFlow<UiState<UserResponse>> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    val isLoggedIn: Boolean get() = TokenManager.isLoggedIn

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            repo.login(username, password)
                .onSuccess { loadCurrentUser() }
                .onFailure { _authState.value = UiState.Error(it.message ?: "Login failed") }
        }
    }

    fun register(username: String, email: String, password: String, fullName: String? = null) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            repo.register(username, email, password, fullName)
                .onSuccess { login(username, password) }
                .onFailure { _authState.value = UiState.Error(it.message ?: "Registration failed") }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            repo.getMe()
                .onSuccess { user ->
                    _currentUser.value = user
                    TokenManager.isAdmin = user.isAdmin
                    _authState.value = UiState.Success(user)
                }
                .onFailure { _authState.value = UiState.Error(it.message ?: "Failed to load user") }
        }
    }

    /** Clears UI state synchronously so the Login screen never sees a stale Success. */
    fun logout() {
        _currentUser.value = null
        _authState.value = UiState.Idle
        viewModelScope.launch { repo.logout() }
    }

    fun onSessionExpired() {
        _currentUser.value = null
        _authState.value = UiState.Error("Your session has expired. Please sign in again.")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MOVIE VIEW MODEL (+ Series + Live)
// ═══════════════════════════════════════════════════════════════════════════════

class MovieViewModel : ViewModel() {

    private val movieRepo  = MovieRepository()
    private val seriesRepo = SeriesRepository()
    private val homeRepo   = HomeContentRepository()

    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()
    private var homeLoaded = false

    /** Non-null while a tap is resolving content by id (e.g. Continue Watching, Up Next). */
    private val _opening = MutableStateFlow<String?>(null)
    val opening: StateFlow<String?> = _opening.asStateFlow()

    // Selected content for detail screens
    private val _selectedMovie = MutableStateFlow<MovieResponse?>(null)
    val selectedMovie: StateFlow<MovieResponse?> = _selectedMovie.asStateFlow()

    private val _selectedSeries = MutableStateFlow<SeriesResponse?>(null)
    val selectedSeries: StateFlow<SeriesResponse?> = _selectedSeries.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<EpisodeResponse?>(null)
    val selectedEpisode: StateFlow<EpisodeResponse?> = _selectedEpisode.asStateFlow()

    private val _selectedLiveStream = MutableStateFlow<LiveStreamPublicResponse?>(null)
    val selectedLiveStream: StateFlow<LiveStreamPublicResponse?> = _selectedLiveStream.asStateFlow()

    fun ensureHomeLoaded() {
        if (!homeLoaded) loadHomeContent()
    }

    fun loadHomeContent() {
        homeLoaded = true
        viewModelScope.launch {
            if (_homeState.value !is HomeUiState.Success) _homeState.value = HomeUiState.Loading
            homeRepo.getHomeContent()
                .onSuccess { _homeState.value = HomeUiState.Success(it) }
                .onFailure {
                    homeLoaded = false
                    _homeState.value = HomeUiState.Error(friendlyMessage(it))
                }
        }
    }

    fun retry() = loadHomeContent()

    fun refreshContinueWatching() {
        if (_homeState.value !is HomeUiState.Success) return
        viewModelScope.launch {
            val row = homeRepo.continueWatchingRow()
            val current = _homeState.value as? HomeUiState.Success ?: return@launch
            val others = current.categories.filter { it.label != CONTINUE_WATCHING }
            _homeState.value = HomeUiState.Success(listOfNotNull(row) + others)
        }
    }

    /** Drops everything tied to the signed-in user (logout, session expiry). */
    fun reset() {
        homeLoaded = false
        _homeState.value = HomeUiState.Loading
        _selectedMovie.value = null
        _selectedSeries.value = null
        _selectedEpisode.value = null
        _selectedLiveStream.value = null
        _opening.value = null
        _openError.value = null
    }

    fun openMovie(movieId: Int, onReady: () -> Unit) = resolve("movie") {
        movieRepo.getMovie(movieId).onSuccess { selectMovie(it); onReady() }
    }

    fun openSeries(seriesId: Int, onReady: () -> Unit) = resolve("series") {
        seriesRepo.getSeries(seriesId).onSuccess { selectSeries(it); onReady() }
    }

    /** Continue Watching card for a series: jump straight into the resume episode. */
    fun resumeSeries(item: SeriesContinueWatching, onPlay: () -> Unit, onDetails: () -> Unit) = resolve("series") {
        seriesRepo.getSeries(item.seriesId).onSuccess { series ->
            selectSeries(series)
            val episode = series.seasons.flatMap { it.episodes }.firstOrNull { it.id == item.resumeEpisodeId }
            if (episode != null) { selectEpisode(episode); onPlay() } else onDetails()
        }
    }

    private fun resolve(what: String, block: suspend () -> Result<*>) {
        if (_opening.value != null) return
        viewModelScope.launch {
            _opening.value = what
            try {
                block().onFailure { _openError.value = "Couldn't open this $what. ${friendlyMessage(it)}" }
            } finally {
                _opening.value = null
            }
        }
    }

    private val _openError = MutableStateFlow<String?>(null)
    val openError: StateFlow<String?> = _openError.asStateFlow()

    fun clearOpenError() { _openError.value = null }

    fun selectMovie(movie: MovieResponse) {
        _selectedMovie.value = movie
    }

    fun selectSeries(series: SeriesResponse) {
        _selectedSeries.value = series
    }

    fun selectEpisode(episode: EpisodeResponse) {
        _selectedEpisode.value = episode
    }

    fun selectLiveStream(stream: LiveStreamPublicResponse) {
        _selectedLiveStream.value = stream
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SERIES VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class SeriesViewModel : ViewModel() {
    private val repo         = SeriesRepository()
    private val episodeWatch = EpisodeWatchRepository()

    private val _seriesDetail = MutableStateFlow<UiState<SeriesResponse>>(UiState.Idle)
    val seriesDetail: StateFlow<UiState<SeriesResponse>> = _seriesDetail.asStateFlow()

    private val _progress = MutableStateFlow<Map<Int, EpisodeProgressItem>>(emptyMap())
    val progress: StateFlow<Map<Int, EpisodeProgressItem>> = _progress.asStateFlow()

    fun loadSeries(seriesId: Int, initial: SeriesResponse? = null) {
        viewModelScope.launch {
            if (initial != null && initial.id == seriesId) _seriesDetail.value = UiState.Success(initial)
            else _seriesDetail.value = UiState.Loading
            repo.getSeries(seriesId)
                .onSuccess { _seriesDetail.value = UiState.Success(it) }
                .onFailure {
                    if (_seriesDetail.value !is UiState.Success) {
                        _seriesDetail.value = UiState.Error(friendlyMessage(it))
                    }
                }
        }
        refreshProgress(seriesId)
    }

    fun refreshProgress(seriesId: Int) {
        viewModelScope.launch {
            episodeWatch.getSeriesProgress(seriesId).onSuccess { items ->
                _progress.value = items.associateBy { it.episodeId }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  LIVE STREAM VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class LiveStreamViewModel : ViewModel() {
    private val repo = LiveStreamRepository()

    private val _streams = MutableStateFlow<UiState<List<LiveStreamPublicResponse>>>(UiState.Idle)
    val streams: StateFlow<UiState<List<LiveStreamPublicResponse>>> = _streams.asStateFlow()

    fun loadStreams() {
        viewModelScope.launch {
            _streams.value = UiState.Loading
            repo.listStreams()
                .onSuccess { _streams.value = UiState.Success(it) }
                .onFailure { _streams.value = UiState.Error(it.message ?: "Failed to load streams") }
        }
    }

    private val _viewerCount = MutableStateFlow<Int?>(null)
    val viewerCount: StateFlow<Int?> = _viewerCount.asStateFlow()

    fun refreshViewerCount(streamId: Int) {
        viewModelScope.launch {
            repo.getStream(streamId).onSuccess { _viewerCount.value = it.viewerCount }
        }
    }

    fun joinStream(streamId: Int) {
        viewModelScope.launch {
            repo.joinStream(streamId)
        }
    }

    fun leaveStream(streamId: Int) {
        viewModelScope.launch {
            repo.leaveStream(streamId)
        }
    }
}

//package com.streaming.sunne.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.streaming.sunne.data.model.*
//import com.streaming.sunne.data.network.TokenManager
//import com.streaming.sunne.repository.AuthRepository
//import com.streaming.sunne.repository.MovieRepository
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//// ─── Shared UI State helpers ───────────────────────────────────────────────────
//
//sealed class UiState<out T> {
//    object Idle    : UiState<Nothing>()
//    object Loading : UiState<Nothing>()
//    data class Success<T>(val data: T) : UiState<T>()
//    data class Error(val message: String) : UiState<Nothing>()
//}
//
//// ─── AuthViewModel ─────────────────────────────────────────────────────────────
//
//class AuthViewModel : ViewModel() {
//
//    private val repo = AuthRepository()
//
//    private val _authState = MutableStateFlow<UiState<UserResponse>>(UiState.Idle)
//    val authState: StateFlow<UiState<UserResponse>> = _authState.asStateFlow()
//
//    private val _currentUser = MutableStateFlow<UserResponse?>(null)
//    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()
//
//    val isLoggedIn: Boolean get() = TokenManager.isLoggedIn
//
//    /** Login with username + password */
//    fun login(username: String, password: String) {
//        viewModelScope.launch {
//            _authState.value = UiState.Loading
//            val result = repo.login(username, password)
//            if (result.isSuccess) {
//                loadCurrentUser()
//            } else {
//                _authState.value = UiState.Error(
//                    result.exceptionOrNull()?.message ?: "Login failed"
//                )
//            }
//        }
//    }
//
//    /** Register a new account then auto-login */
//    fun register(
//        username: String,
//        email: String,
//        password: String,
//        fullName: String? = null
//    ) {
//        viewModelScope.launch {
//            _authState.value = UiState.Loading
//            val result = repo.register(username, email, password, fullName)
//            result.onSuccess { user ->
//                _currentUser.value = user
//                // auto-login after register
//                login(username, password)
//            }.onFailure {
//                _authState.value = UiState.Error(it.message ?: "Registration failed")
//            }
//        }
//    }
//
//    /** Load current user profile (/me) */
//    fun loadCurrentUser() {
//        viewModelScope.launch {
//            val result = repo.getMe()
//            result.onSuccess { user ->
//                _currentUser.value = user
//                TokenManager.isAdmin = user.isAdmin
//                _authState.value = UiState.Success(user)
//            }.onFailure {
//                _authState.value = UiState.Error(it.message ?: "Failed to load user")
//            }
//        }
//    }
//
//    /** Logout */
//    fun logout() {
//        viewModelScope.launch {
//            repo.logout()
//            _currentUser.value = null
//            _authState.value = UiState.Idle
//        }
//    }
//}
//
//// ─── HomeUiState ──────────────────────────────────────────────────────────────
//
//sealed class HomeUiState {
//    object Loading : HomeUiState()
//    data class Success(val categories: List<MovieCategory>) : HomeUiState()
//    data class Error(val message: String) : HomeUiState()
//}
//
//// ─── MovieViewModel ────────────────────────────────────────────────────────────
//
//class MovieViewModel : ViewModel() {
//
//    private val repo = MovieRepository()
//
//    // Home screen state
//    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
//    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()
//
//    // Currently selected movie (for DetailsScreen / PlayerScreen)
//    private val _selectedMovie = MutableStateFlow<MovieResponse?>(null)
//    val selectedMovie: StateFlow<MovieResponse?> = _selectedMovie.asStateFlow()
//
//    // Stream URL for PlayerScreen
//    private val _streamUrl = MutableStateFlow<UiState<String>>(UiState.Idle)
//    val streamUrl: StateFlow<UiState<String>> = _streamUrl.asStateFlow()
//
//    // Search results
//    private val _searchResults = MutableStateFlow<UiState<List<MovieResponse>>>(UiState.Idle)
//    val searchResults: StateFlow<UiState<List<MovieResponse>>> = _searchResults.asStateFlow()
//
//    init {
//        loadCategories()
//    }
//
//    /** Load all category carousels for HomeScreen */
//    fun loadCategories() {
//        viewModelScope.launch {
//            _homeState.value = HomeUiState.Loading
//            val result = repo.getMovieCategories()
//            _homeState.value = if (result.isSuccess) {
//                val cats = result.getOrNull() ?: emptyList()
//                if (cats.isEmpty()) HomeUiState.Error("No content available")
//                else HomeUiState.Success(cats)
//            } else {
//                HomeUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load content")
//            }
//        }
//    }
//
//    /** Select a movie to show in DetailsScreen */
//    fun selectMovie(movie: MovieResponse) {
//        _selectedMovie.value = movie
//    }
//
//    /** Clear selected movie */
//    fun clearSelectedMovie() {
//        _selectedMovie.value = null
//        _streamUrl.value = UiState.Idle
//    }
//
//    /**
//     * Load the streaming URL for the selected movie.
//     * Quality: "1080p" | "720p" | "480p"
//     */
//    fun loadStreamUrl(movieId: Int, quality: String = "1080p") {
//        viewModelScope.launch {
//            _streamUrl.value = UiState.Loading
//            val result = repo.getStreamUrl(movieId, quality)
//            _streamUrl.value = if (result.isSuccess) {
//                val url = result.getOrNull()?.url
//                if (url != null) UiState.Success(url)
//                else UiState.Error("Stream URL not available")
//            } else {
//                // Fallback: use video_url from the movie model if API fails
//                val fallback = _selectedMovie.value?.videoUrl
//                if (fallback != null) UiState.Success(fallback)
//                else UiState.Error(result.exceptionOrNull()?.message ?: "Failed to get stream URL")
//            }
//        }
//    }
//
//    /** Increment view count (fire-and-forget) */
//    fun trackView(movieId: Int) {
//        viewModelScope.launch { repo.incrementViewCount(movieId) }
//    }
//
//    /** Search movies */
//    fun search(query: String) {
//        if (query.isBlank()) {
//            _searchResults.value = UiState.Idle
//            return
//        }
//        viewModelScope.launch {
//            _searchResults.value = UiState.Loading
//            val result = repo.getMovies(search = query, pageSize = 50)
//            _searchResults.value = if (result.isSuccess) {
//                UiState.Success(result.getOrNull()?.movies ?: emptyList())
//            } else {
//                UiState.Error(result.exceptionOrNull()?.message ?: "Search failed")
//            }
//        }
//    }
//
//    fun retry() = loadCategories()
//}
//
//// ─── Backwards compatibility aliases ──────────────────────────────────────────
//// The old codebase used StreamViewModel / StreamUiState — these aliases let
//// existing screen files compile without modification until you migrate them.
//
//typealias StreamViewModel = MovieViewModel
//
//sealed class StreamUiState {
//    object Loading : StreamUiState()
//    data class Success(val categories: List<MovieCategory>) : StreamUiState()
//    data class Error(val message: String) : StreamUiState()
//}
//
///** Extension to expose homeState as uiState for screens using the old name */
//val MovieViewModel.uiState: StateFlow<StreamUiState>
//    get() = homeState.map { home ->
//        when (home) {
//            is HomeUiState.Loading        -> StreamUiState.Loading
//            is HomeUiState.Success        -> StreamUiState.Success(home.categories)
//            is HomeUiState.Error          -> StreamUiState.Error(home.message)
//        }
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5_000),
//        initialValue = StreamUiState.Loading
//    )