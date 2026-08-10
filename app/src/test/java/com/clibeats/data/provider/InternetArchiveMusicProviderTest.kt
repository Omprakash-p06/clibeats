@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.InternetArchiveApi
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

class InternetArchiveMusicProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var provider: InternetArchiveMusicProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        val api: InternetArchiveApi =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(InternetArchiveApi::class.java)
        provider = InternetArchiveMusicProvider(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueJson(body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    private val searchResponse =
        """
        {"responseHeader":{"status":0},"response":{"numFound":1,"docs":[
          {"identifier":"wonderwall-1995","title":"Wonderwall","creator":["Oasis"],"mediatype":"audio","date":"1995"}
        ]}}
        """.trimIndent()

    private val audioMetadata =
        """
        {"metadata":{"identifier":"wonderwall-1995","title":"Wonderwall","creator":["Oasis"],"mediatype":"audio","date":"1995"},
         "files":[{"name":"Wonderwall.mp3","format":"VBR MP3","length":"258.0","size":"4200000"}]}
        """.trimIndent()

    @Test
    fun `search returns ranked playable tracks`() =
        runTest {
            enqueueJson(searchResponse)
            enqueueJson(audioMetadata)

            val result = provider.search("wonderwall")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            val tracks = (result as ProviderResult.Success).data
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].id).isEqualTo("internet_archive:wonderwall-1995")
            assertThat(tracks[0].title).isEqualTo("Wonderwall")
            assertThat(tracks[0].artist).isEqualTo("Oasis")
            assertThat(tracks[0].durationMs).isEqualTo(258_000L)
            assertThat(tracks[0].artworkUrl).isEqualTo("https://archive.org/services/img/wonderwall-1995")
            assertThat(tracks[0].streamUrl).isEqualTo("https://archive.org/download/wonderwall-1995/Wonderwall.mp3")
        }

    @Test
    fun `search skips items without playable audio`() =
        runTest {
            val twoDocs =
                """
                {"responseHeader":{"status":0},"response":{"numFound":2,"docs":[
                  {"identifier":"audio-item","title":"A Song","mediatype":"audio"},
                  {"identifier":"image-item","title":"A Photo Book","mediatype":"audio"}
                ]}}
                """.trimIndent()
            enqueueJson(twoDocs)
            enqueueJson(
                """
                {"metadata":{"identifier":"audio-item","title":"A Song","mediatype":"audio"},
                 "files":[{"name":"song.mp3","format":"128Kbps MP3","length":"200"}]}
                """.trimIndent(),
            )
            enqueueJson(
                """
                {"metadata":{"identifier":"image-item","title":"A Photo Book","mediatype":"audio"},
                 "files":[{"name":"scan.png","format":"PNG"}]}
                """.trimIndent(),
            )

            val result = provider.search("song")

            val tracks = (result as ProviderResult.Success).data
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].id).isEqualTo("internet_archive:audio-item")
        }

    @Test
    fun `search returns Error when api fails`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

            val result = provider.search("wonderwall")

            assertThat(result).isInstanceOf(ProviderResult.Error::class.java)
        }

    @Test
    fun `search returns empty Success when no docs`() =
        runTest {
            enqueueJson("""{"response":{"numFound":0,"docs":[]}}""")

            val result = provider.search("definitely-not-a-track-xyz")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).isEmpty()
        }

    @Test
    fun `search with more docs than metadata parallelism does not deadlock`() =
        runTest {
            // 6 docs > METADATA_PARALLELISM (4): the extra metadata fetches must
            // suspend on the permit gate instead of blocking the dispatcher.
            // A blocking java.util.concurrent.Semaphore here freezes the single
            // runTest thread forever (regression guard for the on-device hang).
            val docs =
                buildString {
                    append("{\"response\":{\"numFound\":6,\"docs\":[")
                    repeat(6) { i ->
                        append("{\"identifier\":\"item-$i\",\"title\":\"Track $i\",\"mediatype\":\"audio\"}")
                        if (i < 5) append(",")
                    }
                    append("]}}")
                }
            enqueueJson(docs)
            repeat(6) { i ->
                enqueueJson(
                    """
                    {"metadata":{"identifier":"item-$i","title":"Track $i","mediatype":"audio"},
                     "files":[{"name":"track.mp3","format":"VBR MP3","length":"200"}]}
                    """.trimIndent(),
                )
            }

            val result = provider.search("track")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data).hasSize(6)
        }

    @Test
    fun `getTrack returns track for composite identifier`() =
        runTest {
            enqueueJson(audioMetadata)

            val result = provider.getTrack("internet_archive:wonderwall-1995")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data.id).isEqualTo("internet_archive:wonderwall-1995")
        }

    @Test
    fun `stream returns direct download url`() =
        runTest {
            enqueueJson(audioMetadata)

            val result = provider.stream("internet_archive:wonderwall-1995")

            assertThat(result).isInstanceOf(ProviderResult.Success::class.java)
            assertThat((result as ProviderResult.Success).data)
                .isEqualTo("https://archive.org/download/wonderwall-1995/Wonderwall.mp3")
        }
}
