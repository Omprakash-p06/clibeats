// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clibeats.data.local.dao.CacheIndexDao
import com.clibeats.data.local.dao.HistoryDao
import com.clibeats.data.local.dao.LikedSongDao
import com.clibeats.data.local.dao.PlaylistDao
import com.clibeats.data.local.dao.QueueDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.CacheIndexEntity
import com.clibeats.data.local.entity.HistoryEntity
import com.clibeats.data.local.entity.LikedSongEntity
import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.data.local.entity.PlaylistSongCrossRef
import com.clibeats.data.local.entity.QueueEntity
import com.clibeats.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        HistoryEntity::class,
        CacheIndexEntity::class,
        QueueEntity::class,
        LikedSongEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(CliBeatsTypeConverters::class)
abstract class CliBeatsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun historyDao(): HistoryDao

    abstract fun cacheIndexDao(): CacheIndexDao

    abstract fun queueDao(): QueueDao

    abstract fun likedSongDao(): LikedSongDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `liked_songs` (
                            `song_id` TEXT NOT NULL,
                            `liked_at` INTEGER NOT NULL,
                            PRIMARY KEY(`song_id`),
                            FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
