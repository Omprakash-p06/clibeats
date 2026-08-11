@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.PlaylistDao
import com.clibeats.data.local.entity.PlaylistEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PlaylistRepositoryImplTest {
    private lateinit var playlistDao: PlaylistDao
    private lateinit var repository: PlaylistRepositoryImpl

    @Before
    fun setUp() {
        playlistDao = mock()
        repository = PlaylistRepositoryImpl(playlistDao)
    }

    @Test
    fun `getAllPlaylistsAsFlow maps entities to domain models`() =
        runTest {
            val entity =
                PlaylistEntity(
                    id = "p1",
                    name = "Favorites",
                    description = "My favorite tracks",
                    artworkUrl = null,
                    trackCount = 2,
                    isOwned = true,
                    providerId = "local",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            whenever(playlistDao.getAllAsFlow()).thenReturn(flowOf(listOf(entity)))

            val playlists = repository.getAllPlaylistsAsFlow().first()
            assertThat(playlists).hasSize(1)
            assertThat(playlists[0].name).isEqualTo("Favorites")
            assertThat(playlists[0].trackCount).isEqualTo(2)
        }

    @Test
    fun `reorderPlaylistSongs delegates to playlistDao`() =
        runTest {
            repository.reorderPlaylistSongs("p1", listOf("s2", "s1"))
            org.mockito.kotlin.verify(playlistDao).reorderPlaylistSongs("p1", listOf("s2", "s1"))
        }
}
