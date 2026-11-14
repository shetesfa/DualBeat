package com.tesfa.dualbeat.ui.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.domain.model.MediaFolder
import com.tesfa.dualbeat.domain.model.MediaType
import com.tesfa.dualbeat.domain.model.VideoItem
import com.tesfa.dualbeat.playback.player.VideoPlaybackState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class VideoViewMode {
    FOLDERS,
    ALL_VIDEOS
}

data class VideoUiState(
    val videos: List<VideoItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val selectedFolder: MediaFolder? = null,
    val viewMode: VideoViewMode = VideoViewMode.FOLDERS,
    val searchQuery: String = "",
    val playbackState: VideoPlaybackState = VideoPlaybackState()
)

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    private val mediaRepo = app.mediaRepository
    val videoController = app.videoController

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                mediaRepo.videos,
                mediaRepo.folders,
                videoController.videoState
            ) { videos, folders, videoState ->
                val videoFolders = folders.filter { it.videoCount > 0 }
                _uiState.update { current ->
                    current.copy(
                        videos = videos,
                        folders = videoFolders,
                        playbackState = videoState
                    )
                }
            }.collect()
        }
    }

    fun setViewMode(mode: VideoViewMode) {
        _uiState.update { it.copy(viewMode = mode, selectedFolder = null) }
    }

    fun selectFolder(folder: MediaFolder?) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun playVideo(videoId: Long) {
        val video = _uiState.value.videos.firstOrNull { it.id == videoId } ?: return
        viewModelScope.launch {
            val lastPos = mediaRepo.getLastPosition(video.id, MediaType.VIDEO)
            videoController.playVideo(video, lastPos)
        }
    }

    fun saveCurrentPosition() {
        val state = _uiState.value.playbackState
        val video = state.currentVideo ?: return
        viewModelScope.launch {
            mediaRepo.recordHistory(
                mediaId = video.id,
                type = MediaType.VIDEO,
                title = video.title,
                subtitle = "Video",
                positionMs = state.currentPositionMs,
                durationMs = state.totalDurationMs
            )
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            mediaRepo.refreshAll()
        }
    }
}
