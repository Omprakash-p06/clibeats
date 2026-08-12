package com.clibeats.presentation.player

import androidx.lifecycle.ViewModel
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.Track
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

        fun playTrack(track: Track) {
            playbackRepository.playTrack(track)
        }

        fun onPlayPauseClick() {
            if (playbackState.value.isPlaying) {
                playbackRepository.pause()
            } else {
                playbackRepository.play()
            }
        }

        fun onSkipNextClick() {
            playbackRepository.skipToNext()
        }

        fun onSkipPreviousClick() {
            playbackRepository.skipToPrevious()
        }

        fun seekTo(positionMs: Long) {
            playbackRepository.seekTo(positionMs)
        }
    }
