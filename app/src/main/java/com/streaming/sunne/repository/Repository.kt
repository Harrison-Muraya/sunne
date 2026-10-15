package com.streaming.sunne.repository

import com.streaming.sunne.data.model.*
import com.streaming.sunne.data.network.RetrofitClient
import com.streaming.sunne.data.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

/** Reads a `/stream` endpoint body (`url` or `stream_url`), falling back to [fallbackUrl]. */
private suspend fun fetchStreamUrl(
    call: suspend () -> retrofit2.Response<Map<String, Any>>,
    fallbackUrl: suspend () -> String?
): Result<StreamUrlResponse> = withContext(Dispatchers.IO) {
    try {
        val response = call()
        val body = if (response.isSuccessful) response.body() else null
        val url = (body?.get("url") ?: body?.get("stream_url"))?.toString()
        if (!url.isNullOrBlank()) {
            return@withContext Result.success(StreamUrlResponse(url, body?.get("quality")?.toString()))
        }
        val fallback = fallbackUrl()
        if (!fallback.isNullOrBlank()) Result.success(StreamUrlResponse(fallback))
        else Result.failure(Exception(
            if (response.isSuccessful) "No stream available for this title"
            else "Stream unavailable (${response.code()})"
        ))
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
        fetchStreamUrl(
            call        = { api.getStreamUrl(movieId, quality) },
            fallbackUrl = { api.getMovie(movieId).body()?.videoUrl }
        )

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

    suspend fun getEpisode(episodeId: Int): Result<EpisodeResponse> =
        safeCall { api.getEpisode(episodeId) }

    suspend fun getEpisodeStreamUrl(
        episode: EpisodeResponse, quality: String = "1080p"
    ): Result<StreamUrlResponse> =
        fetchStreamUrl(
            call        = { api.getEpisodeStreamUrl(episode.id, quality) },
            fallbackUrl = { episode.videoUrl }
        )

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

    suspend fun getContinueWatching(limit: Int = 10): Result<List<ContinueWatchingMovie>> =
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

    suspend fun getSeriesProgress(seriesId: Int): Result<List<EpisodeProgressItem>> =
        safeCall { api.getSeriesProgress(seriesId) }

    suspend fun getContinueWatchingSeries(limit: Int = 10): Result<List<SeriesContinueWatching>> =
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

    suspend fun getNextAfterEpisode(episodeId: Int): Result<PlayNextResponse> =
        safeCall { api.getPlayNextAfterEpisode(episodeId) }

    suspend fun getNextAfterMovie(movieId: Int): Result<PlayNextResponse> =
        safeCall { api.getPlayNextAfterMovie(movieId) }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  DISCOVERY (SEARCH, MY LIST, CATALOG)
// ═══════════════════════════════════════════════════════════════════════════════

class DiscoveryRepository {
    private val api = RetrofitClient.apiService

    suspend fun search(query: String, limit: Int = 20): Result<List<ContentItem>> =
        safeCall { api.search(query, limit) }.map { r ->
            r.movies.map { ContentItem.Movie(it) } + r.series.map { ContentItem.Series(it) }
        }

    suspend fun genres(): Result<List<GenreResponse>> = safeCall { api.getGenres() }

    /** One page of a catalog; an empty page means the end. */
    suspend fun catalogPage(source: CatalogSource, page: Int, pageSize: Int = 30): Result<List<ContentItem>> =
        when (source) {
            is CatalogSource.Movies -> safeCall {
                api.getMovies(page = page, pageSize = pageSize, genreId = source.genreId, status = "ready")
            }.map { list -> if (page > list.totalPages) emptyList() else list.movies.map { ContentItem.Movie(it) } }
            is CatalogSource.Series -> safeCall {
                api.listSeries(page = page, pageSize = pageSize, genreId = source.genreId)
            }.map { list -> if (page > list.totalPages) emptyList() else list.series.map { ContentItem.Series(it) } }
        }
}

class MyListRepository {
    private val api = RetrofitClient.apiService

    suspend fun items(): Result<List<MyListItem>> = safeCall { api.getMyList() }

    suspend fun contains(type: String, id: Int): Result<Boolean> =
        safeCall { api.getMyListStatus(type, id) }.map { it.inList }

    suspend fun add(type: String, id: Int): Result<Unit> =
        safeCall { api.addToMyList(MyListAdd(type, id)) }.map { }

    suspend fun remove(type: String, id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = api.removeFromMyList(type, id)
            if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("API error ${r.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }
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
     * Rows load in parallel; a row that fails is skipped instead of failing the whole screen.
     */
    suspend fun getHomeContent(): Result<List<ContentRow>> = withContext(Dispatchers.IO) {
        coroutineScope {
            val continueWatching = async { continueWatchingRow() }
            val fixedRows = listOf(
                async { row("Live Now") { liveRepo.listStreams().getOrThrow().filter { it.isLive }.map { ContentItem.LiveStream(it) } } },
                async { row("Featured Movies") { movieRepo.getFeaturedMovies(10).getOrThrow().map { ContentItem.Movie(it) } } },
                async { row("Featured Series", CatalogSource.Series("All Series")) { seriesRepo.getFeaturedSeries(10).getOrThrow().map { ContentItem.Series(it) } } },
                async { row("Trending Now") { movieRepo.getTrendingMovies(10).getOrThrow().map { ContentItem.Movie(it) } } },
                async { row("Trending Series", CatalogSource.Series("All Series")) { seriesRepo.getTrendingSeries(10).getOrThrow().map { ContentItem.Series(it) } } },
                async { row("Recently Added", CatalogSource.Movies("All Movies")) { movieRepo.getRecentMovies(10).getOrThrow().map { ContentItem.Movie(it) } } },
            )
            val genres = runCatching { api.getGenres().body().orEmpty() }.getOrDefault(emptyList())
            val genreRows = genres.map { genre ->
                async {
                    row(genre.name, CatalogSource.Movies(genre.name, genre.id)) {
                        coroutineScope {
                            val movies = async {
                                api.getMoviesByGenre(genre.id, pageSize = 10).body()?.movies.orEmpty().map { ContentItem.Movie(it) }
                            }
                            val series = async {
                                runCatching {
                                    api.listSeries(pageSize = 10, genreId = genre.id).body()?.series.orEmpty().map { ContentItem.Series(it) }
                                }.getOrDefault(emptyList())
                            }
                            movies.await() + series.await()
                        }
                    }
                }
            }

            val rows = listOfNotNull(continueWatching.await()) +
                fixedRows.awaitAll().filterNotNull() +
                genreRows.awaitAll().filterNotNull()

            if (rows.isEmpty()) Result.failure(Exception("Couldn't reach the server. Check your connection and try again."))
            else Result.success(rows)
        }
    }

    suspend fun continueWatchingRow(): ContentRow? {
        if (!TokenManager.isLoggedIn) return null
        return coroutineScope {
            val movies = async { watchRepo.getContinueWatching(10).getOrNull().orEmpty() }
            val series = async { episodeWatch.getContinueWatchingSeries(10).getOrNull().orEmpty() }
            val items = movies.await().mapNotNull { h ->
                h.movie?.let { ContentItem.Movie(it, progress = h.watchPercentage / 100f) }
            } + series.await().map { ContentItem.ContinueSeries(it) }
            if (items.isEmpty()) null else ContentRow(CONTINUE_WATCHING, items)
        }
    }

    private suspend fun row(
        label: String,
        source: CatalogSource? = null,
        load: suspend () -> List<ContentItem>
    ): ContentRow? =
        runCatching { load() }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { ContentRow(label, it, source) }

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
