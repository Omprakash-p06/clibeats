package com.clibeats.domain.repository

import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllTracksAsFlow(): Flow<List<Track>>

    fun searchTracksAsFlow(query: String): Flow<List<Track>>

    suspend fun getTrackById(id: String): Track?

    suspend fun upsertTrack(track: Track)

    suspend fun upsertTracks(tracks: List<Track>)

    suspend fun deleteTrack(id: String)
}
