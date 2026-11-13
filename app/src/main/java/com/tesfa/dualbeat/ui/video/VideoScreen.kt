package com.tesfa.dualbeat.ui.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tesfa.dualbeat.core.util.PermissionManager
import com.tesfa.dualbeat.ui.components.ContextualPermissionCard
import com.tesfa.dualbeat.ui.components.DualBeatSearchBar
import com.tesfa.dualbeat.ui.components.EmptyState
import com.tesfa.dualbeat.ui.components.VideoGridCard

@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onVideoClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var hasMediaPermission by remember {
        mutableStateOf(PermissionManager.hasMediaPermissions(context))
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        hasMediaPermission = PermissionManager.hasMediaPermissions(context)
        if (granted) {
            viewModel.refreshVideos()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DualBeatSearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            placeholder = "Search videos…"
        )

        if (!hasMediaPermission) {
            ContextualPermissionCard(
                icon = Icons.Rounded.VideoLibrary,
                title = "Video Storage Access Needed",
                description = "DualBeat needs storage permission to find and play videos on your device. Your videos remain private and offline.",
                onRequestPermission = {
                    mediaPermissionLauncher.launch(PermissionManager.getRequiredMediaPermissions())
                },
                onOpenSettings = {
                    PermissionManager.openAppSettings(context)
                }
            )
        } else {
            val filteredVideos = if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase()
                state.videos.filter { it.title.lowercase().contains(q) }
            } else {
                state.videos
            }

            if (filteredVideos.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.VideocamOff,
                    title = "No Video Files Found",
                    subtitle = "Videos stored on internal or external storage will appear here.",
                    actionButtonText = "Scan Storage",
                    onActionClick = { viewModel.refreshVideos() }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredVideos) { video ->
                        VideoGridCard(
                            video = video,
                            onClick = { onVideoClick(video.id) }
                        )
                    }
                }
            }
        }
    }
}
