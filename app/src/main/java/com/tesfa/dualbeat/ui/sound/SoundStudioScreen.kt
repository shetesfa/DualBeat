package com.tesfa.dualbeat.ui.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesfa.dualbeat.ui.components.EqualizerSliders

@Composable
fun SoundStudioScreen(
    viewModel: SoundStudioViewModel
) {
    val effectState by viewModel.effectState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EqualizerSliders(
            state = effectState,
            onBandChange = { band, level -> viewModel.setBandLevel(band, level) },
            onPresetSelect = { preset -> viewModel.selectPreset(preset) },
            onBassBoostChange = { strength -> viewModel.setBassBoost(strength) },
            onVirtualizerChange = { strength -> viewModel.setVirtualizer(strength) },
            onToggleEnable = { viewModel.toggleEnable(it) }
        )

        OutlinedButton(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Sound Studio to Flat Defaults")
        }
    }
}
