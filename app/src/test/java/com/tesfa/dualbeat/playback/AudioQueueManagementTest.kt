package com.tesfa.dualbeat.playback

import org.junit.Assert.*
import org.junit.Test

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

    @Test
    fun testNextWithRepeatOff() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2"),
            createDummyTrack(3, "Track 3")
        )

        var currentIndex = 1
        val nextIdx = calculateNextIndex(currentIndex, queue.size, RepeatMode.OFF)
        assertEquals("Expected index 2", 2, nextIdx)

        currentIndex = 2 // at end of queue
        val atEndIdx = calculateNextIndex(currentIndex, queue.size, RepeatMode.OFF)
        assertEquals("Expected -1 (stop) at end with RepeatMode.OFF", -1, atEndIdx)
    }

    @Test
    fun testNextWithRepeatAll() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2"),
            createDummyTrack(3, "Track 3")
        )

        val atEndIdx = calculateNextIndex(2, queue.size, RepeatMode.ALL)
        assertEquals("Expected wrap-around to index 0 with RepeatMode.ALL", 0, atEndIdx)
    }

    @Test
    fun testNextWithRepeatOne() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2")
        )

        val idx = calculateNextIndex(1, queue.size, RepeatMode.ONE)
        assertEquals("Expected same index 1 with RepeatMode.ONE", 1, idx)
    }

    @Test
    fun testPreviousNavigation() {
        val queue = listOf(
            createDummyTrack(1, "Track 1"),
            createDummyTrack(2, "Track 2"),
            createDummyTrack(3, "Track 3")
        )

        val prevFromMiddle = calculatePrevIndex(1, queue.size, RepeatMode.OFF)
        assertEquals("Expected 0 from 1", 0, prevFromMiddle)

        val prevFromStartRepeatOff = calculatePrevIndex(0, queue.size, RepeatMode.OFF)
        assertEquals("Expected clamp at 0 for RepeatMode.OFF", 0, prevFromStartRepeatOff)

        val prevFromStartRepeatAll = calculatePrevIndex(0, queue.size, RepeatMode.ALL)
        assertEquals("Expected wrap to last index 2 for RepeatMode.ALL", 2, prevFromStartRepeatAll)
    }

    @Test
    fun testShuffleIntegrity() {
        val original = (1..50).map { createDummyTrack(it.toLong(), "Song $it") }
        val shuffled = original.shuffled()

        assertEquals("Shuffled size must match original", original.size, shuffled.size)
        assertEquals(
            "All track IDs must be preserved after shuffle",
            original.map { it.id }.toSet(),
            shuffled.map { it.id }.toSet()
        )
    }

    @Test
    fun testEmptyQueueBehavior() {
        assertEquals(-1, calculateNextIndex(0, 0, RepeatMode.OFF))
        assertEquals(-1, calculateNextIndex(0, 0, RepeatMode.ALL))
        assertEquals(-1, calculatePrevIndex(0, 0, RepeatMode.OFF))
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
