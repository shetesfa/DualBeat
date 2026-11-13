package com.tesfa.dualbeat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playlistId"),
        Index(value = ["playlistId", "mediaId", "mediaType"], unique = true)
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val mediaId: Long,
    val mediaType: String, // "AUDIO" or "VIDEO"
    val orderIndex: Int
)

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["mediaId", "mediaType"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val mediaType: String, // "AUDIO" or "VIDEO"
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playback_history",
    indices = [Index("mediaId"), Index("playedAt")]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val mediaType: String,
    val title: String,
    val subtitle: String,
    val playedAt: Long,
    val lastPositionMs: Long,
    val durationMs: Long
)

@Entity(tableName = "receiver_nodes")
data class ReceiverNodeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val volumePercent: Int,
    val groupName: String?,
    val lastSeenTimestamp: Long
)
