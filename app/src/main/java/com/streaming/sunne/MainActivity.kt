package com.streaming.sunne

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.streaming.sunne.data.model.CatalogSource
import com.streaming.sunne.data.model.ContentItem
import com.streaming.sunne.data.network.SessionEvents
import com.streaming.sunne.data.network.TokenManager
import com.streaming.sunne.repository.LiveStreamRepository
import com.streaming.sunne.repository.MovieRepository
import com.streaming.sunne.repository.SeriesRepository
import com.streaming.sunne.ui.screens.*
import com.streaming.sunne.ui.theme.DarkBackground
import com.streaming.sunne.ui.theme.NetflixRed
import com.streaming.sunne.ui.theme.SunneTheme
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.LiveStreamViewModel
import com.streaming.sunne.viewmodel.MovieViewModel
import com.streaming.sunne.viewmodel.UpNext
import com.streaming.sunne.viewmodel.friendlyMessage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(applicationContext)
        setContent {
            SunneTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect { systemUiController.setSystemBarsColor(color = DarkBackground) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    SuneApp()
                }
            }
        }
    }
}

@Composable
fun SuneApp() {
    val navController  = rememberNavController()
    val authViewModel  : AuthViewModel  = viewModel()
    val movieViewModel : MovieViewModel = viewModel()
    val movieRepo  = remember { MovieRepository() }
    val seriesRepo = remember { SeriesRepository() }
    val liveRepo   = remember { LiveStreamRepository() }

    LaunchedEffect(Unit) {
        if (TokenManager.isLoggedIn) authViewModel.loadCurrentUser()
    }

    LaunchedEffect(Unit) {
        SessionEvents.expired.collect {
            authViewModel.onSessionExpired()
            movieViewModel.reset()
            navController.resetTo(Screen.Login.route)
        }
    }

    val selectedMovie   by movieViewModel.selectedMovie.collectAsState()
    val selectedSeries  by movieViewModel.selectedSeries.collectAsState()
    val selectedEpisode by movieViewModel.selectedEpisode.collectAsState()
    val selectedStream  by movieViewModel.selectedLiveStream.collectAsState()

    val openItem: (ContentItem) -> Unit = { item ->
        when (item) {
            is ContentItem.Movie -> {
                movieViewModel.selectMovie(item.data)
                navController.navigate(Screen.Details.of(item.data.id))
            }
            is ContentItem.Series -> {
                movieViewModel.selectSeries(item.data)
                navController.navigate(Screen.SeriesDetail.of(item.data.id))
            }
            is ContentItem.ContinueSeries -> movieViewModel.resumeSeries(
                item      = item.data,
                onPlay    = {
                    selectedEpisodeId(movieViewModel)?.let {
                        navController.navigate(Screen.EpisodePlayer.of(it, item.data.seriesId))
                    }
                },
                onDetails = { navController.navigate(Screen.SeriesDetail.of(item.data.seriesId)) }
            )
            is ContentItem.LiveStream -> {
                movieViewModel.selectLiveStream(item.data)
                navController.navigate(Screen.LivePlayer.of(item.data.id))
            }
        }
    }

    val start = if (TokenManager.isLoggedIn) Screen.Home.route else Screen.Splash.route

    NavHost(navController = navController, startDestination = start) {

        composable(Screen.Splash.route) {
            SplashScreen(onNavigateToHome = {
                val dest = if (TokenManager.isLoggedIn) Screen.Home.route else Screen.Login.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel          = authViewModel,
                onLoginSuccess     = { navController.resetTo(Screen.Home.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel          = authViewModel,
                onRegisterSuccess  = { navController.resetTo(Screen.Home.route) },
                onNavigateToLogin  = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel     = movieViewModel,
                authViewModel = authViewModel,
                onItemClick   = openItem,
                onPlayMovie   = { movie ->
                    movieViewModel.selectMovie(movie)
                    navController.navigate(Screen.Player.of(movie.id))
                },
                onLiveClick   = { navController.navigate(Screen.LiveStreamList.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onMyListClick = { navController.navigate(Screen.MyList.route) },
                onSeeAll      = { source -> navController.navigate(Screen.Catalog.of(source)) },
                onLogout      = {
                    authViewModel.logout()
                    movieViewModel.reset()
                    navController.resetTo(Screen.Login.route)
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(onItemClick = openItem, onBack = { navController.popBackStack() })
        }

        composable(Screen.MyList.route) {
            MyListScreen(onItemClick = openItem, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.Catalog.route,
            arguments = listOf(
                navArgument("kind")    { type = NavType.StringType },
                navArgument("title")   { type = NavType.StringType; defaultValue = "" },
                navArgument("genreId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { entry ->
            val args    = entry.arguments
            val title   = args?.getString("title").orEmpty()
            val genreId = args?.getInt("genreId")?.takeIf { it >= 0 }
            val source  = if (args?.getString("kind") == "series") CatalogSource.Series(title.ifEmpty { "Series" }, genreId)
                          else CatalogSource.Movies(title.ifEmpty { "Movies" }, genreId)
            CatalogScreen(source = source, onItemClick = openItem, onBack = { navController.popBackStack() })
        }

        composable(Screen.Details.route, arguments = listOf(idArg("movieId"))) { entry ->
            val movieId = entry.arguments?.getInt("movieId") ?: 0
            LoadById(movieId, selectedMovie?.takeIf { it.id == movieId }, { movieRepo.getMovie(it) }) { movie ->
                DetailsScreen(
                    movie       = movie,
                    onPlayClick = {
                        movieViewModel.selectMovie(movie)
                        navController.navigate(Screen.Player.of(movie.id))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Player.route, arguments = listOf(idArg("movieId"))) { entry ->
            val movieId = entry.arguments?.getInt("movieId") ?: 0
            LoadById(movieId, selectedMovie?.takeIf { it.id == movieId }, { movieRepo.getMovie(it) }) { movie ->
                PlayerScreen(
                    movie       = movie,
                    onBackPress = { navController.popBackStack() },
                    onUpNext    = { next -> handleUpNext(next, null, movieViewModel, navController, Screen.Player.route) }
                )
            }
        }

        composable(Screen.SeriesDetail.route, arguments = listOf(idArg("seriesId"))) { entry ->
            val seriesId = entry.arguments?.getInt("seriesId") ?: 0
            SeriesDetailScreen(
                seriesId       = seriesId,
                initial        = selectedSeries?.takeIf { it.id == seriesId },
                onEpisodeClick = { fullSeries, episode ->
                    movieViewModel.selectSeries(fullSeries)
                    movieViewModel.selectEpisode(episode)
                    navController.navigate(Screen.EpisodePlayer.of(episode.id, fullSeries.id))
                },
                onBackClick    = { navController.popBackStack() }
            )
        }

        composable(
            Screen.EpisodePlayer.route,
            arguments = listOf(idArg("episodeId"), navArgument("seriesId") { type = NavType.IntType; defaultValue = -1 })
        ) { entry ->
            val episodeId = entry.arguments?.getInt("episodeId") ?: 0
            val seriesId  = entry.arguments?.getInt("seriesId")?.takeIf { it >= 0 }
            val knownTitle = selectedSeries?.takeIf { it.id == seriesId }?.title
            val seriesTitle by produceState(knownTitle, seriesId) {
                if (value == null && seriesId != null) value = seriesRepo.getSeries(seriesId).getOrNull()?.title
            }
            LoadById(episodeId, selectedEpisode?.takeIf { it.id == episodeId }, { seriesRepo.getEpisode(it) }) { episode ->
                EpisodePlayerScreen(
                    episode     = episode,
                    seriesTitle = seriesTitle,
                    onBackPress = { navController.popBackStack() },
                    onUpNext    = { next -> handleUpNext(next, seriesId, movieViewModel, navController, Screen.EpisodePlayer.route) }
                )
            }
        }

        composable(Screen.LiveStreamList.route) {
            val liveViewModel: LiveStreamViewModel = viewModel()
            LiveStreamListScreen(
                viewModel     = liveViewModel,
                onStreamClick = { stream ->
                    movieViewModel.selectLiveStream(stream)
                    navController.navigate(Screen.LivePlayer.of(stream.id))
                },
                onBackClick   = { navController.popBackStack() }
            )
        }

        composable(Screen.LivePlayer.route, arguments = listOf(idArg("streamId"))) { entry ->
            val streamId = entry.arguments?.getInt("streamId") ?: 0
            LoadById(streamId, selectedStream?.takeIf { it.id == streamId }, { liveRepo.getStream(it) }) { stream ->
                val liveViewModel: LiveStreamViewModel = viewModel()
                LivePlayerScreen(
                    stream      = stream,
                    viewModel   = liveViewModel,
                    onBackPress = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun selectedEpisodeId(movieViewModel: MovieViewModel): Int? = movieViewModel.selectedEpisode.value?.id

private fun idArg(name: String) = navArgument(name) { type = NavType.IntType }

/**
 * Shows [content] for the item with [id], using [cached] when the caller already has it
 * and fetching otherwise (e.g. after the process was killed and the route was restored).
 */
@Composable
private fun <T : Any> LoadById(
    id: Int,
    cached: T?,
    load: suspend (Int) -> Result<T>,
    content: @Composable (T) -> Unit
) {
    var attempt by remember { mutableIntStateOf(0) }
    var error   by remember(id) { mutableStateOf<String?>(null) }
    val value by produceState(cached, id, attempt) {
        if (value == null) {
            error = null
            load(id).onSuccess { value = it }.onFailure { error = friendlyMessage(it) }
        }
    }
    val loaded = value
    when {
        loaded != null -> content(loaded)
        error != null  -> ErrorScreen(error!!) { attempt++ }
        else -> Box(Modifier.fillMaxSize().background(DarkBackground), Alignment.Center) {
            CircularProgressIndicator(color = NetflixRed)
        }
    }
}

private fun handleUpNext(
    next: UpNext,
    seriesId: Int?,
    movieViewModel: MovieViewModel,
    navController: NavHostController,
    currentRoute: String
) {
    when (next) {
        is UpNext.Movie -> {
            movieViewModel.selectMovie(next.movie)
            navController.navigate(Screen.Player.of(next.movie.id)) {
                popUpTo(currentRoute) { inclusive = true }
            }
        }
        is UpNext.Episode -> {
            movieViewModel.selectEpisode(next.episode)
            navController.navigate(Screen.EpisodePlayer.of(next.episode.id, seriesId)) {
                popUpTo(currentRoute) { inclusive = true }
            }
        }
        is UpNext.Series -> {
            navController.navigate(Screen.SeriesDetail.of(next.seriesId)) {
                popUpTo(currentRoute) { inclusive = true }
            }
        }
    }
}

/** Navigates to [route] and drops the entire back stack. */
private fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object Search         : Screen("search")
    object MyList         : Screen("my-list")
    object LiveStreamList : Screen("live")

    object Details : Screen("details/{movieId}") {
        fun of(movieId: Int) = "details/$movieId"
    }
    object Player : Screen("play/movie/{movieId}") {
        fun of(movieId: Int) = "play/movie/$movieId"
    }
    object SeriesDetail : Screen("series/{seriesId}") {
        fun of(seriesId: Int) = "series/$seriesId"
    }
    object EpisodePlayer : Screen("play/episode/{episodeId}?seriesId={seriesId}") {
        fun of(episodeId: Int, seriesId: Int?) = "play/episode/$episodeId?seriesId=${seriesId ?: -1}"
    }
    object LivePlayer : Screen("live/{streamId}") {
        fun of(streamId: Int) = "live/$streamId"
    }
    object Catalog : Screen("catalog/{kind}?title={title}&genreId={genreId}") {
        fun of(source: CatalogSource): String {
            val (kind, genreId) = when (source) {
                is CatalogSource.Movies -> "movies" to source.genreId
                is CatalogSource.Series -> "series" to source.genreId
            }
            return "catalog/$kind?title=${Uri.encode(source.title)}&genreId=${genreId ?: -1}"
        }
    }
}
