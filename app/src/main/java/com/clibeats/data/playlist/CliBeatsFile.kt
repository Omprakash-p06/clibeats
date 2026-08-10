package com.clibeats.data.playlist

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Portable playlist exchange format (`clibeats.json`).
 *
 * The format is provider-agnostic: tracks carry `providerId` + `sourceId` so a
 * new device can attempt provider resolution on import. No server required.
 */
@Serializable
data class CliBeatsFile(
    val version: Int = 1,
    val playlists: List<CliBeatsPlaylist> = emptyList(),
)

@Serializable
data class CliBeatsPlaylist(
    val name: String,
    val tracks: List<CliBeatsTrack> = emptyList(),
)

@Serializable
data class CliBeatsTrack(
    val providerId: String,
    val sourceId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val sourceUrl: String? = null,
)

/** Pure encode/decode — kept free of Android dependencies so it is unit-testable on the JVM. */
object CliBeatsFileCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    fun encode(file: CliBeatsFile): String = json.encodeToString(file)

    fun decode(raw: String): CliBeatsFile = json.decodeFromString(raw)
}
