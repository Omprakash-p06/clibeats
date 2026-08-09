@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.PlayerResponse
import com.clibeats.data.provider.dto.SearchResponse
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class YouTubeMusicProviderTest {
    private lateinit var api: InnerTubeApi
    private lateinit var provider: YouTubeMusicProvider

    @Before
    fun setUp() {
        api = mock()
        provider = YouTubeMusicProvider(api)
    }

    @Test
    fun `search returns Success with empty list when response has null contents`() =
        runTest {
            whenever(api.search(any())).thenReturn(SearchResponse(contents = null))

            val result = provider.search("Wonderwall")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEmpty()
        }

    @Test
    fun `search returns Error when api throws exception`() =
        runTest {
            whenever(api.search(any())).thenThrow(RuntimeException("network error"))

            val result = provider.search("Wonderwall")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
            assertThat((result as ProviderResult.Error).message).contains("network error")
        }

    @Test
    fun `stream returns Error when player response has null streamingData`() =
        runTest {
            whenever(api.player(any())).thenReturn(PlayerResponse(streamingData = null))

            val result = provider.stream("someVideoId")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }

    @Test
    fun `stream returns Error when api throws exception`() =
        runTest {
            whenever(api.player(any())).thenThrow(RuntimeException("player error"))

            val result = provider.stream("someVideoId")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }

    @Test
    fun `playlists returns empty Success`() =
        runTest {
            val result = provider.playlists()
            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEmpty()
        }

    @Test
    fun `queue returns empty Success`() =
        runTest {
            val result = provider.queue()
            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEmpty()
        }

    @Test
    fun `getTrack returns Error as Phase 5 stub`() =
        runTest {
            val result = provider.getTrack("abc")
            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }

    @Test
    fun `provider has correct id and displayName`() {
        assertThat(provider.providerId).isEqualTo("youtube_music")
        assertThat(provider.displayName).isEqualTo("YouTube Music")
    }
}
