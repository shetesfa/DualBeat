package com.tesfa.dualbeat.domain.model

enum class ReceiverState {
    DISCOVERED,
    CONNECTING,
    CONNECTED_SYNCED,
    BUFFERING,
    DISCONNECTED,
    ERROR
}

data class ReceiverNode(
    val id: String, // Unique UUID or IP:Port
    val name: String,
    val ipAddress: String,
    val port: Int = 8990,
    val state: ReceiverState = ReceiverState.DISCOVERED,
    val latencyMs: Long = 0L,
    val clockOffsetMs: Long = 0L,
    val volumePercent: Int = 100,
    val groupName: String? = null,
    val isPrimary: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class ReceiverGroup(
    val id: String,
    val name: String,
    val memberNodeIds: List<String>,
    val isPlaying: Boolean = false,
    val masterVolume: Int = 100
)
