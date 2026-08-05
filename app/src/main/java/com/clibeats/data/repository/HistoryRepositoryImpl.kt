package com.clibeats.data.repository

import com.clibeats.data.local.dao.HistoryDao
import com.clibeats.data.local.entity.HistoryEntity
import com.clibeats.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {
    override fun getRecentHistoryAsFlow(limit: Int): Flow<List<HistoryEntity>> =
        historyDao.getRecentAsFlow(limit)

    override suspend fun recordPlay(songId: String, providerId: String) =
        historyDao.insert(
            HistoryEntity(
                songId = songId,
                playedAt = System.currentTimeMillis(),
                providerId = providerId,
            )
        )

    override suspend fun clearHistoryBefore(epochMs: Long) = historyDao.clearBefore(epochMs)

    override suspend fun clearAllHistory() = historyDao.clearAll()
}
