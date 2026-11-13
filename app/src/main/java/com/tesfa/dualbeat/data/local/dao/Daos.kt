package com.tesfa.dualbeat.data.local.dao

import androidx.room.*
import com.tesfa.dualbeat.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getItemsForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId AND mediaType = :mediaType")
    suspend fun removePlaylistItem(playlistId: Long, mediaId: Long, mediaType: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId AND mediaType = :mediaType)")
    fun isFavorite(mediaId: Long, mediaType: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId AND mediaType = :mediaType")
    suspend fun removeFavorite(mediaId: Long, mediaType: String)
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT lastPositionMs FROM playback_history WHERE mediaId = :mediaId AND mediaType = :mediaType ORDER BY playedAt DESC LIMIT 1")
    suspend fun getLastPosition(mediaId: Long, mediaType: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlayback(history: PlaybackHistoryEntity): Long

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Dao
interface ReceiverDao {
    @Query("SELECT * FROM receiver_nodes ORDER BY lastSeenTimestamp DESC")
    fun getAllSavedReceivers(): Flow<List<ReceiverNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReceiver(node: ReceiverNodeEntity)

    @Query("DELETE FROM receiver_nodes WHERE id = :id")
    suspend fun deleteReceiver(id: String)
}
