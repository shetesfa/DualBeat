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

enum class DualAudioDetectionStatus(val displayText: String) {
    SYSTEM_MANAGED("System-Managed in One UI"),
    SINGLE_OUTPUT_ACTIVE("Single Audio Route Active"),
    NOT_CONNECTED("No Bluetooth Audio Connected"),
    UNKNOWN("Status Unknown (OS Protected)"),
    UNSUPPORTED_PLATFORM("Standard Android Single Route")
}

data class BluetoothAudioDevice(
    val address: String,
    val name: String,
    val isBonded: Boolean,
    val isConnected: Boolean,
    val isAudioActive: Boolean,
    val batteryLevel: Int? = null,
    val sinkType: AudioSinkType = AudioSinkType.BLUETOOTH_A2DP,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class AudioRouteReport(
    val currentActiveOutput: AudioSinkType,
    val activeOutputName: String,
    val connectedBluetoothSinks: List<BluetoothAudioDevice>,
    val availableOutputs: List<AudioSinkType>,
    val dualAudioStatus: DualAudioDetectionStatus,
    val isPhysicalMultiOutputVerified: Boolean,
    val hardwareLimitationNotice: String
)
