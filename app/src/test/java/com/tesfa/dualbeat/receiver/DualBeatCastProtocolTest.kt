package com.tesfa.dualbeat.receiver

import com.tesfa.dualbeat.receiver.protocol.DualBeatCastProtocol
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DualBeatCastProtocolTest {

    fun testBuildAndParseDiscoveryPongPacket() {
        val deviceName = "Galaxy A24 Speaker"
        val deviceId = "node-8991"

        val packet = DualBeatCastProtocol.buildDiscoveryPongPacket(deviceName, deviceId)
        check(packet.isNotEmpty()) { "Packet should not be empty" }
        check(packet.size > 16) { "Packet should exceed header length" }

        val parsed = DualBeatCastProtocol.parseDiscoveryPacket(packet, packet.size)
        check(parsed != null) { "Parsed packet should not be null" }
        check(parsed.first == deviceName) { "Expected $deviceName but got ${parsed.first}" }
        check(parsed.second == deviceId) { "Expected $deviceId but got ${parsed.second}" }
        println("  [PASS] testBuildAndParseDiscoveryPongPacket")
    }

    fun testBuildAudioPacketHeaderAndPayload() {
        val pcmData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val seq = 42L
        val pts = 1700000000000L

        val packet = DualBeatCastProtocol.buildAudioPacket(
            seq = seq,
            presentationTimestampMs = pts,
            audioData = pcmData,
            offset = 0,
            size = pcmData.size
        )

        check(packet.size == 28 + pcmData.size) { "Expected ${28 + pcmData.size} but got ${packet.size}" }

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        check(buffer.getInt() == DualBeatCastProtocol.MAGIC_HEADER) { "Invalid magic header" }
        check(buffer.getInt() == DualBeatCastProtocol.TYPE_AUDIO_DATA) { "Invalid packet type" }
        check(buffer.getLong() == seq) { "Invalid sequence number" }
        check(buffer.getLong() == pts) { "Invalid PTS timestamp" }
        check(buffer.getInt() == pcmData.size) { "Invalid payload size" }

        val payload = ByteArray(pcmData.size)
        buffer.get(payload)
        check(payload.contentEquals(pcmData)) { "Payload content mismatch" }
        println("  [PASS] testBuildAudioPacketHeaderAndPayload")
    }

    fun testRejectInvalidMagicHeader() {
        val corrupted = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val parsed = DualBeatCastProtocol.parseDiscoveryPacket(corrupted, corrupted.size)
        check(parsed == null) { "Corrupted packet must be rejected" }
        println("  [PASS] testRejectInvalidMagicHeader")
    }
}

fun main() {
    println("--- Executing DualBeatCastProtocolTest ---")
    val test = DualBeatCastProtocolTest()
    test.testBuildAndParseDiscoveryPongPacket()
    test.testBuildAudioPacketHeaderAndPayload()
    test.testRejectInvalidMagicHeader()
    println("=== DualBeatCastProtocolTest: ALL TESTS PASSED ===")
}
