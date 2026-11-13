package com.tesfa.dualbeat.lyrics

import com.tesfa.dualbeat.core.util.LrcParser
import com.tesfa.dualbeat.domain.model.LyricsData
import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {

    @Test
    fun testStandardLrcParsing() {
        val lrc = """
            [ti:Test Track]
            [ar:Tesfa]
            [00:05.00]Intro music starts
            [00:12.50]First verse begins here
            [00:30.00]Chorus explodes with energy
            [01:15.20]Bridge section
        """.trimIndent()

        val parsed = LrcParser.parse(1L, lrc)
        assertTrue("Lyrics should be marked synchronized", parsed.isSynchronized)
        assertEquals("Expected 4 lines", 4, parsed.lines.size)
        assertEquals("Expected 5000ms", 5000L, parsed.lines[0].timestampMs)
        assertEquals("Intro music starts", parsed.lines[0].text)
        assertEquals("Expected 12500ms", 12500L, parsed.lines[1].timestampMs)
        assertEquals("Expected 30000ms", 30000L, parsed.lines[2].timestampMs)
        assertEquals("Expected 75200ms", 75200L, parsed.lines[3].timestampMs)
    }

    @Test
    fun testMultiTimestampSingleLine() {
        val lrc = """
            [00:10.00][00:40.00]Repeating chorus line
        """.trimIndent()

        val parsed = LrcParser.parse(2L, lrc)
        assertEquals("Expected 2 expanded lines", 2, parsed.lines.size)
        assertEquals(10000L, parsed.lines[0].timestampMs)
        assertEquals(40000L, parsed.lines[1].timestampMs)
        assertEquals("Repeating chorus line", parsed.lines[0].text)
        assertEquals("Repeating chorus line", parsed.lines[1].text)
    }

    @Test
    fun testActiveLineIndexCalculation() {
        val lrc = """
            [00:10.00]Line 1
            [00:20.00]Line 2
            [00:30.00]Line 3
        """.trimIndent()

        val parsed = LrcParser.parse(3L, lrc)
        assertEquals("At 0s, active line should be 0", 0, parsed.getActiveLineIndex(0L))
        assertEquals("At 15s, active line should be Line 1 (idx 0)", 0, parsed.getActiveLineIndex(15000L))
        assertEquals("At 20s, active line should be Line 2 (idx 1)", 1, parsed.getActiveLineIndex(20000L))
        assertEquals("At 25s, active line should be Line 2 (idx 1)", 1, parsed.getActiveLineIndex(25000L))
        assertEquals("At 35s, active line should be Line 3 (idx 2)", 2, parsed.getActiveLineIndex(35000L))
    }

    @Test
    fun testEmptyOrInvalidLrc() {
        val empty = LrcParser.parse(4L, "")
        assertFalse("Empty LRC should not be synchronized", empty.isSynchronized)
        assertTrue("Empty lines expected", empty.lines.isEmpty())

        val plainText = "Line one\nLine two without timestamps"
        val parsedPlain = LrcParser.parse(5L, plainText)
        assertFalse("Plain text should not be synchronized", parsedPlain.isSynchronized)
        assertEquals("Expected 2 plain text lines", 2, parsedPlain.lines.size)
    }
}
