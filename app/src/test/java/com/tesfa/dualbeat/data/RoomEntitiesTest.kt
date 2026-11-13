package com.tesfa.dualbeat.data

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

        check(fav1 == fav2) { "Identical FavoriteEntities must be equal" }
        check(fav1 != favVideo) { "Audio and Video favorites must not be equal" }
        check(fav1.mediaId == 101L) { "mediaId mismatch" }
        check(fav1.mediaType == "AUDIO") { "mediaType mismatch" }
        println("  [PASS] testFavoriteEntityCreationAndEquality")
    }

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
        check(progress == 0.5f) { "Expected 50% progress, got $progress" }

        val completedHistory = history.copy(
            lastPositionMs = 180000L
        )
        check(completedHistory.lastPositionMs == history.durationMs) { "History should be at end" }
        check(completedHistory.lastPositionMs == 180000L) { "Updated position mismatch" }
        println("  [PASS] testPlaybackHistoryProgressCalculation")
    }

    fun testPlaylistItemMapping() {
        val playlist = TestPlaylistEntity(id = 10L, name = "Workout Beats", createdAt = 1700000000000L)
        val item1 = TestPlaylistItemEntity(playlistId = playlist.id, mediaId = 101L, mediaType = "AUDIO", orderIndex = 0)
        val item2 = TestPlaylistItemEntity(playlistId = playlist.id, mediaId = 102L, mediaType = "AUDIO", orderIndex = 1)

        check(item1.playlistId == 10L) { "playlistId must match" }
        check(item1.orderIndex == 0) { "Order must be 0" }
        check(item2.orderIndex == 1) { "Order must be 1" }
        println("  [PASS] testPlaylistItemMapping")
    }

    fun testMediaTypeEnumConversion() {
        check(TestMediaType.valueOf("AUDIO") == TestMediaType.AUDIO) { "AUDIO enum mapping failed" }
        check(TestMediaType.valueOf("VIDEO") == TestMediaType.VIDEO) { "VIDEO enum mapping failed" }
        println("  [PASS] testMediaTypeEnumConversion")
    }
}

fun main() {
    println("--- Executing RoomEntitiesTest ---")
    val test = RoomEntitiesTest()
    test.testFavoriteEntityCreationAndEquality()
    test.testPlaybackHistoryProgressCalculation()
    test.testPlaylistItemMapping()
    test.testMediaTypeEnumConversion()
    println("=== RoomEntitiesTest: ALL TESTS PASSED ===")
}
