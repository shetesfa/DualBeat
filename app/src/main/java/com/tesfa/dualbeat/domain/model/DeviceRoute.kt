package com.tesfa.dualbeat.domain.model

enum class AudioSinkType {
    INTERNAL_SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH_A2DP,
    BLUETOOTH_LE,
    USB_AUDIO,
    WIFI_CAST_RECEIVER,
    UNKNOWN
}

enum class BluetoothConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUDIO_ACTIVE,
    DISCONNECTING
}

data class BluetoothAudioDevice(
    val address: String,
    val name: String,
    val isBonded: Boolean,
    val isConnected: Boolean,
    val isAudioActive: Boolean,
    val batteryLevel: Int? = null, // null if unavailable
    val sinkType: AudioSinkType = AudioSinkType.BLUETOOTH_A2DP,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class AudioRouteReport(
    val currentActiveOutput: AudioSinkType,
    val activeOutputName: String,
    val connectedBluetoothSinks: List<BluetoothAudioDevice>,
    val availableOutputs: List<AudioSinkType>,
    val isSamsungDualAudioActive: Boolean,
    val isMultiOutputSupportedByFirmware: Boolean,
    val hardwareLimitationNotice: String
)
