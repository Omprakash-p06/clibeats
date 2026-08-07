package com.clibeats.domain.playback

import com.clibeats.domain.model.Track
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class QueueManagerTest {
    private lateinit var queueManager: QueueManager

    @Before
    fun setUp() {
        queueManager = QueueManager()
    }

    @Test
    fun `setQueue updates queue flow and current track`() {
        val tracks =
            listOf(
                Track(
                    id = "1",
                    title = "Track 1",
                    artist = "Artist 1",
                    album = "Album 1",
                    durationMs = 1000,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "test",
                ),
                Track(
                    id = "2",
                    title = "Track 2",
                    artist = "Artist 2",
                    album = "Album 2",
                    durationMs = 2000,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "test",
                ),
            )
        queueManager.setQueue(tracks, 0)

        assertThat(queueManager.queue.value).hasSize(2)
        assertThat(queueManager.currentTrack()?.id).isEqualTo("1")
    }

    @Test
    fun `nextTrack advances queue to next song`() {
        val tracks =
            listOf(
                Track(
                    id = "1",
                    title = "Track 1",
                    artist = "Artist 1",
                    album = "Album 1",
                    durationMs = 1000,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "test",
                ),
                Track(
                    id = "2",
                    title = "Track 2",
                    artist = "Artist 2",
                    album = "Album 2",
                    durationMs = 2000,
                    artworkUrl = null,
                    streamUrl = null,
                    providerId = "test",
                ),
            )
        queueManager.setQueue(tracks, 0)

        val next = queueManager.nextTrack()
        assertThat(next?.id).isEqualTo("2")
    }
}
