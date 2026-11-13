package com.tesfa.dualbeat.data

data class TestSortableTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dataPath: String,
    val folderPath: String,
    val dateAdded: Long,
    val dateModified: Long
)

class MediaSortingAndSearchTest {

    private val track1 = TestSortableTrack(
        id = 1L,
        title = "Bravo Beat",
        artist = "Artist Beta",
        album = "Album Two",
        durationMs = 180000L,
        sizeBytes = 5000000L,
        dataPath = "/storage/emulated/0/Music/bravo.mp3",
        folderPath = "/storage/emulated/0/Music",
        dateAdded = 1000L,
        dateModified = 2000L
    )

    private val track2 = TestSortableTrack(
        id = 2L,
        title = "Alpha Rhythm",
        artist = "Artist Zeta",
        album = "Album One",
        durationMs = 240000L,
        sizeBytes = 8000000L,
        dataPath = "/storage/emulated/0/Music/alpha.mp3",
        folderPath = "/storage/emulated/0/Music",
        dateAdded = 2000L,
        dateModified = 1000L
    )

    private val track3 = TestSortableTrack(
        id = 3L,
        title = "Charlie Groove",
        artist = "Artist Alpha",
        album = "Album Three",
        durationMs = 120000L,
        sizeBytes = 3000000L,
        dataPath = "/storage/emulated/0/Download/charlie.mp3",
        folderPath = "/storage/emulated/0/Download",
        dateAdded = 3000L,
        dateModified = 3000L
    )

    private val list = listOf(track1, track2, track3)

    fun testSortByNameAscending() {
        val sorted = list.sortedBy { it.title.lowercase() }
        check(sorted[0].title == "Alpha Rhythm") { "Expected Alpha Rhythm" }
        check(sorted[1].title == "Bravo Beat") { "Expected Bravo Beat" }
        check(sorted[2].title == "Charlie Groove") { "Expected Charlie Groove" }
        println("  [PASS] testSortByNameAscending")
    }

    fun testSortByNameDescending() {
        val sorted = list.sortedBy { it.title.lowercase() }.reversed()
        check(sorted[0].title == "Charlie Groove") { "Expected Charlie Groove" }
        check(sorted[1].title == "Bravo Beat") { "Expected Bravo Beat" }
        check(sorted[2].title == "Alpha Rhythm") { "Expected Alpha Rhythm" }
        println("  [PASS] testSortByNameDescending")
    }

    fun testSortByDurationAscending() {
        val sorted = list.sortedBy { it.durationMs }
        check(sorted[0].durationMs == 120000L) { "Expected 120000L" }
        check(sorted[1].durationMs == 180000L) { "Expected 180000L" }
        check(sorted[2].durationMs == 240000L) { "Expected 240000L" }
        println("  [PASS] testSortByDurationAscending")
    }

    fun testSortBySizeDescending() {
        val sorted = list.sortedBy { it.sizeBytes }.reversed()
        check(sorted[0].sizeBytes == 8000000L) { "Expected 8000000L" }
        check(sorted[1].sizeBytes == 5000000L) { "Expected 5000000L" }
        check(sorted[2].sizeBytes == 3000000L) { "Expected 3000000L" }
        println("  [PASS] testSortBySizeDescending")
    }

    fun testSearchFiltering() {
        val query = "alpha"
        val filtered = list.filter {
            it.title.lowercase().contains(query) ||
            it.artist.lowercase().contains(query) ||
            it.album.lowercase().contains(query)
        }
        check(filtered.size == 2) { "Expected 2 matches for query 'alpha'" }
        check(filtered.map { it.id } == listOf(2L, 3L)) { "Expected tracks 2 and 3" }
        println("  [PASS] testSearchFiltering")
    }
}

fun main() {
    println("--- Executing MediaSortingAndSearchTest ---")
    val test = MediaSortingAndSearchTest()
    test.testSortByNameAscending()
    test.testSortByNameDescending()
    test.testSortByDurationAscending()
    test.testSortBySizeDescending()
    test.testSearchFiltering()
    println("=== MediaSortingAndSearchTest: ALL TESTS PASSED ===")
}
