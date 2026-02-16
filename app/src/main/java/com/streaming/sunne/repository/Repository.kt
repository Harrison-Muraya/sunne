package com.streaming.sunne.repository

import com.streaming.sunne.data.model.*
import com.streaming.sunne.data.network.RetrofitClient
import com.streaming.sunne.data.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Safe call helper ──────────────────────────────────────────────────────────

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

// ─── AuthRepository ────────────────────────────────────────────────────────────

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

// ─── MovieRepository ───────────────────────────────────────────────────────────

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

    /**
     * Builds all HomeScreen carousel rows.
     * Adds "Continue Watching" row if user has unfinished movies.
     */
    suspend fun getMovieCategories(): Result<List<MovieCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val featured = api.getFeaturedMovies(10).body() ?: emptyList()
                val trending = api.getTrendingMovies(10).body() ?: emptyList()
                val recent = api.getRecentMovies(10).body() ?: emptyList()
                val genres = api.getGenres().body() ?: emptyList()

                // Continue Watching — only if logged in
                val continueWatching: List<MovieResponse> = if (TokenManager.isLoggedIn) {
                    try {
                        val history = api.getContinueWatching(10).body() ?: emptyList()
                        // Fetch the actual MovieResponse for each history item
                        history.mapNotNull { h ->
                            api.getMovie(h.movieId).body()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()

                val categories = mutableListOf<MovieCategory>()

                if (continueWatching.isNotEmpty())
                    categories += MovieCategory("Continue Watching", continueWatching)
                if (featured.isNotEmpty())
                    categories += MovieCategory("Featured", featured)
                if (trending.isNotEmpty())
                    categories += MovieCategory("Trending Now", trending)
                if (recent.isNotEmpty())
                    categories += MovieCategory("Recently Added", recent)

                genres.forEach { genre ->
                    val genreMovies = api.getMoviesByGenre(genre.id, pageSize = 10).body()?.movies
                    if (!genreMovies.isNullOrEmpty()) {
                        categories += MovieCategory(genre.name, genreMovies)
                    }
                }

                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * GET /api/v1/movies/{id}/stream — returns dynamic {} JSON.
     * Extracts the "url" string from the map.
     */
    suspend fun getStreamUrl(movieId: Int, quality: String = "1080p"): Result<StreamUrlResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getStreamUrl(movieId, quality)
                if (response.isSuccessful) {
                    val body = response.body()
                    // The API returns { "url": "...", "quality": "..." }
                    val url = body?.get("url")?.toString()
                    if (!url.isNullOrBlank()) {
                        val q = body["quality"]?.toString()
                        Result.success(StreamUrlResponse(url = url, quality = q))
                    } else {
                        // Fallback: try the video_url field on the movie itself
                        val movie = api.getMovie(movieId).body()
                        val fallback = movie?.videoUrl
                        if (!fallback.isNullOrBlank()) {
                            Result.success(StreamUrlResponse(url = fallback))
                        } else {
                            Result.failure(Exception("No stream URL available for this movie"))
                        }
                    }
                } else {
                    // On 4xx/5xx also try video_url fallback
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

    /** Fire-and-forget view count increment */
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

// ─── WatchRepository ───────────────────────────────────────────────────────────

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

// ─── RecommendationRepository ──────────────────────────────────────────────────

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
}

// ─── GenreRepository ───────────────────────────────────────────────────────────

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


//package com.streaming.sunne.repository
//
//import com.streaming.sunne.data.model.*
//import com.streaming.sunne.data.network.RetrofitClient
//import com.streaming.sunne.data.network.TokenManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//// ─── Base helper ───────────────────────────────────────────────────────────────
//
//private suspend inline fun <T> safeCall(
//    crossinline call: suspend () -> retrofit2.Response<T>
//): Result<T> = withContext(Dispatchers.IO) {
//    try {
//        val response = call()
//        if (response.isSuccessful) {
//            val body = response.body()
//            if (body != null) Result.success(body)
//            else Result.failure(Exception("Empty response body"))
//        } else {
//            Result.failure(Exception("API error ${response.code()}: ${response.message()}"))
//        }
//    } catch (e: Exception) {
//        Result.failure(e)
//    }
//}
//
//// ─── AuthRepository ────────────────────────────────────────────────────────────
//
//class AuthRepository {
//    private val api = RetrofitClient.apiService
//
//    /** Register a new account */
//    suspend fun register(
//        username: String,
//        email: String,
//        password: String,
//        fullName: String? = null
//    ): Result<UserResponse> = safeCall {
//        api.register(UserCreate(username, email, password, fullName))
//    }
//
//    /**
//     * Login — on success, stores tokens and user info in [TokenManager].
//     */
//    suspend fun login(username: String, password: String): Result<Token> {
//        val result = safeCall { api.login(UserLogin(username, password)) }
//        result.onSuccess { token ->
//            TokenManager.saveTokens(token.accessToken, token.refreshToken)
//            TokenManager.username = username
//        }
//        return result
//    }
//
//    /** Refresh the access token */
//    suspend fun refreshToken(): Result<Token> {
//        val rt = TokenManager.refreshToken
//            ?: return Result.failure(Exception("No refresh token stored"))
//        val result = safeCall { api.refreshToken(rt) }
//        result.onSuccess { token ->
//            TokenManager.saveTokens(token.accessToken, token.refreshToken)
//        }
//        return result
//    }
//
//    /** Get current user profile */
//    suspend fun getMe(): Result<UserResponse> = safeCall { api.getMe() }
//
//    /** Update current user */
//    suspend fun updateMe(update: UserUpdate): Result<UserResponse> =
//        safeCall { api.updateMe(update) }
//
//    /** Logout — clears local tokens */
//    suspend fun logout(): Result<Unit> {
//        return try {
//            api.logout()
//            TokenManager.clear()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            TokenManager.clear()   // clear anyway
//            Result.success(Unit)
//        }
//    }
//
//    /** Delete account permanently */
//    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
//        try {
//            api.deleteAccount()
//            TokenManager.clear()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}
//
//// ─── MovieRepository ───────────────────────────────────────────────────────────
//
//class MovieRepository {
//    private val api = RetrofitClient.apiService
//
//    /**
//     * Fetch paginated movie list with optional filters.
//     */
//    suspend fun getMovies(
//        page: Int = 1,
//        pageSize: Int = 20,
//        search: String? = null,
//        genreId: Int? = null,
//        status: String? = null,
//        isFeatured: Boolean? = null,
//        isTrending: Boolean? = null,
//        releaseYear: Int? = null
//    ): Result<MovieList> = safeCall {
//        api.getMovies(page, pageSize, search, genreId, status, isFeatured, isTrending, releaseYear)
//    }
//
//    /** Fetch a single movie by ID */
//    suspend fun getMovie(movieId: Int): Result<MovieResponse> =
//        safeCall { api.getMovie(movieId) }
//
//    /** Featured movies */
//    suspend fun getFeaturedMovies(limit: Int = 10): Result<List<MovieResponse>> =
//        safeCall { api.getFeaturedMovies(limit) }
//
//    /** Trending movies */
//    suspend fun getTrendingMovies(limit: Int = 10): Result<List<MovieResponse>> =
//        safeCall { api.getTrendingMovies(limit) }
//
//    /** Recently added movies */
//    suspend fun getRecentMovies(limit: Int = 10): Result<List<MovieResponse>> =
//        safeCall { api.getRecentMovies(limit) }
//
//    /**
//     * Fetch movies and group them into [MovieCategory] rows
//     * for the HomeScreen carousels — combines featured, trending,
//     * recent, and per-genre buckets in one call.
//     */
//    suspend fun getMovieCategories(): Result<List<MovieCategory>> =
//        withContext(Dispatchers.IO) {
//            try {
//                val featured  = api.getFeaturedMovies(10).body() ?: emptyList()
//                val trending  = api.getTrendingMovies(10).body() ?: emptyList()
//                val recent    = api.getRecentMovies(10).body()   ?: emptyList()
//                val genres    = api.getGenres().body()           ?: emptyList()
//
//                val categories = mutableListOf<MovieCategory>()
//
//                if (featured.isNotEmpty())
//                    categories += MovieCategory("Featured", featured)
//                if (trending.isNotEmpty())
//                    categories += MovieCategory("Trending Now", trending)
//                if (recent.isNotEmpty())
//                    categories += MovieCategory("Recently Added", recent)
//
//                // Add per-genre rows (skip if already covered above)
//                genres.forEach { genre ->
//                    val genreMovies = api.getMoviesByGenre(genre.id, pageSize = 10).body()?.movies
//                    if (!genreMovies.isNullOrEmpty()) {
//                        categories += MovieCategory(genre.name, genreMovies)
//                    }
//                }
//
//                Result.success(categories)
//            } catch (e: Exception) {
//                Result.failure(e)
//            }
//        }
//
//    /**
//     * Get the streaming URL for a movie.
//     * Quality: "1080p" | "720p" | "480p"
//     */
//    suspend fun getStreamUrl(
//        movieId: Int,
//        quality: String = "1080p"
//    ): Result<StreamUrlResponse> = safeCall { api.getStreamUrl(movieId, quality) }
//
//    /** Increment view count when playback starts */
//    suspend fun incrementViewCount(movieId: Int) {
//        withContext(Dispatchers.IO) {
//            try { api.incrementViewCount(movieId) } catch (_: Exception) {}
//        }
//    }
//
//    /** Create a movie (admin only) */
//    suspend fun createMovie(movie: MovieCreate): Result<MovieResponse> =
//        safeCall { api.createMovie(movie) }
//
//    /** Update a movie (admin only) */
//    suspend fun updateMovie(movieId: Int, update: MovieUpdate): Result<MovieResponse> =
//        safeCall { api.updateMovie(movieId, update) }
//
//    /** Delete a movie (admin only) */
//    suspend fun deleteMovie(movieId: Int): Result<Unit> = withContext(Dispatchers.IO) {
//        try {
//            api.deleteMovie(movieId)
//            Result.success(Unit)
//        } catch (e: Exception) { Result.failure(e) }
//    }
//}
//
//// ─── GenreRepository ───────────────────────────────────────────────────────────
//
//class GenreRepository {
//    private val api = RetrofitClient.apiService
//
//    suspend fun getGenres(): Result<List<GenreResponse>> =
//        safeCall { api.getGenres() }
//
//    suspend fun getGenre(genreId: Int): Result<GenreResponse> =
//        safeCall { api.getGenre(genreId) }
//
//    suspend fun getGenreBySlug(slug: String): Result<GenreResponse> =
//        safeCall { api.getGenreBySlug(slug) }
//
//    suspend fun getMoviesByGenre(
//        genreId: Int,
//        page: Int = 1,
//        pageSize: Int = 20
//    ): Result<MovieList> = safeCall { api.getMoviesByGenre(genreId, page, pageSize) }
//
//    suspend fun createGenre(name: String, slug: String): Result<GenreResponse> =
//        safeCall { api.createGenre(GenreCreate(name, slug)) }
//
//    suspend fun deleteGenre(genreId: Int): Result<Unit> = withContext(Dispatchers.IO) {
//        try {
//            api.deleteGenre(genreId)
//            Result.success(Unit)
//        } catch (e: Exception) { Result.failure(e) }
//    }
//}
