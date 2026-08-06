package com.clibeats.presentation.playlist

import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data class Success(
        val playlists: List<Playlist>,
        val selectedPlaylist: Playlist? = null,
        val selectedPlaylistTracks: List<Track> = emptyList(),
    ) : PlaylistUiState
}
