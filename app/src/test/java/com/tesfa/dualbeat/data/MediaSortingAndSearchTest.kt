package com.tesfa.dualbeat.data

import android.net.Uri
import com.tesfa.dualbeat.domain.model.AudioTrack
import com.tesfa.dualbeat.domain.model.SortDirection
import com.tesfa.dualbeat.domain.model.SortField
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSortingAndSearchTest {

    private val track1 = AudioTrack(
        id = 1L,
        title = "Bravo Beat",
        artist = "Artist Beta",
        album = "Album Two",
        durationMs = 180000L,
        sizeBytes = 5000000L,
        contentUri = Uri.EMPTY,
        dataPath = "/storage/emulated/0/Music/bravo.mp3",
        folderPath = "/storage/emulated/0/Music",
        dateAdded = 1000L,
        dateModified = 2000L
    )

    private val track2 = AudioTrack(
        id = 2L,
        title = "Alpha Rhythm",
        artist = "Artist Zeta",
        album = "Album One",
        durationMs = 240000L,
        sizeBytes = 8000000L,
        contentUri = Uri.EMPTY,
        dataPath = "/storage/emulated/0/Music/alpha.mp3",
        folderPath = "/storage/emulated/0/Music",
        dateAdded = 2000L,
        dateModified = 1000L
    )

    private val track3 = AudioTrack(
        id = 3L,
        title = "Charlie Groove",
        artist = "Artist Alpha",
        album = "Album Three",
        durationMs = 120000L,
        sizeBytes = 3000000L,
        contentUri = Uri.EMPTY,
        dataPath = "/storage/emulated/0/Download/charlie.mp3",
        folderPath = "/storage/emulated/0/Download",
        dateAdded = 3000L,
        dateModified = 3000L
    )

    private val list = listOf(track1, track2, track3)

    @Test
    fun testSortByNameAscending() {
        val sorted = list.sortedBy { it.title.lowercase() }
        assertEquals("Alpha Rhythm", sorted[0].title)
        assertEquals("Bravo Beat", sorted[1].title)
        assertEquals("Charlie Groove", sorted[2].title)
    }

    @Test
    fun testSortByNameDescending() {
        val sorted = list.sortedBy { it.title.lowercase() }.reversed()
        assertEquals("Charlie Groove", sorted[0].title)
        assertEquals("Bravo Beat", sorted[1].title)
        assertEquals("Alpha Rhythm", sorted[2].title)
    }

    @Test
    fun testSortByDurationAscending() {
        val sorted = list.sortedBy { it.durationMs }
        assertEquals(120000L, sorted[0].durationMs)
        assertEquals(180000L, sorted[1].durationMs)
        assertEquals(240000L, sorted[2].durationMs)
    }

    @Test
    fun testSortBySizeDescending() {
        val sorted = list.sortedBy { it.sizeBytes }.reversed()
        assertEquals(8000000L, sorted[0].sizeBytes)
        assertEquals(5000000L, sorted[1].sizeBytes)
        assertEquals(3000000L, sorted[2].sizeBytes)
    }

    @Test
    fun testSearchFiltering() {
        val query = "alpha"
        val filtered = list.filter {
            it.title.lowercase().contains(query) ||
            it.artist.lowercase().contains(query) ||
            it.album.lowercase().contains(query)
        }
        // Should match track2 (title has Alpha) and track3 (artist has Alpha)
        assertEquals(2, filtered.size)
        assertEquals(listOf(2L, 3L), filtered.map { it.id })
    }
}
