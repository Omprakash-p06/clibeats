@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Track
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SongRepositoryImplTest {
    private lateinit var songDao: SongDao
    private lateinit var repository: SongRepositoryImpl

    @Before
    fun setUp() {
        songDao = mock()
        repository = SongRepositoryImpl(songDao)
    }

    @Test
    fun `getAllTracksAsFlow maps song entities to domain tracks`() =
        runTest {
            val entity =
                SongEntity(
                    id = "s1",
                    title = "Track 1",
                    artist = "Artist 1",
                    album = "Album 1",
                    durationMs = 180000L,
                    artworkUrl = null,
                    streamUrl = "http://stream/s1",
                    providerId = "ytmusic",
                )
            whenever(songDao.getAllAsFlow()).thenReturn(flowOf(listOf(entity)))

            val tracks = repository.getAllTracksAsFlow().first()
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].id).isEqualTo("s1")
            assertThat(tracks[0].title).isEqualTo("Track 1")
        }

    @Test
    fun `upsertTrack delegates to songDao`() =
        runTest {
            val track =
                Track(
                    id = "s2",
                    title = "Track 2",
                    artist = "Artist 2",
                    album = "Album 2",
                    durationMs = 200000L,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "local",
                )
            repository.upsertTrack(track)
            verify(songDao).upsert(org.mockito.kotlin.any())
        }
}
