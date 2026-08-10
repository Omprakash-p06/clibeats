@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.JamendoApi
import com.clibeats.data.provider.dto.JamendoResponse
import com.clibeats.data.provider.dto.JamendoTrackDto
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class JamendoMusicProviderTest {
    private lateinit var api: JamendoApi
    private lateinit var provider: JamendoMusicProvider

    @Before
    fun setUp() {
        api = mock()
        provider = JamendoMusicProvider(api, clientId = "test-client")
    }

    private fun sampleTrack() =
        JamendoTrackDto(
            id = "42",
            name = "Sunrise",
            duration = 180L,
            artistName = "The Artists",
            albumName = "Dawn",
            image = "https://img.example/art.jpg",
            audiodownload = "https://dl.example/42.mp3",
            audio = "https://stream.example/42",
        )

    private suspend fun stubTracks(
        search: String? = null,
        id: String? = null,
        order: String? = null,
        limit: Int = 20,
        response: JamendoResponse = JamendoResponse(results = listOf(sampleTrack())),
    ) {
        whenever(api.tracks(clientId = "test-client", search = search, id = id, order = order, limit = limit))
            .thenReturn(response)
    }

    @Test
    fun `search returns mapped composite tracks`() =
        runTest {
            stubTracks(search = "dawn")

            val result = provider.search("dawn")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            val tracks = (result as ProviderResult.Success).data
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].id).isEqualTo("jamendo:42")
            assertThat(tracks[0].title).isEqualTo("Sunrise")
            assertThat(tracks[0].artist).isEqualTo("The Artists")
            assertThat(tracks[0].album).isEqualTo("Dawn")
            assertThat(tracks[0].durationMs).isEqualTo(180_000L)
            assertThat(tracks[0].artworkUrl).isEqualTo("https://img.example/art.jpg")
            assertThat(tracks[0].streamUrl).isEqualTo("https://dl.example/42.mp3")
            assertThat(tracks[0].providerId).isEqualTo("jamendo")
        }

    @Test
    fun `trending requests popularity order`() =
        runTest {
            stubTracks(order = "popularity_week", limit = 10)

            val result = provider.trending(10)

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            verify(api).tracks(
                clientId = "test-client",
                format = "json",
                limit = 10,
                search = null,
                id = null,
                order = "popularity_week",
                audioFormat = "mp32",
            )
        }

    @Test
    fun `getTrack strips composite prefix before api call`() =
        runTest {
            stubTracks(id = "42")

            val result = provider.getTrack("jamendo:42")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data.id).isEqualTo("jamendo:42")
        }

    @Test
    fun `stream prefers audiodownload url`() =
        runTest {
            stubTracks(id = "42")

            val result = provider.stream("jamendo:42")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEqualTo("https://dl.example/42.mp3")
        }

    @Test
    fun `search returns Error when api throws`() =
        runTest {
            whenever(api.tracks(clientId = "test-client", search = "dawn", id = null, order = null, limit = 20))
                .thenThrow(RuntimeException("boom"))

            val result = provider.search("dawn")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
            assertThat((result as ProviderResult.Error).message).contains("boom")
        }

    @Test
    fun `search returns Error when client id not configured`() =
        runTest {
            val unconfigured = JamendoMusicProvider(api, clientId = "")

            val result = unconfigured.search("dawn")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
            assertThat((result as ProviderResult.Error).message).contains("client_id is not configured")
        }
}
