package com.streaming.sunne.data.network

import com.streaming.sunne.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ══════════════════════════════════════════════════════════════════════════
    //  HEALTH
    // ══════════════════════════════════════════════════════════════════════════

    @GET("health")
    suspend fun healthCheck(): Response<Any>

    // ══════════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    //  MOVIES
    // ══════════════════════════════════════════════════════════════════════════

    @GET("api/v1/movies/")
    suspend fun getMovies(
        @Query("page")         page: Int           = 1,
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

    @GET("api/v1/movies/{movie_id}/stream")
    suspend fun getStreamUrl(
        @Path("movie_id") movieId: Int,
        @Query("quality")  quality: String = "1080p"
    ): Response<Map<String, Any>>

    @GET("api/v1/movies/with-progress")
    suspend fun getMoviesWithProgress(
        @Query("page")      page: Int      = 1,
        @Query("page_size") pageSize: Int  = 20,
        @Query("search")    search: String? = null,
        @Query("genre_id")  genreId: Int?  = null,
        @Query("status")    status: String? = null
    ): Response<MovieList>

    // ══════════════════════════════════════════════════════════════════════════
    //  GENRES
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    //  MOVIE COLLECTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @GET("api/v1/movies/collections/featured")
    suspend fun getFeaturedMovies(@Query("limit") limit: Int = 10): Response<List<MovieResponse>>

    @GET("api/v1/movies/collections/trending")
    suspend fun getTrendingMovies(@Query("limit") limit: Int = 10): Response<List<MovieResponse>>

    @GET("api/v1/movies/collections/recent")
    suspend fun getRecentMovies(@Query("limit") limit: Int = 10): Response<List<MovieResponse>>

    // ══════════════════════════════════════════════════════════════════════════
    //  WATCH HISTORY (MOVIES)
    // ══════════════════════════════════════════════════════════════════════════

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

    @GET("api/v1/watch/continue-watching")
    suspend fun getContinueWatching(@Query("limit") limit: Int = 10): Response<List<WatchHistoryResponse>>

    // ══════════════════════════════════════════════════════════════════════════
    //  MOVIE RATINGS
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    //  RECOMMENDATIONS (MOVIES)
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    //  SERIES (TV SHOWS)
    // ══════════════════════════════════════════════════════════════════════════

    @GET("api/v1/series/")
    suspend fun listSeries(
        @Query("page")      page: Int      = 1,
        @Query("page_size") pageSize: Int  = 20,
        @Query("search")    search: String? = null,
        @Query("featured")  featured: Boolean? = null,
        @Query("trending")  trending: Boolean? = null
    ): Response<SeriesListResponse>

    @GET("api/v1/series/{series_id}")
    suspend fun getSeries(@Path("series_id") seriesId: Int): Response<SeriesResponse>

    // ══════════════════════════════════════════════════════════════════════════
    //  EPISODE WATCH HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    @POST("api/v1/series-watch/progress")
    suspend fun updateEpisodeProgress(@Body body: EpisodeProgressUpdate): Response<EpisodeProgressResponse>

    @GET("api/v1/series-watch/progress/{episode_id}")
    suspend fun getEpisodeProgress(@Path("episode_id") episodeId: Int): Response<EpisodeProgressResponse?>

    @GET("api/v1/series-watch/series/{series_id}/progress")
    suspend fun getSeriesProgress(@Path("series_id") seriesId: Int): Response<Map<String, Any>>

    @GET("api/v1/series-watch/continue-watching")
    suspend fun getContinueWatchingSeries(@Query("limit") limit: Int = 10): Response<List<Map<String, Any>>>

    // ══════════════════════════════════════════════════════════════════════════
    //  SERIES RATINGS
    // ══════════════════════════════════════════════════════════════════════════

    @POST("api/v1/series-watch/ratings")
    suspend fun rateSeries(@Body body: SeriesRatingCreate): Response<SeriesRatingResponse>

    @GET("api/v1/series-watch/ratings/{series_id}/average")
    suspend fun getSeriesAverageRating(@Path("series_id") seriesId: Int): Response<Map<String, Any>>

    // ══════════════════════════════════════════════════════════════════════════
    //  PLAY NEXT (AUTO-PLAY ENGINE)
    // ══════════════════════════════════════════════════════════════════════════

    @GET("api/v1/series-watch/play-next/episode/{episode_id}")
    suspend fun getPlayNextAfterEpisode(@Path("episode_id") episodeId: Int): Response<Map<String, Any>>

    @GET("api/v1/series-watch/play-next/movie/{movie_id}")
    suspend fun getPlayNextAfterMovie(@Path("movie_id") movieId: Int): Response<Map<String, Any>>

    // ══════════════════════════════════════════════════════════════════════════
    //  UNIFIED RECOMMENDATIONS (MOVIES + SERIES)
    // ══════════════════════════════════════════════════════════════════════════

    @GET("api/v1/series-watch/recommendations/for-you")
    suspend fun getUnifiedRecommendations(@Query("limit") limit: Int = 20): Response<Map<String, Any>>

    @GET("api/v1/series-watch/recommendations/similar-to/{content_type}/{content_id}")
    suspend fun getSimilarContent(
        @Path("content_type") contentType: String,
        @Path("content_id")   contentId: Int,
        @Query("limit")       limit: Int = 10
    ): Response<Map<String, Any>>

    // ══════════════════════════════════════════════════════════════════════════
    //  LIVE STREAMING
    // ══════════════════════════════════════════════════════════════════════════

    @GET("api/v1/live/")
    suspend fun listLiveStreams(): Response<List<LiveStreamPublicResponse>>

    @GET("api/v1/live/{stream_id}")
    suspend fun getLiveStream(@Path("stream_id") streamId: Int): Response<LiveStreamPublicResponse>

    @POST("api/v1/live/{stream_id}/join")
    suspend fun joinLiveStream(@Path("stream_id") streamId: Int): Response<Any>

    @POST("api/v1/live/{stream_id}/leave")
    suspend fun leaveLiveStream(@Path("stream_id") streamId: Int): Response<Any>

    // ══════════════════════════════════════════════════════════════════════════
    //  ADMIN
    // ══════════════════════════════════════════════════════════════════════════

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


