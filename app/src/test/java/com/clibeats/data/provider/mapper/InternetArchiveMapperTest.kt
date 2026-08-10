@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.IaFile
import com.clibeats.data.provider.dto.IaMetadata
import com.clibeats.data.provider.dto.IaMetadataResponse
import com.clibeats.data.provider.dto.IaSearchDoc
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class InternetArchiveMapperTest {
    @Test
    fun `isPlayableAudio accepts mp3 and rejects images and playlists`() {
        assertThat(IaFile(name = "a.mp3", format = "VBR MP3").isPlayableAudio()).isTrue()
        assertThat(IaFile(name = "a.mp3", format = "128Kbps MP3").isPlayableAudio()).isTrue()
        assertThat(IaFile(name = "a.flac", format = "FLAC").isPlayableAudio()).isTrue()
        assertThat(IaFile(name = "a.png", format = "PNG").isPlayableAudio()).isFalse()
        assertThat(IaFile(name = "a.pdf", format = "Text PDF").isPlayableAudio()).isFalse()
        assertThat(IaFile(name = "a.m3u", format = "M3U Playlist").isPlayableAudio()).isFalse()
        assertThat(IaFile(name = "a_spectrogram.png", format = "PNG").isPlayableAudio()).isFalse()
        assertThat(IaFile(name = "a.m4a", format = "MPEG-4 Audio").isPlayableAudio()).isTrue()
    }

    @Test
    fun `bestAudioFile prefers quality mp3 over low quality`() {
        val files =
            listOf(
                IaFile(name = "low.mp3", format = "64Kbps MP3", length = "100"),
                IaFile(name = "high.mp3", format = "VBR MP3", length = "100"),
                IaFile(name = "cover.jpg", format = "JPEG"),
            )

        assertThat(files.bestAudioFile()?.name).isEqualTo("high.mp3")
    }

    @Test
    fun `bestAudioFile returns null when no audio`() {
        val files = listOf(IaFile(name = "a.jpg", format = "JPEG"), IaFile(name = "b.pdf", format = "PDF"))
        assertThat(files.bestAudioFile()).isNull()
    }

    @Test
    fun `durationSeconds parses fractional length`() {
        assertThat(IaFile(length = "245.5").durationSeconds()).isEqualTo(245L)
        assertThat(IaFile(length = "abc").durationSeconds()).isNull()
    }

    @Test
    fun `creator as string or array both parse`() {
        assertThat(JsonPrimitive("Oasis").creatorName()).isEqualTo("Oasis")
        assertThat(Json.parseToJsonElement("[\"Oasis\",\"Noel\"]").creatorName()).isEqualTo("Oasis")
    }

    @Test
    fun `scoreIaItem prefers exact title and rejects items without audio`() {
        val doc =
            IaSearchDoc(
                identifier = "id1",
                title = "Wonderwall",
                creator = JsonPrimitive("Oasis"),
                mediatype = "audio",
            )
        val meta =
            IaMetadataResponse(
                metadata = IaMetadata(title = "Wonderwall", creator = JsonPrimitive("Oasis")),
                files = listOf(IaFile(name = "Wonderwall.mp3", format = "VBR MP3", length = "258")),
            )
        val noAudio =
            IaMetadataResponse(
                metadata = IaMetadata(title = "Wonderwall"),
                files = listOf(IaFile(name = "cover.jpg", format = "JPEG")),
            )

        assertThat(scoreIaItem(doc, meta, "wonderwall")).isGreaterThan(0)
        assertThat(scoreIaItem(doc, noAudio, "wonderwall")).isEqualTo(-1)
        assertThat(scoreIaItem(doc, meta, "unrelated")).isLessThan(scoreIaItem(doc, meta, "wonderwall"))
    }

    @Test
    fun `toDomainTrack builds composite id, artwork and encoded stream url`() {
        val doc = IaSearchDoc(identifier = "wonderwall-1995", collection = JsonPrimitive("opensource_audio"))
        val meta =
            IaMetadataResponse(
                metadata =
                    IaMetadata(
                        identifier = "wonderwall-1995",
                        title = "Wonderwall",
                        creator = JsonPrimitive("Oasis"),
                        date = "1995",
                    ),
                files = listOf(IaFile(name = "Wonderwall (1995).mp3", format = "VBR MP3", length = "258.0")),
            )

        val track = meta.toDomainTrack(doc)

        assertThat(track).isNotNull()
        track!!.let {
            assertThat(it.id).isEqualTo("internet_archive:wonderwall-1995")
            assertThat(it.title).isEqualTo("Wonderwall")
            assertThat(it.artist).isEqualTo("Oasis")
            assertThat(it.album).isEqualTo("opensource_audio")
            assertThat(it.durationMs).isEqualTo(258_000L)
            assertThat(it.artworkUrl).isEqualTo("https://archive.org/services/img/wonderwall-1995")
            assertThat(it.streamUrl)
                .isEqualTo("https://archive.org/download/wonderwall-1995/Wonderwall%20%281995%29.mp3")
            assertThat(it.providerId).isEqualTo("internet_archive")
        }
    }
}
