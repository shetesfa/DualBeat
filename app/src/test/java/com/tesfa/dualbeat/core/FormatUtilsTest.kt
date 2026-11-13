package com.tesfa.dualbeat.core

import com.tesfa.dualbeat.core.util.FormatUtils

class FormatUtilsTest {

    fun testFormatDurationMinutesAndSeconds() {
        check(FormatUtils.formatDuration(0) == "00:00") { "Expected 00:00" }
        check(FormatUtils.formatDuration(5000) == "00:05") { "Expected 00:05" }
        check(FormatUtils.formatDuration(90000) == "01:30") { "Expected 01:30" }
        check(FormatUtils.formatDuration(615000) == "10:15") { "Expected 10:15" }
        println("  [PASS] testFormatDurationMinutesAndSeconds")
    }

    fun testFormatDurationHours() {
        check(FormatUtils.formatDuration(3600000) == "1:00:00") { "Expected 1:00:00" }
        check(FormatUtils.formatDuration(4530000) == "1:15:30") { "Expected 1:15:30" }
        check(FormatUtils.formatDuration(7508000) == "2:05:08") { "Expected 2:05:08" }
        println("  [PASS] testFormatDurationHours")
    }

    fun testFormatFileSize() {
        check(FormatUtils.formatFileSize(0) == "0 B") { "Expected 0 B" }
        check(FormatUtils.formatFileSize(512) == "512.0 B") { "Expected 512.0 B" }
        check(FormatUtils.formatFileSize(1024) == "1.0 KB") { "Expected 1.0 KB" }
        check(FormatUtils.formatFileSize((1.5 * 1024 * 1024).toLong()) == "1.5 MB") { "Expected 1.5 MB" }
        check(FormatUtils.formatFileSize(2L * 1024 * 1024 * 1024) == "2.0 GB") { "Expected 2.0 GB" }
        println("  [PASS] testFormatFileSize")
    }
}

fun main() {
    println("--- Executing FormatUtilsTest ---")
    val test = FormatUtilsTest()
    test.testFormatDurationMinutesAndSeconds()
    test.testFormatDurationHours()
    test.testFormatFileSize()
    println("=== FormatUtilsTest: ALL TESTS PASSED ===")
}
