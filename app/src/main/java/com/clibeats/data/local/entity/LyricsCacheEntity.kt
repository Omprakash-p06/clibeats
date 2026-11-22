package com.clibeats.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "synced_lrc") val syncedLrc: String?,
    @ColumnInfo(name = "plain_lyrics") val plainLyrics: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)
