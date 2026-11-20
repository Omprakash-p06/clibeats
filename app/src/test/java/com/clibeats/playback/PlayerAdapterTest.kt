@file:Suppress("ForbiddenImport")

package com.clibeats.playback

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.clibeats.domain.model.AudioDeviceType
import com.clibeats.domain.model.AudioOutputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayerAdapterTest {
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var cacheManager: StreamResolver
    private lateinit var audioOutputMonitor: AudioOutputMonitor
    private lateinit var adapter: PlayerAdapter

    @Before
    fun setup() {
        exoPlayer = mock()
        cacheManager = mock()
        audioOutputMonitor = mock()
        whenever(exoPlayer.currentPosition).thenReturn(0L)
        whenever(exoPlayer.bufferedPosition).thenReturn(0L)
        whenever(exoPlayer.isPlaying).thenReturn(false)
        whenever(audioOutputMonitor.currentDevice).thenReturn(
            MutableStateFlow(AudioOutputDevice("Phone speaker", AudioDeviceType.PHONE_SPEAKER)),
        )
        adapter = PlayerAdapter(exoPlayer, cacheManager, mock<Context>(), audioOutputMonitor)
    }

    @Test
    fun initialState_isNotPlayingAndNullTrack() =
        runTest {
            val state = adapter.playbackState.first()
            assertFalse(state.isPlaying)
            assertEquals(null, state.currentTrack)
        }

    @Test
    fun play_delegatesToExoPlayer() {
        adapter.play()
        verify(exoPlayer).play()
    }

    @Test
    fun pause_delegatesToExoPlayer() {
        adapter.pause()
        verify(exoPlayer).pause()
    }

    @Test
    fun seekTo_delegatesToExoPlayer() {
        adapter.seekTo(5000L)
        verify(exoPlayer).seekTo(5000L)
    }
}
