package com.streaming.sunne.repository

import com.streaming.sunne.data.model.*
import com.streaming.sunne.data.network.RetrofitClient
import com.streaming.sunne.data.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════════
//  SAFE CALL HELPER
// ═══════════════════════════════════════════════════════════════════════════════

private suspend inline fun <T> safeCall(
    crossinline call: suspend () -> retrofit2.Response<T>
): Result<T> = withContext(Dispatchers.IO) {
    try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) Result.success(body)
            else Result.failure(Exception("Empty response body"))
        } else {
            Result.failure(Exception("API error ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  AUTH REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun register(
        username: String, email: String,
        password: String, fullName: String? = null
    ): Result<UserResponse> = safeCall {
        api.register(UserCreate(username, email, password, fullName))
    }

    suspend fun login(username: String, password: String): Result<Token> {
        val result = safeCall { api.login(UserLogin(username, password)) }
        result.onSuccess { token ->
            TokenManager.saveTokens(token.accessToken, token.refreshToken)
            TokenManager.username = username
        }
        return result
    }

    suspend fun refreshToken(): Result<Token> {
        val rt = TokenManager.refreshToken
            ?: return Result.failure(Exception("No refresh token"))
        val result = safeCall { api.refreshToken(rt) }
        result.onSuccess { TokenManager.saveTokens(it.accessToken, it.refreshToken) }
        return result
    }

    suspend fun getMe(): Result<UserResponse> = safeCall { api.getMe() }

    suspend fun updateMe(update: UserUpdate): Result<UserResponse> =
        safeCall { api.updateMe(update) }

    suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            TokenManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            TokenManager.clear()
            Result.success(Unit)
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try { api.deleteAccount(); TokenManager.clear(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MOVIE REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class MovieRepository {
    private val api = RetrofitClient.apiService

    suspend fun getMovies(
        page: Int = 1, pageSize: Int = 20,
        search: String? = null, genreId: Int? = null,
        status: String? = null, isFeatured: Boolean? = null,
        isTrending: Boolean? = null, releaseYear: Int? = null
    ): Result<MovieList> = safeCall {
        api.getMovies(page, pageSize, search, genreId, status, isFeatured, isTrending, releaseYear)
    }

    suspend fun getMovie(movieId: Int): Result<MovieResponse> =
        safeCall { api.getMovie(movieId) }

    suspend fun getFeaturedMovies(limit: Int = 10): Result<List<MovieResponse>> =
        safeCall { api.getFeaturedMovies(limit) }

    suspend fun getTrendingMovies(limit: Int = 10): Result<List<MovieResponse>> =
        safeCall { api.getTrendingMovies(limit) }

    suspend fun getRecentMovies(limit: Int = 10): Result<List<MovieResponse>> =
        safeCall { api.getRecentMovies(limit) }

    suspend fun getStreamUrl(movieId: Int, quality: String = "1080p"): Result<StreamUrlResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getStreamUrl(movieId, quality)
                if (response.isSuccessful) {
                    val body = response.body()
                    val url = body?.get("url")?.toString()
                    if (!url.isNullOrBlank()) {
                        val q = body["quality"]?.toString()
                        Result.success(StreamUrlResponse(url = url, quality = q))
                    } else {
                        val movie = api.getMovie(movieId).body()
                        val fallback = movie?.videoUrl
                        if (!fallback.isNullOrBlank()) {
                            Result.success(StreamUrlResponse(url = fallback))
                        } else {
                            Result.failure(Exception("No stream URL available"))
                        }
                    }
                } else {
                    val movie = api.getMovie(movieId).body()
                    val fallback = movie?.videoUrl
                    if (!fallback.isNullOrBlank()) {
                        Result.success(StreamUrlResponse(url = fallback))
                    } else {
                        Result.failure(Exception("Stream unavailable (${response.code()})"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun incrementViewCount(movieId: Int) {
        withContext(Dispatchers.IO) {
            try { api.incrementViewCount(movieId) } catch (_: Exception) {}
        }
    }

    suspend fun createMovie(movie: MovieCreate): Result<MovieResponse> =
        safeCall { api.createMovie(movie) }

    suspend fun updateMovie(movieId: Int, update: MovieUpdate): Result<MovieResponse> =
        safeCall { api.updateMovie(movieId, update) }

    suspend fun deleteMovie(movieId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try { api.deleteMovie(movieId); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SERIES REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class SeriesRepository {
    private val api = RetrofitClient.apiService

    suspend fun listSeries(
        page: Int = 1, pageSize: Int = 20,
        search: String? = null, featured: Boolean? = null, trending: Boolean? = null
    ): Result<SeriesListResponse> = safeCall {
        api.listSeries(page, pageSize, search, featured, trending)
    }

    suspend fun getSeries(seriesId: Int): Result<SeriesResponse> =
        safeCall { api.getSeries(seriesId) }

    suspend fun getFeaturedSeries(limit: Int = 10): Result<List<SeriesResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val result = api.listSeries(featured = true, pageSize = limit)
                if (result.isSuccessful) Result.success(result.body()?.series ?: emptyList())
                else Result.failure(Exception("Failed to fetch featured series"))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getTrendingSeries(limit: Int = 10): Result<List<SeriesResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val result = api.listSeries(trending = true, pageSize = limit)
                if (result.isSuccessful) Result.success(result.body()?.series ?: emptyList())
                else Result.failure(Exception("Failed to fetch trending series"))
            } catch (e: Exception) { Result.failure(e) }
        }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  WATCH REPOSITORY (MOVIES)
// ═══════════════════════════════════════════════════════════════════════════════

class WatchRepository {
    private val api = RetrofitClient.apiService

    suspend fun updateProgress(
        movieId: Int, position: Int, percentage: Float, completed: Boolean = false
    ): Result<WatchHistoryResponse> = safeCall {
        api.updateWatchProgress(WatchProgressUpdate(movieId, position, percentage, completed))
    }

    suspend fun getProgress(movieId: Int): Result<WatchHistoryResponse?> =
        withContext(Dispatchers.IO) {
            try {
                val r = api.getWatchProgress(movieId)
                Result.success(r.body())
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getHistory(completedOnly: Boolean = false, limit: Int = 50)
            : Result<List<WatchHistoryResponse>> =
        safeCall { api.getWatchHistory(completedOnly, limit) }

    suspend fun getContinueWatching(limit: Int = 10): Result<List<WatchHistoryResponse>> =
        safeCall { api.getContinueWatching(limit) }

    suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try { api.clearWatchHistory(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteHistoryItem(movieId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try { api.deleteHistoryItem(movieId); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun rateMovie(movieId: Int, rating: Int, review: String? = null)
            : Result<MovieRatingResponse> =
        safeCall { api.rateMovie(MovieRatingCreate(movieId, rating, review)) }

    suspend fun getMyRating(movieId: Int): Result<MovieRatingResponse?> =
        withContext(Dispatchers.IO) {
            try { Result.success(api.getMyRating(movieId).body()) }
            catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getAverageRating(movieId: Int): Result<AverageRatingResponse> =
        safeCall { api.getAverageRating(movieId) }

    suspend fun deleteMyRating(movieId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try { api.deleteMyRating(movieId); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  EPISODE WATCH REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class EpisodeWatchRepository {
    private val api = RetrofitClient.apiService

    suspend fun updateProgress(
        episodeId: Int, position: Int, percentage: Float, completed: Boolean = false
    ): Result<EpisodeProgressResponse> = safeCall {
        api.updateEpisodeProgress(EpisodeProgressUpdate(episodeId, position, percentage, completed))
    }

    suspend fun getProgress(episodeId: Int): Result<EpisodeProgressResponse?> =
        withContext(Dispatchers.IO) {
            try { Result.success(api.getEpisodeProgress(episodeId).body()) }
            catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getSeriesProgress(seriesId: Int): Result<Map<String, Any>> =
        safeCall { api.getSeriesProgress(seriesId) }

    suspend fun getContinueWatchingSeries(limit: Int = 10): Result<List<Map<String, Any>>> =
        safeCall { api.getContinueWatchingSeries(limit) }

    suspend fun rateSeries(seriesId: Int, rating: Int, review: String? = null)
            : Result<SeriesRatingResponse> =
        safeCall { api.rateSeries(SeriesRatingCreate(seriesId, rating, review)) }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  LIVE STREAM REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class LiveStreamRepository {
    private val api = RetrofitClient.apiService

    suspend fun listStreams(): Result<List<LiveStreamPublicResponse>> =
        safeCall { api.listLiveStreams() }

    suspend fun getStream(streamId: Int): Result<LiveStreamPublicResponse> =
        safeCall { api.getLiveStream(streamId) }

    suspend fun joinStream(streamId: Int) {
        withContext(Dispatchers.IO) {
            try { api.joinLiveStream(streamId) } catch (_: Exception) {}
        }
    }

    suspend fun leaveStream(streamId: Int) {
        withContext(Dispatchers.IO) {
            try { api.leaveLiveStream(streamId) } catch (_: Exception) {}
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  RECOMMENDATION REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class RecommendationRepository {
    private val api = RetrofitClient.apiService

    suspend fun getForYou(limit: Int = 20, strategy: String = "auto")
            : Result<List<RecommendationResponse>> =
        safeCall { api.getRecommendationsForYou(limit, strategy) }

    suspend fun getSimilar(movieId: Int, limit: Int = 10)
            : Result<List<SimilarMovieResponse>> =
        safeCall { api.getSimilarMovies(movieId, limit) }

    suspend fun getTrending(limit: Int = 10)
            : Result<List<TrendingMovieResponse>> =
        safeCall { api.getRecommendationTrending(limit) }

    suspend fun getBecauseYouWatched(movieId: Int, limit: Int = 10)
            : Result<List<RecommendationResponse>> =
        safeCall { api.getBecauseYouWatched(movieId, limit) }

    suspend fun getUnified(limit: Int = 20): Result<Map<String, Any>> =
        safeCall { api.getUnifiedRecommendations(limit) }

    suspend fun getSimilarContent(contentType: String, contentId: Int, limit: Int = 10)
            : Result<Map<String, Any>> =
        safeCall { api.getSimilarContent(contentType, contentId, limit) }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PLAY NEXT REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class PlayNextRepository {
    private val api = RetrofitClient.apiService

    suspend fun getNextAfterEpisode(episodeId: Int): Result<Map<String, Any>> =
        safeCall { api.getPlayNextAfterEpisode(episodeId) }

    suspend fun getNextAfterMovie(movieId: Int): Result<Map<String, Any>> =
        safeCall { api.getPlayNextAfterMovie(movieId) }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GENRE REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════

class GenreRepository {
    private val api = RetrofitClient.apiService

    suspend fun getGenres(): Result<List<GenreResponse>> = safeCall { api.getGenres() }

    suspend fun getGenre(genreId: Int): Result<GenreResponse> =
        safeCall { api.getGenre(genreId) }

    suspend fun getGenreBySlug(slug: String): Result<GenreResponse> =
        safeCall { api.getGenreBySlug(slug) }

    suspend fun getMoviesByGenre(genreId: Int, page: Int = 1, pageSize: Int = 20)
            : Result<MovieList> = safeCall { api.getMoviesByGenre(genreId, page, pageSize) }

    suspend fun createGenre(name: String, slug: String): Result<GenreResponse> =
        safeCall { api.createGenre(GenreCreate(name, slug)) }

    suspend fun deleteGenre(genreId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try { api.deleteGenre(genreId); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HOME CONTENT REPOSITORY (UNIFIED MOVIES + SERIES + LIVE)
// ═══════════════════════════════════════════════════════════════════════════════

class HomeContentRepository {
    private val api          = RetrofitClient.apiService
    private val movieRepo    = MovieRepository()
    private val seriesRepo   = SeriesRepository()
    private val liveRepo     = LiveStreamRepository()
    private val watchRepo    = WatchRepository()
    private val episodeWatch = EpisodeWatchRepository()

    /**
     * Builds all HomeScreen carousel rows with mixed content types.
     */
    suspend fun getHomeContent(): Result<List<ContentRow>> = withContext(Dispatchers.IO) {
        try {
            val rows = mutableListOf<ContentRow>()

            // Continue Watching (Movies + Series combined)
            if (TokenManager.isLoggedIn) {
                val continueMovies = watchRepo.getContinueWatching(10).getOrNull() ?: emptyList()
                val continueSeries = episodeWatch.getContinueWatchingSeries(10).getOrNull() ?: emptyList()

                val continueItems = mutableListOf<ContentItem>()
                continueMovies.forEach { history ->
                    movieRepo.getMovie(history.movieId).getOrNull()?.let {
                        continueItems.add(ContentItem.Movie(it))
                    }
                }
                // Note: continueSeries needs parsing — skip for now or implement parser

                if (continueItems.isNotEmpty()) {
                    rows.add(ContentRow("Continue Watching", continueItems))
                }
            }

            // Featured Movies
            movieRepo.getFeaturedMovies(10).getOrNull()?.let { movies ->
                if (movies.isNotEmpty()) {
                    rows.add(ContentRow("Featured Movies", movies.map { ContentItem.Movie(it) }))
                }
            }

            // Featured Series
            seriesRepo.getFeaturedSeries(10).getOrNull()?.let { series ->
                if (series.isNotEmpty()) {
                    rows.add(ContentRow("Featured Series", series.map { ContentItem.Series(it) }))
                }
            }

            // Trending Movies
            movieRepo.getTrendingMovies(10).getOrNull()?.let { movies ->
                if (movies.isNotEmpty()) {
                    rows.add(ContentRow("Trending Now", movies.map { ContentItem.Movie(it) }))
                }
            }

            // Trending Series
            seriesRepo.getTrendingSeries(10).getOrNull()?.let { series ->
                if (series.isNotEmpty()) {
                    rows.add(ContentRow("Trending Series", series.map { ContentItem.Series(it) }))
                }
            }

            // Recently Added
            movieRepo.getRecentMovies(10).getOrNull()?.let { movies ->
                if (movies.isNotEmpty()) {
                    rows.add(ContentRow("Recently Added", movies.map { ContentItem.Movie(it) }))
                }
            }

            // Live Streams (if any active)
            liveRepo.listStreams().getOrNull()?.let { streams ->
                val live = streams.filter { it.isLive }
                if (live.isNotEmpty()) {
                    rows.add(ContentRow("🔴 Live Now", live.map { ContentItem.LiveStream(it) }))
                }
            }

            // Genre rows
            val genres = api.getGenres().body() ?: emptyList()
            genres.forEach { genre ->
                val genreMovies = api.getMoviesByGenre(genre.id, pageSize = 10).body()?.movies
                if (!genreMovies.isNullOrEmpty()) {
                    rows.add(ContentRow(genre.name, genreMovies.map { ContentItem.Movie(it) }))
                }
            }

            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Legacy compatibility — converts ContentRows back to MovieCategory */
    @Deprecated("Use getHomeContent() instead")
    suspend fun getMovieCategories(): Result<List<MovieCategory>> = withContext(Dispatchers.IO) {
        try {
            val rows = getHomeContent().getOrNull() ?: return@withContext Result.success(emptyList())
            val categories = rows.mapNotNull { row ->
                val movies = row.items.filterIsInstance<ContentItem.Movie>().map { it.data }
                if (movies.isNotEmpty()) MovieCategory(row.label, movies) else null
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

