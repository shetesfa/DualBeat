package com.tesfa.dualbeat.receiver

import com.tesfa.dualbeat.receiver.protocol.DualBeatCastProtocol
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DualBeatCastProtocolTest {

    @Test
    fun testBuildAndParseDiscoveryPongPacket() {
        val deviceName = "Galaxy A24 Speaker"
        val deviceId = "node-8991"

        val packet = DualBeatCastProtocol.buildDiscoveryPongPacket(deviceName, deviceId)
        assertTrue("Packet should not be empty", packet.isNotEmpty())
        assertTrue("Packet should exceed header length", packet.size > 16)

        val parsed = DualBeatCastProtocol.parseDiscoveryPacket(packet, packet.size)
        assertNotNull("Parsed packet should not be null", parsed)
        assertEquals("Expected deviceName mismatch", deviceName, parsed?.first)
        assertEquals("Expected deviceId mismatch", deviceId, parsed?.second)
    }

    @Test
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

        assertEquals("Packet size mismatch", 28 + pcmData.size, packet.size)

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        assertEquals("Invalid magic header", DualBeatCastProtocol.MAGIC_HEADER, buffer.getInt())
        assertEquals("Invalid packet type", DualBeatCastProtocol.TYPE_AUDIO_DATA, buffer.getInt())
        assertEquals("Invalid sequence number", seq, buffer.getLong())
        assertEquals("Invalid PTS timestamp", pts, buffer.getLong())
        assertEquals("Invalid payload size", pcmData.size, buffer.getInt())

        val payload = ByteArray(pcmData.size)
        buffer.get(payload)
        assertArrayEquals("Payload content mismatch", pcmData, payload)
    }

    @Test
    fun testRejectInvalidMagicHeader() {
        val corrupted = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val parsed = DualBeatCastProtocol.parseDiscoveryPacket(corrupted, corrupted.size)
        assertNull("Corrupted packet must be rejected", parsed)
    }

    @Test
    fun testRejectOversizedOrMalformedPackets() {
        // Less than minimum 8 bytes header
        val shortPacket = byteArrayOf(0x44, 0x42, 0x45)
        val parsedShort = DualBeatCastProtocol.parseDiscoveryPacket(shortPacket, shortPacket.size)
        assertNull("Short packet (< 8 bytes) must be rejected", parsedShort)

        // Invalid packet type
        val wrongTypeBuf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
        wrongTypeBuf.putInt(DualBeatCastProtocol.MAGIC_HEADER)
        wrongTypeBuf.putInt(999) // Invalid type
        val parsedWrong = DualBeatCastProtocol.parseDiscoveryPacket(wrongTypeBuf.array(), 16)
        assertNull("Invalid packet type must be rejected", parsedWrong)
    }

    @Test
    fun testAudioFormatConstants() {
        assertEquals(44100, DualBeatCastProtocol.SAMPLE_RATE)
        assertEquals(2, DualBeatCastProtocol.CHANNELS)
        assertEquals(2, DualBeatCastProtocol.BYTES_PER_SAMPLE)
        assertEquals(8990, DualBeatCastProtocol.DISCOVERY_PORT)
        assertEquals(8991, DualBeatCastProtocol.STREAM_PORT)
    }
}
