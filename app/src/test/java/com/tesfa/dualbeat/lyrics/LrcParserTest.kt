package com.tesfa.dualbeat.lyrics

import com.tesfa.dualbeat.core.util.LrcParser
import com.tesfa.dualbeat.domain.model.LyricsData

class LrcParserTest {

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
        check(parsed.isSynchronized) { "Lyrics should be marked synchronized" }
        check(parsed.lines.size == 4) { "Expected 4 lines, got ${parsed.lines.size}" }
        check(parsed.lines[0].timestampMs == 5000L) { "Expected 5000ms, got ${parsed.lines[0].timestampMs}" }
        check(parsed.lines[0].text == "Intro music starts") { "Text mismatch" }
        check(parsed.lines[1].timestampMs == 12500L) { "Expected 12500ms, got ${parsed.lines[1].timestampMs}" }
        check(parsed.lines[2].timestampMs == 30000L) { "Expected 30000ms, got ${parsed.lines[2].timestampMs}" }
        check(parsed.lines[3].timestampMs == 75200L) { "Expected 75200ms, got ${parsed.lines[3].timestampMs}" }
        println("  [PASS] testStandardLrcParsing")
    }

    fun testMultiTimestampSingleLine() {
        val lrc = """
            [00:10.00][00:40.00]Repeating chorus line
        """.trimIndent()

        val parsed = LrcParser.parse(2L, lrc)
        check(parsed.lines.size == 2) { "Expected 2 expanded lines" }
        check(parsed.lines[0].timestampMs == 10000L) { "Line 0 timestamp mismatch" }
        check(parsed.lines[1].timestampMs == 40000L) { "Line 1 timestamp mismatch" }
        check(parsed.lines[0].text == "Repeating chorus line") { "Text mismatch" }
        check(parsed.lines[1].text == "Repeating chorus line") { "Text mismatch" }
        println("  [PASS] testMultiTimestampSingleLine")
    }

    fun testActiveLineIndexCalculation() {
        val lrc = """
            [00:10.00]Line 1
            [00:20.00]Line 2
            [00:30.00]Line 3
        """.trimIndent()

        val parsed = LrcParser.parse(3L, lrc)
        check(parsed.getActiveLineIndex(0L) == 0) { "At 0s, active line should be 0" }
        check(parsed.getActiveLineIndex(15000L) == 0) { "At 15s, active line should be Line 1 (idx 0)" }
        check(parsed.getActiveLineIndex(20000L) == 1) { "At 20s, active line should be Line 2 (idx 1)" }
        check(parsed.getActiveLineIndex(25000L) == 1) { "At 25s, active line should be Line 2 (idx 1)" }
        check(parsed.getActiveLineIndex(35000L) == 2) { "At 35s, active line should be Line 3 (idx 2)" }
        println("  [PASS] testActiveLineIndexCalculation")
    }

    fun testEmptyOrInvalidLrc() {
        val empty = LrcParser.parse(4L, "")
        check(!empty.isSynchronized) { "Empty LRC should not be synchronized" }
        check(empty.lines.isEmpty()) { "Empty lines expected" }

        val plainText = "Line one\nLine two without timestamps"
        val parsedPlain = LrcParser.parse(5L, plainText)
        check(!parsedPlain.isSynchronized) { "Plain text should not be synchronized" }
        check(parsedPlain.lines.size == 2) { "Expected 2 plain text lines" }
        println("  [PASS] testEmptyOrInvalidLrc")
    }
}

fun main() {
    println("--- Executing LrcParserTest ---")
    val test = LrcParserTest()
    test.testStandardLrcParsing()
    test.testMultiTimestampSingleLine()
    test.testActiveLineIndexCalculation()
    test.testEmptyOrInvalidLrc()
    println("=== LrcParserTest: ALL TESTS PASSED ===")
}
