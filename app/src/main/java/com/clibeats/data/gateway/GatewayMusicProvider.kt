package com.clibeats.data.gateway

import com.clibeats.data.gateway.api.GatewayApi
import com.clibeats.data.gateway.dto.GatewayStreamRequest
import com.clibeats.data.gateway.mapper.GatewayErrorMapper
import com.clibeats.data.gateway.mapper.toDomainTracks
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MusicProvider] backed by the CliBeats Provider Gateway instead of a direct
 * InnerTube client. Streaming and search are delegated to the gateway's
 * /api/v1 endpoints, keeping all YouTube-facing logic server-side.
 */
@Singleton
class GatewayMusicProvider
    @Inject
    constructor(
        private val api: GatewayApi,
    ) : MusicProvider {
        override val providerId: String = "youtube_music"
        override val displayName: String = "YouTube Music"

        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val searchQuery = if (query.isBlank()) "Trending Hits" else query
                    val response = api.search(searchQuery, filterSongs = true)
                    ProviderResult.Success(response.tracks.toDomainTracks().take(limit))
                }.getOrElse { e ->
                    ProviderResult.Error(
                        message = GatewayErrorMapper.message(e),
                        cause = e,
                    )
                }
            }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> {
            return ProviderResult.Error("Not implemented in Phase 5")
        }

        override suspend fun stream(trackId: String): ProviderResult<String> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.stream(GatewayStreamRequest(trackId))
                    val url = response.stream.streamUrl
                    if (url.isBlank()) {
                        ProviderResult.Error("Gateway returned an empty stream URL")
                    } else {
                        ProviderResult.Success(url)
                    }
                }.getOrElse { e ->
                    ProviderResult.Error(
                        message = GatewayErrorMapper.message(e),
                        cause = e,
                    )
                }
            }

        override suspend fun playlists(): ProviderResult<List<Playlist>> {
            return ProviderResult.Success(emptyList())
        }

        override suspend fun queue(): ProviderResult<List<Track>> {
            return ProviderResult.Success(emptyList())
        }
    }