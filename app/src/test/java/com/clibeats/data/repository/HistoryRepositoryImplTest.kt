@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.HistoryDao
import com.clibeats.data.local.entity.SongEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryImplTest {
    private lateinit var historyDao: HistoryDao
    private lateinit var repository: HistoryRepositoryImpl

    @Before
    fun setUp() {
        historyDao = mock()
        repository = HistoryRepositoryImpl(historyDao)
    }

    @Test
    fun `getRecentlyPlayedTracks maps entities to domain tracks`() =
        runTest {
            val entity =
                SongEntity(
                    id = "h1",
                    title = "History Track",
                    artist = "Artist",
                    album = "Album",
                    durationMs = 210000L,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "ytmusic",
                )
            whenever(historyDao.getRecentlyPlayedTracksAsFlow(50)).thenReturn(flowOf(listOf(entity)))

            val result = repository.getRecentlyPlayedTracks(50).first()
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("History Track")
        }

    @Test
    fun `recordPlay inserts HistoryEntity into historyDao`() =
        runTest {
            repository.recordPlay("s1", "ytmusic")
            verify(historyDao).insert(any())
        }

    @Test
    fun `clearAllHistory delegates to historyDao`() =
        runTest {
            repository.clearAllHistory()
            verify(historyDao).clearAll()
        }
}
