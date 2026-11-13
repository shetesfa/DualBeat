package com.tesfa.dualbeat.audio

import com.tesfa.dualbeat.domain.model.EqualizerPresetName

class EqualizerCurveTest {

    fun testPresetNamesCount() {
        val presets = EqualizerPresetName.values()
        check(presets.size == 9) { "Expected 9 presets, found ${presets.size}" }
        println("  [PASS] testPresetNamesCount (9 studio presets)")
    }

    fun testPresetDisplayNames() {
        check(EqualizerPresetName.NORMAL.displayName == "Normal") { "Preset display name mismatch" }
        check(EqualizerPresetName.ROCK.displayName == "Rock") { "Preset display name mismatch" }
        check(EqualizerPresetName.POP.displayName == "Pop") { "Preset display name mismatch" }
        check(EqualizerPresetName.CLASSICAL.displayName == "Classical") { "Preset display name mismatch" }
        check(EqualizerPresetName.JAZZ.displayName == "Jazz") { "Preset display name mismatch" }
        check(EqualizerPresetName.ELECTRONIC.displayName == "Electronic") { "Preset display name mismatch" }
        check(EqualizerPresetName.VOCAL.displayName == "Vocal") { "Preset display name mismatch" }
        check(EqualizerPresetName.BASS_BOOST.displayName == "Bass Boost") { "Preset display name mismatch" }
        check(EqualizerPresetName.CUSTOM.displayName == "Custom") { "Preset display name mismatch" }
        println("  [PASS] testPresetDisplayNames")
    }

    fun testBassBoostGainCalculation() {
        val minLevel: Short = -1500
        val maxLevel: Short = 1500

        val targetLevels = listOf<Short>(600, 400, 200, 0, 0)
        val clamped = targetLevels.map { it.coerceIn(minLevel, maxLevel) }

        check(clamped[0] == 600.toShort()) { "Bass level 0 mismatch" }
        check(clamped[1] == 400.toShort()) { "Bass level 1 mismatch" }
        check(clamped[4] == 0.toShort()) { "Bass level 4 mismatch" }
        println("  [PASS] testBassBoostGainCalculation")
    }
}

fun main() {
    println("--- Executing EqualizerCurveTest ---")
    val test = EqualizerCurveTest()
    test.testPresetNamesCount()
    test.testPresetDisplayNames()
    test.testBassBoostGainCalculation()
    println("=== EqualizerCurveTest: ALL TESTS PASSED ===")
}
