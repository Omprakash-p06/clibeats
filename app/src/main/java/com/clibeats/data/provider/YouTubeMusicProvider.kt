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
                val response = api.search(SearchRequest.forQuery(query))
                val tracks = response.toTrackList().take(limit)
                ProviderResult.Success(tracks)
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Search failed",
                    cause = e,
                )
            }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> =
            ProviderResult.Error("getTrack not implemented in Phase 5")

        override suspend fun stream(trackId: String): ProviderResult<String> =
            runCatching {
                val response = api.player(PlayerRequest.forVideoId(trackId))
                val url = response.extractStreamUrl()
                if (url != null) {
                    ProviderResult.Success(url)
                } else {
                    ProviderResult.Error("No audio stream URL found for: $trackId")
                }
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = e.message ?: "Stream failed for: $trackId",
                    cause = e,
                )
            }

        override suspend fun playlists(): ProviderResult<List<Playlist>> =
            ProviderResult.Success(emptyList())

        override suspend fun queue(): ProviderResult<List<Track>> =
            ProviderResult.Success(emptyList())
    }
