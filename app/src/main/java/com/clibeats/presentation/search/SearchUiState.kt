package com.clibeats.presentation.search

import com.clibeats.domain.model.Track

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Success(
        val tracks: List<Track>,
    ) : SearchUiState

    data class Error(
        val message: String,
    ) : SearchUiState
}
