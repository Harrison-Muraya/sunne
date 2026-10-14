package com.streaming.sunne.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════════════════════
//  AUTH
// ═══════════════════════════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════════════════════════
//  GENRE
// ═══════════════════════════════════════════════════════════════════════════════

data class GenreCreate(
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

data class GenreResponse(
    @SerializedName("id")   val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String
)

// ═══════════════════════════════════════════════════════════════════════════════
//  VIDEO FILE
// ═══════════════════════════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════════════════════════
//  MOVIE
// ═══════════════════════════════════════════════════════════════════════════════

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
    @SerializedName("genres")       val genres: List<GenreResponse>     = emptyList(),
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
    val isReady: Boolean get() = status == "ready" && (videoUrl != null || videoFiles.isNotEmpty())
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

// ═══════════════════════════════════════════════════════════════════════════════
//  SERIES (TV SHOWS)
// ═══════════════════════════════════════════════════════════════════════════════

data class SeriesResponse(
    @SerializedName("id")           val id: Int,
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String?,
    @SerializedName("release_year") val releaseYear: Int?,
    @SerializedName("director")     val director: String?,
    @SerializedName("cast")         val cast: String?,
    @SerializedName("rating")       val rating: String?,
    @SerializedName("language")     val language: String,
    @SerializedName("country")      val country: String?,
    @SerializedName("poster_url")   val posterUrl: String?,
    @SerializedName("backdrop_url") val backdropUrl: String?,
    @SerializedName("trailer_url")  val trailerUrl: String?,
    @SerializedName("is_featured")  val isFeatured: Boolean,
    @SerializedName("is_trending")  val isTrending: Boolean,
    @SerializedName("status")       val status: String,
    @SerializedName("view_count")   val viewCount: Int,
    @SerializedName("created_at")   val createdAt: String,
    @SerializedName("updated_at")   val updatedAt: String,
    @SerializedName("seasons")      val seasons: List<SeasonResponse> = emptyList()
) {
    fun formattedSeasonCount(): String {
        val count = seasons.size
        return if (count == 1) "1 Season" else "$count Seasons"
    }
    fun totalEpisodeCount(): Int = seasons.sumOf { it.episodes.size }
    val thumbnailUrl: String? get() = posterUrl ?: backdropUrl
    val bannerUrl:    String? get() = backdropUrl ?: posterUrl
}

data class SeasonResponse(
    @SerializedName("id")            val id: Int,
    @SerializedName("series_id")     val seriesId: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("title")         val title: String?,
    @SerializedName("description")   val description: String?,
    @SerializedName("release_year")  val releaseYear: Int?,
    @SerializedName("poster_url")    val posterUrl: String?,
    @SerializedName("created_at")    val createdAt: String,
    @SerializedName("episodes")      val episodes: List<EpisodeResponse> = emptyList()
) {
    fun displayTitle(): String = title ?: "Season $seasonNumber"
}

data class EpisodeResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("season_id")      val seasonId: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("title")          val title: String,
    @SerializedName("description")    val description: String?,
    @SerializedName("duration")       val duration: Int?,
    @SerializedName("thumbnail_url")  val thumbnailUrl: String?,
    @SerializedName("video_url")      val videoUrl: String?,
    @SerializedName("status")         val status: String,
    @SerializedName("view_count")     val viewCount: Int,
    @SerializedName("created_at")     val createdAt: String,
    @SerializedName("updated_at")     val updatedAt: String,
    @SerializedName("video_files")    val videoFiles: List<EpisodeVideoFileResponse> = emptyList()
) {
    fun formattedDuration(): String? {
        if (duration == null || duration <= 0) return null
        val m = duration / 60
        return "${m}m"
    }
    fun displayTitle(): String = "E$episodeNumber: $title"
    val isReady: Boolean get() = status == "ready" && (videoUrl != null || videoFiles.isNotEmpty())
}

data class EpisodeVideoFileResponse(
    @SerializedName("id")          val id: Int,
    @SerializedName("quality")     val quality: String,
    @SerializedName("file_path")   val filePath: String,
    @SerializedName("file_size")   val fileSize: Long?,
    @SerializedName("codec")       val codec: String?,
    @SerializedName("format_type") val formatType: String?
)

data class SeriesListResponse(
    @SerializedName("series")      val series: List<SeriesResponse>,
    @SerializedName("total")       val total: Int,
    @SerializedName("page")        val page: Int,
    @SerializedName("page_size")   val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

// ═══════════════════════════════════════════════════════════════════════════════
//  STREAM URL
// ═══════════════════════════════════════════════════════════════════════════════

data class StreamUrlResponse(
    val url: String,
    val quality: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
//  WATCH HISTORY (MOVIES)
// ═══════════════════════════════════════════════════════════════════════════════

data class WatchProgressUpdate(
    @SerializedName("movie_id")          val movieId: Int,
    @SerializedName("last_position")     val lastPosition: Int,
    @SerializedName("watch_percentage")  val watchPercentage: Float,
    @SerializedName("completed")         val completed: Boolean = false
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

data class ContinueWatchingMovie(
    @SerializedName("movie_id")         val movieId: Int,
    @SerializedName("watch_percentage") val watchPercentage: Float,
    @SerializedName("last_position")    val lastPosition: Int,
    @SerializedName("movie")            val movie: MovieResponse?
)

// ═══════════════════════════════════════════════════════════════════════════════
//  EPISODE WATCH HISTORY
// ═══════════════════════════════════════════════════════════════════════════════

data class EpisodeProgressUpdate(
    @SerializedName("episode_id")        val episodeId: Int,
    @SerializedName("last_position")     val lastPosition: Int,
    @SerializedName("watch_percentage")  val watchPercentage: Float,
    @SerializedName("completed")         val completed: Boolean = false
)

data class EpisodeProgressResponse(
    @SerializedName("id")               val id: Int,
    @SerializedName("user_id")          val userId: Int,
    @SerializedName("episode_id")       val episodeId: Int,
    @SerializedName("watch_percentage") val watchPercentage: Float,
    @SerializedName("last_position")    val lastPosition: Int,
    @SerializedName("completed")        val completed: Boolean,
    @SerializedName("watched_at")       val watchedAt: String
)

data class EpisodeProgressItem(
    @SerializedName("episode_id")       val episodeId: Int,
    @SerializedName("watch_percentage") val watchPercentage: Float,
    @SerializedName("last_position")    val lastPosition: Int,
    @SerializedName("completed")        val completed: Boolean
)

data class SeriesContinueWatching(
    @SerializedName("series_id")               val seriesId: Int,
    @SerializedName("series_title")            val seriesTitle: String,
    @SerializedName("series_poster")           val seriesPoster: String?,
    @SerializedName("series_backdrop")         val seriesBackdrop: String?,
    @SerializedName("resume_episode_id")       val resumeEpisodeId: Int,
    @SerializedName("resume_episode_number")   val resumeEpisodeNumber: Int,
    @SerializedName("resume_season_number")    val resumeSeasonNumber: Int,
    @SerializedName("resume_episode_title")    val resumeEpisodeTitle: String?,
    @SerializedName("last_position")           val lastPosition: Int,
    @SerializedName("watch_percentage")        val watchPercentage: Float
) {
    val episodeLabel: String get() = "S$resumeSeasonNumber:E$resumeEpisodeNumber"
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PLAY NEXT
// ═══════════════════════════════════════════════════════════════════════════════

data class PlayNextSuggestion(
    @SerializedName("series_id")  val seriesId: Int?,
    @SerializedName("title")      val title: String,
    @SerializedName("poster_url") val posterUrl: String?
)

data class PlayNextResponse(
    @SerializedName("type")           val type: String,
    @SerializedName("reason")         val reason: String?,
    @SerializedName("movie_id")       val movieId: Int?,
    @SerializedName("series_id")      val seriesId: Int?,
    @SerializedName("episode_id")     val episodeId: Int?,
    @SerializedName("season_id")      val seasonId: Int?,
    @SerializedName("episode_number") val episodeNumber: Int?,
    @SerializedName("season_number")  val seasonNumber: Int?,
    @SerializedName("title")          val title: String?,
    @SerializedName("description")    val description: String?,
    @SerializedName("duration")       val duration: Int?,
    @SerializedName("thumbnail_url")  val thumbnailUrl: String?,
    @SerializedName("poster_url")     val posterUrl: String?,
    @SerializedName("video_url")      val videoUrl: String?,
    @SerializedName("items")          val items: List<PlayNextSuggestion>?
) {
    /** Builds a playable episode from a `type = "episode"` response. */
    fun toEpisode(): EpisodeResponse? {
        val id = episodeId ?: return null
        return EpisodeResponse(
            id            = id,
            seasonId      = seasonId ?: 0,
            episodeNumber = episodeNumber ?: 0,
            title         = title ?: "",
            description   = description,
            duration      = duration,
            thumbnailUrl  = thumbnailUrl,
            videoUrl      = videoUrl,
            status        = "ready",
            viewCount     = 0,
            createdAt     = "",
            updatedAt     = ""
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  RATINGS
// ═══════════════════════════════════════════════════════════════════════════════

data class MovieRatingCreate(
    @SerializedName("movie_id") val movieId: Int,
    @SerializedName("rating")   val rating: Int,
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

data class SeriesRatingCreate(
    @SerializedName("series_id") val seriesId: Int,
    @SerializedName("rating")    val rating: Int,
    @SerializedName("review")    val review: String? = null
)

data class SeriesRatingResponse(
    @SerializedName("id")         val id: Int,
    @SerializedName("user_id")    val userId: Int,
    @SerializedName("series_id")  val seriesId: Int,
    @SerializedName("rating")     val rating: Int,
    @SerializedName("review")     val review: String?,
    @SerializedName("created_at") val createdAt: String
)

// ═══════════════════════════════════════════════════════════════════════════════
//  RECOMMENDATIONS
// ═══════════════════════════════════════════════════════════════════════════════

data class RecommendationResponse(
    @SerializedName("movie_id")             val movieId: Int,
    @SerializedName("title")                val title: String,
    @SerializedName("description")          val description: String?,
    @SerializedName("poster_url")           val posterUrl: String?,
    @SerializedName("backdrop_url")         val backdropUrl: String?,
    @SerializedName("release_year")         val releaseYear: Int?,
    @SerializedName("duration")             val duration: Int?,
    @SerializedName("genres")               val genres: List<String>,
    @SerializedName("recommendation_score") val recommendationScore: Double,
    @SerializedName("reason")               val reason: String
)

data class SimilarMovieResponse(
    @SerializedName("movie_id")         val movieId: Int,
    @SerializedName("title")            val title: String,
    @SerializedName("description")      val description: String?,
    @SerializedName("poster_url")       val posterUrl: String?,
    @SerializedName("release_year")     val releaseYear: Int?,
    @SerializedName("genres")           val genres: List<String>,
    @SerializedName("similarity_score") val similarityScore: Double
)

data class TrendingMovieResponse(
    @SerializedName("movie_id")      val movieId: Int,
    @SerializedName("title")         val title: String,
    @SerializedName("description")   val description: String?,
    @SerializedName("poster_url")    val posterUrl: String?,
    @SerializedName("backdrop_url")  val backdropUrl: String?,
    @SerializedName("release_year")  val releaseYear: Int?,
    @SerializedName("genres")        val genres: List<String>,
    @SerializedName("watch_count")   val watchCount: Int
)

// ═══════════════════════════════════════════════════════════════════════════════
//  LIVE STREAMING
// ═══════════════════════════════════════════════════════════════════════════════

data class LiveStreamPublicResponse(
    @SerializedName("id")            val id: Int,
    @SerializedName("title")         val title: String,
    @SerializedName("description")   val description: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("is_live")       val isLive: Boolean,
    @SerializedName("viewer_count")  val viewerCount: Int,
    @SerializedName("stream_url")    val streamUrl: String?,
    @SerializedName("started_at")    val startedAt: String?
) {
    fun formattedViewerCount(): String {
        return when {
            viewerCount < 1000 -> "$viewerCount"
            viewerCount < 1_000_000 -> "${viewerCount / 1000}K"
            else -> "${viewerCount / 1_000_000}M"
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ADMIN
// ═══════════════════════════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════════════════════════
//  UI HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

const val CONTINUE_WATCHING = "Continue Watching"

/** Client-side grouping for carousels — supports both movies and series */
data class ContentRow(
    val label: String,
    val items: List<ContentItem>
)

/** Unified content type for mixed carousels */
sealed class ContentItem {
    /** [progress] is the watched fraction (0..1) for Continue Watching cards. */
    data class Movie(val data: MovieResponse, val progress: Float? = null) : ContentItem() {
        val id get() = data.id
        val title get() = data.title
        val thumbnailUrl get() = data.thumbnailUrl
        val isTrending get() = data.isTrending
    }
    data class ContinueSeries(val data: SeriesContinueWatching) : ContentItem()
    data class Series(val data: SeriesResponse) : ContentItem() {
        val id get() = data.id
        val title get() = data.title
        val thumbnailUrl get() = data.thumbnailUrl
        val isTrending get() = data.isTrending
    }
    data class LiveStream(val data: LiveStreamPublicResponse) : ContentItem() {
        val id get() = data.id
        val title get() = data.title
        val thumbnailUrl get() = data.thumbnailUrl
        val isTrending get() = false
    }
}

// Legacy adapter for existing code
@Deprecated("Use ContentRow instead")
data class MovieCategory(
    val label: String,
    val movies: List<MovieResponse>
)




//package com.streaming.sunne.data.model
//
//import com.google.gson.annotations.SerializedName
//
//// ─── Auth ──────────────────────────────────────────────────────────────────────
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
//// ─── Genre ─────────────────────────────────────────────────────────────────────
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
//// ─── Video File ────────────────────────────────────────────────────────────────
//
//data class VideoFileResponse(
//    @SerializedName("id")                val id: Int,
//    @SerializedName("quality")           val quality: String,
//    @SerializedName("file_path")         val filePath: String,
//    @SerializedName("file_size")         val fileSize: Long?,
//    @SerializedName("codec")             val codec: String?,
//    @SerializedName("bitrate")           val bitrate: Int?,
//    @SerializedName("resolution_width")  val resolutionWidth: Int?,
//    @SerializedName("resolution_height") val resolutionHeight: Int?,
//    @SerializedName("format_type")       val formatType: String?
//)
//
//// ─── Movie ─────────────────────────────────────────────────────────────────────
//data class StreamUrlResponse(
//    val url: String,
//    val quality: String? = null
//)
//data class MovieResponse(
//    @SerializedName("id")           val id: Int,
//    @SerializedName("title")        val title: String,
//    @SerializedName("description")  val description: String?,
//    @SerializedName("duration")     val duration: Int?,
//    @SerializedName("release_year") val releaseYear: Int?,
//    @SerializedName("director")     val director: String?,
//    @SerializedName("cast")         val cast: String?,
//    @SerializedName("rating")       val rating: String?,
//    @SerializedName("language")     val language: String,
//    @SerializedName("country")      val country: String?,
//    @SerializedName("poster_url")   val posterUrl: String?,
//    @SerializedName("backdrop_url") val backdropUrl: String?,
//    @SerializedName("trailer_url")  val trailerUrl: String?,
//    @SerializedName("video_url")    val videoUrl: String?,
//    @SerializedName("status")       val status: String,
//    @SerializedName("view_count")   val viewCount: Int,
//    @SerializedName("is_featured")  val isFeatured: Boolean,
//    @SerializedName("is_trending")  val isTrending: Boolean,
//    @SerializedName("created_at")   val createdAt: String,
//    @SerializedName("updated_at")   val updatedAt: String,
//    @SerializedName("genres")       val genres: List<GenreResponse> = emptyList(),
//    @SerializedName("video_files")  val videoFiles: List<VideoFileResponse> = emptyList()
//) {
//    fun formattedDuration(): String? {
//        if (duration == null || duration <= 0) return null
//        val h = duration / 3600
//        val m = (duration % 3600) / 60
//        return if (h > 0) "${h}h ${m}m" else "${m}m"
//    }
//
//    fun genreNames(): String = genres.joinToString(" · ") { it.name }
//
//    val thumbnailUrl: String? get() = posterUrl ?: backdropUrl
//    val bannerUrl:    String? get() = backdropUrl ?: posterUrl
//
//    /** Whether this movie has a processable video ready to stream */
//    val isReady: Boolean get() = status == "ready"
//}
//
//data class MovieCreate(
//    @SerializedName("title")        val title: String,
//    @SerializedName("description")  val description: String?  = null,
//    @SerializedName("duration")     val duration: Int?        = null,
//    @SerializedName("release_year") val releaseYear: Int?     = null,
//    @SerializedName("director")     val director: String?     = null,
//    @SerializedName("cast")         val cast: String?         = null,
//    @SerializedName("rating")       val rating: String?       = null,
//    @SerializedName("language")     val language: String      = "English",
//    @SerializedName("country")      val country: String?      = null,
//    @SerializedName("genre_ids")    val genreIds: List<Int>   = emptyList(),
//    @SerializedName("poster_url")   val posterUrl: String?    = null,
//    @SerializedName("backdrop_url") val backdropUrl: String?  = null,
//    @SerializedName("trailer_url")  val trailerUrl: String?   = null
//)
//
//data class MovieUpdate(
//    @SerializedName("title")        val title: String?        = null,
//    @SerializedName("description")  val description: String?  = null,
//    @SerializedName("duration")     val duration: Int?        = null,
//    @SerializedName("release_year") val releaseYear: Int?     = null,
//    @SerializedName("director")     val director: String?     = null,
//    @SerializedName("cast")         val cast: String?         = null,
//    @SerializedName("rating")       val rating: String?       = null,
//    @SerializedName("language")     val language: String?     = null,
//    @SerializedName("country")      val country: String?      = null,
//    @SerializedName("genre_ids")    val genreIds: List<Int>?  = null,
//    @SerializedName("poster_url")   val posterUrl: String?    = null,
//    @SerializedName("backdrop_url") val backdropUrl: String?  = null,
//    @SerializedName("trailer_url")  val trailerUrl: String?   = null,
//    @SerializedName("is_featured")  val isFeatured: Boolean?  = null,
//    @SerializedName("is_trending")  val isTrending: Boolean?  = null,
//    @SerializedName("status")       val status: String?       = null
//)
//
//data class MovieList(
//    @SerializedName("movies")      val movies: List<MovieResponse>,
//    @SerializedName("total")       val total: Int,
//    @SerializedName("page")        val page: Int,
//    @SerializedName("page_size")   val pageSize: Int,
//    @SerializedName("total_pages") val totalPages: Int
//)
//
///** Client-side grouping used by HomeScreen carousels */
//data class MovieCategory(
//    val label: String,
//    val movies: List<MovieResponse>
//)
//
//// ─── Watch History ─────────────────────────────────────────────────────────────
//
//data class WatchProgressUpdate(
//    @SerializedName("movie_id")         val movieId: Int,
//    @SerializedName("last_position")    val lastPosition: Int,
//    @SerializedName("watch_percentage") val watchPercentage: Float,
//    @SerializedName("completed")        val completed: Boolean = false
//)
//
//data class WatchHistoryResponse(
//    @SerializedName("id")               val id: Int,
//    @SerializedName("user_id")          val userId: Int,
//    @SerializedName("movie_id")         val movieId: Int,
//    @SerializedName("watch_percentage") val watchPercentage: Float,
//    @SerializedName("last_position")    val lastPosition: Int,
//    @SerializedName("completed")        val completed: Boolean,
//    @SerializedName("watched_at")       val watchedAt: String,
//    @SerializedName("created_at")       val createdAt: String
//)
//
//// ─── Ratings ───────────────────────────────────────────────────────────────────
//
//data class MovieRatingCreate(
//    @SerializedName("movie_id") val movieId: Int,
//    @SerializedName("rating")   val rating: Int,   // 1–5
//    @SerializedName("review")   val review: String? = null
//)
//
//data class MovieRatingResponse(
//    @SerializedName("id")         val id: Int,
//    @SerializedName("user_id")    val userId: Int,
//    @SerializedName("movie_id")   val movieId: Int,
//    @SerializedName("rating")     val rating: Int,
//    @SerializedName("review")     val review: String?,
//    @SerializedName("created_at") val createdAt: String,
//    @SerializedName("updated_at") val updatedAt: String
//)
//
//data class AverageRatingResponse(
//    @SerializedName("movie_id")       val movieId: Int,
//    @SerializedName("average_rating") val averageRating: Double?,
//    @SerializedName("total_ratings")  val totalRatings: Int
//)
//
//// ─── Recommendations ───────────────────────────────────────────────────────────
//
//data class RecommendationResponse(
//    @SerializedName("movie_id")             val movieId: Int,
//    @SerializedName("title")               val title: String,
//    @SerializedName("description")          val description: String?,
//    @SerializedName("poster_url")           val posterUrl: String?,
//    @SerializedName("backdrop_url")         val backdropUrl: String?,
//    @SerializedName("release_year")         val releaseYear: Int?,
//    @SerializedName("duration")             val duration: Int?,
//    @SerializedName("genres")              val genres: List<String>,
//    @SerializedName("recommendation_score") val recommendationScore: Double,
//    @SerializedName("reason")              val reason: String
//)
//
//data class SimilarMovieResponse(
//    @SerializedName("movie_id")         val movieId: Int,
//    @SerializedName("title")           val title: String,
//    @SerializedName("description")      val description: String?,
//    @SerializedName("poster_url")       val posterUrl: String?,
//    @SerializedName("release_year")     val releaseYear: Int?,
//    @SerializedName("genres")          val genres: List<String>,
//    @SerializedName("similarity_score") val similarityScore: Double
//)
//
//data class TrendingMovieResponse(
//    @SerializedName("movie_id")     val movieId: Int,
//    @SerializedName("title")       val title: String,
//    @SerializedName("description")  val description: String?,
//    @SerializedName("poster_url")   val posterUrl: String?,
//    @SerializedName("backdrop_url") val backdropUrl: String?,
//    @SerializedName("release_year") val releaseYear: Int?,
//    @SerializedName("genres")      val genres: List<String>,
//    @SerializedName("watch_count")  val watchCount: Int
//)
//
//// ─── Admin / Upload ────────────────────────────────────────────────────────────
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


//--------------------------------------------------------------------------------
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
