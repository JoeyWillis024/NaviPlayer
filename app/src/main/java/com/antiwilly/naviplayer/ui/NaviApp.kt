package com.antiwilly.naviplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antiwilly.naviplayer.core.data.repository.MusicRepository
import com.antiwilly.naviplayer.core.network.SubsonicUrlHelper
import com.antiwilly.naviplayer.feature.album.AlbumDetailScreen
import com.antiwilly.naviplayer.feature.artist.ArtistDetailScreen
import com.antiwilly.naviplayer.feature.downloads.DownloadsScreen
import com.antiwilly.naviplayer.feature.downloads.DownloadsViewModel
import com.antiwilly.naviplayer.feature.home.HomeScreen
import com.antiwilly.naviplayer.feature.home.HomeViewModel
import com.antiwilly.naviplayer.feature.library.LibraryScreen
import com.antiwilly.naviplayer.feature.library.LibraryViewModel
import com.antiwilly.naviplayer.feature.player.NowPlayingScreen
import com.antiwilly.naviplayer.feature.player.PlayerViewModel
import com.antiwilly.naviplayer.feature.playlist.PlaylistDetailScreen
import com.antiwilly.naviplayer.feature.search.SearchScreen
import com.antiwilly.naviplayer.feature.search.SearchViewModel
import com.antiwilly.naviplayer.feature.settings.SettingsScreen
import com.antiwilly.naviplayer.feature.settings.SettingsViewModel
import com.antiwilly.naviplayer.ui.components.MiniPlayer

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun NaviApp(
    musicRepository: MusicRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel()
) {
    val playbackState by playerViewModel.playbackState.collectAsState()
    val activeProfile by settingsViewModel.activeProfile.collectAsState()
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Search,
        Screen.Downloads,
        Screen.Settings
    )

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    val coverUrl = playbackState.currentSong?.coverArtId?.let {
        activeProfile?.let { prof -> SubsonicUrlHelper.buildCoverArtUrl(prof, it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavScreens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            activeProfile = activeProfile,
                            onAlbumClick = { album -> navController.navigate("album/${album.id}") }
                        )
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            activeProfile = activeProfile,
                            onAlbumClick = { album -> navController.navigate("album/${album.id}") },
                            onArtistClick = { artist -> navController.navigate("artist/${artist.id}") },
                            onPlaylistClick = { playlist -> navController.navigate("playlist/${playlist.id}") }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            activeProfile = activeProfile,
                            onAlbumClick = { album -> navController.navigate("album/${album.id}") },
                            onArtistClick = { artist -> navController.navigate("artist/${artist.id}") },
                            onPlaySong = { song -> playerViewModel.playSong(song) }
                        )
                    }

                    composable(Screen.Downloads.route) {
                        DownloadsScreen(
                            viewModel = downloadsViewModel,
                            onPlaySong = { song -> playerViewModel.playSong(song) }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(viewModel = settingsViewModel)
                    }

                    composable(
                        route = "album/{albumId}",
                        arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                        AlbumDetailScreen(
                            albumId = albumId,
                            musicRepository = musicRepository,
                            activeProfile = activeProfile,
                            onBackClick = { navController.popBackStack() },
                            onPlaySong = { song, queue -> playerViewModel.playSong(song, queue) },
                            onDownloadAlbum = { songs -> songs.forEach { playerViewModel.downloadSong(it) } },
                            onToggleStar = { song -> playerViewModel.toggleStar(song) }
                        )
                    }

                    composable(
                        route = "artist/{artistId}",
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                        ArtistDetailScreen(
                            artistId = artistId,
                            musicRepository = musicRepository,
                            activeProfile = activeProfile,
                            onBackClick = { navController.popBackStack() },
                            onAlbumClick = { album -> navController.navigate("album/${album.id}") }
                        )
                    }

                    composable(
                        route = "playlist/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            musicRepository = musicRepository,
                            activeProfile = activeProfile,
                            onBackClick = { navController.popBackStack() },
                            onPlaySong = { song, queue -> playerViewModel.playSong(song, queue) },
                            onToggleStar = { song -> playerViewModel.toggleStar(song) }
                        )
                    }
                }

                // Mini Player pinned above navigation bar
                if (playbackState.currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        coverArtUrl = coverUrl,
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onSkipNextClick = { playerViewModel.skipNext() },
                        onClick = { isNowPlayingExpanded = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // Full Screen Animated Now Playing
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                playbackState = playbackState,
                coverArtUrl = coverUrl,
                onDismiss = { isNowPlayingExpanded = false },
                onPlayPause = { playerViewModel.togglePlayPause() },
                onSeekTo = { playerViewModel.seekTo(it) },
                onSkipNext = { playerViewModel.skipNext() },
                onSkipPrevious = { playerViewModel.skipPrevious() },
                onToggleShuffle = { playerViewModel.toggleShuffle() },
                onCycleRepeat = { playerViewModel.cycleRepeatMode() },
                onToggleStar = { playerViewModel.toggleStar(it) },
                onDownloadSong = { playerViewModel.downloadSong(it) },
                onPresetSelected = { playerViewModel.setEqualizerPreset(it) },
                onBassBoostChanged = { playerViewModel.setBassBoost(it) },
                onStartSleepTimer = { playerViewModel.startSleepTimer(it) },
                onCancelSleepTimer = { playerViewModel.cancelSleepTimer() },
                onSelectQueueItem = { playerViewModel.playSong(it, playbackState.queue) }
            )
        }
    }
}
