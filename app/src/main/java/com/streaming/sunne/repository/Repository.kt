package com.streaming.sunne.repository

import com.streaming.sunne.data.model.*
import com.streaming.sunne.data.network.RetrofitClient
import com.streaming.sunne.data.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Base helper ───────────────────────────────────────────────────────────────

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

    /** Register a new account */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String? = null
    ): Result<UserResponse> = safeCall {
        api.register(UserCreate(username, email, password, fullName))
    }

    /**
     * Login — on success, stores tokens and user info in [TokenManager].
     */
    suspend fun login(username: String, password: String): Result<Token> {
        val result = safeCall { api.login(UserLogin(username, password)) }
        result.onSuccess { token ->
            TokenManager.saveTokens(token.accessToken, token.refreshToken)
            TokenManager.username = username
        }
        return result
    }

    /** Refresh the access token */
    suspend fun refreshToken(): Result<Token> {
        val rt = TokenManager.refreshToken
            ?: return Result.failure(Exception("No refresh token stored"))
        val result = safeCall { api.refreshToken(rt) }
        result.onSuccess { token ->
            TokenManager.saveTokens(token.accessToken, token.refreshToken)
        }
        return result
    }

    /** Get current user profile */
    suspend fun getMe(): Result<UserResponse> = safeCall { api.getMe() }

    /** Update current user */
    suspend fun updateMe(update: UserUpdate): Result<UserResponse> =
        safeCall { api.updateMe(update) }

    /** Logout — clears local tokens */
    suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            TokenManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            TokenManager.clear()   // clear anyway
            Result.success(Unit)
        }
    }

    /** Delete account permanently */
    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteAccount()
            TokenManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ─── MovieRepository ───────────────────────────────────────────────────────────

class MovieRepository {
    private val api = RetrofitClient.apiService

    /**
     * Fetch paginated movie list with optional filters.
     */
    suspend fun getMovies(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        genreId: Int? = null,
        status: String? = null,
        isFeatured: Boolean? = null,
        isTrending: Boolean? = null,
        releaseYear: Int? = null
    ): Result<MovieList> = safeCall {
        api.getMovies(page, pageSize, search, genreId, status, isFeatured, isTrending, releaseYear)
    }

    /** Fetch a single movie by ID */
    suspend fun getMovie(movieId: Int): Result<MovieResponse> =
        safeCall { api.getMovie(movieId) }

    /** Featured movies */
    suspend fun getFeaturedMovies(limit: Int = 10): Result<List<MovieResponse>> =
        safeCall { api.getFeaturedMovies(limit) }

    /** Trending movies */
    suspend fun getTrendingMovies(limit: Int = 10): Result<List<MovieResponse>> =
        safeCall { api.getTrendingMovies(limit) }

    /** Recently added movies */
    suspend fun getRecentMovies(limit: Int = 10): Result<List<MovieResponse>> =
        safeCall { api.getRecentMovies(limit) }

    /**
     * Fetch movies and group them into [MovieCategory] rows
     * for the HomeScreen carousels — combines featured, trending,
     * recent, and per-genre buckets in one call.
     */
    suspend fun getMovieCategories(): Result<List<MovieCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val featured  = api.getFeaturedMovies(10).body() ?: emptyList()
                val trending  = api.getTrendingMovies(10).body() ?: emptyList()
                val recent    = api.getRecentMovies(10).body()   ?: emptyList()
                val genres    = api.getGenres().body()           ?: emptyList()

                val categories = mutableListOf<MovieCategory>()

                if (featured.isNotEmpty())
                    categories += MovieCategory("Featured", featured)
                if (trending.isNotEmpty())
                    categories += MovieCategory("Trending Now", trending)
                if (recent.isNotEmpty())
                    categories += MovieCategory("Recently Added", recent)

                // Add per-genre rows (skip if already covered above)
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
     * Get the streaming URL for a movie.
     * Quality: "1080p" | "720p" | "480p"
     */
    suspend fun getStreamUrl(
        movieId: Int,
        quality: String = "1080p"
    ): Result<StreamUrlResponse> = safeCall { api.getStreamUrl(movieId, quality) }

    /** Increment view count when playback starts */
    suspend fun incrementViewCount(movieId: Int) {
        withContext(Dispatchers.IO) {
            try { api.incrementViewCount(movieId) } catch (_: Exception) {}
        }
    }

    /** Create a movie (admin only) */
    suspend fun createMovie(movie: MovieCreate): Result<MovieResponse> =
        safeCall { api.createMovie(movie) }

    /** Update a movie (admin only) */
    suspend fun updateMovie(movieId: Int, update: MovieUpdate): Result<MovieResponse> =
        safeCall { api.updateMovie(movieId, update) }

    /** Delete a movie (admin only) */
    suspend fun deleteMovie(movieId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteMovie(movieId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

// ─── GenreRepository ───────────────────────────────────────────────────────────

class GenreRepository {
    private val api = RetrofitClient.apiService

    suspend fun getGenres(): Result<List<GenreResponse>> =
        safeCall { api.getGenres() }

    suspend fun getGenre(genreId: Int): Result<GenreResponse> =
        safeCall { api.getGenre(genreId) }

    suspend fun getGenreBySlug(slug: String): Result<GenreResponse> =
        safeCall { api.getGenreBySlug(slug) }

    suspend fun getMoviesByGenre(
        genreId: Int,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<MovieList> = safeCall { api.getMoviesByGenre(genreId, page, pageSize) }

    suspend fun createGenre(name: String, slug: String): Result<GenreResponse> =
        safeCall { api.createGenre(GenreCreate(name, slug)) }

    suspend fun deleteGenre(genreId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteGenre(genreId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}