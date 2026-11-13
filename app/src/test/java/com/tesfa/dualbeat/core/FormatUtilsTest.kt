package com.tesfa.dualbeat.core

import com.tesfa.dualbeat.core.util.FormatUtils
import org.junit.Assert.*
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatDurationMinutesAndSeconds() {
        assertEquals("00:00", FormatUtils.formatDuration(0))
        assertEquals("00:05", FormatUtils.formatDuration(5000))
        assertEquals("01:30", FormatUtils.formatDuration(90000))
        assertEquals("10:15", FormatUtils.formatDuration(615000))
    }

    @Test
    fun testFormatDurationHours() {
        assertEquals("1:00:00", FormatUtils.formatDuration(3600000))
        assertEquals("1:15:30", FormatUtils.formatDuration(4530000))
        assertEquals("2:05:08", FormatUtils.formatDuration(7508000))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FormatUtils.formatFileSize(0))
        assertEquals("512.0 B", FormatUtils.formatFileSize(512))
        assertEquals("1.0 KB", FormatUtils.formatFileSize(1024))
        assertEquals("1.5 MB", FormatUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.0 GB", FormatUtils.formatFileSize(2L * 1024 * 1024 * 1024))
    }
}
