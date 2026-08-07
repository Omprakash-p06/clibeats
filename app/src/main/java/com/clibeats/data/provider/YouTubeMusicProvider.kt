@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.SearchRequest
import com.clibeats.data.provider.mapper.toTrackList
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.provider.StreamResolver
import com.clibeats.domain.provider.StreamResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicProvider
    @Inject
    constructor(
        private val api: InnerTubeApi,
        private val streamResolver: StreamResolver,
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

        override suspend fun stream(trackId: String): ProviderResult<String> {
            return when (val res = streamResolver.resolve(trackId)) {
                is StreamResult.Success -> ProviderResult.Success(res.url)
                is StreamResult.Error -> ProviderResult.Error(res.message, res.cause)
                StreamResult.NoFormats -> ProviderResult.Error("No audio formats available")
                StreamResult.GeoBlocked -> ProviderResult.Error("Track is geo-blocked")
                StreamResult.LoginRequired -> ProviderResult.Error("Track requires login")
                StreamResult.RateLimited -> ProviderResult.Error("Rate limit exceeded")
                StreamResult.InvalidSignature -> ProviderResult.Error("Invalid signature")
                StreamResult.SignatureExpired -> ProviderResult.Error("Signature expired")
            }
        }

        override suspend fun playlists(): ProviderResult<List<Playlist>> {
            return ProviderResult.Success(emptyList())
        }

        override suspend fun queue(): ProviderResult<List<Track>> {
            return ProviderResult.Success(emptyList())
        }
    }
