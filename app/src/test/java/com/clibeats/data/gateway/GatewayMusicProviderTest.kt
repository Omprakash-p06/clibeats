@file:Suppress("ForbiddenImport")

package com.clibeats.data.gateway

import com.clibeats.data.gateway.api.GatewayApi
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class GatewayMusicProviderTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun provider(): GatewayMusicProvider {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        return GatewayMusicProvider(retrofit.create(GatewayApi::class.java))
    }

    @Test
    fun `search returns Success with mapped tracks`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "tracks": [
                        {
                          "id": "vid1",
                          "providerId": "youtube",
                          "title": "Song Title",
                          "artist": "Artist",
                          "album": "Album",
                          "durationSeconds": 259,
                          "artworkUrl": "https://art.example/t.jpg",
                          "explicit": true
                        },
                        {
                          "id": "vid2",
                          "providerId": "youtube",
                          "title": "Song Two",
                          "artist": "Artist2"
                        }
                      ],
                      "cached": false
                    }
                    """.trimIndent(),
                ).setHeader("Content-Type", "application/json"),
            )

            val result = provider().search("chill", limit = 2)

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            val tracks = (result as ProviderResult.Success).data
            assertThat(tracks).hasSize(2)
            assertThat(tracks[0].id).isEqualTo("vid1")
            assertThat(tracks[0].durationMs).isEqualTo(259_000L)
        }

    @Test
    fun `search returns Success with empty list when no tracks`() =
        runTest {
            server.enqueue(
                MockResponse().setBody("""{"tracks":[],"cached":false}""")
                    .setHeader("Content-Type", "application/json"),
            )

            val result = provider().search("nothing", limit = 20)

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEmpty()
        }

    @Test
    fun `search returns Error with mapped message on RATE_LIMITED`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(429).setBody(
                    """{"error":{"code":"RATE_LIMITED","message":"too many","providerId":"youtube"}}""",
                ).setHeader("Content-Type", "application/json"),
            )

            val result = provider().search("hot", limit = 5)

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
            assertThat((result as ProviderResult.Error).message)
                .isEqualTo("Rate limit exceeded, try again shortly")
        }

    @Test
    fun `stream returns Success with stream URL`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "stream": {
                        "trackId": "vid1",
                        "providerId": "youtube",
                        "streamUrl": "https://googlevideo.example/videoplayback?id=1",
                        "mimeType": "audio/mp4",
                        "expiresAtEpochSeconds": 9999999999
                      }
                    }
                    """.trimIndent(),
                ).setHeader("Content-Type", "application/json"),
            )

            val result = provider().stream("vid1")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data)
                .isEqualTo("https://googlevideo.example/videoplayback?id=1")
        }

    @Test
    fun `stream returns Error when URL is blank`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """{"stream":{"trackId":"vid1","providerId":"youtube","streamUrl":"","expiresAtEpochSeconds":0}}""",
                ).setHeader("Content-Type", "application/json"),
            )

            val result = provider().stream("vid1")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }
}
