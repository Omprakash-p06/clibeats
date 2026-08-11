@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.LikedSongDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Track
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryImplTest {
    private lateinit var songDao: SongDao
    private lateinit var likedSongDao: LikedSongDao
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setUp() {
        songDao = mock()
        likedSongDao = mock()
        repository = LibraryRepositoryImpl(songDao, likedSongDao)
    }

    @Test
    fun `getLikedSongs converts entities to domain tracks`() =
        runTest {
            val entity =
                SongEntity(
                    id = "t1",
                    title = "Liked Song",
                    artist = "Artist",
                    album = "Album",
                    durationMs = 180000L,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "ytmusic",
                )
            whenever(likedSongDao.getLikedSongsAsFlow()).thenReturn(flowOf(listOf(entity)))

            val result = repository.getLikedSongs().first()
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("Liked Song")
        }

    @Test
    fun `toggleLike unlikes song if currently liked`() =
        runTest {
            val track = Track("t1", "Liked Song", "Artist", "Album", 180000L, null, null, "ytmusic")
            whenever(likedSongDao.isLiked("t1")).thenReturn(true)

            repository.toggleLike(track)
            verify(likedSongDao).unlikeSong("t1")
        }

    @Test
    fun `toggleLike likes song and upserts song if not currently liked`() =
        runTest {
            val track = Track("t1", "Liked Song", "Artist", "Album", 180000L, null, null, "ytmusic")
            whenever(likedSongDao.isLiked("t1")).thenReturn(false)

            repository.toggleLike(track)
            verify(songDao).upsert(any())
            verify(likedSongDao).likeSong(eq("t1"), any())
        }
}
