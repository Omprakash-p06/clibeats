package com.clibeats.presentation.library

import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.domain.repository.SongRepository
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var songRepository: SongRepository
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var viewModel: LibraryViewModel

    private val fakeTracksFlow = MutableStateFlow<List<Track>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        songRepository = mock()
        playbackRepository = mock()
        whenever(songRepository.getAllTracksAsFlow()).thenReturn(fakeTracksFlow)
        viewModel = LibraryViewModel(songRepository, playbackRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState is Empty when no tracks in repository`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            fakeTracksFlow.value = emptyList()
            testDispatcher.scheduler.runCurrent()

            assertThat(viewModel.uiState.value).isEqualTo(LibraryUiState.Empty)
        }

    @Test
    fun `uiState is Success with grouped artists and albums when tracks exist`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            val t1 = Track("1", "Song 1", "Artist A", "Album X", 180000L, null, null, "local")
            val t2 = Track("2", "Song 2", "Artist A", "Album Y", 200000L, null, null, "local")
            fakeTracksFlow.value = listOf(t1, t2)
            testDispatcher.scheduler.runCurrent()

            assertThat(viewModel.uiState.value).isInstanceOf(LibraryUiState.Success::class.java)
            val success = viewModel.uiState.value as LibraryUiState.Success
            assertThat(success.tracks).hasSize(2)
            assertThat(success.artists).hasSize(1)
            assertThat(success.artists[0].name).isEqualTo("Artist A")
            assertThat(success.artists[0].trackCount).isEqualTo(2)
            assertThat(success.albums).hasSize(2)
        }
}
