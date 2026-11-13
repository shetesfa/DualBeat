package com.tesfa.dualbeat.ui.music

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.core.util.FormatUtils
import com.tesfa.dualbeat.domain.model.LyricsData
import com.tesfa.dualbeat.domain.model.ReceiverNode
import com.tesfa.dualbeat.playback.player.MusicPlaybackController
import com.tesfa.dualbeat.playback.player.PlaybackState
import com.tesfa.dualbeat.playback.player.RepeatMode
import com.tesfa.dualbeat.ui.components.*
import kotlinx.coroutines.launch

enum class PlayerDisplayTab {
    COVER_AND_VISUALIZER,
    LIVE_LYRICS,
    SOUND_MESH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullMusicPlayerBottomSheet(
    playbackState: PlaybackState,
    musicController: MusicPlaybackController,
    onDismiss: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val track = playbackState.currentTrack ?: return
    val app = DualBeatApp.instance
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(PlayerDisplayTab.COVER_AND_VISUALIZER) }
    var lyricsData by remember { mutableStateOf(LyricsData(track.id, false)) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var currentSleepOption by remember { mutableStateOf(SleepTimerOption.OFF) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    // Wi-Fi Cast Mesh States
    val castServer = app.castServer
    val castClient = app.castClient
    val isTransmitterRunning by castServer.isBroadcasting.collectAsState()
    val isReceiverRunning by castClient.isReceiving.collectAsState()
    val discoveredReceivers by castServer.discoveredReceivers.collectAsState()

    // Fetch synchronized lyrics whenever track changes
    LaunchedEffect(track.id) {
        lyricsData = app.lyricsRepository.getLyricsForTrack(track)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Segmented Modern Tab Selector (Cover, Lyrics, Mesh)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            TabPill(
                                title = "Cover",
                                isSelected = activeTab == PlayerDisplayTab.COVER_AND_VISUALIZER,
                                onClick = { activeTab = PlayerDisplayTab.COVER_AND_VISUALIZER }
                            )
                            TabPill(
                                title = "Lyrics",
                                isSelected = activeTab == PlayerDisplayTab.LIVE_LYRICS,
                                onClick = { activeTab = PlayerDisplayTab.LIVE_LYRICS }
                            )
                            TabPill(
                                title = "Mesh",
                                isSelected = activeTab == PlayerDisplayTab.SOUND_MESH,
                                onClick = { activeTab = PlayerDisplayTab.SOUND_MESH }
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (currentSleepOption != SleepTimerOption.OFF) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(onClick = {
                            onDismiss()
                            onNavigateToEqualizer()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = "Equalizer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Center Dynamic Content View (Visualizer Cover, Lyrics, or Multi-Speaker Mesh)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeTab) {
                        PlayerDisplayTab.COVER_AND_VISUALIZER -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Glowing Art Box
                                Box(
                                    modifier = Modifier
                                        .size(230.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(90.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Real-Time Animated Sound Spectrum Visualizer
                                AnimatedAudioSpectrumVisualizer(
                                    isPlaying = playbackState.isPlaying,
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                )
                            }
                        }

                        PlayerDisplayTab.LIVE_LYRICS -> {
                            LyricsView(
                                lyricsData = lyricsData,
                                currentPositionMs = playbackState.currentPositionMs,
                                onSeekToTimestamp = { musicController.seekTo(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        PlayerDisplayTab.SOUND_MESH -> {
                            SoundMeshHubView(
                                isTransmitterRunning = isTransmitterRunning,
                                isReceiverRunning = isReceiverRunning,
                                discoveredReceivers = discoveredReceivers,
                                onToggleTransmitter = {
                                    if (isTransmitterRunning) castServer.stopServer() else castServer.startServer()
                                },
                                onToggleReceiver = {
                                    if (isReceiverRunning) castClient.stopReceiverMode() else castClient.startReceiverMode()
                                },
                                onScanReceivers = { castServer.scanForReceivers() },
                                onToggleReceiverConnection = { receiver ->
                                    if (receiver.isConnected) {
                                        castServer.disconnectReceiver(receiver)
                                    } else {
                                        castServer.connectToReceiver(receiver)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Song Title & Artist
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Slider
                val progressFraction = if (playbackState.totalDurationMs > 0) {
                    playbackState.currentPositionMs.toFloat()
                } else 0f

                Slider(
                    value = progressFraction,
                    onValueChange = { musicController.seekTo(it.toLong()) },
                    valueRange = 0f..playbackState.totalDurationMs.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FormatUtils.formatDuration(playbackState.currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatDuration(playbackState.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { musicController.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Previous
                    IconButton(onClick = { musicController.previous() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Play/Pause with Glowing Gradient Ring
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { musicController.togglePlayPause() }) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(onClick = { musicController.next() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(onClick = { musicController.cycleRepeatMode() }) {
                        val icon = when (playbackState.repeatMode) {
                            RepeatMode.OFF -> Icons.Rounded.Repeat
                            RepeatMode.ALL -> Icons.Rounded.Repeat
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat Mode",
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Utilities Row (Speed Toggle & Quick Output Pill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "Speed",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playbackSpeed}x Speed",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        playbackSpeed = speed
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Party Sound Mesh Quick Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.clickable { activeTab = PlayerDisplayTab.SOUND_MESH }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SpeakerGroup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTransmitterRunning) "Mesh: Streaming" else "Mesh: Ready",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentOption = currentSleepOption,
            onSelectOption = { option ->
                currentSleepOption = option
                if (option != SleepTimerOption.OFF && option != SleepTimerOption.END_OF_TRACK) {
                    scope.launch {
                        kotlinx.coroutines.delay(option.minutes * 60 * 1000L)
                        musicController.pause()
                        currentSleepOption = SleepTimerOption.OFF
                    }
                }
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
private fun TabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
