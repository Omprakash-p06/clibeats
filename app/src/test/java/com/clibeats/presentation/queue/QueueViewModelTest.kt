package com.clibeats.presentation.queue

import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var viewModel: QueueViewModel

    private val fakeQueueState = MutableStateFlow<List<Track>>(emptyList())
    private val fakePlaybackState =
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
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        playbackRepository = mock()
        whenever(playbackRepository.queueState).thenReturn(fakeQueueState)
        whenever(playbackRepository.playbackState).thenReturn(fakePlaybackState)
        viewModel = QueueViewModel(playbackRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is Empty when queue is empty`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            assertThat(viewModel.uiState.value).isEqualTo(QueueUiState.Empty)
        }

    @Test
    fun `uiState is Success when queue is non-empty`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            val track = Track("1", "Song 1", "Artist", "Album", 180000L, null, null, "local")
            fakeQueueState.value = listOf(track)
            testDispatcher.scheduler.runCurrent()

            assertThat(viewModel.uiState.value).isInstanceOf(QueueUiState.Success::class.java)
            val success = viewModel.uiState.value as QueueUiState.Success
            assertThat(success.tracks).hasSize(1)
        }

    @Test
    fun `onClear delegates to playbackRepository`() {
        viewModel.onClear()
        verify(playbackRepository).clearQueue()
    }

    @Test
    fun `onRemove delegates to playbackRepository`() {
        viewModel.onRemove(1)
        verify(playbackRepository).removeFromQueue(1)
    }

    @Test
    fun `onMove delegates to playbackRepository`() {
        viewModel.onMove(0, 2)
        verify(playbackRepository).moveTrackInQueue(0, 2)
    }
}
