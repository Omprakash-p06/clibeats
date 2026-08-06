package com.clibeats.presentation.queue

import com.clibeats.domain.model.Track

sealed interface QueueUiState {
    data object Empty : QueueUiState

    data class Success(
        val tracks: List<Track>,
        val currentTrackId: String?,
    ) : QueueUiState
}
