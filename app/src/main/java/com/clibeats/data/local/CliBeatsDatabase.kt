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
import com.clibeats.data.local.dao.SavedAlbumDao
import com.clibeats.data.local.dao.SavedArtistDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.CacheIndexEntity
import com.clibeats.data.local.entity.HistoryEntity
import com.clibeats.data.local.entity.LikedSongEntity
import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.data.local.entity.PlaylistSongCrossRef
import com.clibeats.data.local.entity.QueueEntity
import com.clibeats.data.local.entity.SavedAlbumEntity
import com.clibeats.data.local.entity.SavedArtistEntity
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
        SavedAlbumEntity::class,
        SavedArtistEntity::class,
    ],
    version = 3,
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

    abstract fun savedAlbumDao(): SavedAlbumDao

    abstract fun savedArtistDao(): SavedArtistDao

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

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_albums` (
                            `id` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `artist` TEXT NOT NULL,
                            `year` INTEGER,
                            `artwork_url` TEXT,
                            `track_count` INTEGER NOT NULL,
                            `provider_id` TEXT NOT NULL,
                            `saved_at` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_artists` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `artwork_url` TEXT,
                            `provider_id` TEXT NOT NULL,
                            `saved_at` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
