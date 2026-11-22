@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel
    @Inject
    constructor(
        private val playbackRepository: PlaybackRepository,
        private val appPreferences: AppPreferences? = null,
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

        val autoplayEnabled: StateFlow<Boolean> =
            appPreferences?.autoplayEnabled?.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = true,
            ) ?: MutableStateFlow(true)

        fun toggleAutoplay() {
            viewModelScope.launch {
                appPreferences?.setAutoplayEnabled(!autoplayEnabled.value)
            }
        }

        fun onMove(
            from: Int,
            to: Int,
        ) {
            playbackRepository.moveTrackInQueue(from, to)
        }

        fun onRemove(index: Int) {
            playbackRepository.removeFromQueue(index)
        }

        fun onClear() {
            playbackRepository.clearQueue()
        }

        fun onShuffle() {
            playbackRepository.toggleShuffle()
        }

        @Suppress("UnusedParameter")
        fun onTrackClick(
            track: Track,
            index: Int,
        ) {
            // Jump within the existing queue; re-setting the queue restarted every item.
            playbackRepository.skipToIndex(index)
        }
    }
