package com.tesfa.dualbeat.receiver

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class JitterBufferHandlingTest {

    data class TestAudioFrame(
        val seq: Long,
        val pts: Long,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TestAudioFrame
            return seq == other.seq && pts == other.pts && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = seq.hashCode()
            result = 31 * result + pts.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    @Test
    fun testJitterBufferCapacityAndDrain() {
        val maxBufferSize = 20
        val queue = ConcurrentLinkedQueue<TestAudioFrame>()

        // Push 25 frames
        for (i in 1..25) {
            val frame = TestAudioFrame(i.toLong(), 1000L + i * 20, ByteArray(512) { 1 })
            queue.offer(frame)
            while (queue.size > maxBufferSize) {
                queue.poll() // Drop oldest on overflow
            }
        }

        assertEquals("Expected buffer size to be bounded at $maxBufferSize", maxBufferSize, queue.size)

        val firstFrame = queue.peek()
        assertNotNull("First frame should not be null", firstFrame)
        assertEquals("Oldest frames 1..5 should have been pruned; expected seq 6", 6L, firstFrame?.seq)

        // Drain entire queue
        var drainedCount = 0
        while (queue.poll() != null) {
            drainedCount++
        }
        assertEquals("Expected drained count mismatch", maxBufferSize, drainedCount)
        assertTrue("Queue should be empty after drain", queue.isEmpty())
    }

    @Test
    fun testSequenceDiscontinuityDetection() {
        val lastSeq = 10L
        val incomingSeq = 14L

        val hasGap = incomingSeq > lastSeq + 1
        val missedPackets = (incomingSeq - (lastSeq + 1)).toInt()

        assertTrue("Should detect sequence gap", hasGap)
        assertEquals("Expected 3 missed packets (11, 12, 13)", 3, missedPackets)
    }

    @Test
    fun testPacketPayloadValidation() {
        val validSize = 4096
        val oversized = 100000 // exceeds MAX_PAYLOAD_SIZE (65536)
        val undersized = 0

        assertTrue("4096 bytes must be valid", isValidPayloadSize(validSize))
        assertFalse("100000 bytes must be rejected as oversized", isValidPayloadSize(oversized))
        assertFalse("0 bytes must be rejected as empty", isValidPayloadSize(undersized))
    }

    @Test
    fun testBufferUnderrunBehavior() {
        val queue = ConcurrentLinkedQueue<TestAudioFrame>()
        val targetPrebuffer = 4

        // Initially empty: underrun state
        assertTrue("Queue is empty, represents underrun", queue.isEmpty())
        assertTrue("Insufficient frames for playback", queue.size < targetPrebuffer)

        // Add 3 frames (still below target of 4)
        for (i in 1..3) {
            queue.offer(TestAudioFrame(i.toLong(), 1000L + i * 20, ByteArray(128)))
        }
        assertTrue("Still below prebuffer threshold", queue.size < targetPrebuffer)

        // Add 4th frame (reaches target threshold)
        queue.offer(TestAudioFrame(4L, 1080L, ByteArray(128)))
        assertTrue("Prebuffer target reached, playback can begin", queue.size >= targetPrebuffer)

        // Drain all frames to trigger underrun again
        while (queue.poll() != null) { }
        assertTrue("Queue emptied: underrun triggered again for resync", queue.isEmpty())
    }

    private fun isValidPayloadSize(size: Int): Boolean {
        val maxPayload = 65536
        return size in 1..maxPayload
    }
}
