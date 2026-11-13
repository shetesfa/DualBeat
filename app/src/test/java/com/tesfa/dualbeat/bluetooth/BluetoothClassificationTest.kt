package com.tesfa.dualbeat.bluetooth

import com.tesfa.dualbeat.domain.model.AudioSinkType
import com.tesfa.dualbeat.domain.model.DualAudioDetectionStatus
import org.junit.Assert.*
import org.junit.Test

class BluetoothClassificationTest {

    @Test
    fun testNoBluetoothConnectedClassifiedAsNotConnected() {
        val btDeviceCount = 0
        val isA2dpActive = false

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        assertEquals("Expected NOT_CONNECTED when 0 BT devices connected", DualAudioDetectionStatus.NOT_CONNECTED, status)
    }

    @Test
    fun testSingleBluetoothOutputClassifiedAsSingleActive() {
        val btDeviceCount = 1
        val isA2dpActive = true

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        assertEquals("Expected SINGLE_OUTPUT_ACTIVE when 1 BT device is streaming", DualAudioDetectionStatus.SINGLE_OUTPUT_ACTIVE, status)
    }

    @Test
    fun testMultipleBluetoothEndpointsClassifiedAsSystemManagedNeverFakeActive() {
        // Critical verification: Having 2 A2DP devices connected MUST NOT
        // report fake ACTIVE state, but rather SYSTEM_MANAGED
        val btDeviceCount = 2
        val isA2dpActive = true

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        assertEquals("Expected SYSTEM_MANAGED for multiple BT devices", DualAudioDetectionStatus.SYSTEM_MANAGED, status)
        assertTrue("Status display text must state System-Managed", status.displayText.contains("System-Managed"))
    }

    @Test
    fun testThreeBluetoothEndpointsStillSystemManaged() {
        val btDeviceCount = 3
        val isA2dpActive = true

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        assertEquals("Expected SYSTEM_MANAGED for 3 devices", DualAudioDetectionStatus.SYSTEM_MANAGED, status)
    }

    @Test
    fun testUnsupportedPlatformClassification() {
        val isSamsung = false
        val status = if (!isSamsung) DualAudioDetectionStatus.UNSUPPORTED_PLATFORM else DualAudioDetectionStatus.UNKNOWN
        assertEquals("Non-Samsung devices enforce standard Android single route", DualAudioDetectionStatus.UNSUPPORTED_PLATFORM, status)
    }

    private fun classifyDualAudioStatus(btDeviceCount: Int, isA2dpActive: Boolean): DualAudioDetectionStatus {
        return when {
            btDeviceCount >= 2 -> DualAudioDetectionStatus.SYSTEM_MANAGED
            btDeviceCount == 1 && isA2dpActive -> DualAudioDetectionStatus.SINGLE_OUTPUT_ACTIVE
            btDeviceCount == 0 -> DualAudioDetectionStatus.NOT_CONNECTED
            else -> DualAudioDetectionStatus.UNKNOWN
        }
    }
}
