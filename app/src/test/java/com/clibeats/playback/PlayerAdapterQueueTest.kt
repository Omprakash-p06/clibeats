package com.clibeats.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayerAdapterQueueTest {
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var adapter: PlayerAdapter

    @Before
    fun setup() {
        exoPlayer = mock()
        whenever(exoPlayer.currentPosition).thenReturn(0L)
        whenever(exoPlayer.bufferedPosition).thenReturn(0L)
        whenever(exoPlayer.isPlaying).thenReturn(false)
        adapter = PlayerAdapter(exoPlayer)
    }

    private fun track(id: String = "t1") =
        Track(
            id = id,
            title = "Title $id",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            artworkUrl = null,
            streamUrl = null,
            providerId = "local",
        )

    @Test
    fun setQueue_buildsMediaItemsFromTracksInOrder() {
        val tracks = listOf(track("t1"), track("t2"))
        val mediaItems = argumentCaptor<List<MediaItem>>()

        adapter.setQueue(tracks)

        verify(exoPlayer).setMediaItems(mediaItems.capture(), eq(0), eq(0L))
        verify(exoPlayer).prepare()
        verify(exoPlayer).play()
        assertEquals(listOf("t1", "t2"), mediaItems.firstValue.map { it.mediaId })
    }

    @Test
    fun setQueue_usesStartIndex() {
        val tracks = listOf(track("t1"), track("t2"), track("t3"))

        adapter.setQueue(tracks, startIndex = 2)

        verify(exoPlayer).setMediaItems(any<List<MediaItem>>(), eq(2), eq(0L))
    }

    @Test
    fun setQueue_updatesCurrentTrackFromPlayerIndex() {
        whenever(exoPlayer.currentMediaItemIndex).thenReturn(1)
        val listener = argumentCaptor<Player.Listener>()
        verify(exoPlayer).addListener(listener.capture())
        adapter.setQueue(listOf(track("t1"), track("t2")))

        listener.firstValue.onMediaItemTransition(null, 0)

        assertEquals("t2", adapter.playbackState.value.currentTrack?.id)
    }

    @Test
    fun playTrack_setsSingleMediaItemAndPlays() {
        adapter.playTrack(track("t1"))

        verify(exoPlayer).setMediaItem(any())
        verify(exoPlayer).prepare()
        verify(exoPlayer).play()
    }

    @Test
    fun skipToNext_whenHasNext_seeksToNext() {
        whenever(exoPlayer.hasNextMediaItem()).thenReturn(true)

        adapter.skipToNext()

        verify(exoPlayer).seekToNextMediaItem()
    }

    @Test
    fun skipToNext_whenNoNext_doesNothing() {
        whenever(exoPlayer.hasNextMediaItem()).thenReturn(false)

        adapter.skipToNext()

        verify(exoPlayer, never()).seekToNextMediaItem()
    }

    @Test
    fun skipToPrevious_whenHasPrevious_seeksToPrevious() {
        whenever(exoPlayer.hasPreviousMediaItem()).thenReturn(true)

        adapter.skipToPrevious()

        verify(exoPlayer).seekToPreviousMediaItem()
    }

    @Test
    fun skipToPrevious_whenNoPrevious_doesNothing() {
        whenever(exoPlayer.hasPreviousMediaItem()).thenReturn(false)

        adapter.skipToPrevious()

        verify(exoPlayer, never()).seekToPreviousMediaItem()
    }

    @Test
    fun setRepeatMode_all_mapsToPlayerAll() {
        adapter.setRepeatMode(RepeatMode.ALL)

        verify(exoPlayer).setRepeatMode(Player.REPEAT_MODE_ALL)
    }

    @Test
    fun setRepeatMode_updatesStateFromPlayer() {
        whenever(exoPlayer.repeatMode).thenReturn(Player.REPEAT_MODE_ONE)

        adapter.setRepeatMode(RepeatMode.ONE)

        assertEquals(RepeatMode.ONE, adapter.playbackState.value.repeatMode)
    }

    @Test
    fun toggleShuffle_flipsPlayerShuffleMode() {
        whenever(exoPlayer.shuffleModeEnabled).thenReturn(true)

        adapter.toggleShuffle()

        verify(exoPlayer).setShuffleModeEnabled(false)
    }

    @Test
    fun toggleShuffle_updatesStateFromPlayer() {
        whenever(exoPlayer.shuffleModeEnabled).thenReturn(true)

        adapter.toggleShuffle()

        assertEquals(true, adapter.playbackState.value.shuffleEnabled)
    }
}
