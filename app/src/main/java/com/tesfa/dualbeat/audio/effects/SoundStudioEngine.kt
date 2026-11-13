package com.tesfa.dualbeat.audio.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.tesfa.dualbeat.domain.model.AudioEffectState
import com.tesfa.dualbeat.domain.model.EqualizerBand
import com.tesfa.dualbeat.domain.model.EqualizerPresetName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundStudioEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(AudioEffectState())
    val state: StateFlow<AudioEffectState> = _state.asStateFlow()

    fun attachSession(audioSessionId: Int) {
        if (audioSessionId == 0 && currentSessionId != 0) return
        if (audioSessionId == currentSessionId && equalizer != null) return

        releaseEffects()
        currentSessionId = audioSessionId

        try {
            // Attach Equalizer
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            eq.enabled = _state.value.isEnabled

            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange // e.g. [-1500, 1500]
            val minRange = range[0]
            val maxRange = range[1]

            val bands = (0 until numBands).map { i ->
                val shortIdx = i.toShort()
                EqualizerBand(
                    index = shortIdx,
                    centerFreqMhz = eq.getCenterFreq(shortIdx),
                    minLevelMilliBel = minRange,
                    maxLevelMilliBel = maxRange,
                    currentLevelMilliBel = eq.getBandLevel(shortIdx)
                )
            }

            // Attach BassBoost
            val bb = BassBoost(0, audioSessionId)
            bassBoost = bb
            val bbSupported = bb.strengthSupported
            bb.enabled = _state.value.isEnabled && bbSupported
            if (bbSupported) {
                bb.setStrength(_state.value.bassBoostStrength)
            }

            // Attach Virtualizer
            val virt = Virtualizer(0, audioSessionId)
            virtualizer = virt
            val virtSupported = virt.strengthSupported
            virt.enabled = _state.value.isEnabled && virtSupported
            if (virtSupported) {
                virt.setStrength(_state.value.virtualizerStrength)
            }

            _state.value = _state.value.copy(
                audioSessionId = audioSessionId,
                bands = bands,
                isBassBoostSupported = bbSupported,
                isVirtualizerSupported = virtSupported,
                statusMessage = "Connected to Audio Session #$audioSessionId"
            )

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                statusMessage = "AudioFX initialization note: ${e.localizedMessage ?: "Default profile active"}"
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        runCatching { equalizer?.enabled = enabled }
        runCatching { if (_state.value.isBassBoostSupported) bassBoost?.enabled = enabled }
        runCatching { if (_state.value.isVirtualizerSupported) virtualizer?.enabled = enabled }
    }

    fun setBandLevel(bandIndex: Short, levelMilliBel: Short) {
        val eq = equalizer ?: return
        val currentBands = _state.value.bands.toMutableList()
        val index = currentBands.indexOfFirst { it.index == bandIndex }
        if (index != -1) {
            val band = currentBands[index]
            val clamped = levelMilliBel.coerceIn(band.minLevelMilliBel, band.maxLevelMilliBel)
            runCatching { eq.setBandLevel(bandIndex, clamped) }
            currentBands[index] = band.copy(currentLevelMilliBel = clamped)
            _state.value = _state.value.copy(
                bands = currentBands,
                selectedPreset = EqualizerPresetName.CUSTOM
            )
        }
    }

    fun applyPreset(preset: EqualizerPresetName) {
        val eq = equalizer ?: return
        _state.value = _state.value.copy(selectedPreset = preset)

        val bands = _state.value.bands
        if (bands.isEmpty()) return

        // Compute curves based on frequency position (low to high)
        val count = bands.size
        val levels = when (preset) {
            EqualizerPresetName.NORMAL -> List(count) { 0.toShort() }
            EqualizerPresetName.BASS_BOOST -> List(count) { idx ->
                when {
                    idx == 0 -> 600.toShort()
                    idx == 1 -> 400.toShort()
                    idx == 2 -> 200.toShort()
                    else -> 0.toShort()
                }
            }
            EqualizerPresetName.ROCK -> List(count) { idx ->
                when {
                    idx == 0 -> 500.toShort()
                    idx == 1 -> 300.toShort()
                    idx == count - 2 -> 400.toShort()
                    idx == count - 1 -> 600.toShort()
                    else -> (-100).toShort()
                }
            }
            EqualizerPresetName.POP -> List(count) { idx ->
                when {
                    idx == 0 -> (-200).toShort()
                    idx == 1 -> 200.toShort()
                    idx == 2 -> 500.toShort()
                    idx == 3 -> 300.toShort()
                    else -> (-100).toShort()
                }
            }
            EqualizerPresetName.CLASSICAL -> List(count) { idx ->
                when {
                    idx == 0 -> 400.toShort()
                    idx == 1 -> 300.toShort()
                    idx == count - 1 -> 400.toShort()
                    else -> (-200).toShort()
                }
            }
            EqualizerPresetName.JAZZ -> List(count) { idx ->
                when {
                    idx == 0 -> 300.toShort()
                    idx == 1 -> 200.toShort()
                    idx == count - 1 -> 300.toShort()
                    else -> 100.toShort()
                }
            }
            EqualizerPresetName.ELECTRONIC -> List(count) { idx ->
                when {
                    idx == 0 -> 600.toShort()
                    idx == 1 -> 500.toShort()
                    idx == count - 1 -> 500.toShort()
                    else -> 0.toShort()
                }
            }
            EqualizerPresetName.VOCAL -> List(count) { idx ->
                when {
                    idx == 0 -> (-300).toShort()
                    idx == 1 -> (-100).toShort()
                    idx == count / 2 -> 500.toShort()
                    else -> (-200).toShort()
                }
            }
            EqualizerPresetName.CUSTOM -> return
        }

        val updatedBands = bands.mapIndexed { idx, band ->
            val lvl = (levels.getOrNull(idx) ?: 0.toShort()).coerceIn(band.minLevelMilliBel, band.maxLevelMilliBel)
            runCatching { eq.setBandLevel(band.index, lvl) }
            band.copy(currentLevelMilliBel = lvl)
        }

        _state.value = _state.value.copy(bands = updatedBands)
    }

    fun setBassBoost(strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassBoostStrength = clamped)
        if (_state.value.isBassBoostSupported) {
            runCatching { bassBoost?.setStrength(clamped) }
        }
    }

    fun setVirtualizer(strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(virtualizerStrength = clamped)
        if (_state.value.isVirtualizerSupported) {
            runCatching { virtualizer?.setStrength(clamped) }
        }
    }

    fun reset() {
        applyPreset(EqualizerPresetName.NORMAL)
        setBassBoost(0)
        setVirtualizer(0)
    }

    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
