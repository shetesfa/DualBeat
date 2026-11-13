package com.tesfa.dualbeat.domain.model

import android.net.Uri

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class SortField {
    NAME,
    DATE_ADDED,
    DATE_MODIFIED,
    SIZE,
    DURATION,
    ARTIST,
    ALBUM
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val contentUri: Uri,
    val dataPath: String,
    val folderPath: String,
    val dateAdded: Long,
    val dateModified: Long,
    val albumId: Long = 0,
    val albumArtUri: Uri? = null,
    val isFavorite: Boolean = false
)

data class VideoItem(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val contentUri: Uri,
    val dataPath: String,
    val folderPath: String,
    val dateAdded: Long,
    val dateModified: Long,
    val resolutionWidth: Int = 0,
    val resolutionHeight: Int = 0,
    val lastPositionMs: Long = 0,
    val isFavorite: Boolean = false
)

data class MediaFolder(
    val path: String,
    val name: String,
    val audioCount: Int,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val lastModified: Long
)
