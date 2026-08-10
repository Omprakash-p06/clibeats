package com.clibeats.presentation.home

import com.clibeats.domain.model.Track

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(val tracks: List<Track>) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
