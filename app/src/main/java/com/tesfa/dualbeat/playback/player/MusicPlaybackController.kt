package com.tesfa.dualbeat.playback.player

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.tesfa.dualbeat.domain.model.AudioTrack
import com.tesfa.dualbeat.playback.service.DualBeatMediaSessionService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<AudioTrack> = emptyList(),
    val queueIndex: Int = -1,
    val audioSessionId: Int = 0,
    val isControllerConnected: Boolean = false,
    val errorMessage: String? = null
)

@UnstableApi
class MusicPlaybackController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressJob: Job? = null

    // In-memory queue cache mapped to MediaItems
    private val currentQueueTracks = mutableListOf<AudioTrack>()

    init {
        connectToMediaSession()
    }

    private fun connectToMediaSession() {
        val sessionToken = DualBeatMediaSessionService.getSessionToken(context)
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    attachControllerListeners(controller)
                    syncStateFromController(controller)
                } catch (e: Exception) {
                    _playbackState.value = _playbackState.value.copy(
                        errorMessage = "Failed to connect to MediaSession: ${e.localizedMessage}"
                    )
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun attachControllerListeners(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _playbackState.value = _playbackState.value.copy(
                            totalDurationMs = controller.duration.coerceAtLeast(0L),
                            audioSessionId = DualBeatMediaSessionService.activeAudioSessionId,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        stopProgressTracker()
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            currentPositionMs = 0L
                        )
                    }
                    Player.STATE_IDLE -> {
                        stopProgressTracker()
                    }
                    Player.STATE_BUFFERING -> {}
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentIdx = controller.currentMediaItemIndex
                if (currentIdx in currentQueueTracks.indices) {
                    val track = currentQueueTracks[currentIdx]
                    _playbackState.value = _playbackState.value.copy(
                        currentTrack = track,
                        queueIndex = currentIdx,
                        totalDurationMs = controller.duration.coerceAtLeast(0L)
                    )
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.value = _playbackState.value.copy(isShuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _playbackState.value = _playbackState.value.copy(repeatMode = mode)
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    errorMessage = "Playback error: ${error.localizedMessage}"
                )
            }
        })
    }

    private fun syncStateFromController(controller: MediaController) {
        val currentIdx = controller.currentMediaItemIndex
        val track = currentQueueTracks.getOrNull(currentIdx)
        val repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        _playbackState.value = _playbackState.value.copy(
            isControllerConnected = true,
            isPlaying = controller.isPlaying,
            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
            totalDurationMs = controller.duration.coerceAtLeast(0L),
            isShuffleEnabled = controller.shuffleModeEnabled,
            repeatMode = repeatMode,
            currentTrack = track,
            queue = currentQueueTracks.toList(),
            queueIndex = currentIdx,
            audioSessionId = DualBeatMediaSessionService.activeAudioSessionId
        )

        if (controller.isPlaying) {
            startProgressTracker()
        }
    }

    fun getController(): MediaController? = mediaController

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (tracks.isEmpty()) return
        val controller = mediaController ?: return

        currentQueueTracks.clear()
        currentQueueTracks.addAll(tracks)

        val validIndex = startIndex.coerceIn(0, tracks.size - 1)
        val selectedTrack = tracks[validIndex]

        _playbackState.value = _playbackState.value.copy(
            queue = tracks,
            queueIndex = validIndex,
            currentTrack = selectedTrack,
            errorMessage = null
        )

        val mediaItems = tracks.map { track ->
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.albumArtUri)
                .build()

            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.contentUri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(mediaItems, validIndex, startPositionMs)
        controller.prepare()
        controller.play()
    }

    fun playSingle(track: AudioTrack) {
        playQueue(listOf(track), 0)
    }

    fun resume() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun next() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL && currentQueueTracks.isNotEmpty()) {
            controller.seekToDefaultPosition(0)
        }
    }

    fun previous() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000) {
            controller.seekTo(0)
            return
        }
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextShuffle
        _playbackState.value = _playbackState.value.copy(isShuffleEnabled = nextShuffle)
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val current = _playbackState.value.repeatMode
        val (nextMode, playerMode) = when (current) {
            RepeatMode.OFF -> Pair(RepeatMode.ALL, Player.REPEAT_MODE_ALL)
            RepeatMode.ALL -> Pair(RepeatMode.ONE, Player.REPEAT_MODE_ONE)
            RepeatMode.ONE -> Pair(RepeatMode.OFF, Player.REPEAT_MODE_OFF)
        }
        controller.repeatMode = playerMode
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                            totalDurationMs = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
