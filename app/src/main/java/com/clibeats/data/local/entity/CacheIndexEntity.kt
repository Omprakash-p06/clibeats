package com.clibeats.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_index")
data class CacheIndexEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long?,
)
