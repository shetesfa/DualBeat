package com.tesfa.dualbeat.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesfa.dualbeat.ui.components.DualBeatSearchBar
import com.tesfa.dualbeat.ui.components.EmptyState
import com.tesfa.dualbeat.ui.components.VideoGridCard

@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onVideoClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

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
                subtitle = "Videos stored on internal or external storage will appear here."
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
