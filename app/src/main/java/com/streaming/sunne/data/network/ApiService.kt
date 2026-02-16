package com.streaming.sunne.data.network

import com.streaming.sunne.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Health ────────────────────────────────────────────────────────────────

    @GET("health")
    suspend fun healthCheck(): Response<Any>

    // ── Authentication ────────────────────────────────────────────────────────

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: UserCreate): Response<UserResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: UserLogin): Response<Token>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Query("refresh_token") refreshToken: String): Response<Token>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<UserResponse>

    @PUT("api/v1/auth/me")
    suspend fun updateMe(@Body body: UserUpdate): Response<UserResponse>

    @DELETE("api/v1/auth/me")
    suspend fun deleteAccount(): Response<Unit>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Any>

    // ── Movies ────────────────────────────────────────────────────────────────

    @GET("api/v1/movies/")
    suspend fun getMovies(
        @Query("page")         page: Int          = 1,
        @Query("page_size")    pageSize: Int       = 20,
        @Query("search")       search: String?     = null,
        @Query("genre_id")     genreId: Int?       = null,
        @Query("status")       status: String?     = null,
        @Query("is_featured")  isFeatured: Boolean? = null,
        @Query("is_trending")  isTrending: Boolean? = null,
        @Query("release_year") releaseYear: Int?   = null
    ): Response<MovieList>

    @GET("api/v1/movies/{movie_id}")
    suspend fun getMovie(@Path("movie_id") movieId: Int): Response<MovieResponse>

    @POST("api/v1/movies/")
    suspend fun createMovie(@Body body: MovieCreate): Response<MovieResponse>

    @PUT("api/v1/movies/{movie_id}")
    suspend fun updateMovie(
        @Path("movie_id") movieId: Int,
        @Body body: MovieUpdate
    ): Response<MovieResponse>

    @DELETE("api/v1/movies/{movie_id}")
    suspend fun deleteMovie(@Path("movie_id") movieId: Int): Response<Unit>

    @POST("api/v1/movies/{movie_id}/view")
    suspend fun incrementViewCount(@Path("movie_id") movieId: Int): Response<Any>

    // ── Stream URL ────────────────────────────────────────────────────────────
    // Returns {} (dynamic) — we parse as Map<String, Any> and extract "url" key.
    @GET("api/v1/movies/{movie_id}/stream")
    suspend fun getStreamUrl(
        @Path("movie_id") movieId: Int,
        @Query("quality")  quality: String = "1080p"
    ): Response<Map<String, Any>>

    // ── Movies with progress ──────────────────────────────────────────────────

    @GET("api/v1/movies/with-progress")
    suspend fun getMoviesWithProgress(
        @Query("page")      page: Int      = 1,
        @Query("page_size") pageSize: Int  = 20,
        @Query("search")    search: String? = null,
        @Query("genre_id")  genreId: Int?  = null,
        @Query("status")    status: String? = null
    ): Response<MovieList>

    // ── Genres ────────────────────────────────────────────────────────────────

    @GET("api/v1/movies/genres")
    suspend fun getGenres(): Response<List<GenreResponse>>

    @POST("api/v1/movies/genres")
    suspend fun createGenre(@Body body: GenreCreate): Response<GenreResponse>

    @GET("api/v1/movies/genres/{genre_id}")
    suspend fun getGenre(@Path("genre_id") genreId: Int): Response<GenreResponse>

    @DELETE("api/v1/movies/genres/{genre_id}")
    suspend fun deleteGenre(@Path("genre_id") genreId: Int): Response<Unit>

    @GET("api/v1/movies/genres/slug/{slug}")
    suspend fun getGenreBySlug(@Path("slug") slug: String): Response<GenreResponse>

    @GET("api/v1/movies/genres/{genre_id}/movies")
    suspend fun getMoviesByGenre(
        @Path("genre_id") genreId: Int,
        @Query("page")      page: Int     = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<MovieList>

    // ── Collections ───────────────────────────────────────────────────────────

    @GET("api/v1/movies/collections/featured")
    suspend fun getFeaturedMovies(@Query("limit") limit: Int = 10): Response<List<MovieResponse>>

    @GET("api/v1/movies/collections/trending")
    suspend fun getTrendingMovies(@Query("limit") limit: Int = 10): Response<List<MovieResponse>>

    @GET("api/v1/movies/collections/recent")
    suspend fun getRecentMovies(@Query("limit") limit: Int = 10): Response<List<MovieResponse>>

    // ── Watch History ─────────────────────────────────────────────────────────

    @POST("api/v1/watch/progress")
    suspend fun updateWatchProgress(@Body body: WatchProgressUpdate): Response<WatchHistoryResponse>

    @GET("api/v1/watch/progress/{movie_id}")
    suspend fun getWatchProgress(@Path("movie_id") movieId: Int): Response<WatchHistoryResponse?>

    @GET("api/v1/watch/history")
    suspend fun getWatchHistory(
        @Query("completed_only") completedOnly: Boolean = false,
        @Query("limit")          limit: Int             = 50
    ): Response<List<WatchHistoryResponse>>

    @DELETE("api/v1/watch/history")
    suspend fun clearWatchHistory(): Response<Any>

    @DELETE("api/v1/watch/history/{movie_id}")
    suspend fun deleteHistoryItem(@Path("movie_id") movieId: Int): Response<Unit>

    // "Continue Watching" — movies started but not finished
    @GET("api/v1/watch/continue-watching")
    suspend fun getContinueWatching(@Query("limit") limit: Int = 10): Response<List<WatchHistoryResponse>>

    // ── Ratings ───────────────────────────────────────────────────────────────

    @POST("api/v1/watch/ratings")
    suspend fun rateMovie(@Body body: MovieRatingCreate): Response<MovieRatingResponse>

    @GET("api/v1/watch/ratings/{movie_id}/my-rating")
    suspend fun getMyRating(@Path("movie_id") movieId: Int): Response<MovieRatingResponse?>

    @GET("api/v1/watch/ratings/{movie_id}")
    suspend fun getMovieRatings(
        @Path("movie_id") movieId: Int,
        @Query("limit")   limit: Int = 50
    ): Response<List<MovieRatingResponse>>

    @GET("api/v1/watch/ratings/{movie_id}/average")
    suspend fun getAverageRating(@Path("movie_id") movieId: Int): Response<AverageRatingResponse>

    @DELETE("api/v1/watch/ratings/{movie_id}")
    suspend fun deleteMyRating(@Path("movie_id") movieId: Int): Response<Unit>

    // ── Recommendations ───────────────────────────────────────────────────────

    @GET("api/v1/recommendations/for-you")
    suspend fun getRecommendationsForYou(
        @Query("limit")    limit: Int    = 20,
        @Query("strategy") strategy: String = "auto"
    ): Response<List<RecommendationResponse>>

    @GET("api/v1/recommendations/similar/{movie_id}")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("limit")   limit: Int = 10
    ): Response<List<SimilarMovieResponse>>

    @GET("api/v1/recommendations/trending")
    suspend fun getRecommendationTrending(
        @Query("limit") limit: Int = 10
    ): Response<List<TrendingMovieResponse>>

    @GET("api/v1/recommendations/because-you-watched/{movie_id}")
    suspend fun getBecauseYouWatched(
        @Path("movie_id") movieId: Int,
        @Query("limit")   limit: Int = 10
    ): Response<List<RecommendationResponse>>

    // ── Admin ─────────────────────────────────────────────────────────────────

    @POST("api/v1/admin/upload/{movie_id}")
    @Multipart
    suspend fun uploadVideo(
        @Path("movie_id") movieId: Int,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<UploadResponse>

    @GET("api/v1/admin/conversions/{job_id}")
    suspend fun getConversionStatus(@Path("job_id") jobId: Int): Response<ConversionStatusResponse>

    @DELETE("api/v1/admin/conversions/{job_id}")
    suspend fun cancelConversion(@Path("job_id") jobId: Int): Response<Any>

    @GET("api/v1/admin/conversions")
    suspend fun listConversionJobs(
        @Query("status") status: String? = null,
        @Query("limit")  limit: Int      = 20
    ): Response<List<ConversionJobResponse>>
}





//package com.streaming.sunne.data.network
//
//import com.streaming.sunne.data.model.*
//import retrofit2.Response
//import retrofit2.http.*
//
///**
// * Retrofit interface — mirrors every endpoint in the FastAPI OpenAPI spec.
// *
// * Auth header is injected automatically by [AuthInterceptor] for secured routes.
// * The @Header("Authorization") parameter is NOT needed here because of that.
// */
//interface ApiService {
//
//    // ── Health ────────────────────────────────────────────────────────────────
//
//    @GET("health")
//    suspend fun healthCheck(): Response<Any>
//
//    // ── Authentication ────────────────────────────────────────────────────────
//
//    /** POST /api/v1/auth/register */
//    @POST("api/v1/auth/register")
//    suspend fun register(
//        @Body body: UserCreate
//    ): Response<UserResponse>
//
//    /** POST /api/v1/auth/login */
//    @POST("api/v1/auth/login")
//    suspend fun login(
//        @Body body: UserLogin
//    ): Response<Token>
//
//    /** POST /api/v1/auth/refresh?refresh_token=... */
//    @POST("api/v1/auth/refresh")
//    suspend fun refreshToken(
//        @Query("refresh_token") refreshToken: String
//    ): Response<Token>
//
//    /** GET /api/v1/auth/me */
//    @GET("api/v1/auth/me")
//    suspend fun getMe(): Response<UserResponse>
//
//    /** PUT /api/v1/auth/me */
//    @PUT("api/v1/auth/me")
//    suspend fun updateMe(
//        @Body body: UserUpdate
//    ): Response<UserResponse>
//
//    /** DELETE /api/v1/auth/me */
//    @DELETE("api/v1/auth/me")
//    suspend fun deleteAccount(): Response<Unit>
//
//    /** POST /api/v1/auth/logout */
//    @POST("api/v1/auth/logout")
//    suspend fun logout(): Response<Any>
//
//    // ── Movies ────────────────────────────────────────────────────────────────
//
//    /**
//     * GET /api/v1/movies/
//     * Full pagination + filtering support.
//     */
//    @GET("api/v1/movies/")
//    suspend fun getMovies(
//        @Query("page")         page: Int     = 1,
//        @Query("page_size")    pageSize: Int = 20,
//        @Query("search")       search: String?  = null,
//        @Query("genre_id")     genreId: Int?    = null,
//        @Query("status")       status: String?  = null,
//        @Query("is_featured")  isFeatured: Boolean? = null,
//        @Query("is_trending")  isTrending: Boolean? = null,
//        @Query("release_year") releaseYear: Int?     = null
//    ): Response<MovieList>
//
//    /** GET /api/v1/movies/{movie_id} */
//    @GET("api/v1/movies/{movie_id}")
//    suspend fun getMovie(
//        @Path("movie_id") movieId: Int
//    ): Response<MovieResponse>
//
//    /** POST /api/v1/movies/ — Admin only */
//    @POST("api/v1/movies/")
//    suspend fun createMovie(
//        @Body body: MovieCreate
//    ): Response<MovieResponse>
//
//    /** PUT /api/v1/movies/{movie_id} — Admin only */
//    @PUT("api/v1/movies/{movie_id}")
//    suspend fun updateMovie(
//        @Path("movie_id") movieId: Int,
//        @Body body: MovieUpdate
//    ): Response<MovieResponse>
//
//    /** DELETE /api/v1/movies/{movie_id} — Admin only */
//    @DELETE("api/v1/movies/{movie_id}")
//    suspend fun deleteMovie(
//        @Path("movie_id") movieId: Int
//    ): Response<Unit>
//
//    /** POST /api/v1/movies/{movie_id}/view — increment view count */
//    @POST("api/v1/movies/{movie_id}/view")
//    suspend fun incrementViewCount(
//        @Path("movie_id") movieId: Int
//    ): Response<Any>
//
//    /**
//     * GET /api/v1/movies/{movie_id}/stream?quality=1080p
//     * Returns the streaming URL for the given quality.
//     */
//    @GET("api/v1/movies/{movie_id}/stream")
//    suspend fun getStreamUrl(
//        @Path("movie_id") movieId: Int,
//        @Query("quality")  quality: String = "1080p"
//    ): Response<StreamUrlResponse>
//
//    // ── Genres ────────────────────────────────────────────────────────────────
//
//    /** GET /api/v1/movies/genres */
//    @GET("api/v1/movies/genres")
//    suspend fun getGenres(): Response<List<GenreResponse>>
//
//    /** POST /api/v1/movies/genres — Admin only */
//    @POST("api/v1/movies/genres")
//    suspend fun createGenre(
//        @Body body: GenreCreate
//    ): Response<GenreResponse>
//
//    /** GET /api/v1/movies/genres/{genre_id} */
//    @GET("api/v1/movies/genres/{genre_id}")
//    suspend fun getGenre(
//        @Path("genre_id") genreId: Int
//    ): Response<GenreResponse>
//
//    /** DELETE /api/v1/movies/genres/{genre_id} — Admin only */
//    @DELETE("api/v1/movies/genres/{genre_id}")
//    suspend fun deleteGenre(
//        @Path("genre_id") genreId: Int
//    ): Response<Unit>
//
//    /** GET /api/v1/movies/genres/slug/{slug} */
//    @GET("api/v1/movies/genres/slug/{slug}")
//    suspend fun getGenreBySlug(
//        @Path("slug") slug: String
//    ): Response<GenreResponse>
//
//    /** GET /api/v1/movies/genres/{genre_id}/movies */
//    @GET("api/v1/movies/genres/{genre_id}/movies")
//    suspend fun getMoviesByGenre(
//        @Path("genre_id") genreId: Int,
//        @Query("page")      page: Int     = 1,
//        @Query("page_size") pageSize: Int = 20
//    ): Response<MovieList>
//
//    // ── Collections (no auth required) ───────────────────────────────────────
//
//    /** GET /api/v1/movies/collections/featured */
//    @GET("api/v1/movies/collections/featured")
//    suspend fun getFeaturedMovies(
//        @Query("limit") limit: Int = 10
//    ): Response<List<MovieResponse>>
//
//    /** GET /api/v1/movies/collections/trending */
//    @GET("api/v1/movies/collections/trending")
//    suspend fun getTrendingMovies(
//        @Query("limit") limit: Int = 10
//    ): Response<List<MovieResponse>>
//
//    /** GET /api/v1/movies/collections/recent */
//    @GET("api/v1/movies/collections/recent")
//    suspend fun getRecentMovies(
//        @Query("limit") limit: Int = 10
//    ): Response<List<MovieResponse>>
//
//    // ── Admin ─────────────────────────────────────────────────────────────────
//
//    /** POST /api/v1/admin/upload/{movie_id} — Admin only, multipart */
//    @POST("api/v1/admin/upload/{movie_id}")
//    @Multipart
//    suspend fun uploadVideo(
//        @Path("movie_id") movieId: Int,
//        @Part file: okhttp3.MultipartBody.Part
//    ): Response<UploadResponse>
//
//    /** GET /api/v1/admin/conversions/{job_id} */
//    @GET("api/v1/admin/conversions/{job_id}")
//    suspend fun getConversionStatus(
//        @Path("job_id") jobId: Int
//    ): Response<ConversionStatusResponse>
//
//    /** DELETE /api/v1/admin/conversions/{job_id} */
//    @DELETE("api/v1/admin/conversions/{job_id}")
//    suspend fun cancelConversion(
//        @Path("job_id") jobId: Int
//    ): Response<Any>
//
//    /** GET /api/v1/admin/conversions */
//    @GET("api/v1/admin/conversions")
//    suspend fun listConversionJobs(
//        @Query("status") status: String? = null,
//        @Query("limit")  limit: Int      = 20
//    ): Response<List<ConversionJobResponse>>
//}
