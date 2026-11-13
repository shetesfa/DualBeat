package com.tesfa.dualbeat.core.util

import com.tesfa.dualbeat.domain.model.LyricLine
import com.tesfa.dualbeat.domain.model.LyricsData
import java.util.regex.Pattern

object LrcParser {

    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    private val OFFSET_PATTERN = Pattern.compile("\\[offset:([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE)

    /**
     * Parses LRC string content into structured LyricsData with sorted LyricLines.
     */
    fun parse(trackId: Long, lrcContent: String): LyricsData {
        if (lrcContent.isBlank()) {
            return LyricsData(trackId = trackId, isSynchronized = false)
        }

        var globalOffsetMs = 0L
        val lines = lrcContent.lines()
        val parsedLines = mutableListOf<LyricLine>()

        // Check for [offset:+/-ms] tag
        for (line in lines) {
            val offsetMatcher = OFFSET_PATTERN.matcher(line)
            if (offsetMatcher.find()) {
                globalOffsetMs = offsetMatcher.group(1)?.toLongOrNull() ?: 0L
                break
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val matcher = TIME_TAG_PATTERN.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val fracStr = matcher.group(3) ?: "0"

                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toLongOrNull() ?: 0L
                val millis = when (fracStr.length) {
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }

                val totalMs = (minutes * 60 * 1000L) + (seconds * 1000L) + millis + globalOffsetMs
                timestamps.add(totalMs.coerceAtLeast(0L))
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = trimmed.substring(lastMatchEnd).trim()
                for (ts in timestamps) {
                    parsedLines.add(LyricLine(timestampMs = ts, text = text))
                }
            }
        }

        val sorted = parsedLines.sortedBy { it.timestampMs }

        return if (sorted.isNotEmpty()) {
            LyricsData(
                trackId = trackId,
                isSynchronized = true,
                lines = sorted,
                rawPlainText = lrcContent
            )
        } else {
            // Unsynchronized plain text lyrics fallback
            val plainLines = lines.filter { it.isNotBlank() && !it.startsWith("[") }
            LyricsData(
                trackId = trackId,
                isSynchronized = false,
                lines = plainLines.mapIndexed { idx, txt -> LyricLine(idx * 4000L, txt.trim()) },
                rawPlainText = lrcContent
            )
        }
    }
}
