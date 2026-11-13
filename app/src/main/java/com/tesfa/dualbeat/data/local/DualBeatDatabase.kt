package com.tesfa.dualbeat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tesfa.dualbeat.data.local.dao.FavoriteDao
import com.tesfa.dualbeat.data.local.dao.PlaybackHistoryDao
import com.tesfa.dualbeat.data.local.dao.PlaylistDao
import com.tesfa.dualbeat.data.local.dao.ReceiverDao
import com.tesfa.dualbeat.data.local.entity.FavoriteEntity
import com.tesfa.dualbeat.data.local.entity.PlaybackHistoryEntity
import com.tesfa.dualbeat.data.local.entity.PlaylistEntity
import com.tesfa.dualbeat.data.local.entity.PlaylistItemEntity
import com.tesfa.dualbeat.data.local.entity.ReceiverNodeEntity

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        ReceiverNodeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DualBeatDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun receiverDao(): ReceiverDao

    companion object {
        @Volatile
        private var INSTANCE: DualBeatDatabase? = null

        fun getInstance(context: Context): DualBeatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DualBeatDatabase::class.java,
                    "dualbeat_database.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
