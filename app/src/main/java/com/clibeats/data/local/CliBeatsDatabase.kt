package com.clibeats.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clibeats.data.local.dao.CacheIndexDao
import com.clibeats.data.local.dao.HistoryDao
import com.clibeats.data.local.dao.PlaylistDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.CacheIndexEntity
import com.clibeats.data.local.entity.HistoryEntity
import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.data.local.entity.PlaylistSongCrossRef
import com.clibeats.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        HistoryEntity::class,
        CacheIndexEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(CliBeatsTypeConverters::class)
abstract class CliBeatsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun cacheIndexDao(): CacheIndexDao
}
