package com.tesfa.dualbeat.domain.model

enum class EqualizerPresetName(val displayName: String) {
    NORMAL("Normal"),
    ROCK("Rock"),
    POP("Pop"),
    CLASSICAL("Classical"),
    JAZZ("Jazz"),
    ELECTRONIC("Electronic"),
    VOCAL("Vocal"),
    BASS_BOOST("Bass Boost"),
    CUSTOM("Custom")
}

data class EqualizerBand(
    val index: Short,
    val centerFreqMhz: Int, // e.g. 60000 = 60Hz
    val minLevelMilliBel: Short, // e.g. -1500 (-15dB)
    val maxLevelMilliBel: Short, // e.g. +1500 (+15dB)
    val currentLevelMilliBel: Short
)

data class AudioEffectState(
    val isEnabled: Boolean = false,
    val selectedPreset: EqualizerPresetName = EqualizerPresetName.NORMAL,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Short = 0, // 0..1000
    val isBassBoostSupported: Boolean = false,
    val virtualizerStrength: Short = 0, // 0..1000
    val isVirtualizerSupported: Boolean = false,
    val audioSessionId: Int = 0,
    val statusMessage: String = "Engine initialized"
)
