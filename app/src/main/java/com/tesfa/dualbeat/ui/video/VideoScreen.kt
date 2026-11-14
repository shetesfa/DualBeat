package com.tesfa.dualbeat.ui.video

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesfa.dualbeat.core.util.PermissionManager
import com.tesfa.dualbeat.ui.components.*

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

    // Handle back button when drilled into a folder
    BackHandler(enabled = state.selectedFolder != null) {
        viewModel.selectFolder(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar
        DualBeatSearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            placeholder = if (state.selectedFolder != null) "Search in ${state.selectedFolder?.name}…" else "Search videos, folders…"
        )

        // View Mode Selector Tabs (Folders vs All Videos)
        if (state.selectedFolder == null && state.searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.viewMode == VideoViewMode.FOLDERS,
                    onClick = { viewModel.setViewMode(VideoViewMode.FOLDERS) },
                    label = { Text("Folders (${state.folders.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = state.viewMode == VideoViewMode.ALL_VIDEOS,
                    onClick = { viewModel.setViewMode(VideoViewMode.ALL_VIDEOS) },
                    label = { Text("All Videos (${state.videos.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (!hasMediaPermission) {
            ContextualPermissionCard(
                icon = Icons.Rounded.VideoLibrary,
                title = "Video Storage Access Needed",
                description = "DualBeat needs storage permission to find and classify your videos into folders. Your videos remain private and offline.",
                onRequestPermission = {
                    mediaPermissionLauncher.launch(PermissionManager.getRequiredMediaPermissions())
                },
                onOpenSettings = {
                    PermissionManager.openAppSettings(context)
                }
            )
        } else {
            // Selected Folder Active Header
            if (state.selectedFolder != null) {
                val folder = state.selectedFolder!!
                val folderVideos = state.videos.filter { video ->
                    video.folderPath == folder.path ||
                    video.folderPath.endsWith("/${folder.name}") ||
                    video.folderPath == folder.name
                }.let { list ->
                    if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.trim().lowercase()
                        list.filter { it.title.lowercase().contains(q) }
                    } else list
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.selectFolder(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to Folders",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${folderVideos.size} videos in this folder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (folderVideos.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.VideocamOff,
                        title = "No Videos in This Folder",
                        subtitle = "No matching videos found inside '${folder.name}'.",
                        actionButtonText = "Back to Folders",
                        onActionClick = { viewModel.selectFolder(null) }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folderVideos) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onVideoClick(video.id) }
                            )
                        }
                    }
                }
            } else if (state.searchQuery.isNotBlank()) {
                // Search Query Active
                val q = state.searchQuery.trim().lowercase()
                val searchedVideos = state.videos.filter {
                    it.title.lowercase().contains(q) || it.folderPath.lowercase().contains(q)
                }

                if (searchedVideos.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.VideocamOff,
                        title = "No Matching Videos",
                        subtitle = "Could not find any videos matching '${state.searchQuery}'.",
                        actionButtonText = "Clear Search",
                        onActionClick = { viewModel.setSearchQuery("") }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchedVideos) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onVideoClick(video.id) }
                            )
                        }
                    }
                }
            } else if (state.viewMode == VideoViewMode.FOLDERS) {
                // MX Player Style: Classify videos by folder
                if (state.folders.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.FolderOff,
                        title = "No Video Folders Found",
                        subtitle = "Folders containing video files will appear here like in MX Player.",
                        actionButtonText = "Scan Storage",
                        onActionClick = { viewModel.refreshVideos() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.folders) { folder ->
                            VideoFolderCard(
                                folder = folder,
                                onClick = { viewModel.selectFolder(folder) }
                            )
                        }
                    }
                }
            } else {
                // ViewMode == ALL_VIDEOS
                if (state.videos.isEmpty()) {
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
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.videos) { video ->
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
}
