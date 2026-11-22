@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.preferences.AppPreferences
import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.PlayabilityStatus
import com.clibeats.data.provider.dto.PlayerResponse
import com.clibeats.data.provider.youtube.ExtractedStreamInfo
import com.clibeats.data.provider.youtube.NewPipeExtractorResolver
import com.clibeats.data.provider.youtube.PoTokenGenerator
import com.clibeats.data.provider.youtube.StreamCacheManager
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class YouTubeMusicProviderTest {
    private lateinit var api: InnerTubeApi
    private lateinit var poTokenGenerator: PoTokenGenerator
    private lateinit var streamCacheManager: StreamCacheManager
    private lateinit var newPipeExtractorResolver: NewPipeExtractorResolver
    private lateinit var appPreferences: AppPreferences
    private lateinit var provider: YouTubeMusicProvider

    @Before
    fun setup() {
        api = mock()
        poTokenGenerator = mock()
        streamCacheManager = mock()
        newPipeExtractorResolver = mock()
        appPreferences = mock()
        whenever(streamCacheManager.get(any(), any())).thenReturn(null)
        whenever(appPreferences.highQualityStreaming).thenReturn(flowOf(true))
        provider =
            YouTubeMusicProvider(
                api = api,
                poTokenGenerator = poTokenGenerator,
                streamCacheManager = streamCacheManager,
                newPipeExtractorResolver = newPipeExtractorResolver,
                appPreferences = appPreferences,
            )
    }

    private fun audioFormat(
        itag: Int,
        bitrate: Int,
        url: String,
        mimeType: String = "audio/mp4",
    ) = """{"itag":$itag,"mimeType":"$mimeType","bitrate":$bitrate,"url":"$url"}"""

    private fun streamingData(vararg formats: String): JsonElement =
        Json.parseToJsonElement("""{"adaptiveFormats":[${formats.joinToString(",")}]}""")

    private fun okResponse(vararg formats: String) =
        PlayerResponse(
            playabilityStatus = PlayabilityStatus(status = "OK"),
            streamingData = streamingData(*formats),
        )

    @Test
    fun `fast InnerTube path resolves before NewPipe runs`() =
        runTest {
            whenever(api.player(any())).thenReturn(
                okResponse(audioFormat(140, 128_000, "https://rr1.googlevideo.com/a?expire=4102444800")),
            )

            val result = provider.stream("yt1")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).contains("rr1.googlevideo.com")
            verify(newPipeExtractorResolver, never()).resolveStream(any(), any())
        }

    @Test
    fun `high quality streaming picks the highest bitrate`() =
        runTest {
            whenever(appPreferences.highQualityStreaming).thenReturn(flowOf(true))
            whenever(api.player(any())).thenReturn(
                okResponse(
                    audioFormat(139, 48_000, "https://rr1.googlevideo.com/low"),
                    audioFormat(140, 128_000, "https://rr1.googlevideo.com/high"),
                ),
            )

            val result = provider.stream("yt1")

            assertThat((result as ProviderResult.Success).data).contains("/high")
        }

    @Test
    fun `data saver picks the lowest bitrate`() =
        runTest {
            whenever(appPreferences.highQualityStreaming).thenReturn(flowOf(false))
            whenever(api.player(any())).thenReturn(
                okResponse(
                    audioFormat(139, 48_000, "https://rr1.googlevideo.com/low"),
                    audioFormat(140, 128_000, "https://rr1.googlevideo.com/high"),
                ),
            )

            val result = provider.stream("yt1")

            assertThat((result as ProviderResult.Success).data).contains("/low")
        }

    @Test
    fun `falls back to NewPipe when the fast path fails`() =
        runTest {
            whenever(api.player(any())).thenThrow(RuntimeException("blocked"))
            whenever(newPipeExtractorResolver.resolveStream(any(), any())).thenReturn(
                ExtractedStreamInfo(
                    url = "https://newpipe.example/stream",
                    host = "newpipe.example",
                    itag = 251,
                    mimeType = "audio/webm",
                    bitrate = 160_000,
                    expiresAtMs = System.currentTimeMillis() + 3_600_000L,
                ),
            )

            val result = provider.stream("yt1")

            assertThat((result as ProviderResult.Success).data).isEqualTo("https://newpipe.example/stream")
            verify(newPipeExtractorResolver).resolveStream(any(), any())
        }

    @Test
    fun `a cached stream URL is returned without any network call`() =
        runTest {
            whenever(streamCacheManager.get(any(), any())).thenReturn(
                ExtractedStreamInfo(
                    url = "https://cache.example/stream",
                    host = "cache.example",
                    itag = 251,
                    mimeType = "audio/webm",
                    bitrate = 160_000,
                    expiresAtMs = System.currentTimeMillis() + 3_600_000L,
                ),
            )

            val result = provider.stream("yt1")

            assertThat((result as ProviderResult.Success).data).isEqualTo("https://cache.example/stream")
            verify(api, never()).player(any())
            verify(newPipeExtractorResolver, never()).resolveStream(any(), any())
        }
}
