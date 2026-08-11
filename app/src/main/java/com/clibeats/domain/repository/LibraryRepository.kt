package com.clibeats.domain.repository

import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun getLikedSongs(): Flow<List<Track>>

    fun isLiked(trackId: String): Flow<Boolean>

    suspend fun toggleLike(track: Track)
}
