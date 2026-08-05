package com.clibeats.data.repository

import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.data.local.mapper.toDomain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SongRepositoryImplTest {

    private lateinit var songDao: SongDao
    private lateinit var repository: SongRepositoryImpl

    @Before
    fun setup() {
        songDao = mock()
        repository = SongRepositoryImpl(songDao)
    }

    private fun testEntity(id: String = "s1") = SongEntity(
        id = id, title = "Test", artist = "Artist", album = "Album",
        durationMs = 180_000L, artworkUrl = null, streamUrl = null, providerId = "local",
    )

    @Test
    fun getAllTracksAsFlow_mapsToDomain() = runTest {
        val entities = listOf(testEntity("s1"), testEntity("s2"))
        whenever(songDao.getAllAsFlow()).thenReturn(flowOf(entities))

        val tracks = repository.getAllTracksAsFlow().first()
        assertEquals(2, tracks.size)
        assertEquals("s1", tracks[0].id)
        assertEquals("s2", tracks[1].id)
    }

    @Test
    fun upsertTrack_callsDaoUpsert() = runTest {
        val track = testEntity("s1").toDomain()
        repository.upsertTrack(track)
        verify(songDao).upsert(testEntity("s1"))
    }
}
