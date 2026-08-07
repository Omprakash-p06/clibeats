@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.dto.SearchRequest
import com.clibeats.data.provider.mapper.extractStreamUrl
import com.clibeats.data.provider.mapper.toTrackList
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicProvider
    @Inject
    constructor(
        private val api: InnerTubeApi,
    ) : MusicProvider {
        override val providerId: String = "youtube_music"
        override val displayName: String = "YouTube Music"

        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> =
            runCatching {
                val searchQuery = if (query.isBlank()) "Trending Hits" else query
                val response = api.search(SearchRequest.forQuery(searchQuery))
                val tracks = response.toTrackList().take(limit)
                ProviderResult.Success(tracks)
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Search failed",
                    cause = e,
                )
            }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> {
            return ProviderResult.Error("Not implemented in Phase 5")
        }

        override suspend fun stream(trackId: String): ProviderResult<String> =
            runCatching {
                val requests =
                    listOf(
                        PlayerRequest(
                            context = com.clibeats.data.provider.dto.InnerTubeContext.tvHtml5(),
                            videoId = trackId,
                        ),
                        PlayerRequest(
                            context = com.clibeats.data.provider.dto.InnerTubeContext.android(),
                            videoId = trackId,
                        ),
                        PlayerRequest.forVideoId(trackId),
                    )

                for (request in requests) {
                    val response = runCatching { api.player(request) }.getOrNull()
                    val url = response?.extractStreamUrl()
                    if (!url.isNullOrBlank()) {
                        logDebug("YouTubeMusicProvider", "Resolved InnerTube stream URL for $trackId")
                        return ProviderResult.Success(url)
                    }
                }

                logWarn("YouTubeMusicProvider", "InnerTube returned no stream URL for $trackId, trying Piped fallback")
                val fallbackUrl = fetchPipedStreamUrl(trackId)
                if (!fallbackUrl.isNullOrBlank()) {
                    logDebug("YouTubeMusicProvider", "Resolved Piped fallback stream URL for $trackId")
                    return ProviderResult.Success(fallbackUrl)
                }

                ProviderResult.Error("No audio stream URL found for: $trackId")
            }.getOrElse { e ->
                logError("YouTubeMusicProvider", "Stream failed for $trackId: ${e.message}", e)
                ProviderResult.Error(
                    message = e.message ?: "Stream failed for: $trackId",
                    cause = e,
                )
            }

        private suspend fun fetchPipedStreamUrl(videoId: String): String? =
            withContext(Dispatchers.IO) {
                val instances =
                    listOf(
                        "https://pipedapi.kavin.rocks",
                        "https://api.piped.video",
                        "https://pipedapi.tokhmi.xyz",
                        "https://pipedapi.mha.fi",
                    )
                val client =
                    OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .build()

                val json = Json { ignoreUnknownKeys = true }

                for (baseUrl in instances) {
                    runCatching {
                        val request =
                            Request.Builder()
                                .url("$baseUrl/streams/$videoId")
                                .header("User-Agent", "Mozilla/5.0")
                                .build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val jsonObj = json.parseToJsonElement(bodyString).jsonObject
                            val audioStreams = jsonObj["audioStreams"]?.jsonArray
                            val streamUrl =
                                audioStreams?.firstOrNull()
                                    ?.jsonObject?.get("url")
                                    ?.jsonPrimitive?.contentOrNull
                            if (!streamUrl.isNullOrBlank()) {
                                return@withContext streamUrl
                            }
                        }
                    }
                }
                null
            }

        override suspend fun playlists(): ProviderResult<List<Playlist>> {
            return ProviderResult.Success(emptyList())
        }

        override suspend fun queue(): ProviderResult<List<Track>> {
            return ProviderResult.Success(emptyList())
        }
    }

private fun logDebug(
    tag: String,
    msg: String,
) {
    runCatching { android.util.Log.d(tag, msg) }
}

private fun logWarn(
    tag: String,
    msg: String,
) {
    runCatching { android.util.Log.w(tag, msg) }
}

private fun logError(
    tag: String,
    msg: String,
    tr: Throwable? = null,
) {
    runCatching { android.util.Log.e(tag, msg, tr) }
}
