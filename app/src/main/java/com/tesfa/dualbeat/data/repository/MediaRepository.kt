package com.tesfa.dualbeat.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.tesfa.dualbeat.data.local.dao.FavoriteDao
import com.tesfa.dualbeat.data.local.dao.PlaybackHistoryDao
import com.tesfa.dualbeat.data.local.entity.FavoriteEntity
import com.tesfa.dualbeat.data.local.entity.PlaybackHistoryEntity
import com.tesfa.dualbeat.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao,
    private val historyDao: PlaybackHistoryDao
) {

    private val _audioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val folders: StateFlow<List<MediaFolder>> = _folders.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val audioList = scanAudioInternal()
            val videoList = scanVideoInternal()
            _audioTracks.value = audioList
            _videos.value = videoList
            _folders.value = computeFolders(audioList, videoList)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isScanning.value = false
        }
    }

    private fun scanAudioInternal(): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol) ?: "<unknown>"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val dataPath = cursor.getString(dataCol) ?: ""
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                val dateMod = cursor.getLong(dateModCol) * 1000L
                val albumId = cursor.getLong(albumIdCol)

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val folderPath = if (dataPath.isNotEmpty()) {
                    File(dataPath).parent ?: ""
                } else ""

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                tracks.add(
                    AudioTrack(
                        id = id,
                        title = title,
                        artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                        album = album,
                        durationMs = duration,
                        sizeBytes = size,
                        contentUri = contentUri,
                        dataPath = dataPath,
                        folderPath = folderPath,
                        dateAdded = dateAdded,
                        dateModified = dateMod,
                        albumId = albumId,
                        albumArtUri = albumArtUri
                    )
                )
            }
        }
        return tracks
    }

    private fun scanVideoInternal(): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Untitled Video"
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val dataPath = cursor.getString(dataCol) ?: ""
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                val dateMod = cursor.getLong(dateModCol) * 1000L
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)

                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val folderPath = if (dataPath.isNotEmpty()) {
                    File(dataPath).parent ?: ""
                } else ""

                videos.add(
                    VideoItem(
                        id = id,
                        title = title,
                        durationMs = duration,
                        sizeBytes = size,
                        contentUri = contentUri,
                        dataPath = dataPath,
                        folderPath = folderPath,
                        dateAdded = dateAdded,
                        dateModified = dateMod,
                        resolutionWidth = width,
                        resolutionHeight = height
                    )
                )
            }
        }
        return videos
    }

    private fun computeFolders(audioList: List<AudioTrack>, videoList: List<VideoItem>): List<MediaFolder> {
        val folderMap = mutableMapOf<String, MutableList<Pair<Long, Long>>>() // path -> list of (size, lastModified)
        val audioCounts = mutableMapOf<String, Int>()
        val videoCounts = mutableMapOf<String, Int>()

        for (track in audioList) {
            if (track.folderPath.isNotEmpty()) {
                folderMap.getOrPut(track.folderPath) { mutableListOf() }
                    .add(Pair(track.sizeBytes, track.dateModified))
                audioCounts[track.folderPath] = (audioCounts[track.folderPath] ?: 0) + 1
            }
        }

        for (video in videoList) {
            if (video.folderPath.isNotEmpty()) {
                folderMap.getOrPut(video.folderPath) { mutableListOf() }
                    .add(Pair(video.sizeBytes, video.dateModified))
                videoCounts[video.folderPath] = (videoCounts[video.folderPath] ?: 0) + 1
            }
        }

        return folderMap.map { (path, items) ->
            val folderFile = File(path)
            val folderName = folderFile.name.ifEmpty { path }
            val totalSize = items.sumOf { it.first }
            val latestMod = items.maxOfOrNull { it.second } ?: 0L

            MediaFolder(
                path = path,
                name = folderName,
                audioCount = audioCounts[path] ?: 0,
                videoCount = videoCounts[path] ?: 0,
                totalSizeBytes = totalSize,
                lastModified = latestMod
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun sortAudioTracks(tracks: List<AudioTrack>, field: SortField, direction: SortDirection): List<AudioTrack> {
        val sorted = when (field) {
            SortField.NAME -> tracks.sortedBy { it.title.lowercase() }
            SortField.DATE_ADDED -> tracks.sortedBy { it.dateAdded }
            SortField.DATE_MODIFIED -> tracks.sortedBy { it.dateModified }
            SortField.SIZE -> tracks.sortedBy { it.sizeBytes }
            SortField.DURATION -> tracks.sortedBy { it.durationMs }
            SortField.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
            SortField.ALBUM -> tracks.sortedBy { it.album.lowercase() }
        }
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    fun sortVideos(videos: List<VideoItem>, field: SortField, direction: SortDirection): List<VideoItem> {
        val sorted = when (field) {
            SortField.NAME -> videos.sortedBy { it.title.lowercase() }
            SortField.DATE_ADDED -> videos.sortedBy { it.dateAdded }
            SortField.DATE_MODIFIED -> videos.sortedBy { it.dateModified }
            SortField.SIZE -> videos.sortedBy { it.sizeBytes }
            SortField.DURATION -> videos.sortedBy { it.durationMs }
            else -> videos.sortedBy { it.title.lowercase() }
        }
        return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    fun searchAudio(query: String): List<AudioTrack> {
        if (query.isBlank()) return _audioTracks.value
        val q = query.trim().lowercase()
        return _audioTracks.value.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.folderPath.lowercase().contains(q)
        }
    }

    fun searchVideos(query: String): List<VideoItem> {
        if (query.isBlank()) return _videos.value
        val q = query.trim().lowercase()
        return _videos.value.filter {
            it.title.lowercase().contains(q) ||
            it.folderPath.lowercase().contains(q)
        }
    }

    // Favorites
    fun isFavorite(mediaId: Long, type: MediaType): Flow<Boolean> =
        favoriteDao.isFavorite(mediaId, type.name)

    suspend fun toggleFavorite(mediaId: Long, type: MediaType, currentFav: Boolean) {
        if (currentFav) {
            favoriteDao.removeFavorite(mediaId, type.name)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(mediaId = mediaId, mediaType = type.name))
        }
    }

    // Playback History
    fun getRecentHistory(limit: Int = 30): Flow<List<PlaybackHistoryEntity>> =
        historyDao.getRecentHistory(limit)

    suspend fun recordHistory(
        mediaId: Long,
        type: MediaType,
        title: String,
        subtitle: String,
        positionMs: Long,
        durationMs: Long
    ) {
        historyDao.recordPlayback(
            PlaybackHistoryEntity(
                mediaId = mediaId,
                mediaType = type.name,
                title = title,
                subtitle = subtitle,
                playedAt = System.currentTimeMillis(),
                lastPositionMs = positionMs,
                durationMs = durationMs
            )
        )
    }

    suspend fun getLastPosition(mediaId: Long, type: MediaType): Long =
        historyDao.getLastPosition(mediaId, type.name) ?: 0L
}
