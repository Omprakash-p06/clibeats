// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.HistoryDao
import com.clibeats.data.local.entity.HistoryEntity
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Indentation: detekt 1.23.6 misparses ktlint_official @Inject constructor() style (false positive).
@Suppress("Indentation")
@Singleton
class HistoryRepositoryImpl
    @Inject
    constructor(
        private val historyDao: HistoryDao,
    ) : HistoryRepository {
        override fun getRecentHistoryAsFlow(limit: Int): Flow<List<HistoryEntity>> = historyDao.getRecentAsFlow(limit)

        override fun getRecentlyPlayedTracks(limit: Int): Flow<List<Track>> =
            historyDao.getRecentlyPlayedTracksAsFlow(limit).map { entities ->
                entities.map { it.toDomainTrack() }
            }

        override suspend fun recordPlay(
            songId: String,
            providerId: String,
        ) = historyDao.insert(
            HistoryEntity(
                songId = songId,
                playedAt = System.currentTimeMillis(),
                providerId = providerId,
            ),
        )

        override suspend fun clearHistoryBefore(epochMs: Long) = historyDao.clearBefore(epochMs)

        override suspend fun clearAllHistory() = historyDao.clearAll()
    }

private fun SongEntity.toDomainTrack(): Track =
    Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        providerId = providerId,
    )
