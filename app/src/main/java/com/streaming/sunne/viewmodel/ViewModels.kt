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

    fun updateProfile(email: String? = null, fullName: String? = null, password: String? = null) {
        viewModelScope.launch {
            repo.updateMe(UserUpdate(email, fullName, password))
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = UiState.Success(user)
                }
                .onFailure { _authState.value = UiState.Error(it.message ?: "Update failed") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _currentUser.value = null
            _authState.value = UiState.Idle
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repo.deleteAccount()
            _currentUser.value = null
            _authState.value = UiState.Idle
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MOVIE VIEW MODEL  (movies + series + live selection hub)
// ═══════════════════════════════════════════════════════════════════════════════

class MovieViewModel : ViewModel() {

    private val movieRepo  = MovieRepository()
    private val homeRepo   = HomeContentRepository()
    private val watchRepo  = WatchRepository()
    private val recRepo    = RecommendationRepository()

    // ── Home ──────────────────────────────────────────────────────────────────
    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    // ── Stream URL ────────────────────────────────────────────────────────────
    private val _streamUrl = MutableStateFlow<UiState<String>>(UiState.Idle)
    val streamUrl: StateFlow<UiState<String>> = _streamUrl.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────────
    private val _searchQuery   = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<MovieResponse>>>(UiState.Idle)
    val searchResults: StateFlow<UiState<List<MovieResponse>>> = _searchResults.asStateFlow()

    // ── Ratings ───────────────────────────────────────────────────────────────
    private val _myRating      = MutableStateFlow<MovieRatingResponse?>(null)
    val myRating: StateFlow<MovieRatingResponse?> = _myRating.asStateFlow()

    private val _avgRating     = MutableStateFlow<AverageRatingResponse?>(null)
    val avgRating: StateFlow<AverageRatingResponse?> = _avgRating.asStateFlow()

    private val _ratingState   = MutableStateFlow<UiState<MovieRatingResponse>>(UiState.Idle)
    val ratingState: StateFlow<UiState<MovieRatingResponse>> = _ratingState.asStateFlow()

    // ── Recommendations ───────────────────────────────────────────────────────
    private val _recommendations = MutableStateFlow<UiState<List<RecommendationResponse>>>(UiState.Idle)
    val recommendations: StateFlow<UiState<List<RecommendationResponse>>> = _recommendations.asStateFlow()

    private val _similarMovies = MutableStateFlow<List<SimilarMovieResponse>>(emptyList())
    val similarMovies: StateFlow<List<SimilarMovieResponse>> = _similarMovies.asStateFlow()

    private val _becauseYouWatched = MutableStateFlow<List<RecommendationResponse>>(emptyList())
    val becauseYouWatched: StateFlow<List<RecommendationResponse>> = _becauseYouWatched.asStateFlow()

    // ── Watch History ─────────────────────────────────────────────────────────
    private val _watchHistory  = MutableStateFlow<UiState<List<WatchHistoryResponse>>>(UiState.Idle)
    val watchHistory: StateFlow<UiState<List<WatchHistoryResponse>>> = _watchHistory.asStateFlow()

    // ── Selected content ──────────────────────────────────────────────────────
    private val _selectedMovie      = MutableStateFlow<MovieResponse?>(null)
    val selectedMovie: StateFlow<MovieResponse?> = _selectedMovie.asStateFlow()

    private val _selectedSeries     = MutableStateFlow<SeriesResponse?>(null)
    val selectedSeries: StateFlow<SeriesResponse?> = _selectedSeries.asStateFlow()

    private val _selectedEpisode    = MutableStateFlow<EpisodeResponse?>(null)
    val selectedEpisode: StateFlow<EpisodeResponse?> = _selectedEpisode.asStateFlow()

    private val _selectedLiveStream = MutableStateFlow<LiveStreamPublicResponse?>(null)
    val selectedLiveStream: StateFlow<LiveStreamPublicResponse?> = _selectedLiveStream.asStateFlow()

    init { loadHomeContent() }

    // ── Home ──────────────────────────────────────────────────────────────────

    fun loadHomeContent() {
        viewModelScope.launch {
            _homeState.value = HomeUiState.Loading
            homeRepo.getHomeContent()
                .onSuccess { _homeState.value = HomeUiState.Success(it) }
                .onFailure { _homeState.value = HomeUiState.Error(it.message ?: "Failed to load content") }
        }
    }

    fun retry() = loadHomeContent()

    // ── Selection helpers ─────────────────────────────────────────────────────

    fun selectMovie(movie: MovieResponse) {
        _selectedMovie.value = movie
        // Pre-load ancillary data for the detail screen
        loadSimilarMovies(movie.id)
        loadBecauseYouWatched(movie.id)
        loadMyRating(movie.id)
        loadAverageRating(movie.id)
    }

    fun selectSeries(series: SeriesResponse) {
        // Set immediately so navigation can proceed without blocking
        _selectedSeries.value = series
        // Fetch full detail in background — includes seasons & episodes
        // SeriesDetailScreen will recompose automatically when the richer data arrives
        viewModelScope.launch {
            SeriesRepository().getSeries(series.id)
                .onSuccess { fullSeries -> _selectedSeries.value = fullSeries }
            // On failure the partial series stays — user still sees title/banner
        }
    }
    fun selectEpisode(episode: EpisodeResponse) { _selectedEpisode.value = episode }
    fun selectLiveStream(stream: LiveStreamPublicResponse) { _selectedLiveStream.value = stream }

    // ── Stream URL ────────────────────────────────────────────────────────────

    fun loadStreamUrl(movieId: Int, quality: String = "1080p") {
        viewModelScope.launch {
            _streamUrl.value = UiState.Loading
            movieRepo.getStreamUrl(movieId, quality)
                .onSuccess { _streamUrl.value = UiState.Success(it.url) }
                .onFailure { _streamUrl.value = UiState.Error(it.message ?: "Failed to load stream") }
        }
    }

    fun trackView(movieId: Int) {
        viewModelScope.launch { movieRepo.incrementViewCount(movieId) }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = UiState.Idle
            return
        }
        viewModelScope.launch {
            _searchResults.value = UiState.Loading
            movieRepo.getMovies(search = query, pageSize = 50)
                .onSuccess { _searchResults.value = UiState.Success(it.movies) }
                .onFailure { _searchResults.value = UiState.Error(it.message ?: "Search failed") }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = UiState.Idle
    }

    // ── Ratings ───────────────────────────────────────────────────────────────

    fun loadMyRating(movieId: Int) {
        viewModelScope.launch {
            watchRepo.getMyRating(movieId)
                .onSuccess { _myRating.value = it }
        }
    }

    fun loadAverageRating(movieId: Int) {
        viewModelScope.launch {
            watchRepo.getAverageRating(movieId)
                .onSuccess { _avgRating.value = it }
        }
    }

    fun rateMovie(movieId: Int, rating: Int, review: String? = null) {
        viewModelScope.launch {
            _ratingState.value = UiState.Loading
            watchRepo.rateMovie(movieId, rating, review)
                .onSuccess {
                    _myRating.value = it
                    _ratingState.value = UiState.Success(it)
                    loadAverageRating(movieId)
                }
                .onFailure { _ratingState.value = UiState.Error(it.message ?: "Rating failed") }
        }
    }

    fun deleteMyRating(movieId: Int) {
        viewModelScope.launch {
            watchRepo.deleteMyRating(movieId)
                .onSuccess {
                    _myRating.value = null
                    _ratingState.value = UiState.Idle
                    loadAverageRating(movieId)
                }
        }
    }

    // ── Recommendations ───────────────────────────────────────────────────────

    fun loadRecommendations(strategy: String = "auto") {
        viewModelScope.launch {
            _recommendations.value = UiState.Loading
            recRepo.getForYou(limit = 20, strategy = strategy)
                .onSuccess { _recommendations.value = UiState.Success(it) }
                .onFailure { _recommendations.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    fun loadSimilarMovies(movieId: Int) {
        viewModelScope.launch {
            recRepo.getSimilar(movieId)
                .onSuccess { _similarMovies.value = it }
        }
    }

    fun loadBecauseYouWatched(movieId: Int) {
        viewModelScope.launch {
            recRepo.getBecauseYouWatched(movieId)
                .onSuccess { _becauseYouWatched.value = it }
        }
    }

    // ── Watch History ─────────────────────────────────────────────────────────

    fun loadWatchHistory(completedOnly: Boolean = false) {
        viewModelScope.launch {
            _watchHistory.value = UiState.Loading
            watchRepo.getHistory(completedOnly)
                .onSuccess { _watchHistory.value = UiState.Success(it) }
                .onFailure { _watchHistory.value = UiState.Error(it.message ?: "Failed to load history") }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            watchRepo.clearHistory()
                .onSuccess { _watchHistory.value = UiState.Success(emptyList()) }
        }
    }

    fun deleteHistoryItem(movieId: Int) {
        viewModelScope.launch {
            watchRepo.deleteHistoryItem(movieId)
                .onSuccess {
                    val current = (_watchHistory.value as? UiState.Success)?.data ?: return@onSuccess
                    _watchHistory.value = UiState.Success(current.filter { it.movieId != movieId })
                }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SERIES VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class SeriesViewModel : ViewModel() {
    private val repo = SeriesRepository()
    private val watchRepo = EpisodeWatchRepository()

    private val _seriesDetail = MutableStateFlow<UiState<SeriesResponse>>(UiState.Idle)
    val seriesDetail: StateFlow<UiState<SeriesResponse>> = _seriesDetail.asStateFlow()

    private val _seriesProgress = MutableStateFlow<Map<String, Any>>(emptyMap())
    val seriesProgress: StateFlow<Map<String, Any>> = _seriesProgress.asStateFlow()

    private val _avgRating = MutableStateFlow<Map<String, Any>>(emptyMap())
    val avgRating: StateFlow<Map<String, Any>> = _avgRating.asStateFlow()

    private val _ratingState = MutableStateFlow<UiState<SeriesRatingResponse>>(UiState.Idle)
    val ratingState: StateFlow<UiState<SeriesRatingResponse>> = _ratingState.asStateFlow()

    fun loadSeries(seriesId: Int) {
        viewModelScope.launch {
            _seriesDetail.value = UiState.Loading
            repo.getSeries(seriesId)
                .onSuccess {
                    _seriesDetail.value = UiState.Success(it)
                    loadProgress(seriesId)
                    loadAverageRating(seriesId)
                }
                .onFailure { _seriesDetail.value = UiState.Error(it.message ?: "Failed to load series") }
        }
    }

    private fun loadProgress(seriesId: Int) {
        viewModelScope.launch {
            watchRepo.getSeriesProgress(seriesId)
                .onSuccess { _seriesProgress.value = it }
        }
    }

    private fun loadAverageRating(seriesId: Int) {
        viewModelScope.launch {
            val result = RetrofitClient_apiService_call(seriesId)
            result?.let { _avgRating.value = it }
        }
    }

    // Shim — calls through repository
    private suspend fun RetrofitClient_apiService_call(seriesId: Int): Map<String, Any>? {
        return try {
            com.streaming.sunne.data.network.RetrofitClient.apiService
                .getSeriesAverageRating(seriesId).body()
        } catch (_: Exception) { null }
    }

    fun rateSeries(seriesId: Int, rating: Int, review: String? = null) {
        viewModelScope.launch {
            _ratingState.value = UiState.Loading
            watchRepo.rateSeries(seriesId, rating, review)
                .onSuccess { _ratingState.value = UiState.Success(it) }
                .onFailure { _ratingState.value = UiState.Error(it.message ?: "Rating failed") }
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

    init { loadStreams() }

    fun loadStreams() {
        viewModelScope.launch {
            _streams.value = UiState.Loading
            repo.listStreams()
                .onSuccess { _streams.value = UiState.Success(it) }
                .onFailure { _streams.value = UiState.Error(it.message ?: "Failed to load streams") }
        }
    }

    fun joinStream(streamId: Int) {
        viewModelScope.launch { repo.joinStream(streamId) }
    }

    fun leaveStream(streamId: Int) {
        viewModelScope.launch { repo.leaveStream(streamId) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SEARCH VIEW MODEL  (standalone — used by SearchScreen)
// ═══════════════════════════════════════════════════════════════════════════════

class SearchViewModel : ViewModel() {
    private val movieRepo  = MovieRepository()
    private val seriesRepo = SeriesRepository()
    private val genreRepo  = GenreRepository()

    private val _query   = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _movieResults  = MutableStateFlow<List<MovieResponse>>(emptyList())
    val movieResults: StateFlow<List<MovieResponse>> = _movieResults.asStateFlow()

    private val _seriesResults = MutableStateFlow<List<SeriesResponse>>(emptyList())
    val seriesResults: StateFlow<List<SeriesResponse>> = _seriesResults.asStateFlow()

    private val _genres   = MutableStateFlow<List<GenreResponse>>(emptyList())
    val genres: StateFlow<List<GenreResponse>> = _genres.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedGenre = MutableStateFlow<GenreResponse?>(null)
    val selectedGenre: StateFlow<GenreResponse?> = _selectedGenre.asStateFlow()

    init { loadGenres() }

    private fun loadGenres() {
        viewModelScope.launch {
            genreRepo.getGenres().onSuccess { _genres.value = it }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _movieResults.value = emptyList()
            _seriesResults.value = emptyList()
            _isSearching.value = false
            return
        }
        search(q)
    }

    fun selectGenre(genre: GenreResponse?) {
        _selectedGenre.value = genre
        if (genre != null) {
            searchByGenre(genre.id)
        } else if (_query.value.isNotBlank()) {
            search(_query.value)
        } else {
            _movieResults.value = emptyList()
        }
    }

    private fun search(q: String) {
        viewModelScope.launch {
            _isSearching.value = true
            movieRepo.getMovies(search = q, pageSize = 30)
                .onSuccess { _movieResults.value = it.movies }
            seriesRepo.listSeries(search = q, pageSize = 20)
                .onSuccess { _seriesResults.value = it.series }
            _isSearching.value = false
        }
    }

    private fun searchByGenre(genreId: Int) {
        viewModelScope.launch {
            _isSearching.value = true
            genreRepo.getMoviesByGenre(genreId, pageSize = 30)
                .onSuccess { _movieResults.value = it.movies }
            _isSearching.value = false
        }
    }

    fun clear() {
        _query.value = ""
        _movieResults.value = emptyList()
        _seriesResults.value = emptyList()
        _selectedGenre.value = null
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  WATCH HISTORY VIEW MODEL  (standalone — used by WatchHistoryScreen)
// ═══════════════════════════════════════════════════════════════════════════════

class WatchHistoryViewModel : ViewModel() {
    private val repo = WatchRepository()

    private val _history = MutableStateFlow<UiState<List<WatchHistoryResponse>>>(UiState.Loading)
    val history: StateFlow<UiState<List<WatchHistoryResponse>>> = _history.asStateFlow()

    private val _showCompletedOnly = MutableStateFlow(false)
    val showCompletedOnly: StateFlow<Boolean> = _showCompletedOnly.asStateFlow()

    init { load() }

    fun load(completedOnly: Boolean = false) {
        _showCompletedOnly.value = completedOnly
        viewModelScope.launch {
            _history.value = UiState.Loading
            repo.getHistory(completedOnly)
                .onSuccess { _history.value = UiState.Success(it) }
                .onFailure { _history.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    fun toggleFilter() = load(!_showCompletedOnly.value)

    fun deleteItem(movieId: Int) {
        viewModelScope.launch {
            repo.deleteHistoryItem(movieId).onSuccess {
                val cur = (_history.value as? UiState.Success)?.data ?: return@onSuccess
                _history.value = UiState.Success(cur.filter { it.movieId != movieId })
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.clearHistory().onSuccess { _history.value = UiState.Success(emptyList()) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PROFILE VIEW MODEL  (standalone — used by ProfileScreen)
// ═══════════════════════════════════════════════════════════════════════════════

class ProfileViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val recRepo  = RecommendationRepository()

    private val _updateState = MutableStateFlow<UiState<UserResponse>>(UiState.Idle)
    val updateState: StateFlow<UiState<UserResponse>> = _updateState.asStateFlow()

    private val _forYou = MutableStateFlow<List<RecommendationResponse>>(emptyList())
    val forYou: StateFlow<List<RecommendationResponse>> = _forYou.asStateFlow()

    init { loadRecommendations() }

    private fun loadRecommendations() {
        viewModelScope.launch {
            recRepo.getForYou(limit = 10)
                .onSuccess { _forYou.value = it }
        }
    }

    fun updateProfile(email: String?, fullName: String?, password: String?) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            authRepo.updateMe(UserUpdate(email, fullName, password))
                .onSuccess { _updateState.value = UiState.Success(it) }
                .onFailure { _updateState.value = UiState.Error(it.message ?: "Update failed") }
        }
    }
}