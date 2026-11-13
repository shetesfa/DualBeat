package com.tesfa.dualbeat.data.repository

import com.tesfa.dualbeat.data.local.dao.PlaylistDao
import com.tesfa.dualbeat.data.local.entity.PlaylistEntity
import com.tesfa.dualbeat.data.local.entity.PlaylistItemEntity
import com.tesfa.dualbeat.domain.model.MediaType
import com.tesfa.dualbeat.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.updatePlaylistName(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    fun getItemsForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>> {
        return playlistDao.getItemsForPlaylist(playlistId)
    }

    suspend fun addItemToPlaylist(playlistId: Long, mediaId: Long, type: MediaType, orderIndex: Int) {
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                mediaId = mediaId,
                mediaType = type.name,
                orderIndex = orderIndex
            )
        )
    }

    suspend fun removeItemFromPlaylist(playlistId: Long, mediaId: Long, type: MediaType) {
        playlistDao.removePlaylistItem(playlistId, mediaId, type.name)
    }
}
