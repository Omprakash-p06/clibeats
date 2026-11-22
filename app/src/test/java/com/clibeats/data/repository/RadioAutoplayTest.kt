@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.QueueDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.network.NetworkMonitor
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.HistoryRepository
import com.clibeats.playback.PlayerAdapter
import com.clibeats.playback.StreamResolver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RadioAutoplayTest {
    private val testDispatcher = StandardTestDispatcher()

    private val playerAdapter: PlayerAdapter = mock()
    private val streamResolver: StreamResolver = mock()
    private val queueDao: QueueDao = mock()
    private val songDao: SongDao = mock()
    private val appPreferences: AppPreferences = mock()
    private val historyRepository: HistoryRepository = mock()
    private val providerRegistry: ProviderRegistry = mock()
    private val networkMonitor: NetworkMonitor = mock()
    private val musicProvider: MusicProvider = mock()

    private val queueFlow = MutableStateFlow<List<Track>>(emptyList())
    private val playbackStateFlow =
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
    private val isOnlineFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(playerAdapter.queueFlow).thenReturn(queueFlow)
        whenever(playerAdapter.playbackState).thenReturn(playbackStateFlow)
        whenever(networkMonitor.isOnline).thenReturn(isOnlineFlow)

        whenever(appPreferences.lastQueueIndex).thenReturn(flowOf(0))
        whenever(appPreferences.lastPlaybackPosition).thenReturn(flowOf(0L))
        whenever(appPreferences.savedRepeatMode).thenReturn(flowOf("OFF"))
        whenever(appPreferences.savedShuffleEnabled).thenReturn(flowOf(false))
        whenever(appPreferences.autoplayEnabled).thenReturn(flowOf(true))

        whenever(providerRegistry.getProvider(any())).thenReturn(musicProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `autoplay triggers when reaching second to last track in queue`() =
        runTest(testDispatcher) {
            val track1 = testTrack("1")
            val track2 = testTrack("2")
            val relatedTrack = testTrack("3")

            queueFlow.value = listOf(track1, track2)
            whenever(musicProvider.relatedTracks("1", 15))
                .thenReturn(ProviderResult.Success(listOf(relatedTrack)))
            whenever(historyRepository.getRecentlyPlayedTracks(25))
                .thenReturn(flowOf(emptyList()))

            val repo =
                PlaybackRepositoryImpl(
                    playerAdapter = playerAdapter,
                    streamResolver = streamResolver,
                    queueDao = queueDao,
                    songDao = songDao,
                    appPreferences = appPreferences,
                    ioDispatcher = testDispatcher,
                    historyRepository = historyRepository,
                    providerRegistry = providerRegistry,
                    networkMonitor = networkMonitor,
                )

            playbackStateFlow.value =
                playbackStateFlow.value.copy(currentTrack = track1)

            advanceUntilIdle()

            assertThat(repo).isNotNull()
            verify(playerAdapter).addAllToQueue(listOf(relatedTrack))
        }

    @Test
    fun `autoplay does not trigger when autoplay is disabled`() =
        runTest(testDispatcher) {
            whenever(appPreferences.autoplayEnabled).thenReturn(flowOf(false))
            val track1 = testTrack("1")
            val track2 = testTrack("2")
            queueFlow.value = listOf(track1, track2)

            val repo =
                PlaybackRepositoryImpl(
                    playerAdapter = playerAdapter,
                    streamResolver = streamResolver,
                    queueDao = queueDao,
                    songDao = songDao,
                    appPreferences = appPreferences,
                    ioDispatcher = testDispatcher,
                    historyRepository = historyRepository,
                    providerRegistry = providerRegistry,
                    networkMonitor = networkMonitor,
                )

            playbackStateFlow.value =
                playbackStateFlow.value.copy(currentTrack = track1)

            advanceUntilIdle()

            assertThat(repo).isNotNull()
            verify(playerAdapter, never()).addAllToQueue(any())
        }

    private fun testTrack(id: String) =
        Track(
            id = id,
            title = "Track $id",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            artworkUrl = null,
            streamUrl = null,
            providerId = "youtube_music",
        )
}
