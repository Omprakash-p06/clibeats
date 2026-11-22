// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clibeats.data.local.entity.DownloadedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {
    @Query("SELECT * FROM downloaded_tracks WHERE song_id = :songId")
    suspend fun getById(songId: String): DownloadedTrackEntity?

    @Query("SELECT * FROM downloaded_tracks WHERE song_id = :songId")
    fun getByIdFlow(songId: String): Flow<DownloadedTrackEntity?>

    @Query("SELECT * FROM downloaded_tracks")
    suspend fun getAll(): List<DownloadedTrackEntity>

    @Query("SELECT * FROM downloaded_tracks")
    fun getAllFlow(): Flow<List<DownloadedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE song_id = :songId")
    suspend fun deleteById(songId: String)
}
