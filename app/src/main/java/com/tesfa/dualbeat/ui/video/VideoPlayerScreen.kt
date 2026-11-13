package com.tesfa.dualbeat.ui.video

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tesfa.dualbeat.core.util.FormatUtils
import com.tesfa.dualbeat.playback.player.VideoAspectRatio

@Composable
fun VideoPlayerScreen(
    videoId: Long,
    viewModel: VideoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val state by viewModel.uiState.collectAsState()
    val videoState = state.playbackState

    // Live gesture HUD states
    var gestureHudType by remember { mutableStateOf<String?>(null) } // "brightness", "volume", "seek"
    var gestureHudValue by remember { mutableStateOf(0) } // percentage 0..100
    var gestureSeekPreviewMs by remember { mutableStateOf(0L) }

    LaunchedEffect(videoId) {
        viewModel.playVideo(videoId)
    }

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        onDispose {
            viewModel.saveCurrentPosition()
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        viewModel.saveCurrentPosition()
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(videoState.isLocked) {
                if (videoState.isLocked) {
                    detectTapGestures { viewModel.videoController.toggleControlsVisibility() }
                } else {
                    detectTapGestures(
                        onTap = { viewModel.videoController.toggleControlsVisibility() },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                viewModel.videoController.seekRelative(-10000L)
                            } else {
                                viewModel.videoController.seekRelative(10000L)
                            }
                        }
                    )
                }
            }
            .pointerInput(videoState.isLocked) {
                if (!videoState.isLocked) {
                    var totalDragY = 0f
                    var totalDragX = 0f
                    var isDragHorizontal = false
                    var startDragPos = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragY = 0f
                            totalDragX = 0f
                            isDragHorizontal = false
                            startDragPos = videoState.currentPositionMs
                        },
                        onDragEnd = {
                            if (isDragHorizontal) {
                                viewModel.videoController.seekTo(gestureSeekPreviewMs)
                            }
                            gestureHudType = null
                        },
                        onDragCancel = {
                            gestureHudType = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            val screenWidth = size.width
                            val screenHeight = size.height

                            // Horizontal Seek Scrubbing
                            if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY) * 1.5f || isDragHorizontal) {
                                isDragHorizontal = true
                                val duration = videoState.totalDurationMs.coerceAtLeast(1L)
                                val scrubOffsetMs = (totalDragX / screenWidth * 60000L).toLong() // 60s per full screen width
                                val targetPos = (startDragPos + scrubOffsetMs).coerceIn(0L, duration)
                                gestureSeekPreviewMs = targetPos
                                gestureHudType = "seek"
                            } else {
                                // Vertical Gestures: Left half = Brightness, Right half = Volume
                                val isLeftHalf = change.position.x < screenWidth / 2
                                if (isLeftHalf) {
                                    // Window Brightness Control
                                    activity?.window?.let { window ->
                                        val attrs = window.attributes
                                        val currentBrightness = if (attrs.screenBrightness < 0) 0.5f else attrs.screenBrightness
                                        val delta = -dragAmount.y / (screenHeight * 0.75f)
                                        val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                        attrs.screenBrightness = newBrightness
                                        window.attributes = attrs
                                        gestureHudType = "brightness"
                                        gestureHudValue = (newBrightness * 100).toInt()
                                    }
                                } else {
                                    // System Audio Volume Control
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = -dragAmount.y / (screenHeight / (maxVol * 2f))
                                    if (kotlin.math.abs(delta) >= 1f) {
                                        val newVol = (curVol + delta.toInt()).coerceIn(0, maxVol)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        gestureHudType = "volume"
                                        gestureHudValue = (newVol.toFloat() / maxVol * 100).toInt()
                                        totalDragY = 0f
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = viewModel.videoController.getPlayer()
                    resizeMode = when (videoState.aspectRatio) {
                        VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoAspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoAspectRatio.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        VideoAspectRatio.FIXED_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    }
                }
            },
            update = { playerView ->
                playerView.player = viewModel.videoController.getPlayer()
                playerView.resizeMode = when (videoState.aspectRatio) {
                    VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    VideoAspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoAspectRatio.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    VideoAspectRatio.FIXED_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Live Gesture Feedback HUD (Brightness / Volume / Scrubbing)
        if (gestureHudType != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.Black.copy(alpha = 0.75f),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (gestureHudType) {
                        "brightness" -> {
                            Icon(imageVector = Icons.Rounded.BrightnessMedium, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Brightness $gestureHudValue%", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        "volume" -> {
                            Icon(imageVector = Icons.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Volume $gestureHudValue%", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        "seek" -> {
                            Icon(imageVector = Icons.Rounded.FastForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${FormatUtils.formatDuration(gestureSeekPreviewMs)} / ${FormatUtils.formatDuration(videoState.totalDurationMs)}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = videoState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.saveCurrentPosition()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = videoState.currentVideo?.title ?: "Video",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    // Aspect Ratio Button
                    IconButton(onClick = { viewModel.videoController.cycleAspectRatio() }) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White
                        )
                    }

                    // Lock Button
                    IconButton(onClick = { viewModel.videoController.toggleLock() }) {
                        Icon(
                            imageVector = if (videoState.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = if (videoState.isLocked) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                // Center Play/Pause & Seek Indicators
                if (!videoState.isLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.videoController.seekRelative(-10000L) }) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { viewModel.videoController.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (videoState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.videoController.seekRelative(10000L) }) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Controls
                if (!videoState.isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = FormatUtils.formatDuration(videoState.currentPositionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Text(
                                text = "${videoState.playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = FormatUtils.formatDuration(videoState.totalDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }

                        Slider(
                            value = videoState.currentPositionMs.toFloat(),
                            onValueChange = { viewModel.videoController.seekTo(it.toLong()) },
                            valueRange = 0f..videoState.totalDurationMs.coerceAtLeast(1L).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}
