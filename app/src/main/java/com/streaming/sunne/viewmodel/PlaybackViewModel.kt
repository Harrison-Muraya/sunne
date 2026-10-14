package com.streaming.sunne.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streaming.sunne.data.model.*
import com.streaming.sunne.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Outlives screens and ViewModels so the final progress save on exit isn't cancelled. */
private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

sealed class Playable {
    abstract val key: String
    abstract val title: String
    abstract val qualities: List<String>

    data class Movie(val movie: MovieResponse) : Playable() {
        override val key get() = "movie-${movie.id}"
        override val title get() = movie.title
        override val qualities get() = sortQualities(movie.videoFiles.map { it.quality })
    }

    data class Episode(val episode: EpisodeResponse, val seriesTitle: String?) : Playable() {
        override val key get() = "episode-${episode.id}"
        override val title get() = seriesTitle?.let { "$it · ${episode.displayTitle()}" } ?: episode.displayTitle()
        override val qualities get() = sortQualities(episode.videoFiles.map { it.quality })
    }
}

data class PlaybackSource(
    val url: String,
    val quality: String?,
    val startPositionMs: Long,
    /** True when [startPositionMs] came from saved progress (shows the "Start over" chip). */
    val resumed: Boolean
)

sealed class PlaybackState {
    object Loading : PlaybackState()
    data class Ready(val source: PlaybackSource) : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

sealed class UpNext {
    data class Movie(val movie: MovieResponse, val reason: String?) : UpNext()
    data class Episode(val episode: EpisodeResponse, val label: String) : UpNext()
    data class Series(val seriesId: Int, val title: String, val posterUrl: String?, val reason: String?) : UpNext()
}

sealed class UpNextState {
    object Idle : UpNextState()
    object Loading : UpNextState()
    object None : UpNextState()
    data class Ready(val next: UpNext) : UpNextState()
}

object PlaybackPrefs {
    /** Quality the user picked this session; overrides the network-based default. */
    var preferredQuality: String? = null

    fun defaultQuality(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        return if (cellular) "720p" else "1080p"
    }
}

class PlaybackViewModel : ViewModel() {

    private val movieRepo    = MovieRepository()
    private val seriesRepo   = SeriesRepository()
    private val watchRepo    = WatchRepository()
    private val episodeWatch = EpisodeWatchRepository()
    private val playNextRepo = PlayNextRepository()

    private var playable: Playable? = null
    private var currentQuality: String? = null
    private var viewTracked = false

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Loading)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _upNext = MutableStateFlow<UpNextState>(UpNextState.Idle)
    val upNext: StateFlow<UpNextState> = _upNext.asStateFlow()

    fun start(content: Playable, defaultQuality: String) {
        if (playable?.key == content.key) return
        playable = content
        load(PlaybackPrefs.preferredQuality ?: defaultQuality, positionMs = null)
    }

    fun retry(positionMs: Long) = load(currentQuality, positionMs.takeIf { it > 0 })

    fun changeQuality(quality: String, positionMs: Long) {
        PlaybackPrefs.preferredQuality = quality
        load(quality, positionMs)
    }

    private fun load(preferred: String?, positionMs: Long?) {
        val content = playable ?: return
        val quality = chooseQuality(content.qualities, preferred)
        currentQuality = quality
        viewModelScope.launch {
            _state.value = PlaybackState.Loading
            val stream = async {
                when (content) {
                    is Playable.Movie   -> movieRepo.getStreamUrl(content.movie.id, quality)
                    is Playable.Episode -> seriesRepo.getEpisodeStreamUrl(content.episode, quality)
                }
            }
            val startMs = positionMs ?: savedPositionMs(content)
            stream.await()
                .onSuccess {
                    _state.value = PlaybackState.Ready(
                        PlaybackSource(
                            url             = it.url,
                            quality         = quality,
                            startPositionMs = startMs,
                            resumed         = positionMs == null && startMs > 0
                        )
                    )
                }
                .onFailure { _state.value = PlaybackState.Error(friendlyMessage(it)) }
        }
    }

    private suspend fun savedPositionMs(content: Playable): Long {
        val (position, percentage, completed) = when (content) {
            is Playable.Movie -> watchRepo.getProgress(content.movie.id).getOrNull()
                ?.let { Triple(it.lastPosition, it.watchPercentage, it.completed) }
            is Playable.Episode -> episodeWatch.getProgress(content.episode.id).getOrNull()
                ?.let { Triple(it.lastPosition, it.watchPercentage, it.completed) }
        } ?: return 0L
        return if (!completed && position >= 10 && percentage < 95f) position * 1000L else 0L
    }

    fun onPlaybackStarted() {
        val content = playable as? Playable.Movie ?: return
        if (viewTracked) return
        viewTracked = true
        viewModelScope.launch { movieRepo.incrementViewCount(content.movie.id) }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val content = playable ?: return
        if (durationMs <= 0 || positionMs < 0) return
        val seconds = (positionMs / 1000).toInt()
        val pct = (positionMs.toFloat() / durationMs * 100f).coerceIn(0f, 100f)
        val completed = pct >= 90f
        appScope.launch {
            when (content) {
                is Playable.Movie   -> watchRepo.updateProgress(content.movie.id, seconds, pct, completed)
                is Playable.Episode -> episodeWatch.updateProgress(content.episode.id, seconds, pct, completed)
            }
        }
    }

    fun loadUpNext() {
        val content = playable ?: return
        if (_upNext.value != UpNextState.Idle) return
        _upNext.value = UpNextState.Loading
        viewModelScope.launch {
            val next = when (content) {
                is Playable.Movie   -> playNextRepo.getNextAfterMovie(content.movie.id).getOrNull()?.let { resolveAfterMovie(it) }
                is Playable.Episode -> playNextRepo.getNextAfterEpisode(content.episode.id).getOrNull()?.let { resolveAfterEpisode(it) }
            }
            _upNext.value = next?.let { UpNextState.Ready(it) } ?: UpNextState.None
        }
    }

    private suspend fun resolveAfterMovie(r: PlayNextResponse): UpNext? = when (r.type) {
        "movie"  -> r.movieId?.let { id -> movieRepo.getMovie(id).getOrNull()?.let { UpNext.Movie(it, r.reason) } }
        "series" -> r.seriesId?.let { UpNext.Series(it, r.title.orEmpty(), r.posterUrl, r.reason) }
        else     -> null
    }

    private fun resolveAfterEpisode(r: PlayNextResponse): UpNext? = when (r.type) {
        "episode" -> r.toEpisode()?.let { ep ->
            UpNext.Episode(ep, "S${r.seasonNumber ?: "?"}:E${ep.episodeNumber} · ${ep.title}")
        }
        "series_recommendation" -> r.items?.firstOrNull { it.seriesId != null }?.let {
            UpNext.Series(it.seriesId!!, it.title, it.posterUrl, r.reason)
        }
        else -> null
    }
}

private fun qualityRank(q: String): Int = q.filter { it.isDigit() }.toIntOrNull() ?: 0

fun sortQualities(qualities: List<String>): List<String> =
    qualities.distinct().sortedByDescending { qualityRank(it) }

/** Exact match if available, else the best quality below the preference, else the lowest offered. */
private fun chooseQuality(available: List<String>, preferred: String?): String {
    if (available.isEmpty()) return preferred ?: "1080p"
    if (preferred != null && preferred in available) return preferred
    val target = preferred?.let { qualityRank(it) } ?: Int.MAX_VALUE
    return available.firstOrNull { qualityRank(it) <= target } ?: available.last()
}

fun friendlyMessage(t: Throwable): String = when (t) {
    is UnknownHostException, is ConnectException ->
        "Can't reach the server. Check your internet connection."
    is SocketTimeoutException -> "The server took too long to respond. Try again."
    is IOException -> "Network error. Check your connection and try again."
    else -> t.message ?: "Something went wrong"
}
