// ForbiddenImport: HistoryRepository returns HistoryEntity (data-layer type) per Plan 03-03 spec.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.domain.repository

import com.clibeats.data.local.entity.HistoryEntity
import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getRecentHistoryAsFlow(limit: Int = 50): Flow<List<HistoryEntity>>

    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<Track>>

    suspend fun recordPlay(
        songId: String,
        providerId: String,
    )

    suspend fun clearHistoryBefore(epochMs: Long)

    suspend fun clearAllHistory()
}
