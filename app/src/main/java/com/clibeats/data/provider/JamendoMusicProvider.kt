@file:Suppress("ForbiddenImport", "TooGenericExceptionCaught")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.JamendoApi
import com.clibeats.data.provider.mapper.toDomainTrack
import com.clibeats.di.ProviderModule.JAMENDO_CLIENT_ID_QUALIFIER
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderId
import com.clibeats.domain.provider.ProviderResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * MusicProvider backed by the Jamendo Creative Commons catalog.
 *
 * Requires a free `client_id` from developer.jamendo.com. The id is supplied
 * via the gradle property `JAMENDO_CLIENT_ID` (BuildConfig) and is injectable
 * for tests — it is never hardcoded.
 */
@Singleton
class JamendoMusicProvider
    @Inject
    constructor(
        private val api: JamendoApi,
        @Named(JAMENDO_CLIENT_ID_QUALIFIER) private val clientId: String,
    ) : MusicProvider {
        override val providerId: String = "jamendo"
        override val displayName: String = "Jamendo"

        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> =
            withClientId { id ->
                ProviderResult.Success(
                    api.tracks(clientId = id, search = query, limit = limit)
                        .results.mapNotNull { it.toDomainTrack() },
                )
            }

        override suspend fun trending(limit: Int): ProviderResult<List<Track>> =
            withClientId { id ->
                ProviderResult.Success(
                    api.tracks(clientId = id, limit = limit, order = "popularity_week")
                        .results.mapNotNull { it.toDomainTrack() },
                )
            }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> =
            withClientId { id ->
                val rawId = ProviderId.rawSourceId(providerId, trackId)
                val track = api.tracks(clientId = id, id = rawId).results.firstOrNull()?.toDomainTrack()
                if (track != null) {
                    ProviderResult.Success(track)
                } else {
                    ProviderResult.Error("No track found for: $trackId")
                }
            }

        override suspend fun stream(trackId: String): ProviderResult<String> =
            withClientId { id ->
                val rawId = ProviderId.rawSourceId(providerId, trackId)
                val track = api.tracks(clientId = id, id = rawId).results.firstOrNull()
                val url = track?.audiodownload ?: track?.audio
                if (url != null) {
                    ProviderResult.Success(url)
                } else {
                    ProviderResult.Error("No audio stream URL found for: $trackId")
                }
            }

        override suspend fun playlists(): ProviderResult<List<Playlist>> = ProviderResult.Success(emptyList())

        override suspend fun queue(): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())

        private suspend fun <T> withClientId(block: suspend (String) -> ProviderResult<T>): ProviderResult<T> {
            if (clientId.isBlank()) {
                return ProviderResult.Error(
                    "Jamendo client_id is not configured. Register a free app at developer.jamendo.com " +
                        "and set the JAMENDO_CLIENT_ID gradle property.",
                )
            }
            return try {
                block(clientId)
            } catch (e: Exception) {
                ProviderResult.Error(e.message ?: "Jamendo request failed", e)
            }
        }
    }
