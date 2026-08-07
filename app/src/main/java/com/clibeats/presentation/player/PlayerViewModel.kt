package com.clibeats.presentation.player

import androidx.lifecycle.ViewModel
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playbackRepository: PlaybackRepository,
    ) : ViewModel() {
        val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

        fun onPlayPauseClick() {
            val state = playbackState.value
            if (state.isPlaying) {
                playbackRepository.pause()
            } else {
                val currentTrack = state.currentTrack
                if (currentTrack != null) {
                    playbackRepository.playTrack(currentTrack)
                } else {
                    playbackRepository.play()
                }
            }
        }

        fun onSkipNextClick() {
            playbackRepository.skipToNext()
        }

        fun onSkipPreviousClick() {
            playbackRepository.skipToPrevious()
        }

        fun playTrack(track: com.clibeats.domain.model.Track) {
            playbackRepository.playTrack(track)
        }
    }
