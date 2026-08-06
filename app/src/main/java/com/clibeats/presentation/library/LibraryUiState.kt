package com.clibeats.presentation.library

import com.clibeats.domain.model.Track

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data object Empty : LibraryUiState

    data class Success(
        val tracks: List<Track>,
        val artists: List<ArtistGroup>,
        val albums: List<AlbumGroup>,
    ) : LibraryUiState
}

data class ArtistGroup(
    val name: String,
    val trackCount: Int,
)

data class AlbumGroup(
    val title: String,
    val artist: String,
    val trackCount: Int,
)
