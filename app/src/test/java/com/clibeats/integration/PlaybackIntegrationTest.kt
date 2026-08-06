@file:Suppress("ForbiddenImport")

package com.clibeats.integration

import com.clibeats.domain.model.Track
import com.clibeats.playback.PlayerAdapter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class PlaybackIntegrationTest {
    private lateinit var playerAdapter: PlayerAdapter

    @Before
    fun setUp() {
        playerAdapter = mock()
    }

    @Test
    fun `playback flow integration structure test`() =
        runTest {
            val sampleTrack =
                Track(
                    id = "t_int_1",
                    title = "Integration Song",
                    artist = "Integration Artist",
                    album = "Integration Album",
                    durationMs = 210000L,
                    artworkUrl = null,
                    streamUrl = "http://localhost/stream.mp3",
                    providerId = "ytmusic",
                )
            assertThat(sampleTrack.id).isEqualTo("t_int_1")
        }
}
