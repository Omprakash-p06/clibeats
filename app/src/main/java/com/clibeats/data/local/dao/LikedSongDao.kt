// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.clibeats.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {
    @Query("INSERT OR REPLACE INTO liked_songs (song_id, liked_at) VALUES (:songId, :likedAt)")
    suspend fun likeSong(
        songId: String,
        likedAt: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM liked_songs WHERE song_id = :songId")
    suspend fun unlikeSong(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE song_id = :songId)")
    fun isLikedFlow(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE song_id = :songId)")
    suspend fun isLiked(songId: String): Boolean

    @Query("SELECT s.* FROM songs s INNER JOIN liked_songs l ON s.id = l.song_id ORDER BY l.liked_at DESC")
    fun getLikedSongsAsFlow(): Flow<List<SongEntity>>

    @Query("SELECT song_id FROM liked_songs")
    fun getLikedSongIdsAsFlow(): Flow<List<String>>
}
