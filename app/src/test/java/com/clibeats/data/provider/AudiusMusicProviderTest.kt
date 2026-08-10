@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.AudiusApi
import com.clibeats.data.provider.dto.AudiusSearchResponse
import com.clibeats.data.provider.dto.AudiusStreamDto
import com.clibeats.data.provider.dto.AudiusTrackDto
import com.clibeats.data.provider.dto.AudiusTrackResponse
import com.clibeats.data.provider.dto.AudiusUserDto
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AudiusMusicProviderTest {
    private lateinit var api: AudiusApi
    private lateinit var provider: AudiusMusicProvider

    @Before
    fun setUp() {
        api = mock()
        provider = AudiusMusicProvider(api)
    }

    @Test
    fun `search returns Success with mapped composite tracks`() =
        runTest {
            whenever(api.searchTracks(any(), any(), any()))
                .thenReturn(
                    AudiusSearchResponse(
                        data =
                            listOf(
                                AudiusTrackDto(
                                    id = "95wro",
                                    title = "Stars In The Sky - Lofi Beats",
                                    duration = 71L,
                                    user = AudiusUserDto(name = "Lofi Beats"),
                                    artwork = null,
                                    stream = AudiusStreamDto(url = "https://cdn.example/stream/abc"),
                                ),
                            ),
                    ),
                )

            val result = provider.search("lofi")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            val tracks = (result as ProviderResult.Success).data
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].id).isEqualTo("audius:95wro")
            assertThat(tracks[0].streamUrl).isEqualTo(streamEndpoint("95wro"))
            assertThat(tracks[0].providerId).isEqualTo("audius")
        }

    @Test
    fun `search returns Error when api throws exception`() =
        runTest {
            whenever(api.searchTracks(any(), any(), any())).thenThrow(RuntimeException("network error"))

            val result = provider.search("lofi")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
            assertThat((result as ProviderResult.Error).message).contains("network error")
        }

    @Test
    fun `getTrack returns Success when track found`() =
        runTest {
            whenever(api.getTrack(any(), any()))
                .thenReturn(
                    AudiusTrackResponse(
                        data =
                            AudiusTrackDto(
                                id = "95wro",
                                title = "Stars In The Sky",
                                duration = 71L,
                                stream = AudiusStreamDto(url = "https://cdn.example/stream/abc"),
                            ),
                    ),
                )

            val result = provider.getTrack("audius:95wro")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data.id).isEqualTo("audius:95wro")
            assertThat(result.data.streamUrl).isEqualTo(streamEndpoint("95wro"))
        }

    @Test
    fun `getTrack returns Error when track missing`() =
        runTest {
            whenever(api.getTrack(any(), any())).thenReturn(AudiusTrackResponse(data = null))

            val result = provider.getTrack("audius:missing")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }

    @Test
    fun `stream returns the working stream endpoint without an api call`() =
        runTest {
            val result = provider.stream("audius:95wro")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEqualTo(streamEndpoint("95wro"))
        }

    @Test
    fun `playlists and queue return empty Success`() =
        runTest {
            assertThat((provider.playlists() as ProviderResult.Success).data).isEmpty()
            assertThat((provider.queue() as ProviderResult.Success).data).isEmpty()
        }

    private fun streamEndpoint(rawId: String): String {
        return "${AudiusApi.BASE_URL}tracks/$rawId/stream?app_name=${AudiusApi.DEFAULT_APP_NAME}"
    }
}
