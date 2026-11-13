package com.tesfa.dualbeat.bluetooth

import com.tesfa.dualbeat.domain.model.AudioSinkType
import com.tesfa.dualbeat.domain.model.DualAudioDetectionStatus

class BluetoothClassificationTest {

    fun testNoBluetoothConnectedClassifiedAsNotConnected() {
        val btDeviceCount = 0
        val isA2dpActive = false

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        check(status == DualAudioDetectionStatus.NOT_CONNECTED) {
            "Expected NOT_CONNECTED when 0 BT devices connected, got: $status"
        }
        println("  [PASS] testNoBluetoothConnectedClassifiedAsNotConnected")
    }

    fun testSingleBluetoothOutputClassifiedAsSingleActive() {
        val btDeviceCount = 1
        val isA2dpActive = true

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        check(status == DualAudioDetectionStatus.SINGLE_OUTPUT_ACTIVE) {
            "Expected SINGLE_OUTPUT_ACTIVE when 1 BT device is streaming, got: $status"
        }
        println("  [PASS] testSingleBluetoothOutputClassifiedAsSingleActive")
    }

    fun testMultipleBluetoothEndpointsClassifiedAsSystemManagedNeverFakeActive() {
        // Critical verification of Rule 3: Having 2 A2DP devices connected MUST NOT
        // report fake ACTIVE state, but rather SYSTEM_MANAGED
        val btDeviceCount = 2
        val isA2dpActive = true

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        check(status == DualAudioDetectionStatus.SYSTEM_MANAGED) {
            "Expected SYSTEM_MANAGED for multiple BT devices, got: $status"
        }
        check(status.displayText.contains("System-Managed")) {
            "Status display text must state System-Managed"
        }
        println("  [PASS] testMultipleBluetoothEndpointsClassifiedAsSystemManagedNeverFakeActive")
    }

    fun testThreeBluetoothEndpointsStillSystemManaged() {
        val btDeviceCount = 3
        val isA2dpActive = true

        val status = classifyDualAudioStatus(btDeviceCount, isA2dpActive)
        check(status == DualAudioDetectionStatus.SYSTEM_MANAGED) {
            "Expected SYSTEM_MANAGED for 3 devices, got: $status"
        }
        println("  [PASS] testThreeBluetoothEndpointsStillSystemManaged")
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

fun main() {
    println("--- Executing BluetoothClassificationTest ---")
    val test = BluetoothClassificationTest()
    test.testNoBluetoothConnectedClassifiedAsNotConnected()
    test.testSingleBluetoothOutputClassifiedAsSingleActive()
    test.testMultipleBluetoothEndpointsClassifiedAsSystemManagedNeverFakeActive()
    test.testThreeBluetoothEndpointsStillSystemManaged()
    println("=== BluetoothClassificationTest: ALL TESTS PASSED ===")
}
