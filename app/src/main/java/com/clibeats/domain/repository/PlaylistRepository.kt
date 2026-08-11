package com.clibeats.domain.repository

import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylistsAsFlow(): Flow<List<Playlist>>

    suspend fun getPlaylistById(id: String): Playlist?

    suspend fun upsertPlaylist(playlist: Playlist)

    suspend fun deletePlaylist(id: String)

    fun getSongsForPlaylistAsFlow(playlistId: String): Flow<List<Track>>

    suspend fun addSongToPlaylist(
        playlistId: String,
        songId: String,
        position: Int,
    )

    suspend fun removeSongFromPlaylist(
        playlistId: String,
        songId: String,
    )

    suspend fun reorderPlaylistSongs(
        playlistId: String,
        songIds: List<String>,
    )
}
