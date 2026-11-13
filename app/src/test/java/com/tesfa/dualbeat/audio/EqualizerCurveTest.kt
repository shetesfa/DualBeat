package com.tesfa.dualbeat.audio

import com.tesfa.dualbeat.domain.model.EqualizerPresetName
import org.junit.Assert.*
import org.junit.Test

class EqualizerCurveTest {

    @Test
    fun testPresetNamesCount() {
        val presets = EqualizerPresetName.values()
        assertEquals("Expected 9 presets", 9, presets.size)
    }

    @Test
    fun testPresetDisplayNames() {
        assertEquals("Normal", EqualizerPresetName.NORMAL.displayName)
        assertEquals("Rock", EqualizerPresetName.ROCK.displayName)
        assertEquals("Pop", EqualizerPresetName.POP.displayName)
        assertEquals("Classical", EqualizerPresetName.CLASSICAL.displayName)
        assertEquals("Jazz", EqualizerPresetName.JAZZ.displayName)
        assertEquals("Electronic", EqualizerPresetName.ELECTRONIC.displayName)
        assertEquals("Vocal", EqualizerPresetName.VOCAL.displayName)
        assertEquals("Bass Boost", EqualizerPresetName.BASS_BOOST.displayName)
        assertEquals("Custom", EqualizerPresetName.CUSTOM.displayName)
    }

    @Test
    fun testBassBoostGainCalculation() {
        val minLevel: Short = -1500
        val maxLevel: Short = 1500

        val targetLevels = listOf<Short>(600, 400, 200, 0, 0)
        val clamped = targetLevels.map { it.coerceIn(minLevel, maxLevel) }

        assertEquals(600.toShort(), clamped[0])
        assertEquals(400.toShort(), clamped[1])
        assertEquals(0.toShort(), clamped[4])
    }
}
