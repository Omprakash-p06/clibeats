package com.clibeats.presentation.search

import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var musicProvider: MusicProvider
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        musicProvider = mock()
        viewModel = SearchViewModel(musicProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.searchResults.value).isEqualTo(SearchUiState.Loading)
    }

    @Test
    fun `clearQuery resets query to empty string`() =
        runTest {
            viewModel.onQueryChange("test")
            viewModel.clearQuery()
            assertThat(viewModel.query.value).isEmpty()
        }

    @Test
    fun `search returns Success state on successful provider call`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.searchResults.collect()
            }
            val fakeTrack =
                Track(
                    id = "abc",
                    title = "Wonderwall",
                    artist = "Oasis",
                    album = "(What's The Story) Morning Glory?",
                    durationMs = 259_000L,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "youtube_music",
                )
            whenever(musicProvider.search(any(), any())).thenReturn(
                ProviderResult.Success(listOf(fakeTrack)),
            )

            viewModel.onQueryChange("Wonderwall")
            advanceTimeBy(400L)
            testDispatcher.scheduler.runCurrent()

            assertThat(viewModel.searchResults.value).isInstanceOf(SearchUiState.Success::class.java)
            val success = viewModel.searchResults.value as SearchUiState.Success
            assertThat(success.tracks).hasSize(1)
            assertThat(success.tracks[0].title).isEqualTo("Wonderwall")
        }

    @Test
    fun `search returns Error state on provider error`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.searchResults.collect()
            }
            whenever(musicProvider.search(any(), any())).thenReturn(
                ProviderResult.Error("Search failed"),
            )

            viewModel.onQueryChange("broken query")
            advanceTimeBy(400L)
            testDispatcher.scheduler.runCurrent()

            assertThat(viewModel.searchResults.value).isInstanceOf(SearchUiState.Error::class.java)
            val error = viewModel.searchResults.value as SearchUiState.Error
            assertThat(error.message).isEqualTo("Search failed")
        }
}
