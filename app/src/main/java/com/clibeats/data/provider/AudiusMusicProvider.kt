@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.AudiusApi
import com.clibeats.data.provider.mapper.toDomainTrack
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderId
import com.clibeats.domain.provider.ProviderResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MusicProvider backed by the Audius open music catalog.
 *
 * Audius requires no account, no API key, no ads and no user tracking — only
 * an `app_name` query parameter.
 *
 * Stream resolution uses the verified working endpoint
 * `GET /v1/tracks/{id}/stream?app_name=clibeats` (302 → 206 audio/mpeg) instead
 * of the embedded signed `stream.url`, which the discovery API currently mints
 * with signatures its own gateways reject (HTTP 401 "invalid signature").
 */
@Singleton
class AudiusMusicProvider
    @Inject
    constructor(
        private val api: AudiusApi,
    ) : MusicProvider {
        override val providerId: String = "audius"
        override val displayName: String = "Audius"

        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> =
            runCatching {
                val response = api.searchTracks(query = query, limit = limit)
                val tracks = response.data.mapNotNull { it.toDomainTrack() }.map { it.withWorkingStreamUrl() }
                ProviderResult.Success(tracks)
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Search failed",
                    cause = e,
                )
            }

        override suspend fun trending(limit: Int): ProviderResult<List<Track>> =
            runCatching {
                val response = api.trendingTracks(limit = limit)
                val tracks = response.data.mapNotNull { it.toDomainTrack() }.map { it.withWorkingStreamUrl() }
                ProviderResult.Success(tracks)
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Trending fetch failed",
                    cause = e,
                )
            }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> =
            runCatching {
                val rawId = ProviderId.rawSourceId(providerId, trackId)
                val response = api.getTrack(rawId)
                val track = response.data?.toDomainTrack()?.withWorkingStreamUrl()
                if (track != null) {
                    ProviderResult.Success(track)
                } else {
                    ProviderResult.Error("No track found for: $trackId")
                }
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Track lookup failed for: $trackId",
                    cause = e,
                )
            }

        override suspend fun stream(trackId: String): ProviderResult<String> =
            runCatching {
                val rawId = ProviderId.rawSourceId(providerId, trackId)
                ProviderResult.Success(streamEndpointUrl(rawId))
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Stream failed for: $trackId",
                    cause = e,
                )
            }

        override suspend fun playlists(): ProviderResult<List<Playlist>> = ProviderResult.Success(emptyList())

        override suspend fun queue(): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())

        private fun Track.withWorkingStreamUrl(): Track {
            return copy(streamUrl = streamEndpointUrl(ProviderId.rawSourceId(providerId, id)))
        }

        private fun streamEndpointUrl(rawTrackId: String): String =
            "${AudiusApi.BASE_URL}tracks/$rawTrackId/stream?app_name=${AudiusApi.DEFAULT_APP_NAME}"
    }
