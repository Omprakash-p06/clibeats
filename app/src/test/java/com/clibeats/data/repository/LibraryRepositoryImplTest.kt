@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.LikedSongDao
import com.clibeats.data.local.dao.SavedAlbumDao
import com.clibeats.data.local.dao.SavedArtistDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Album
import com.clibeats.domain.model.Artist
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
    private lateinit var savedAlbumDao: SavedAlbumDao
    private lateinit var savedArtistDao: SavedArtistDao
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setUp() {
        songDao = mock()
        likedSongDao = mock()
        savedAlbumDao = mock()
        savedArtistDao = mock()
        repository = LibraryRepositoryImpl(songDao, likedSongDao, savedAlbumDao, savedArtistDao)
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

    @Test
    fun `toggleSaveAlbum unsaves album if currently saved`() =
        runTest {
            val album = Album("a1", "Test Album", "Artist", 2024, null, 10, "ytmusic")
            whenever(savedAlbumDao.isSaved("a1")).thenReturn(true)

            repository.toggleSaveAlbum(album)
            verify(savedAlbumDao).deleteById("a1")
        }

    @Test
    fun `toggleSaveAlbum upserts album if not currently saved`() =
        runTest {
            val album = Album("a1", "Test Album", "Artist", 2024, null, 10, "ytmusic")
            whenever(savedAlbumDao.isSaved("a1")).thenReturn(false)

            repository.toggleSaveAlbum(album)
            verify(savedAlbumDao).upsert(any())
        }

    @Test
    fun `toggleSaveArtist unsaves artist if currently saved`() =
        runTest {
            val artist = Artist("ar1", "Test Artist", null, "ytmusic")
            whenever(savedArtistDao.isSaved("ar1")).thenReturn(true)

            repository.toggleSaveArtist(artist)
            verify(savedArtistDao).deleteById("ar1")
        }

    @Test
    fun `toggleSaveArtist upserts artist if not currently saved`() =
        runTest {
            val artist = Artist("ar1", "Test Artist", null, "ytmusic")
            whenever(savedArtistDao.isSaved("ar1")).thenReturn(false)

            repository.toggleSaveArtist(artist)
            verify(savedArtistDao).upsert(any())
        }

    @Test
    fun `searchSavedAlbums delegates to savedAlbumDao`() =
        runTest {
            whenever(savedAlbumDao.searchAsFlow("rock")).thenReturn(flowOf(emptyList()))
            val result = repository.searchSavedAlbums("rock").first()
            assertThat(result).isEmpty()
            verify(savedAlbumDao).searchAsFlow("rock")
        }

    @Test
    fun `searchSavedArtists delegates to savedArtistDao`() =
        runTest {
            whenever(savedArtistDao.searchAsFlow("metal")).thenReturn(flowOf(emptyList()))
            val result = repository.searchSavedArtists("metal").first()
            assertThat(result).isEmpty()
            verify(savedArtistDao).searchAsFlow("metal")
        }
}
