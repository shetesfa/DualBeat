package com.tesfa.dualbeat.ui.split

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesfa.dualbeat.ui.music.MusicScreen
import com.tesfa.dualbeat.ui.music.MusicViewModel
import com.tesfa.dualbeat.ui.sound.SoundStudioScreen
import com.tesfa.dualbeat.ui.sound.SoundStudioViewModel
import com.tesfa.dualbeat.ui.speakers.SpeakersScreen
import com.tesfa.dualbeat.ui.speakers.SpeakersViewModel

enum class SplitMode(val title: String) {
    MUSIC_AND_EQUALIZER("Music + Equalizer"),
    SPEAKERS_AND_SOUND("Speakers + Sound")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSplitViewScreen(
    musicViewModel: MusicViewModel,
    soundViewModel: SoundStudioViewModel,
    speakersViewModel: SpeakersViewModel,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(SplitMode.MUSIC_AND_EQUALIZER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Split Multitasking Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode Selector
            TabRow(
                selectedTabIndex = selectedMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SplitMode.values().forEach { mode ->
                    Tab(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        text = { Text(mode.title) }
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth > 600.dp

                if (isWide) {
                    // Landscape / Tablet Side-by-Side Dual Pane
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (selectedMode) {
                                SplitMode.MUSIC_AND_EQUALIZER -> MusicScreen(viewModel = musicViewModel, onNavigateToEqualizer = {})
                                SplitMode.SPEAKERS_AND_SOUND -> SpeakersScreen(viewModel = speakersViewModel)
                            }
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SoundStudioScreen(viewModel = soundViewModel)
                        }
                    }
                } else {
                    // Phone Vertical Split
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedMode) {
                                SplitMode.MUSIC_AND_EQUALIZER -> MusicScreen(viewModel = musicViewModel, onNavigateToEqualizer = {})
                                SplitMode.SPEAKERS_AND_SOUND -> SpeakersScreen(viewModel = speakersViewModel)
                            }
                        }
                        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            SoundStudioScreen(viewModel = soundViewModel)
                        }
                    }
                }
            }
        }
    }
}
