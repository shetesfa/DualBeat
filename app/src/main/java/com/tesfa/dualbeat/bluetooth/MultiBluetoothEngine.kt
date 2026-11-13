package com.tesfa.dualbeat.bluetooth

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.tesfa.dualbeat.domain.model.AudioRouteReport
import com.tesfa.dualbeat.domain.model.AudioSinkType

class MultiBluetoothEngine(
    private val context: Context,
    private val deviceManager: BluetoothDeviceManager
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun analyzeRoutingCapabilities(): AudioRouteReport {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val activeBtOutputs = outputs.filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }

        val (currentOutput, activeName) = deviceManager.getActiveAudioOutput()
        val bondedDevices = deviceManager.bondedDevices.value
        val connectedBtDevices = bondedDevices.filter { it.isConnected }

        // Evaluation of Samsung Dual Audio / Android Multi-A2DP
        val isSystemDualAudioActive = activeBtOutputs.size >= 2
        val isSamsungDevice = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val model = Build.MODEL

        val availableSinks = mutableListOf<AudioSinkType>()
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }) availableSinks.add(AudioSinkType.INTERNAL_SPEAKER)
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }) availableSinks.add(AudioSinkType.WIRED_HEADSET)
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }) availableSinks.add(AudioSinkType.BLUETOOTH_A2DP)
        if (outputs.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE }) availableSinks.add(AudioSinkType.USB_AUDIO)
        availableSinks.add(AudioSinkType.WIFI_CAST_RECEIVER)

        val limitationNotice = buildString {
            append("Device: $model (Manufacturer: ${Build.MANUFACTURER})\n")
            if (isSamsungDevice) {
                append("Samsung One UI Platform Detected. ")
                if (isSystemDualAudioActive) {
                    append("Samsung Dual Audio is currently ACTIVE in system audio routing. Both speakers receive synchronized audio.")
                } else if (connectedBtDevices.size >= 2) {
                    append("Multiple Bluetooth speakers connected. To activate simultaneous playback, open Samsung One UI Quick Panel → Media Output → check both Bluetooth devices. Third-party apps cannot override system Bluetooth HAL permissions without platform signature.")
                } else {
                    append("Connect at least two Bluetooth audio devices to evaluate Dual Audio routing.")
                }
            } else {
                append("Standard Android OS enforces a single active A2DP audio route for 3rd-party apps. Use DualBeat Wi-Fi Multi-Speaker Cast for multi-speaker synchronization.")
            }
        }

        return AudioRouteReport(
            currentActiveOutput = currentOutput,
            activeOutputName = activeName,
            connectedBluetoothSinks = connectedBtDevices,
            availableOutputs = availableSinks,
            isSamsungDualAudioActive = isSystemDualAudioActive,
            isMultiOutputSupportedByFirmware = isSamsungDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            hardwareLimitationNotice = limitationNotice
        )
    }
}
