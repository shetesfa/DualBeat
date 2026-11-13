package com.tesfa.dualbeat.bluetooth

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.tesfa.dualbeat.domain.model.AudioRouteReport
import com.tesfa.dualbeat.domain.model.AudioSinkType
import com.tesfa.dualbeat.domain.model.DualAudioDetectionStatus

class MultiBluetoothEngine(
    private val context: Context,
    private val deviceManager: BluetoothDeviceManager
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun analyzeRoutingCapabilities(): AudioRouteReport {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val (currentOutput, activeName) = deviceManager.getActiveAudioOutput()
        val bondedDevices = deviceManager.bondedDevices.value
        val connectedBtDevices = bondedDevices.filter { it.isConnected }

        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

        val dualAudioStatus = when {
            !isSamsung -> DualAudioDetectionStatus.UNSUPPORTED_PLATFORM
            connectedBtDevices.isEmpty() -> DualAudioDetectionStatus.NOT_CONNECTED
            connectedBtDevices.size == 1 -> DualAudioDetectionStatus.SINGLE_OUTPUT_ACTIVE
            connectedBtDevices.size >= 2 -> DualAudioDetectionStatus.SYSTEM_MANAGED
            else -> DualAudioDetectionStatus.UNKNOWN
        }

        val availableSinks = mutableListOf<AudioSinkType>()
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }) availableSinks.add(AudioSinkType.INTERNAL_SPEAKER)
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }) availableSinks.add(AudioSinkType.WIRED_HEADSET)
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }) availableSinks.add(AudioSinkType.BLUETOOTH_A2DP)
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE }) availableSinks.add(AudioSinkType.USB_AUDIO)
        availableSinks.add(AudioSinkType.WIFI_CAST_RECEIVER)

        val limitationNotice = buildString {
            append("Hardware: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n")
            if (isSamsung) {
                if (connectedBtDevices.size >= 2) {
                    append("2 or more Bluetooth speakers connected. Samsung Dual Audio is controlled exclusively by One UI system policy. To route media to both speakers: Swipe down Quick Panel → Tap 'Media Output' → Check both speakers. Public Android SDK APIs cannot force dual A2DP replication or confirm hardware clock synchronization.")
                } else if (connectedBtDevices.size == 1) {
                    append("1 Bluetooth audio device connected. Connect a second Bluetooth speaker to configure Dual Audio via Samsung Quick Panel.")
                } else {
                    append("No Bluetooth speakers connected. Pair devices in Android Bluetooth settings.")
                }
            } else {
                append("Standard Android OS enforces a single active A2DP audio route for third-party apps. DualBeat Wi-Fi Cast provides multi-device synchronized streaming.")
            }
        }

        return AudioRouteReport(
            currentActiveOutput = currentOutput,
            activeOutputName = activeName,
            connectedBluetoothSinks = connectedBtDevices,
            availableOutputs = availableSinks,
            dualAudioStatus = dualAudioStatus,
            isPhysicalMultiOutputVerified = false, // Not physically verified with dual hardware probes
            hardwareLimitationNotice = limitationNotice
        )
    }
}
