package com.tesfa.dualbeat.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L
)

data class PlaylistItem(
    val playlistId: Long,
    val mediaId: Long,
    val mediaType: MediaType,
    val orderIndex: Int
)

data class PlaybackHistory(
    val id: Long = 0,
    val mediaId: Long,
    val mediaType: MediaType,
    val title: String,
    val subtitle: String,
    val playedAt: Long,
    val lastPositionMs: Long,
    val durationMs: Long
)
