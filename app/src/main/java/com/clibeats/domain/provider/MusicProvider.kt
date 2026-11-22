package com.clibeats.domain.provider

import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track

interface MusicProvider {
    val providerId: String
    val displayName: String

    suspend fun search(
        query: String,
        limit: Int = 20,
    ): ProviderResult<List<Track>>

    suspend fun trending(limit: Int = 20): ProviderResult<List<Track>>

    suspend fun getTrack(trackId: String): ProviderResult<Track>

    suspend fun stream(trackId: String): ProviderResult<String>

    suspend fun relatedTracks(
        trackId: String,
        limit: Int = 15,
    ): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())

    /**
     * Tracks of a provider playlist, in playlist order. Providers without playlist access keep
     * this default so a URL import fails loudly instead of substituting unrelated results.
     */
    suspend fun playlistTracks(playlistId: String): ProviderResult<List<Track>> =
        ProviderResult.Error("Playlist import is not supported by $displayName")

    suspend fun playlists(): ProviderResult<List<Playlist>>

    suspend fun queue(): ProviderResult<List<Track>>
}
