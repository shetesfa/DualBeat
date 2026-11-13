package com.tesfa.dualbeat.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesfa.dualbeat.domain.model.SortDirection
import com.tesfa.dualbeat.domain.model.SortField
import com.tesfa.dualbeat.ui.components.*

@Composable
fun MusicScreen(
    viewModel: MusicViewModel,
    onNavigateToEqualizer: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar
        DualBeatSearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            placeholder = "Search songs, artists, albums…"
        )

        // Library Navigation Tabs
        TabRow(
            selectedTabIndex = state.currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            MusicTab.values().forEach { tab ->
                val isSelected = state.currentTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setTab(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                MusicTab.ALL_SONGS -> "All Songs"
                                MusicTab.FOLDERS -> "Folders"
                                MusicTab.PLAYLISTS -> "Playlists"
                                MusicTab.FAVORITES -> "Favorites"
                            }
                        )
                    }
                )
            }
        }

        // Sorting Row & Track Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${state.tracks.size} Tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sort: ${state.sortField.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortField.values().forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.name) },
                            onClick = {
                                viewModel.setSort(field, state.sortDirection)
                                showSortMenu = false
                            }
                        )
                    }
                    Divider()
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

        // Content Area depending on Tab
        when (state.currentTab) {
            MusicTab.ALL_SONGS -> {
                if (state.tracks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.MusicOff,
                        title = "No Music Files Found",
                        subtitle = "Audio tracks stored in device memory will appear here automatically."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(state.tracks) { track ->
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
                if (state.folders.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.FolderOff,
                        title = "No Folders Detected",
                        subtitle = "Directories containing media files will appear here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(state.folders) { folder ->
                            FolderListItem(
                                folder = folder,
                                onClick = {}
                            )
                        }
                    }
                }
            }

            MusicTab.PLAYLISTS -> {
                EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = "Playlists",
                    subtitle = "Create custom playlists to group your favorite songs and audio tracks.",
                    actionButtonText = "Create New Playlist",
                    onActionClick = { viewModel.createPlaylist("Favorites Mix") }
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
                        contentPadding = PaddingValues(bottom = 90.dp)
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
