package com.tesfa.dualbeat.playback.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import com.tesfa.dualbeat.domain.model.AudioTrack
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
    val errorMessage: String? = null
)

class MusicPlaybackController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

    private var exoPlayer: ExoPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressJob: Job? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _playbackState.value = _playbackState.value.copy(
                            totalDurationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L,
                            audioSessionId = exoPlayer?.audioSessionId ?: 0,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        stopProgressTracker()
                    }
                    Player.STATE_BUFFERING -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    errorMessage = "Playback error: ${error.localizedMessage}"
                )
            }
        })

        exoPlayer = player
        _playbackState.value = _playbackState.value.copy(audioSessionId = player.audioSessionId)
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (tracks.isEmpty()) return
        val player = exoPlayer ?: return

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

        player.setMediaItems(mediaItems, validIndex, startPositionMs)
        player.prepare()
        player.play()
    }

    fun playSingle(track: AudioTrack) {
        playQueue(listOf(track), 0)
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun next() {
        val player = exoPlayer ?: return
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        if (state.isShuffleEnabled) {
            val randomIndex = state.queue.indices.random()
            playQueue(state.queue, randomIndex)
            return
        }

        val nextIndex = state.queueIndex + 1
        if (nextIndex < state.queue.size) {
            player.seekToDefaultPosition(nextIndex)
            _playbackState.value = state.copy(
                queueIndex = nextIndex,
                currentTrack = state.queue[nextIndex]
            )
            player.play()
        } else if (state.repeatMode == RepeatMode.ALL) {
            player.seekToDefaultPosition(0)
            _playbackState.value = state.copy(
                queueIndex = 0,
                currentTrack = state.queue[0]
            )
            player.play()
        }
    }

    fun previous() {
        val player = exoPlayer ?: return
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }

        val prevIndex = state.queueIndex - 1
        if (prevIndex >= 0) {
            player.seekToDefaultPosition(prevIndex)
            _playbackState.value = state.copy(
                queueIndex = prevIndex,
                currentTrack = state.queue[prevIndex]
            )
            player.play()
        } else {
            player.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleShuffle() {
        val nextShuffle = !_playbackState.value.isShuffleEnabled
        _playbackState.value = _playbackState.value.copy(isShuffleEnabled = nextShuffle)
        exoPlayer?.shuffleModeEnabled = nextShuffle
    }

    fun cycleRepeatMode() {
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = next)
        exoPlayer?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                if (state.queueIndex + 1 < state.queue.size || state.repeatMode == RepeatMode.ALL) {
                    next()
                } else {
                    stopProgressTracker()
                    _playbackState.value = state.copy(isPlaying = false, currentPositionMs = 0L)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
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
