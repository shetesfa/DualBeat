package com.tesfa.dualbeat.data

import org.junit.Assert.*
import org.junit.Test

enum class TestMediaType {
    AUDIO,
    VIDEO
}

data class TestFavoriteEntity(
    val id: Long = 0,
    val mediaId: Long,
    val mediaType: String,
    val addedAt: Long = System.currentTimeMillis()
)

data class TestPlaybackHistoryEntity(
    val id: Long = 0,
    val mediaId: Long,
    val mediaType: String,
    val title: String,
    val subtitle: String,
    val playedAt: Long,
    val lastPositionMs: Long,
    val durationMs: Long
)

data class TestPlaylistEntity(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class TestPlaylistItemEntity(
    val id: Long = 0,
    val playlistId: Long,
    val mediaId: Long,
    val mediaType: String,
    val orderIndex: Int
)

class RoomEntitiesTest {

    @Test
    fun testFavoriteEntityCreationAndEquality() {
        val fav1 = TestFavoriteEntity(
            mediaId = 101L,
            mediaType = TestMediaType.AUDIO.name,
            addedAt = 1700000000000L
        )
        val fav2 = TestFavoriteEntity(
            mediaId = 101L,
            mediaType = TestMediaType.AUDIO.name,
            addedAt = 1700000000000L
        )
        val favVideo = TestFavoriteEntity(
            mediaId = 101L,
            mediaType = TestMediaType.VIDEO.name,
            addedAt = 1700000000000L
        )

        assertEquals("Identical FavoriteEntities must be equal", fav1, fav2)
        assertNotEquals("Audio and Video favorites must not be equal", fav1, favVideo)
        assertEquals("mediaId mismatch", 101L, fav1.mediaId)
        assertEquals("mediaType mismatch", "AUDIO", fav1.mediaType)
    }

    @Test
    fun testPlaybackHistoryProgressCalculation() {
        val history = TestPlaybackHistoryEntity(
            id = 1L,
            mediaId = 505L,
            mediaType = TestMediaType.AUDIO.name,
            title = "Acoustic Horizon",
            subtitle = "Tesfa Originals",
            lastPositionMs = 90000L,
            durationMs = 180000L,
            playedAt = 1700000050000L
        )

        val progress = history.lastPositionMs.toFloat() / history.durationMs.toFloat()
        assertEquals("Expected 50% progress", 0.5f, progress, 0.001f)

        val completedHistory = history.copy(
            lastPositionMs = 180000L
        )
        assertEquals("History should be at end", history.durationMs, completedHistory.lastPositionMs)
        assertEquals("Updated position mismatch", 180000L, completedHistory.lastPositionMs)
    }

    @Test
    fun testPlaylistItemMapping() {
        val playlist = TestPlaylistEntity(id = 10L, name = "Workout Beats", createdAt = 1700000000000L)
        val item1 = TestPlaylistItemEntity(playlistId = playlist.id, mediaId = 101L, mediaType = "AUDIO", orderIndex = 0)
        val item2 = TestPlaylistItemEntity(playlistId = playlist.id, mediaId = 102L, mediaType = "AUDIO", orderIndex = 1)

        assertEquals("playlistId must match", 10L, item1.playlistId)
        assertEquals("Order must be 0", 0, item1.orderIndex)
        assertEquals("Order must be 1", 1, item2.orderIndex)
    }

    @Test
    fun testMediaTypeEnumConversion() {
        assertEquals("AUDIO enum mapping failed", TestMediaType.AUDIO, TestMediaType.valueOf("AUDIO"))
        assertEquals("VIDEO enum mapping failed", TestMediaType.VIDEO, TestMediaType.valueOf("VIDEO"))
    }
}
