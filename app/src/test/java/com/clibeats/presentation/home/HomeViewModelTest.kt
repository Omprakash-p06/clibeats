@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.home

import com.clibeats.data.network.NetworkMonitor
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.playback.StreamResolver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var musicProvider: MusicProvider
    private lateinit var providerRegistry: ProviderRegistry
    private lateinit var appPreferences: AppPreferences
    private lateinit var streamResolver: StreamResolver
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        musicProvider = mock()
        providerRegistry = mock()
        appPreferences = mock()
        streamResolver = mock()

        whenever(providerRegistry.getProvider(any())).thenReturn(musicProvider)
        whenever(providerRegistry.defaultProvider()).thenReturn(musicProvider)
        whenever(providerRegistry.providers).thenReturn(listOf(musicProvider))
        whenever(appPreferences.activeProviderId).thenReturn(flowOf("audius"))
        // Trending loads as soon as the ViewModel starts; tests that care stub their own result.
        runBlocking { whenever(musicProvider.trending(any())).thenReturn(ProviderResult.Success(emptyList())) }

        val networkMonitor: NetworkMonitor = mock()
        whenever(networkMonitor.isOnline).thenReturn(MutableStateFlow(true))

        viewModel = HomeViewModel(providerRegistry, appPreferences, networkMonitor, streamResolver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value).isEqualTo(HomeUiState.Loading)
    }

    @Test
    fun `trending success populates tracks`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            whenever(musicProvider.trending(any())).thenReturn(
                ProviderResult.Success(listOf(fakeTrack())),
            )

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(HomeUiState.Success::class.java)
            assertThat((state as HomeUiState.Success).tracks).hasSize(1)
            assertThat(state.tracks[0].title).isEqualTo("Trending Track")
        }

    @Test
    fun `trending error surfaces message`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            whenever(musicProvider.trending(any())).thenReturn(
                ProviderResult.Error("provider offline"),
            )

            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(HomeUiState.Error::class.java)
            assertThat((state as HomeUiState.Error).message).isEqualTo("provider offline")
        }

    @Test
    fun `trending loads once and only reloads on request`() =
        runTest {
            whenever(musicProvider.trending(any())).thenReturn(ProviderResult.Success(listOf(fakeTrack())))
            // Leaving Home and coming back re-subscribes to the state; that must not fetch again.
            repeat(2) {
                val job =
                    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
                testDispatcher.scheduler.advanceUntilIdle()
                job.cancel()
                testDispatcher.scheduler.advanceTimeBy(10_000)
            }
            verify(musicProvider, times(1)).trending(any())

            viewModel.refreshTrending()
            testDispatcher.scheduler.advanceUntilIdle()
            verify(musicProvider, times(2)).trending(any())
            assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Success::class.java)
        }

    @Test
    fun `a failed first load is retried`() =
        runTest {
            whenever(musicProvider.trending(any()))
                .thenReturn(ProviderResult.Error("no connection yet"))
                .thenReturn(ProviderResult.Success(listOf(fakeTrack())))

            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Success::class.java)
        }

    @Test
    fun `activeProviderName reflects selected provider`() =
        runTest {
            whenever(musicProvider.displayName).thenReturn("Audius")
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.activeProviderName.collect()
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.activeProviderName.value).isEqualTo("Audius")
        }

    private fun fakeTrack(): Track =
        Track(
            id = "audius:abc",
            title = "Trending Track",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            artworkUrl = null,
            streamUrl = "https://cdn.example/stream",
            providerId = "audius",
        )
}
