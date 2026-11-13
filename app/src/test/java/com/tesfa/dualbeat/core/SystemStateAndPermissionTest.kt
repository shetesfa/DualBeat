package com.tesfa.dualbeat.core

import com.tesfa.dualbeat.domain.model.AudioSinkType
import com.tesfa.dualbeat.domain.model.ReceiverNode
import com.tesfa.dualbeat.domain.model.ReceiverState
import com.tesfa.dualbeat.domain.model.isConnected
import com.tesfa.dualbeat.playback.player.PlaybackState
import com.tesfa.dualbeat.playback.player.RepeatMode
import org.junit.Assert.*
import org.junit.Test

class SystemStateAndPermissionTest {

    @Test
    fun testReceiverNodeStateTransitions() {
        val node = ReceiverNode(
            id = "rec-101",
            name = "Kitchen Speaker",
            ipAddress = "192.168.1.50",
            port = 8991,
            state = ReceiverState.DISCOVERED
        )

        assertFalse("Discovered node must not be connected", node.isConnected)

        val connectingNode = node.copy(state = ReceiverState.CONNECTING)
        assertFalse("Connecting node must not be marked connected", connectingNode.isConnected)

        val connectedNode = node.copy(state = ReceiverState.CONNECTED_SYNCED)
        assertTrue("Synced node must be connected", connectedNode.isConnected)

        val disconnectedNode = node.copy(state = ReceiverState.DISCONNECTED)
        assertFalse("Disconnected node must not be connected", disconnectedNode.isConnected)

        val errorNode = node.copy(state = ReceiverState.ERROR)
        assertFalse("Error node must not be connected", errorNode.isConnected)
    }

    @Test
    fun testPlaybackStateHandling() {
        val state = PlaybackState(
            isPlaying = false,
            currentPositionMs = 0L,
            totalDurationMs = 240000L,
            repeatMode = RepeatMode.OFF,
            isShuffleEnabled = false
        )

        assertFalse(state.isPlaying)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(240000L, state.totalDurationMs)

        val playingState = state.copy(isPlaying = true, currentPositionMs = 15000L)
        assertTrue(playingState.isPlaying)
        assertEquals(15000L, playingState.currentPositionMs)

        val errorState = state.copy(errorMessage = "Unsupported audio track codec")
        assertNotNull(errorState.errorMessage)
        assertEquals("Unsupported audio track codec", errorState.errorMessage)
    }

    @Test
    fun testAudioSinkTypesCompleteness() {
        val sinks = AudioSinkType.values()
        assertTrue(sinks.contains(AudioSinkType.INTERNAL_SPEAKER))
        assertTrue(sinks.contains(AudioSinkType.BLUETOOTH_A2DP))
        assertTrue(sinks.contains(AudioSinkType.WIRED_HEADSET))
        assertTrue(sinks.contains(AudioSinkType.USB_AUDIO))
        assertTrue(sinks.contains(AudioSinkType.WIFI_CAST_RECEIVER))
    }
}
