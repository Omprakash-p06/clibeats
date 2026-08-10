package com.clibeats.data.playlist

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CliBeatsFileCodecTest {
    @Test
    fun `encode then decode roundtrips`() {
        val file =
            CliBeatsFile(
                version = 1,
                playlists =
                    listOf(
                        CliBeatsPlaylist(
                            name = "Road Trip",
                            tracks =
                                listOf(
                                    CliBeatsTrack(
                                        providerId = "internet_archive",
                                        sourceId = "wonderwall-1995",
                                        title = "Wonderwall",
                                        artist = "Oasis",
                                        album = "Morning Glory",
                                        durationMs = 258_000L,
                                        artworkUrl = "https://archive.org/services/img/wonderwall-1995",
                                        sourceUrl = "https://archive.org/download/wonderwall-1995/Wonderwall.mp3",
                                    ),
                                ),
                        ),
                    ),
            )

        val decoded = CliBeatsFileCodec.decode(CliBeatsFileCodec.encode(file))

        assertThat(decoded.version).isEqualTo(1)
        assertThat(decoded.playlists).hasSize(1)
        val track = decoded.playlists[0].tracks[0]
        assertThat(track.providerId).isEqualTo("internet_archive")
        assertThat(track.sourceId).isEqualTo("wonderwall-1995")
        assertThat(track.title).isEqualTo("Wonderwall")
        assertThat(track.artist).isEqualTo("Oasis")
        assertThat(track.durationMs).isEqualTo(258_000L)
    }

    @Test
    fun `decode ignores unknown keys`() {
        val raw =
            """
            {"version":1,"future_field":true,"playlists":[{"name":"P","tracks":[{"providerId":"audius","sourceId":"abc","title":"T","artist":"A","album":"","durationMs":0,"extra":"x"}]}]}
            """.trimIndent()

        val decoded = CliBeatsFileCodec.decode(raw)

        assertThat(decoded.playlists[0].tracks[0].title).isEqualTo("T")
    }
}
