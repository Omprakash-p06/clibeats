// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clibeats.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY played_at DESC LIMIT :limit")
    fun getRecentAsFlow(limit: Int = 50): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY played_at DESC")
    fun getAllAsFlow(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history WHERE played_at < :beforeEpochMs")
    suspend fun clearBefore(beforeEpochMs: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
