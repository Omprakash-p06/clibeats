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

    suspend fun playlists(): ProviderResult<List<Playlist>>

    suspend fun queue(): ProviderResult<List<Track>>
}
