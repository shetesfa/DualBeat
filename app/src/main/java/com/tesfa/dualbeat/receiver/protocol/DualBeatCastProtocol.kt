package com.tesfa.dualbeat.receiver.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DualBeatCastProtocol {
    const val DISCOVERY_PORT = 8990
    const val STREAM_PORT = 8991
    const val MAGIC_HEADER: Int = 0x44424554 // "DBET" in ASCII

    const val TYPE_DISCOVERY_PING = 1
    const val TYPE_DISCOVERY_PONG = 2
    const val TYPE_SYNC_PING = 3
    const val TYPE_SYNC_PONG = 4
    const val TYPE_AUDIO_DATA = 5
    const val TYPE_CONTROL_VOLUME = 6
    const val TYPE_DISCONNECT = 7

    // Audio format constants
    const val SAMPLE_RATE = 44100
    const val CHANNELS = 2
    const val BYTES_PER_SAMPLE = 2 // 16-bit PCM

    fun buildDiscoveryPongPacket(deviceName: String, deviceId: String): ByteArray {
        val nameBytes = deviceName.toByteArray(Charsets.UTF_8)
        val idBytes = deviceId.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(16 + nameBytes.size + idBytes.size).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(MAGIC_HEADER)
        buffer.putInt(TYPE_DISCOVERY_PONG)
        buffer.putInt(nameBytes.size)
        buffer.put(nameBytes)
        buffer.putInt(idBytes.size)
        buffer.put(idBytes)
        return buffer.array()
    }

    fun parseDiscoveryPacket(data: ByteArray, length: Int): Pair<String, String>? {
        if (length < 8) return null
        val buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.getInt()
        if (magic != MAGIC_HEADER) return null
        val type = buffer.getInt()
        if (type != TYPE_DISCOVERY_PONG) return null

        val nameLen = buffer.getInt()
        if (buffer.remaining() < nameLen) return null
        val nameBytes = ByteArray(nameLen)
        buffer.get(nameBytes)

        val idLen = buffer.getInt()
        if (buffer.remaining() < idLen) return null
        val idBytes = ByteArray(idLen)
        buffer.get(idBytes)

        return Pair(String(nameBytes, Charsets.UTF_8), String(idBytes, Charsets.UTF_8))
    }

    fun buildAudioPacket(seq: Long, presentationTimestampMs: Long, audioData: ByteArray, offset: Int, size: Int): ByteArray {
        val totalSize = 4 + 4 + 8 + 8 + 4 + size
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(MAGIC_HEADER)
        buffer.putInt(TYPE_AUDIO_DATA)
        buffer.putLong(seq)
        buffer.putLong(presentationTimestampMs)
        buffer.putInt(size)
        buffer.put(audioData, offset, size)
        return buffer.array()
    }
}
