package com.clibeats.domain.repository

import com.clibeats.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getRecentHistoryAsFlow(limit: Int = 50): Flow<List<HistoryEntity>>

    suspend fun recordPlay(
        songId: String,
        providerId: String,
    )

    suspend fun clearHistoryBefore(epochMs: Long)

    suspend fun clearAllHistory()
}
