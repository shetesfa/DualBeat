package com.tesfa.dualbeat.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val audioTracks: List<AudioTrack> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val isScanning: Boolean = false,
    val searchQuery: String = "",
    val activeOutputSink: Pair<AudioSinkType, String> = Pair(AudioSinkType.INTERNAL_SPEAKER, "Phone Speaker")
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    private val mediaRepo = app.mediaRepository
    private val btManager = app.bluetoothDeviceManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRepository()
        refresh()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            combine(
                mediaRepo.audioTracks,
                mediaRepo.videos,
                mediaRepo.folders,
                mediaRepo.isScanning
            ) { audio, videos, folders, scanning ->
                _uiState.update { current ->
                    current.copy(
                        audioTracks = audio,
                        videos = videos,
                        folders = folders,
                        isScanning = scanning,
                        activeOutputSink = btManager.getActiveAudioOutput()
                    )
                }
            }.collect()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            mediaRepo.refreshAll()
            btManager.refreshDevices()
            _uiState.update { it.copy(activeOutputSink = btManager.getActiveAudioOutput()) }
        }
    }

    fun playTrack(track: AudioTrack) {
        val tracks = _uiState.value.audioTracks
        val idx = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        app.musicController.playQueue(tracks, idx)
    }
}
