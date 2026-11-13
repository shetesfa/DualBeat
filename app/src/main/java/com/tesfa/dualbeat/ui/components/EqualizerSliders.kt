package com.tesfa.dualbeat.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesfa.dualbeat.domain.model.AudioEffectState
import com.tesfa.dualbeat.domain.model.EqualizerPresetName

@Composable
fun EqualizerSliders(
    state: AudioEffectState,
    onBandChange: (Short, Short) -> Unit,
    onPresetSelect: (EqualizerPresetName) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onVirtualizerChange: (Short) -> Unit,
    onToggleEnable: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sound Studio Equalizer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleEnable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Horizontal Row
            Text(
                text = "Studio Presets",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPresetName.values().forEach { preset ->
                    val isSelected = state.selectedPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelect(preset) },
                        label = { Text(preset.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Equalizer Bands Sliders
            if (state.bands.isNotEmpty()) {
                Text(
                    text = "Graphic Frequency Bands",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                state.bands.forEach { band ->
                    val freqHz = band.centerFreqMhz / 1000
                    val freqLabel = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"
                    val currentDb = band.currentLevelMilliBel / 100

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            modifier = Modifier.width(60.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Slider(
                            value = band.currentLevelMilliBel.toFloat(),
                            onValueChange = { onBandChange(band.index, it.toInt().toShort()) },
                            valueRange = band.minLevelMilliBel.toFloat()..band.maxLevelMilliBel.toFloat(),
                            enabled = state.isEnabled,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${if (currentDb > 0) "+" else ""}$currentDb dB",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.width(48.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bass Boost & Virtualizer
            if (state.isBassBoostSupported) {
                Text(
                    text = "Bass Boost: ${state.bassBoostStrength / 10}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = state.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                    valueRange = 0f..1000f,
                    enabled = state.isEnabled
                )
            }

            if (state.isVirtualizerSupported) {
                Text(
                    text = "3D Virtualizer: ${state.virtualizerStrength / 10}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = state.virtualizerStrength.toFloat(),
                    onValueChange = { onVirtualizerChange(it.toInt().toShort()) },
                    valueRange = 0f..1000f,
                    enabled = state.isEnabled
                )
            }
        }
    }
}
