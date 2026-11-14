package com.tesfa.dualbeat.ui.music

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesfa.dualbeat.core.util.PermissionManager
import com.tesfa.dualbeat.domain.model.SortDirection
import com.tesfa.dualbeat.domain.model.SortField
import com.tesfa.dualbeat.ui.components.*

@Composable
fun MusicScreen(
    viewModel: MusicViewModel,
    onNavigateToEqualizer: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    var hasMediaPermission by remember {
        mutableStateOf(PermissionManager.hasMediaPermissions(context))
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        hasMediaPermission = PermissionManager.hasMediaPermissions(context)
        if (granted) {
            viewModel.refreshLibrary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Search Bar
        DualBeatSearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            placeholder = "Search songs, artists, albums…"
        )

        // Modern Pill Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = state.currentTab == MusicTab.ALL_SONGS,
                    onClick = { viewModel.setTab(MusicTab.ALL_SONGS) },
                    label = { Text("Tracks (${state.tracks.size})") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = state.currentTab == MusicTab.FOLDERS,
                    onClick = { viewModel.setTab(MusicTab.FOLDERS) },
                    label = { Text("Folders (${state.folders.size})") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                val favCount = state.tracks.count { it.isFavorite }
                FilterChip(
                    selected = state.currentTab == MusicTab.FAVORITES,
                    onClick = { viewModel.setTab(MusicTab.FAVORITES) },
                    label = { Text("Favorites ($favCount)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = state.currentTab == MusicTab.PLAYLISTS,
                    onClick = { viewModel.setTab(MusicTab.PLAYLISTS) },
                    label = { Text("Playlists") },
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Selected Folder Filter Active Bar
        if (state.selectedFolder != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Folder: ${state.selectedFolder?.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearFolderFilter() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear Filter", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Quick Action & Sort Header Bar (Play All / Shuffle / Sort)
        if (state.currentTab == MusicTab.ALL_SONGS && state.tracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.playAll() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = { viewModel.shuffleAll() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shuffle", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortField.values().forEach { field ->
                            DropdownMenuItem(
                                text = { Text("Sort by ${field.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}") },
                                onClick = {
                                    viewModel.setSort(field, state.sortDirection)
                                    showSortMenu = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.sortDirection == SortDirection.ASCENDING) "Direction: Descending"
                                    else "Direction: Ascending"
                                )
                            },
                            onClick = {
                                val newDir = if (state.sortDirection == SortDirection.ASCENDING)
                                    SortDirection.DESCENDING else SortDirection.ASCENDING
                                viewModel.setSort(state.sortField, newDir)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Content Area depending on Tab
        if (!hasMediaPermission) {
            ContextualPermissionCard(
                icon = Icons.Rounded.AudioFile,
                title = "Music Storage Permission Needed",
                description = "DualBeat requires access to your audio library to scan and organize your music files. Your music never leaves your device.",
                onRequestPermission = {
                    mediaPermissionLauncher.launch(PermissionManager.getRequiredMediaPermissions())
                },
                onOpenSettings = {
                    PermissionManager.openAppSettings(context)
                }
            )
        } else {
            when (state.currentTab) {
                MusicTab.ALL_SONGS -> {
                    val tracksToShow = if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.trim().lowercase()
                        state.tracks.filter {
                            it.title.lowercase().contains(q) ||
                            it.artist.lowercase().contains(q) ||
                            it.album.lowercase().contains(q)
                        }
                    } else if (state.selectedFolder != null) {
                        val folder = state.selectedFolder!!
                        state.tracks.filter {
                            it.folderPath == folder.path ||
                            it.folderPath.endsWith("/${folder.name}") ||
                            it.folderPath == folder.name
                        }
                    } else {
                        state.tracks
                    }

                    if (tracksToShow.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.MusicOff,
                            title = if (state.searchQuery.isNotBlank()) "No Songs Found" else "No Music Files Found",
                            subtitle = if (state.searchQuery.isNotBlank()) "Try searching for a different track or artist."
                            else "Audio tracks stored in device memory will appear here automatically.",
                            actionButtonText = if (state.searchQuery.isNotBlank()) "Clear Search" else "Scan Library",
                            onActionClick = {
                                if (state.searchQuery.isNotBlank()) viewModel.setSearchQuery("")
                                else viewModel.refreshLibrary()
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                        ) {
                            items(tracksToShow) { track ->
                                val isPlaying = state.playbackState.currentTrack?.id == track.id
                                SongListItem(
                                    track = track,
                                    isPlaying = isPlaying,
                                    onClick = { viewModel.playTrack(track) },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) }
                                )
                            }
                        }
                    }
                }

                MusicTab.FOLDERS -> {
                    val foldersWithAudio = state.folders.filter { it.audioCount > 0 }
                    if (foldersWithAudio.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.FolderOff,
                            title = "No Music Folders Detected",
                            subtitle = "Directories containing audio files will appear here.",
                            actionButtonText = "Scan Folders",
                            onActionClick = { viewModel.refreshLibrary() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 90.dp)
                        ) {
                            items(foldersWithAudio) { folder ->
                                FolderListItem(
                                    folder = folder,
                                    onClick = { viewModel.selectFolder(folder) }
                                )
                            }
                        }
                    }
                }

                MusicTab.PLAYLISTS -> {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        title = "Custom Playlists",
                        subtitle = "Create custom playlists to group your favorite songs and audio tracks for any mood.",
                        actionButtonText = "Create New Playlist",
                        onActionClick = { viewModel.createPlaylist("DualBeat Favorites") }
                    )
                }

                MusicTab.FAVORITES -> {
                    val favTracks = state.tracks.filter { it.isFavorite }
                    if (favTracks.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.FavoriteBorder,
                            title = "No Favorites Yet",
                            subtitle = "Tap the heart icon on any track to add it to your quick favorites."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                        ) {
                            items(favTracks) { track ->
                                val isPlaying = state.playbackState.currentTrack?.id == track.id
                                SongListItem(
                                    track = track,
                                    isPlaying = isPlaying,
                                    onClick = { viewModel.playTrack(track) },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Player Modal Bottom Sheet
    if (state.isFullPlayerOpen) {
        FullMusicPlayerBottomSheet(
            playbackState = state.playbackState,
            musicController = viewModel.musicController,
            onDismiss = { viewModel.closeFullPlayer() },
            onNavigateToEqualizer = onNavigateToEqualizer
        )
    }
}
