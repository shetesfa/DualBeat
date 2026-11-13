package com.tesfa.dualbeat.playback.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.tesfa.dualbeat.domain.model.VideoItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VideoAspectRatio {
    FIT,
    FILL,
    ZOOM,
    FIXED_16_9,
    FIXED_4_3
}

data class VideoPlaybackState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    val isLocked: Boolean = false,
    val isControlsVisible: Boolean = true,
    val brightnessLevel: Float = 0.5f,
    val volumeLevel: Float = 0.5f,
    val errorMessage: String? = null
)

class VideoPlaybackController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

    private var exoPlayer: ExoPlayer? = null

    private val _videoState = MutableStateFlow(VideoPlaybackState())
    val videoState: StateFlow<VideoPlaybackState> = _videoState.asStateFlow()

    private var progressJob: Job? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _videoState.value = _videoState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _videoState.value = _videoState.value.copy(
                            totalDurationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        stopProgressTracker()
                        _videoState.value = _videoState.value.copy(isPlaying = false)
                    }
                    Player.STATE_IDLE -> {
                        stopProgressTracker()
                    }
                    Player.STATE_BUFFERING -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _videoState.value = _videoState.value.copy(
                    isPlaying = false,
                    errorMessage = "Video playback error: ${error.localizedMessage ?: "Unsupported codec or file error"}"
                )
            }
        })

        exoPlayer = player
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun playVideo(video: VideoItem, startPositionMs: Long = 0L) {
        val player = exoPlayer ?: return
        _videoState.value = _videoState.value.copy(
            currentVideo = video,
            currentPositionMs = startPositionMs,
            errorMessage = null
        )

        val mediaItem = MediaItem.fromUri(video.contentUri)
        player.setMediaItem(mediaItem, startPositionMs)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val validPos = positionMs.coerceIn(0L, _videoState.value.totalDurationMs.coerceAtLeast(0L))
        exoPlayer?.seekTo(validPos)
        _videoState.value = _videoState.value.copy(currentPositionMs = validPos)
    }

    fun seekRelative(offsetMs: Long) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current + offsetMs)
    }

    fun setSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.25f, 3.0f)
        exoPlayer?.setPlaybackSpeed(validSpeed)
        _videoState.value = _videoState.value.copy(playbackSpeed = validSpeed)
    }

    fun cycleAspectRatio() {
        val nextRatio = when (_videoState.value.aspectRatio) {
            VideoAspectRatio.FIT -> VideoAspectRatio.FILL
            VideoAspectRatio.FILL -> VideoAspectRatio.ZOOM
            VideoAspectRatio.ZOOM -> VideoAspectRatio.FIXED_16_9
            VideoAspectRatio.FIXED_16_9 -> VideoAspectRatio.FIXED_4_3
            VideoAspectRatio.FIXED_4_3 -> VideoAspectRatio.FIT
        }
        _videoState.value = _videoState.value.copy(aspectRatio = nextRatio)
    }

    fun toggleLock() {
        _videoState.value = _videoState.value.copy(isLocked = !_videoState.value.isLocked)
    }

    fun toggleControlsVisibility() {
        if (_videoState.value.isLocked) return
        _videoState.value = _videoState.value.copy(isControlsVisible = !_videoState.value.isControlsVisible)
    }

    fun setBrightness(level: Float) {
        _videoState.value = _videoState.value.copy(brightnessLevel = level.coerceIn(0f, 1f))
    }

    fun setVolume(level: Float) {
        _videoState.value = _videoState.value.copy(volumeLevel = level.coerceIn(0f, 1f))
        exoPlayer?.volume = level.coerceIn(0f, 1f)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _videoState.value = _videoState.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            totalDurationMs = player.duration.coerceAtLeast(0L)
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
        exoPlayer?.release()
        exoPlayer = null
    }
}
