package com.clibeats.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "liked_songs",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LikedSongEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "liked_at") val likedAt: Long = System.currentTimeMillis(),
)
