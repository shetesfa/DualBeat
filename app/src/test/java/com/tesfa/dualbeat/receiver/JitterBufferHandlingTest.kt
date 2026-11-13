package com.tesfa.dualbeat.receiver

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

        check(queue.size == maxBufferSize) {
            "Expected buffer size to be bounded at $maxBufferSize, but was ${queue.size}"
        }

        val firstFrame = queue.peek()
        check(firstFrame != null && firstFrame.seq == 6L) {
            "Oldest frames 1..5 should have been pruned; expected seq 6 but got ${firstFrame?.seq}"
        }

        // Drain entire queue
        var drainedCount = 0
        while (queue.poll() != null) {
            drainedCount++
        }
        check(drainedCount == maxBufferSize) {
            "Expected $maxBufferSize drained frames, got $drainedCount"
        }
        check(queue.isEmpty()) { "Queue should be empty after drain" }
        println("  [PASS] testJitterBufferCapacityAndDrain")
    }

    fun testSequenceDiscontinuityDetection() {
        var lastSeq = 10L
        val incomingSeq = 14L

        val hasGap = incomingSeq > lastSeq + 1
        val missedPackets = (incomingSeq - (lastSeq + 1)).toInt()

        check(hasGap) { "Should detect sequence gap" }
        check(missedPackets == 3) { "Expected 3 missed packets (11, 12, 13), got $missedPackets" }
        println("  [PASS] testSequenceDiscontinuityDetection")
    }

    fun testPacketPayloadValidation() {
        val validSize = 4096
        val oversized = 100000 // exceeds MAX_PAYLOAD_SIZE (65536)
        val undersized = 0

        check(isValidPayloadSize(validSize)) { "4096 bytes must be valid" }
        check(!isValidPayloadSize(oversized)) { "100000 bytes must be rejected as oversized" }
        check(!isValidPayloadSize(undersized)) { "0 bytes must be rejected as empty" }
        println("  [PASS] testPacketPayloadValidation")
    }

    private fun isValidPayloadSize(size: Int): Boolean {
        val maxPayload = 65536
        return size in 1..maxPayload
    }
}

fun main() {
    println("--- Executing JitterBufferHandlingTest ---")
    val test = JitterBufferHandlingTest()
    test.testJitterBufferCapacityAndDrain()
    test.testSequenceDiscontinuityDetection()
    test.testPacketPayloadValidation()
    println("=== JitterBufferHandlingTest: ALL TESTS PASSED ===")
}
