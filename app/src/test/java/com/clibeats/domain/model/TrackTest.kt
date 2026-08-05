package com.clibeats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TrackTest {
    @Test
    fun track_construction_succeeds_with_valid_fields() {
        val track =
            Track(
                id = "track_1",
                title = "Midnight Sun",
                artist = "Vaporwave",
                album = "Neon Lights",
                durationMs = 210000L,
                artworkUrl = "https://example.com/art.jpg",
                streamUrl = "https://example.com/audio.mp3",
                providerId = "local",
            )

        assertEquals("track_1", track.id)
        assertEquals("Midnight Sun", track.title)
        assertEquals("Vaporwave", track.artist)
        assertEquals(210000L, track.durationMs)
    }

    @Test
    fun track_equality_is_structural() {
        val track1 = Track("1", "T", "A", "Al", 100L, null, null, "p")
        val track2 = Track("1", "T", "A", "Al", 100L, null, null, "p")

        assertEquals(track1, track2)
    }

    @Test
    fun track_copy_updates_single_field() {
        val original = Track("1", "Title", "Artist", "Album", 100L, null, null, "p")
        val updated = original.copy(title = "New Title")

        assertEquals("New Title", updated.title)
        assertEquals("Artist", updated.artist)
    }

    @Test
    fun playbackState_defaults_to_not_playing() {
        val state =
            PlaybackState(
                currentTrack = null,
                isPlaying = false,
                positionMs = 0L,
                bufferedPositionMs = 0L,
                repeatMode = RepeatMode.OFF,
                shuffleEnabled = false,
            )

        assertNull(state.currentTrack)
        assertFalse(state.isPlaying)
        assertEquals(RepeatMode.OFF, state.repeatMode)
    }
}
