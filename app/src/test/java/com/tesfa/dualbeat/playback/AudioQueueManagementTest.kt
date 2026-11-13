package com.tesfa.dualbeat.playback

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

data class TestQueueTrack(
    val id: Long,
    val title: String,
    val artist: String = "Test Artist",
    val durationMs: Long = 180000L
)

class AudioQueueManagementTest {

    private fun createDummyTrack(id: Long, title: String): TestQueueTrack {
        return TestQueueTrack(
            id = id,
            title = title,
            artist = "Artist $id",
            durationMs = 180000L
        )
    }

    fun testNextWithRepeatOff() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2"),
            createDummyTrack(3, "Track 3")
        )

        var currentIndex = 1
        val nextIdx = calculateNextIndex(currentIndex, queue.size, RepeatMode.OFF)
        check(nextIdx == 2) { "Expected index 2, got $nextIdx" }

        currentIndex = 2 // at end of queue
        val atEndIdx = calculateNextIndex(currentIndex, queue.size, RepeatMode.OFF)
        check(atEndIdx == -1) { "Expected -1 (stop) at end with RepeatMode.OFF, got $atEndIdx" }
        println("  [PASS] testNextWithRepeatOff")
    }

    fun testNextWithRepeatAll() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2"),
            createDummyTrack(3, "Track 3")
        )

        val atEndIdx = calculateNextIndex(2, queue.size, RepeatMode.ALL)
        check(atEndIdx == 0) { "Expected wrap-around to index 0 with RepeatMode.ALL, got $atEndIdx" }
        println("  [PASS] testNextWithRepeatAll")
    }

    fun testNextWithRepeatOne() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2")
        )

        val idx = calculateNextIndex(1, queue.size, RepeatMode.ONE)
        check(idx == 1) { "Expected same index 1 with RepeatMode.ONE, got $idx" }
        println("  [PASS] testNextWithRepeatOne")
    }

    fun testPreviousNavigation() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2"),
            createDummyTrack(3, "Track 3")
        )

        val prevFromMiddle = calculatePrevIndex(1, queue.size, RepeatMode.OFF)
        check(prevFromMiddle == 0) { "Expected 0 from 1, got $prevFromMiddle" }

        val prevFromStartRepeatOff = calculatePrevIndex(0, queue.size, RepeatMode.OFF)
        check(prevFromStartRepeatOff == 0) { "Expected clamp at 0 for RepeatMode.OFF, got $prevFromStartRepeatOff" }

        val prevFromStartRepeatAll = calculatePrevIndex(0, queue.size, RepeatMode.ALL)
        check(prevFromStartRepeatAll == 2) { "Expected wrap to last index 2 for RepeatMode.ALL, got $prevFromStartRepeatAll" }
        println("  [PASS] testPreviousNavigation")
    }

    fun testShuffleIntegrity() {
        val original = (1..50).map { createDummyTrack(it.toLong(), "Song $it") }
        val shuffled = original.shuffled()

        check(shuffled.size == original.size) { "Shuffled size must match original" }
        check(shuffled.map { it.id }.toSet() == original.map { it.id }.toSet()) {
            "All track IDs must be preserved after shuffle"
        }
        println("  [PASS] testShuffleIntegrity")
    }

    private fun calculateNextIndex(current: Int, size: Int, mode: RepeatMode): Int {
        if (size <= 0) return -1
        return when (mode) {
            RepeatMode.ONE -> current
            RepeatMode.ALL -> (current + 1) % size
            RepeatMode.OFF -> if (current + 1 < size) current + 1 else -1
        }
    }

    private fun calculatePrevIndex(current: Int, size: Int, mode: RepeatMode): Int {
        if (size <= 0) return -1
        return when (mode) {
            RepeatMode.ALL -> if (current - 1 >= 0) current - 1 else size - 1
            else -> if (current - 1 >= 0) current - 1 else 0
        }
    }
}

fun main() {
    println("--- Executing AudioQueueManagementTest ---")
    val test = AudioQueueManagementTest()
    test.testNextWithRepeatOff()
    test.testNextWithRepeatAll()
    test.testNextWithRepeatOne()
    test.testPreviousNavigation()
    test.testShuffleIntegrity()
    println("=== AudioQueueManagementTest: ALL TESTS PASSED ===")
}
