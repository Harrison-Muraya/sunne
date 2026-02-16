package com.streaming.sunne.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth ──────────────────────────────────────────────────────────────────────

data class UserCreate(
    @SerializedName("username")  val username: String,
    @SerializedName("email")     val email: String,
    @SerializedName("password")  val password: String,
    @SerializedName("full_name") val fullName: String? = null
)

data class UserLogin(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class UserUpdate(
    @SerializedName("email")     val email: String?    = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("password")  val password: String? = null
)

data class UserResponse(
    @SerializedName("id")         val id: Int,
    @SerializedName("username")   val username: String,
    @SerializedName("email")      val email: String,
    @SerializedName("full_name")  val fullName: String?,
    @SerializedName("is_active")  val isActive: Boolean,
    @SerializedName("is_admin")   val isAdmin: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("last_login") val lastLogin: String?
)

data class Token(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type")    val tokenType: String = "bearer"
)

// ─── Genre ─────────────────────────────────────────────────────────────────────

data class GenreCreate(
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

data class GenreResponse(
    @SerializedName("id")   val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

// ─── Video File ────────────────────────────────────────────────────────────────

data class VideoFileResponse(
    @SerializedName("id")                val id: Int,
    @SerializedName("quality")           val quality: String,
    @SerializedName("file_path")         val filePath: String,
    @SerializedName("file_size")         val fileSize: Long?,
    @SerializedName("codec")             val codec: String?,
    @SerializedName("bitrate")           val bitrate: Int?,
    @SerializedName("resolution_width")  val resolutionWidth: Int?,
    @SerializedName("resolution_height") val resolutionHeight: Int?,
    @SerializedName("format_type")       val formatType: String?
)

// ─── Movie ─────────────────────────────────────────────────────────────────────
data class StreamUrlResponse(
    val url: String,
    val quality: String? = null
)
data class MovieResponse(
    @SerializedName("id")           val id: Int,
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String?,
    @SerializedName("duration")     val duration: Int?,
    @SerializedName("release_year") val releaseYear: Int?,
    @SerializedName("director")     val director: String?,
    @SerializedName("cast")         val cast: String?,
    @SerializedName("rating")       val rating: String?,
    @SerializedName("language")     val language: String,
    @SerializedName("country")      val country: String?,
    @SerializedName("poster_url")   val posterUrl: String?,
    @SerializedName("backdrop_url") val backdropUrl: String?,
    @SerializedName("trailer_url")  val trailerUrl: String?,
    @SerializedName("video_url")    val videoUrl: String?,
    @SerializedName("status")       val status: String,
    @SerializedName("view_count")   val viewCount: Int,
    @SerializedName("is_featured")  val isFeatured: Boolean,
    @SerializedName("is_trending")  val isTrending: Boolean,
    @SerializedName("created_at")   val createdAt: String,
    @SerializedName("updated_at")   val updatedAt: String,
    @SerializedName("genres")       val genres: List<GenreResponse> = emptyList(),
    @SerializedName("video_files")  val videoFiles: List<VideoFileResponse> = emptyList()
) {
    fun formattedDuration(): String? {
        if (duration == null || duration <= 0) return null
        val h = duration / 3600
        val m = (duration % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun genreNames(): String = genres.joinToString(" · ") { it.name }

    val thumbnailUrl: String? get() = posterUrl ?: backdropUrl
    val bannerUrl:    String? get() = backdropUrl ?: posterUrl

    /** Whether this movie has a processable video ready to stream */
    val isReady: Boolean get() = status == "ready"
}

data class MovieCreate(
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String?  = null,
    @SerializedName("duration")     val duration: Int?        = null,
    @SerializedName("release_year") val releaseYear: Int?     = null,
    @SerializedName("director")     val director: String?     = null,
    @SerializedName("cast")         val cast: String?         = null,
    @SerializedName("rating")       val rating: String?       = null,
    @SerializedName("language")     val language: String      = "English",
    @SerializedName("country")      val country: String?      = null,
    @SerializedName("genre_ids")    val genreIds: List<Int>   = emptyList(),
    @SerializedName("poster_url")   val posterUrl: String?    = null,
    @SerializedName("backdrop_url") val backdropUrl: String?  = null,
    @SerializedName("trailer_url")  val trailerUrl: String?   = null
)

data class MovieUpdate(
    @SerializedName("title")        val title: String?        = null,
    @SerializedName("description")  val description: String?  = null,
    @SerializedName("duration")     val duration: Int?        = null,
    @SerializedName("release_year") val releaseYear: Int?     = null,
    @SerializedName("director")     val director: String?     = null,
    @SerializedName("cast")         val cast: String?         = null,
    @SerializedName("rating")       val rating: String?       = null,
    @SerializedName("language")     val language: String?     = null,
    @SerializedName("country")      val country: String?      = null,
    @SerializedName("genre_ids")    val genreIds: List<Int>?  = null,
    @SerializedName("poster_url")   val posterUrl: String?    = null,
    @SerializedName("backdrop_url") val backdropUrl: String?  = null,
    @SerializedName("trailer_url")  val trailerUrl: String?   = null,
    @SerializedName("is_featured")  val isFeatured: Boolean?  = null,
    @SerializedName("is_trending")  val isTrending: Boolean?  = null,
    @SerializedName("status")       val status: String?       = null
)

data class MovieList(
    @SerializedName("movies")      val movies: List<MovieResponse>,
    @SerializedName("total")       val total: Int,
    @SerializedName("page")        val page: Int,
    @SerializedName("page_size")   val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

/** Client-side grouping used by HomeScreen carousels */
data class MovieCategory(
    val label: String,
    val movies: List<MovieResponse>
)

// ─── Watch History ─────────────────────────────────────────────────────────────

data class WatchProgressUpdate(
    @SerializedName("movie_id")         val movieId: Int,
    @SerializedName("last_position")    val lastPosition: Int,
    @SerializedName("watch_percentage") val watchPercentage: Float,
    @SerializedName("completed")        val completed: Boolean = false
)

data class WatchHistoryResponse(
    @SerializedName("id")               val id: Int,
    @SerializedName("user_id")          val userId: Int,
    @SerializedName("movie_id")         val movieId: Int,
    @SerializedName("watch_percentage") val watchPercentage: Float,
    @SerializedName("last_position")    val lastPosition: Int,
    @SerializedName("completed")        val completed: Boolean,
    @SerializedName("watched_at")       val watchedAt: String,
    @SerializedName("created_at")       val createdAt: String
)

// ─── Ratings ───────────────────────────────────────────────────────────────────

data class MovieRatingCreate(
    @SerializedName("movie_id") val movieId: Int,
    @SerializedName("rating")   val rating: Int,   // 1–5
    @SerializedName("review")   val review: String? = null
)

data class MovieRatingResponse(
    @SerializedName("id")         val id: Int,
    @SerializedName("user_id")    val userId: Int,
    @SerializedName("movie_id")   val movieId: Int,
    @SerializedName("rating")     val rating: Int,
    @SerializedName("review")     val review: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class AverageRatingResponse(
    @SerializedName("movie_id")       val movieId: Int,
    @SerializedName("average_rating") val averageRating: Double?,
    @SerializedName("total_ratings")  val totalRatings: Int
)

// ─── Recommendations ───────────────────────────────────────────────────────────

data class RecommendationResponse(
    @SerializedName("movie_id")             val movieId: Int,
    @SerializedName("title")               val title: String,
    @SerializedName("description")          val description: String?,
    @SerializedName("poster_url")           val posterUrl: String?,
    @SerializedName("backdrop_url")         val backdropUrl: String?,
    @SerializedName("release_year")         val releaseYear: Int?,
    @SerializedName("duration")             val duration: Int?,
    @SerializedName("genres")              val genres: List<String>,
    @SerializedName("recommendation_score") val recommendationScore: Double,
    @SerializedName("reason")              val reason: String
)

data class SimilarMovieResponse(
    @SerializedName("movie_id")         val movieId: Int,
    @SerializedName("title")           val title: String,
    @SerializedName("description")      val description: String?,
    @SerializedName("poster_url")       val posterUrl: String?,
    @SerializedName("release_year")     val releaseYear: Int?,
    @SerializedName("genres")          val genres: List<String>,
    @SerializedName("similarity_score") val similarityScore: Double
)

data class TrendingMovieResponse(
    @SerializedName("movie_id")     val movieId: Int,
    @SerializedName("title")       val title: String,
    @SerializedName("description")  val description: String?,
    @SerializedName("poster_url")   val posterUrl: String?,
    @SerializedName("backdrop_url") val backdropUrl: String?,
    @SerializedName("release_year") val releaseYear: Int?,
    @SerializedName("genres")      val genres: List<String>,
    @SerializedName("watch_count")  val watchCount: Int
)

// ─── Admin / Upload ────────────────────────────────────────────────────────────

data class UploadResponse(
    @SerializedName("message")  val message: String,
    @SerializedName("movie_id") val movieId: Int,
    @SerializedName("job_id")   val jobId: Int,
    @SerializedName("task_id")  val taskId: String,
    @SerializedName("status")   val status: String
)

data class ConversionStatusResponse(
    @SerializedName("job_id")        val jobId: Int,
    @SerializedName("movie_id")      val movieId: Int,
    @SerializedName("status")        val status: String,
    @SerializedName("progress")      val progress: Int,
    @SerializedName("current_step")  val currentStep: String?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("started_at")    val startedAt: String?,
    @SerializedName("completed_at")  val completedAt: String?
)

data class ConversionJobResponse(
    @SerializedName("id")            val id: Int,
    @SerializedName("movie_id")      val movieId: Int,
    @SerializedName("status")        val status: String,
    @SerializedName("progress")      val progress: Int,
    @SerializedName("current_step")  val currentStep: String?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("started_at")    val startedAt: String?,
    @SerializedName("completed_at")  val completedAt: String?,
    @SerializedName("created_at")    val createdAt: String
)



//package com.streaming.sunne.data.model
//
//import com.google.gson.annotations.SerializedName
//
//// ─── Auth Models ───────────────────────────────────────────────────────────────
//
//data class UserCreate(
//    @SerializedName("username")  val username: String,
//    @SerializedName("email")     val email: String,
//    @SerializedName("password")  val password: String,
//    @SerializedName("full_name") val fullName: String? = null
//)
//
//data class UserLogin(
//    @SerializedName("username") val username: String,
//    @SerializedName("password") val password: String
//)
//
//data class UserUpdate(
//    @SerializedName("email")     val email: String?    = null,
//    @SerializedName("full_name") val fullName: String? = null,
//    @SerializedName("password")  val password: String? = null
//)
//
//data class UserResponse(
//    @SerializedName("id")         val id: Int,
//    @SerializedName("username")   val username: String,
//    @SerializedName("email")      val email: String,
//    @SerializedName("full_name")  val fullName: String?,
//    @SerializedName("is_active")  val isActive: Boolean,
//    @SerializedName("is_admin")   val isAdmin: Boolean,
//    @SerializedName("created_at") val createdAt: String,
//    @SerializedName("last_login") val lastLogin: String?
//)
//
//data class Token(
//    @SerializedName("access_token")  val accessToken: String,
//    @SerializedName("refresh_token") val refreshToken: String,
//    @SerializedName("token_type")    val tokenType: String = "bearer"
//)
//
//// ─── Genre Models ──────────────────────────────────────────────────────────────
//
//data class GenreCreate(
//    @SerializedName("name") val name: String,
//    @SerializedName("slug") val slug: String
//)
//
//data class GenreResponse(
//    @SerializedName("id")   val id: Int,
//    @SerializedName("name") val name: String,
//    @SerializedName("slug") val slug: String
//)
//
//// ─── Video File Model ──────────────────────────────────────────────────────────
//
//data class VideoFileResponse(
//    @SerializedName("id")               val id: Int,
//    @SerializedName("quality")          val quality: String,       // "1080p", "720p", "480p"
//    @SerializedName("file_path")        val filePath: String,
//    @SerializedName("file_size")        val fileSize: Long?,
//    @SerializedName("codec")            val codec: String?,
//    @SerializedName("bitrate")          val bitrate: Int?,
//    @SerializedName("resolution_width") val resolutionWidth: Int?,
//    @SerializedName("resolution_height")val resolutionHeight: Int?,
//    @SerializedName("format_type")      val formatType: String?
//)
//
//// ─── Movie Models ──────────────────────────────────────────────────────────────
//
//data class MovieResponse(
//    @SerializedName("id")           val id: Int,
//    @SerializedName("title")        val title: String,
//    @SerializedName("description")  val description: String?,
//    @SerializedName("duration")     val duration: Int?,          // seconds
//    @SerializedName("release_year") val releaseYear: Int?,
//    @SerializedName("director")     val director: String?,
//    @SerializedName("cast")         val cast: String?,
//    @SerializedName("rating")       val rating: String?,         // "PG", "PG-13", "R", etc.
//    @SerializedName("language")     val language: String,
//    @SerializedName("country")      val country: String?,
//    @SerializedName("poster_url")   val posterUrl: String?,
//    @SerializedName("backdrop_url") val backdropUrl: String?,
//    @SerializedName("trailer_url")  val trailerUrl: String?,
//    @SerializedName("video_url")    val videoUrl: String?,
//    @SerializedName("status")       val status: String,          // "pending","processing","ready","failed"
//    @SerializedName("view_count")   val viewCount: Int,
//    @SerializedName("is_featured")  val isFeatured: Boolean,
//    @SerializedName("is_trending")  val isTrending: Boolean,
//    @SerializedName("created_at")   val createdAt: String,
//    @SerializedName("updated_at")   val updatedAt: String,
//    @SerializedName("genres")       val genres: List<GenreResponse> = emptyList(),
//    @SerializedName("video_files")  val videoFiles: List<VideoFileResponse> = emptyList()
//) {
//    /** Human-readable duration, e.g. "1h 42m" */
//    fun formattedDuration(): String? {
//        if (duration == null || duration <= 0) return null
//        val h = duration / 3600
//        val m = (duration % 3600) / 60
//        return if (h > 0) "${h}h ${m}m" else "${m}m"
//    }
//
//    /** Comma-separated genre names */
//    fun genreNames(): String = genres.joinToString(" · ") { it.name }
//
//    /** Best available thumbnail — prefer backdrop for hero, poster for cards */
//    val thumbnailUrl: String? get() = posterUrl ?: backdropUrl
//    val bannerUrl:    String? get() = backdropUrl ?: posterUrl
//}
//
//data class MovieCreate(
//    @SerializedName("title")        val title: String,
//    @SerializedName("description")  val description: String?   = null,
//    @SerializedName("duration")     val duration: Int?         = null,
//    @SerializedName("release_year") val releaseYear: Int?      = null,
//    @SerializedName("director")     val director: String?      = null,
//    @SerializedName("cast")         val cast: String?          = null,
//    @SerializedName("rating")       val rating: String?        = null,
//    @SerializedName("language")     val language: String       = "English",
//    @SerializedName("country")      val country: String?       = null,
//    @SerializedName("genre_ids")    val genreIds: List<Int>    = emptyList(),
//    @SerializedName("poster_url")   val posterUrl: String?     = null,
//    @SerializedName("backdrop_url") val backdropUrl: String?   = null,
//    @SerializedName("trailer_url")  val trailerUrl: String?    = null
//)
//
//data class MovieUpdate(
//    @SerializedName("title")        val title: String?         = null,
//    @SerializedName("description")  val description: String?   = null,
//    @SerializedName("duration")     val duration: Int?         = null,
//    @SerializedName("release_year") val releaseYear: Int?      = null,
//    @SerializedName("director")     val director: String?      = null,
//    @SerializedName("cast")         val cast: String?          = null,
//    @SerializedName("rating")       val rating: String?        = null,
//    @SerializedName("language")     val language: String?      = null,
//    @SerializedName("country")      val country: String?       = null,
//    @SerializedName("genre_ids")    val genreIds: List<Int>?   = null,
//    @SerializedName("poster_url")   val posterUrl: String?     = null,
//    @SerializedName("backdrop_url") val backdropUrl: String?   = null,
//    @SerializedName("trailer_url")  val trailerUrl: String?    = null,
//    @SerializedName("is_featured")  val isFeatured: Boolean?   = null,
//    @SerializedName("is_trending")  val isTrending: Boolean?   = null,
//    @SerializedName("status")       val status: String?        = null
//)
//
///** Paginated movie list — returned by GET /api/v1/movies/ */
//data class MovieList(
//    @SerializedName("movies")      val movies: List<MovieResponse>,
//    @SerializedName("total")       val total: Int,
//    @SerializedName("page")        val page: Int,
//    @SerializedName("page_size")   val pageSize: Int,
//    @SerializedName("total_pages") val totalPages: Int
//)
//
///** Categories used by HomeScreen carousels (built client-side from API data) */
//data class MovieCategory(
//    val label: String,
//    val movies: List<MovieResponse>
//)
//
//// ─── Upload / Admin Models ─────────────────────────────────────────────────────
//
//data class UploadResponse(
//    @SerializedName("message")  val message: String,
//    @SerializedName("movie_id") val movieId: Int,
//    @SerializedName("job_id")   val jobId: Int,
//    @SerializedName("task_id")  val taskId: String,
//    @SerializedName("status")   val status: String
//)
//
//data class ConversionStatusResponse(
//    @SerializedName("job_id")        val jobId: Int,
//    @SerializedName("movie_id")      val movieId: Int,
//    @SerializedName("status")        val status: String,
//    @SerializedName("progress")      val progress: Int,
//    @SerializedName("current_step")  val currentStep: String?,
//    @SerializedName("error_message") val errorMessage: String?,
//    @SerializedName("started_at")    val startedAt: String?,
//    @SerializedName("completed_at")  val completedAt: String?
//)
//
//data class ConversionJobResponse(
//    @SerializedName("id")            val id: Int,
//    @SerializedName("movie_id")      val movieId: Int,
//    @SerializedName("status")        val status: String,
//    @SerializedName("progress")      val progress: Int,
//    @SerializedName("current_step")  val currentStep: String?,
//    @SerializedName("error_message") val errorMessage: String?,
//    @SerializedName("started_at")    val startedAt: String?,
//    @SerializedName("completed_at")  val completedAt: String?,
//    @SerializedName("created_at")    val createdAt: String
//)
//
/** Returned by GET /api/v1/movies/{id}/stream */
//data class StreamUrlResponse(
//    @SerializedName("url")     val url: String,
//    @SerializedName("fallback")  val fallback: String,
//    @SerializedName("quality") val quality: String?
//)
