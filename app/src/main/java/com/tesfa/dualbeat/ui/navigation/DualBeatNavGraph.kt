package com.tesfa.dualbeat.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tesfa.dualbeat.ui.components.DualBeatBottomBar
import com.tesfa.dualbeat.ui.components.DualBeatTopBar
import com.tesfa.dualbeat.ui.components.MiniPlayer
import com.tesfa.dualbeat.ui.home.HomeScreen
import com.tesfa.dualbeat.ui.home.HomeViewModel
import com.tesfa.dualbeat.ui.music.MusicScreen
import com.tesfa.dualbeat.ui.music.MusicViewModel
import com.tesfa.dualbeat.ui.settings.AboutScreen
import com.tesfa.dualbeat.ui.settings.DeveloperProfileScreen
import com.tesfa.dualbeat.ui.settings.SettingsScreen
import com.tesfa.dualbeat.ui.settings.SettingsViewModel
import com.tesfa.dualbeat.ui.sound.SoundStudioScreen
import com.tesfa.dualbeat.ui.sound.SoundStudioViewModel
import com.tesfa.dualbeat.ui.speakers.SpeakersScreen
import com.tesfa.dualbeat.ui.speakers.SpeakersViewModel
import com.tesfa.dualbeat.ui.split.AdaptiveSplitViewScreen
import com.tesfa.dualbeat.ui.video.VideoPlayerScreen
import com.tesfa.dualbeat.ui.video.VideoScreen
import com.tesfa.dualbeat.ui.video.VideoViewModel

@Composable
fun DualBeatNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeViewModel: HomeViewModel = viewModel()
    val musicViewModel: MusicViewModel = viewModel()
    val videoViewModel: VideoViewModel = viewModel()
    val speakersViewModel: SpeakersViewModel = viewModel()
    val soundViewModel: SoundStudioViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val playbackState by musicViewModel.musicController.playbackState.collectAsState()

    val isTopLevelRoute = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        topBar = {
            if (isTopLevelRoute) {
                val currentScreen = Screen.bottomNavItems.firstOrNull { it.route == currentRoute } ?: Screen.Home
                DualBeatTopBar(
                    title = currentScreen.title,
                    onSplitViewClick = { navController.navigate(Screen.SplitView.route) }
                )
            }
        },
        bottomBar = {
            if (isTopLevelRoute) {
                DualBeatBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToVideo = { videoId ->
                            navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                        },
                        onNavigateToSpeakers = { navController.navigate(Screen.Speakers.route) },
                        onNavigateToSound = { navController.navigate(Screen.Sound.route) }
                    )
                }

                composable(Screen.Videos.route) {
                    VideoScreen(
                        viewModel = videoViewModel,
                        onVideoClick = { videoId ->
                            navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                        }
                    )
                }

                composable(Screen.Music.route) {
                    MusicScreen(
                        viewModel = musicViewModel,
                        onNavigateToEqualizer = { navController.navigate(Screen.Sound.route) }
                    )
                }

                composable(Screen.Speakers.route) {
                    SpeakersScreen(viewModel = speakersViewModel)
                }

                composable(Screen.Sound.route) {
                    SoundStudioScreen(viewModel = soundViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToAbout = { navController.navigate(Screen.About.route) },
                        onNavigateToDeveloperProfile = { navController.navigate(Screen.DeveloperProfile.route) }
                    )
                }

                composable(
                    route = Screen.VideoPlayer.route,
                    arguments = listOf(navArgument("videoId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                    VideoPlayerScreen(
                        videoId = videoId,
                        viewModel = videoViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SplitView.route) {
                    AdaptiveSplitViewScreen(
                        musicViewModel = musicViewModel,
                        soundViewModel = soundViewModel,
                        speakersViewModel = speakersViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.About.route) {
                    AboutScreen(
                        onNavigateToDeveloperProfile = { navController.navigate(Screen.DeveloperProfile.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.DeveloperProfile.route) {
                    DeveloperProfileScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Floating Mini-Player pinned directly above the bottom bar
            if (isTopLevelRoute && playbackState.currentTrack != null) {
                MiniPlayer(
                    playbackState = playbackState,
                    onPlayPauseClick = { musicViewModel.musicController.togglePlayPause() },
                    onNextClick = { musicViewModel.musicController.next() },
                    onExpandClick = { musicViewModel.openFullPlayer() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
