package com.tesfa.dualbeat.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.domain.model.*
import com.tesfa.dualbeat.playback.player.PlaybackState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MusicTab {
    ALL_SONGS,
    FOLDERS,
    PLAYLISTS,
    FAVORITES
}

data class MusicUiState(
    val currentTab: MusicTab = MusicTab.ALL_SONGS,
    val tracks: List<AudioTrack> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favorites: List<AudioTrack> = emptyList(),
    val searchQuery: String = "",
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val playbackState: PlaybackState = PlaybackState(),
    val isFullPlayerOpen: Boolean = false
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    private val mediaRepo = app.mediaRepository
    private val playlistRepo = app.playlistRepository
    val musicController = app.musicController

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                mediaRepo.audioTracks,
                mediaRepo.folders,
                playlistRepo.allPlaylists,
                musicController.playbackState
            ) { tracks, folders, playlists, playback ->
                _uiState.update { current ->
                    val sorted = mediaRepo.sortAudioTracks(tracks, current.sortField, current.sortDirection)
                    current.copy(
                        tracks = sorted,
                        folders = folders,
                        playlists = playlists,
                        playbackState = playback
                    )
                }
            }.collect()
        }
    }

    fun setTab(tab: MusicTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSort(field: SortField, direction: SortDirection) {
        _uiState.update { current ->
            val sorted = mediaRepo.sortAudioTracks(current.tracks, field, direction)
            current.copy(sortField = field, sortDirection = direction, tracks = sorted)
        }
    }

    fun playTrack(track: AudioTrack) {
        val tracksToPlay = if (_uiState.value.searchQuery.isNotBlank()) {
            mediaRepo.searchAudio(_uiState.value.searchQuery)
        } else {
            _uiState.value.tracks
        }
        val idx = tracksToPlay.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        musicController.playQueue(tracksToPlay, idx)
    }

    fun toggleFavorite(track: AudioTrack) {
        viewModelScope.launch {
            mediaRepo.toggleFavorite(track.id, MediaType.AUDIO, track.isFavorite)
        }
    }

    fun openFullPlayer() {
        _uiState.update { it.copy(isFullPlayerOpen = true) }
    }

    fun closeFullPlayer() {
        _uiState.update { it.copy(isFullPlayerOpen = false) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                playlistRepo.createPlaylist(name.trim())
            }
        }
    }
}
