@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.search

import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.PlaybackRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var musicProvider: MusicProvider
    private lateinit var providerRegistry: ProviderRegistry
    private lateinit var appPreferences: AppPreferences
    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        musicProvider = mock()
        providerRegistry = mock()
        appPreferences = mock()
        playbackRepository = mock()

        whenever(providerRegistry.getProvider(any())).thenReturn(musicProvider)
        whenever(providerRegistry.defaultProvider()).thenReturn(musicProvider)
        whenever(providerRegistry.providers).thenReturn(listOf(musicProvider))
        whenever(appPreferences.activeProviderId).thenReturn(flowOf("audius"))

        viewModel = SearchViewModel(providerRegistry, appPreferences, playbackRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertThat(viewModel.searchResults.value).isEqualTo(SearchUiState.Idle)
    }

    @Test
    fun `single char query stays Idle after debounce`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.searchResults.collect()
            }
            viewModel.onQueryChange("a")
            advanceTimeBy(500L)
            assertThat(viewModel.searchResults.value).isEqualTo(SearchUiState.Idle)
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
                    id = "internet_archive:wonderwall",
                    title = "Wonderwall",
                    artist = "Oasis",
                    album = "(What's The Story) Morning Glory?",
                    durationMs = 259_000L,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "internet_archive",
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

    @Test
    fun `onAddToQueue delegates to playbackRepository`() {
        val track = Track("1", "Title", "Artist", "Album", 180000L, null, null, "local")
        viewModel.onAddToQueue(track)
        verify(playbackRepository).addToQueue(track)
    }
}
