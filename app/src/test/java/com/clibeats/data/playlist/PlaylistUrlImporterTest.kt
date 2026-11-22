@file:Suppress("ForbiddenImport")

package com.clibeats.data.playlist

import com.clibeats.data.local.dao.SongDao
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.PlaylistRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlaylistUrlImporterTest {
    private val okHttpClient: OkHttpClient = mock()
    private val providerRegistry: ProviderRegistry = mock()
    private val playlistRepository: PlaylistRepository = mock()
    private val songDao: SongDao = mock()

    private val playlistTracks =
        listOf(
            Track(
                id = "aaa",
                title = "First",
                artist = "Artist A",
                album = "",
                durationMs = 1_000L,
                artworkUrl = null,
                streamUrl = null,
                providerId = "youtube_music",
            ),
            Track(
                id = "bbb",
                title = "Second",
                artist = "Artist B",
                album = "",
                durationMs = 2_000L,
                artworkUrl = null,
                streamUrl = null,
                providerId = "youtube_music",
            ),
        )

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
    fun `youtube playlist imports the provider playlist instead of search results`() =
        runTest {
            val provider: MusicProvider = mock()
            whenever(providerRegistry.getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID))
                .thenReturn(provider)
            whenever(provider.playlistTracks("PL123"))
                .thenReturn(ProviderResult.Success(playlistTracks))
            whenever(playlistRepository.createAppPlaylist(any(), any())).thenReturn("playlist-1")

            val result = importer.importPlaylistFromUrl("https://music.youtube.com/playlist?list=PL123")

            assertThat(result.getOrNull()).isEqualTo("playlist-1")
            verify(provider).playlistTracks("PL123")
            verify(provider, never()).search(any(), any())
            verify(playlistRepository)
                .createAppPlaylist("Imported YouTube Playlist", listOf("aaa", "bbb"))
        }

    @Test
    fun `youtube playlist reports the provider reason when it yields no tracks`() =
        runTest {
            val provider: MusicProvider = mock()
            whenever(providerRegistry.getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID))
                .thenReturn(provider)
            whenever(provider.playlistTracks("PL999"))
                .thenReturn(ProviderResult.Error("playlist is private"))

            val result = importer.importPlaylistFromUrl("https://youtube.com/playlist?list=PL999")

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("playlist is private")
        }

    @Test
    fun `youtube url without list id fails`() =
        runTest {
            val result = importer.importPlaylistFromUrl("https://youtube.com/watch?v=abcd1234")
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("No playlist ID found")
        }
}
