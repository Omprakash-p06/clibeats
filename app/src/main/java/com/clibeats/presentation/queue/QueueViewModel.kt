package com.clibeats.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class QueueViewModel
    @Inject
    constructor(
        private val playbackRepository: PlaybackRepository,
    ) : ViewModel() {
        val uiState: StateFlow<QueueUiState> =
            combine(
                playbackRepository.queueState,
                playbackRepository.playbackState,
            ) { queue, state ->
                if (queue.isEmpty()) {
                    QueueUiState.Empty
                } else {
                    QueueUiState.Success(
                        tracks = queue,
                        currentTrackId = state.currentTrack?.id,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = QueueUiState.Empty,
            )

        fun onMove(from: Int, to: Int) {
            playbackRepository.moveTrackInQueue(from, to)
        }

        fun onRemove(index: Int) {
            playbackRepository.removeFromQueue(index)
        }

        fun onClear() {
            playbackRepository.clearQueue()
        }

        fun onTrackClick(track: Track, index: Int) {
            playbackRepository.setQueue(playbackRepository.queueState.value, index)
        }
    }
