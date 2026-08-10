@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.SongRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LocalMusicProviderTest {
    private lateinit var songRepository: SongRepository
    private lateinit var provider: LocalMusicProvider

    @Before
    fun setUp() {
        songRepository = mock()
        provider = LocalMusicProvider(songRepository)
    }

    private fun localTrack() =
        Track(
            id = "local:/sdcard/music/a.mp3",
            title = "Local Song",
            artist = "Me",
            album = "Home",
            durationMs = 120_000L,
            artworkUrl = null,
            streamUrl = "/sdcard/music/a.mp3",
            providerId = "local",
        )

    @Test
    fun `search maps bare local paths to file uris`() =
        runTest {
            whenever(songRepository.searchTracksAsFlow("song")).thenReturn(flowOf(listOf(localTrack())))

            val result = provider.search("song")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            val tracks = (result as ProviderResult.Success).data
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].streamUrl).isEqualTo("file:///sdcard/music/a.mp3")
            assertThat(tracks[0].providerId).isEqualTo("local")
        }

    @Test
    fun `search keeps http stream urls untouched`() =
        runTest {
            val remote = localTrack().copy(streamUrl = "https://cdn.example/stream")
            whenever(songRepository.searchTracksAsFlow("song")).thenReturn(flowOf(listOf(remote)))

            val result = provider.search("song")

            assertThat(((result as ProviderResult.Success).data)[0].streamUrl).isEqualTo("https://cdn.example/stream")
        }

    @Test
    fun `trending returns library tracks`() =
        runTest {
            whenever(songRepository.getAllTracksAsFlow()).thenReturn(flowOf(listOf(localTrack())))

            val result = provider.trending(10)

            assertThat((result as ProviderResult.Success).data).hasSize(1)
        }

    @Test
    fun `stream returns file uri for local track`() =
        runTest {
            whenever(songRepository.getTrackById("local:/sdcard/music/a.mp3")).thenReturn(localTrack())

            val result = provider.stream("local:/sdcard/music/a.mp3")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEqualTo("file:///sdcard/music/a.mp3")
        }

    @Test
    fun `search returns Error when repository throws`() =
        runTest {
            whenever(songRepository.searchTracksAsFlow(any())).thenThrow(RuntimeException("db error"))

            val result = provider.search("song")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }
}
