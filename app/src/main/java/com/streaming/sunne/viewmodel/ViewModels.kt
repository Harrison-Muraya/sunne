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
