package com.clibeats.data.gateway.mapper

import com.clibeats.data.gateway.dto.GatewayTrackDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GatewayMapperTest {
    @Test
    fun `toDomainTrack maps gateway fields to domain Track`() {
        val dto =
            GatewayTrackDto(
                id = "vid1",
                providerId = "youtube",
                title = "Song Title",
                artist = "Artist",
                album = "Album",
                durationSeconds = 259,
                artworkUrl = "https://art.example/t.jpg",
                explicit = true,
            )

        val track = dto.toDomainTrack()

        assertThat(track.id).isEqualTo("vid1")
        assertThat(track.providerId).isEqualTo("youtube")
        assertThat(track.title).isEqualTo("Song Title")
        assertThat(track.artist).isEqualTo("Artist")
        assertThat(track.album).isEqualTo("Album")
        assertThat(track.durationMs).isEqualTo(259_000L)
        assertThat(track.artworkUrl).isEqualTo("https://art.example/t.jpg")
        assertThat(track.streamUrl).isNull()
    }

    @Test
    fun `maps list of dtos and preserves limit semantics`() {
        val dtos =
            listOf(
                GatewayTrackDto(id = "a", providerId = "youtube", title = "A", artist = "X", durationSeconds = 100),
                GatewayTrackDto(id = "b", providerId = "youtube", title = "B", artist = "Y", durationSeconds = 200),
            )

        assertThat(dtos.toDomainTracks()).hasSize(2)
        assertThat(dtos.toDomainTracks().map { it.id }).containsExactly("a", "b").inOrder()
    }
}
