package com.clibeats.presentation.player

import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayerViewModelTest {
    private lateinit var repository: PlaybackRepository
    private lateinit var viewModel: PlayerViewModel
    private val stateFlow =
        MutableStateFlow(
            PlaybackState(
                currentTrack = null,
                isPlaying = false,
                positionMs = 0L,
                bufferedPositionMs = 0L,
                repeatMode = RepeatMode.OFF,
                shuffleEnabled = false,
            ),
        )

    @Before
    fun setup() {
        repository = mock()
        whenever(repository.playbackState).thenReturn(stateFlow)
        viewModel = PlayerViewModel(repository)
    }

    @Test
    fun onPlayPauseClick_whenNotPlaying_callsPlay() {
        stateFlow.value = stateFlow.value.copy(isPlaying = false)
        viewModel.onPlayPauseClick()
        verify(repository).play()
    }

    @Test
    fun onPlayPauseClick_whenPlaying_callsPause() {
        stateFlow.value = stateFlow.value.copy(isPlaying = true)
        viewModel.onPlayPauseClick()
        verify(repository).pause()
    }

    @Test
    fun onSkipNextClick_callsSkipNext() {
        viewModel.onSkipNextClick()
        verify(repository).skipToNext()
    }

    @Test
    fun onSkipPreviousClick_callsSkipPrevious() {
        viewModel.onSkipPreviousClick()
        verify(repository).skipToPrevious()
    }
}
