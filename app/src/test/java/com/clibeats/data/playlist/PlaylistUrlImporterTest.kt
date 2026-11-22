@file:Suppress("ForbiddenImport")

package com.clibeats.data.playlist

import com.clibeats.data.local.dao.SongDao
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.repository.PlaylistRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test
import org.mockito.kotlin.mock

class PlaylistUrlImporterTest {
    private val okHttpClient: OkHttpClient = mock()
    private val providerRegistry: ProviderRegistry = mock()
    private val playlistRepository: PlaylistRepository = mock()
    private val songDao: SongDao = mock()

    private val importer =
        PlaylistUrlImporter(
            okHttpClient = okHttpClient,
            providerRegistry = providerRegistry,
            playlistRepository = playlistRepository,
            songDao = songDao,
        )

    @Test
    fun `unsupported url returns failure`() =
        runTest {
            val result = importer.importPlaylistFromUrl("https://example.com/not-a-playlist")
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("Unsupported playlist URL")
        }

    @Test
    fun `invalid spotify url without playlist id fails`() =
        runTest {
            val result = importer.importPlaylistFromUrl("https://open.spotify.com/album/12345")
            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `youtube url without list id fails`() =
        runTest {
            val result = importer.importPlaylistFromUrl("https://youtube.com/watch?v=abcd1234")
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("No playlist ID found")
        }
}
