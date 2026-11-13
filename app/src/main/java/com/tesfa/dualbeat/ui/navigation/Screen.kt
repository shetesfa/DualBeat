package com.tesfa.dualbeat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Videos : Screen("videos", "Videos", Icons.Rounded.VideoLibrary)
    object Music : Screen("music", "Music", Icons.Rounded.MusicNote)
    object Speakers : Screen("speakers", "Speakers", Icons.Rounded.SpeakerGroup)
    object Sound : Screen("sound", "Sound", Icons.Rounded.Equalizer)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)

    object VideoPlayer : Screen("video_player/{videoId}", "Video Player") {
        fun createRoute(videoId: Long): String = "video_player/$videoId"
    }

    object SplitView : Screen("split_view", "Split View", Icons.Rounded.ViewSidebar)
    object About : Screen("about", "About DualBeat")
    object DeveloperProfile : Screen("developer_profile", "Developer Profile")

    companion object {
        val bottomNavItems = listOf(Home, Videos, Music, Speakers, Sound, Settings)
    }
}
