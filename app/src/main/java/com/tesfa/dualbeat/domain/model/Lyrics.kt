package com.tesfa.dualbeat.domain.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsData(
    val trackId: Long,
    val isSynchronized: Boolean,
    val lines: List<LyricLine> = emptyList(),
    val rawPlainText: String = ""
) {
    /**
     * Finds the index of the currently active lyric line based on current playback position.
     * Returns -1 if playback position is before the first line.
     */
    fun getActiveLineIndex(currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        for (i in lines.indices.reversed()) {
            if (currentPositionMs >= lines[i].timestampMs) {
                return i
            }
        }
        return 0
    }
}
