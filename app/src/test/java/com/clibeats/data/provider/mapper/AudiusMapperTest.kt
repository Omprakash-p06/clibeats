@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.AudiusArtworkDto
import com.clibeats.data.provider.dto.AudiusStreamDto
import com.clibeats.data.provider.dto.AudiusTrackDto
import com.clibeats.data.provider.dto.AudiusUserDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudiusMapperTest {
    private fun sampleTrack() =
        AudiusTrackDto(
            id = "95wro",
            title = "Stars In The Sky - Lofi Beats",
            duration = 71L,
            genre = "Hip-Hop/Rap",
            isStreamable = true,
            user = AudiusUserDto(name = "Lofi Beats", handle = "lofibeats"),
            artwork =
                AudiusArtworkDto(
                    small = "https://cdn.example/150x150.jpg",
                    medium = "https://cdn.example/480x480.jpg",
                    large = "https://cdn.example/1000x1000.jpg",
                ),
            stream = AudiusStreamDto(url = "https://cdn.example/stream/abc?signature=x"),
        )

    @Test
    fun `toDomainTrack maps all fields`() {
        val track = sampleTrack().toDomainTrack()

        assertThat(track).isNotNull()
        track!!.let {
            assertThat(it.id).isEqualTo("audius:95wro")
            assertThat(it.title).isEqualTo("Stars In The Sky - Lofi Beats")
            assertThat(it.artist).isEqualTo("Lofi Beats")
            assertThat(it.album).isEqualTo("Hip-Hop/Rap")
            assertThat(it.durationMs).isEqualTo(71_000L)
            assertThat(it.artworkUrl).isEqualTo("https://cdn.example/1000x1000.jpg")
            assertThat(it.streamUrl).isEqualTo("https://cdn.example/stream/abc?signature=x")
            assertThat(it.providerId).isEqualTo("audius")
        }
    }

    @Test
    fun `toDomainTrack prefers highest resolution artwork`() {
        val track =
            sampleTrack().copy(
                artwork =
                    AudiusArtworkDto(
                        small = "https://cdn.example/150x150.jpg",
                        medium = "https://cdn.example/480x480.jpg",
                        large = null,
                    ),
            ).toDomainTrack()

        assertThat(track?.artworkUrl).isEqualTo("https://cdn.example/480x480.jpg")
    }

    @Test
    fun `toDomainTrack returns null when id is missing`() {
        assertThat(sampleTrack().copy(id = null).toDomainTrack()).isNull()
    }

    @Test
    fun `toDomainTrack returns null when stream url is missing`() {
        assertThat(sampleTrack().copy(stream = AudiusStreamDto(url = null)).toDomainTrack()).isNull()
    }

    @Test
    fun `toDomainTrack handles missing optional metadata`() {
        val track =
            AudiusTrackDto(
                id = "abc",
                title = "Untitled",
                duration = null,
                genre = null,
                user = null,
                artwork = null,
                stream = AudiusStreamDto(url = "https://cdn.example/stream"),
            ).toDomainTrack()

        assertThat(track).isNotNull()
        track!!.let {
            assertThat(it.id).isEqualTo("audius:abc")
            assertThat(it.artist).isEmpty()
            assertThat(it.album).isEmpty()
            assertThat(it.durationMs).isEqualTo(0L)
            assertThat(it.artworkUrl).isNull()
            assertThat(it.streamUrl).isEqualTo("https://cdn.example/stream")
        }
    }
}
