@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.QueueDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.playback.PlayerAdapter
import com.clibeats.playback.StreamResolver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRepositoryImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var streamResolver: StreamResolver
    private lateinit var queueDao: QueueDao
    private lateinit var songDao: SongDao
    private lateinit var appPreferences: AppPreferences
    private lateinit var repository: PlaybackRepositoryImpl

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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        playerAdapter = mock()
        streamResolver = mock()
        queueDao = mock()
        songDao = mock()
        appPreferences = mock()

        whenever(playerAdapter.queueFlow).thenReturn(queueFlow)
        whenever(playerAdapter.playbackState).thenReturn(playbackStateFlow)

        whenever(appPreferences.lastQueueIndex).thenReturn(flowOf(0))
        whenever(appPreferences.lastPlaybackPosition).thenReturn(flowOf(0L))
        whenever(appPreferences.savedRepeatMode).thenReturn(flowOf("OFF"))
        whenever(appPreferences.savedShuffleEnabled).thenReturn(flowOf(false))

        repository =
            PlaybackRepositoryImpl(
                playerAdapter,
                streamResolver,
                queueDao,
                songDao,
                appPreferences,
                testDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playbackState delegates to playerAdapter`() {
        assertThat(repository.playbackState.value).isEqualTo(playbackStateFlow.value)
    }

    @Test
    fun `play delegates to playerAdapter`() {
        repository.play()
        verify(playerAdapter).play()
    }

    @Test
    fun `pause delegates to playerAdapter`() {
        repository.pause()
        verify(playerAdapter).pause()
    }

    @Test
    fun `seekTo delegates to playerAdapter`() {
        repository.seekTo(1500L)
        verify(playerAdapter).seekTo(1500L)
    }

    @Test
    fun `restores queue when saved songs exist`() =
        runTest(testDispatcher) {
            val entity =
                SongEntity(
                    id = "s1",
                    title = "Restored Track",
                    artist = "Artist",
                    album = "Album",
                    durationMs = 200000L,
                    artworkUrl = null,
                    streamUrl = "http://stream/s1",
                    providerId = "ytmusic",
                )
            whenever(queueDao.getQueueSongs()).thenReturn(listOf(entity))

            PlaybackRepositoryImpl(
                playerAdapter,
                streamResolver,
                queueDao,
                songDao,
                appPreferences,
                testDispatcher,
            )

            verify(playerAdapter).restoreQueue(
                tracks = any(),
                startIndex = eq(0),
                positionMs = eq(0L),
                repeatMode = eq(RepeatMode.OFF),
                shuffleEnabled = eq(false),
            )
        }

    @Test
    fun `removeFromQueue delegates to playerAdapter`() {
        repository.removeFromQueue(2)
        verify(playerAdapter).removeFromQueue(2)
    }

    @Test
    fun `clearQueue delegates to playerAdapter`() {
        repository.clearQueue()
        verify(playerAdapter).clearQueue()
    }
}
