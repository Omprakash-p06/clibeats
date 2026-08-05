package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clibeats.data.local.entity.CacheIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheIndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CacheIndexEntity)

    @Query("SELECT * FROM cache_index WHERE song_id = :songId")
    suspend fun getById(songId: String): CacheIndexEntity?

    @Query("SELECT * FROM cache_index ORDER BY cached_at DESC")
    fun getAllAsFlow(): Flow<List<CacheIndexEntity>>

    @Query("DELETE FROM cache_index WHERE song_id = :songId")
    suspend fun deleteById(songId: String)

    @Query("DELETE FROM cache_index WHERE cached_at < :beforeEpochMs")
    suspend fun deleteBefore(beforeEpochMs: Long)

    @Query("SELECT SUM(file_size_bytes) FROM cache_index")
    suspend fun getTotalCacheSizeBytes(): Long?
}
