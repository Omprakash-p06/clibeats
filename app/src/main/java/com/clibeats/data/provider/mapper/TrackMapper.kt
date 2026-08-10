@file:Suppress(
    "ForbiddenImport",
    "ReturnCount",
    "LoopWithTooManyJumpStatements",
    "MagicNumber",
)

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.PlayerResponse
import com.clibeats.data.provider.dto.SearchResponse
import com.clibeats.domain.model.Track
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val PROVIDER_ID = "youtube_music"
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
private const val MS_PER_SECOND = 1000L

/**
 * Parse InnerTube search response into a list of Track domain objects.
 * Navigation through the JSON tree is fully null-safe.
 * Tracks missing a videoId are skipped.
 */
fun SearchResponse.toTrackList(): List<Track> {
    val contents = this.contents ?: return emptyList()

    // Navigate: contents → tabbedSearchResultsRenderer → tabs[0] → tabRenderer
    //          → content → sectionListRenderer → contents[*]
    //          → musicShelfRenderer → contents[*] → musicResponsiveListItemRenderer
    val tabs = contents.nav("tabbedSearchResultsRenderer", "tabs") ?: return emptyList()
    val tabContent =
        tabs.safeArray()?.getOrNull(0)
            ?.nav("tabRenderer", "content") ?: return emptyList()

    val shelves =
        tabContent.nav("sectionListRenderer", "contents")
            ?.safeArray() ?: return emptyList()

    val tracks = mutableListOf<Track>()
    for (shelf in shelves) {
        val items =
            shelf.nav("musicShelfRenderer", "contents")
                ?.safeArray() ?: continue

        for (item in items) {
            val renderer = item.nav("musicResponsiveListItemRenderer") ?: continue
            val track = renderer.parseTrack() ?: continue
            tracks.add(track)
        }
    }
    return tracks
}

private fun JsonElement.parseTrack(): Track? {
    val flexCols = this.nav("flexColumns")?.safeArray() ?: return null

    // Column 0: title + videoId
    val col0 =
        flexCols.getOrNull(0)
            ?.nav("musicResponsiveListItemFlexColumnRenderer", "text", "runs")
            ?.safeArray()?.getOrNull(0) ?: return null

    val title = col0.nav("text")?.safeString() ?: return null
    val videoId =
        this.nav("playlistItemData", "videoId")?.safeString()
            ?: this.nav("navigationEndpoint", "watchEndpoint", "videoId")?.safeString()
            ?: col0.nav("navigationEndpoint", "watchEndpoint", "videoId")?.safeString()
            ?: return null

    // Column 1: artist • album • duration (runs array)
    val col1Runs =
        flexCols.getOrNull(1)
            ?.nav("musicResponsiveListItemFlexColumnRenderer", "text", "runs")
            ?.safeArray() ?: emptyList()

    val artist = col1Runs.getOrNull(0)?.nav("text")?.safeString() ?: ""
    val album = col1Runs.getOrNull(2)?.nav("text")?.safeString() ?: ""
    val durationStr = col1Runs.lastOrNull()?.nav("text")?.safeString() ?: ""
    val durationMs = parseDurationMs(durationStr)

    // Thumbnail — pick highest resolution
    val artworkUrl =
        this.nav("thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails")
            ?.safeArray()
            ?.mapNotNull { it.nav("url")?.safeString() }
            ?.lastOrNull()

    return Track(
        id = videoId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        // streamUrl populated separately via /player
        streamUrl = null,
        providerId = PROVIDER_ID,
    )
}

/**
 * Parse duration string "4:19" → 259_000L ms. Returns 0 on parse failure.
 */
internal fun parseDurationMs(duration: String): Long {
    val parts = duration.trim().split(":").mapNotNull { it.toLongOrNull() }
    return when (parts.size) {
        2 -> (parts[0] * SECONDS_PER_MINUTE + parts[1]) * MS_PER_SECOND
        3 -> (parts[0] * SECONDS_PER_HOUR + parts[1] * SECONDS_PER_MINUTE + parts[2]) * MS_PER_SECOND
        else -> 0L
    }
}

/**
 * Extract the best audio stream URL from an InnerTube player response.
 * Looks for audio/mp4 or audio/webm formats.
 */
fun PlayerResponse.extractStreamUrl(): String? {
    val streamingData = this.streamingData ?: return null
    val formats =
        streamingData.nav("adaptiveFormats")?.safeArray()
            ?: streamingData.nav("formats")?.safeArray()
            ?: return null

    val audioFormats =
        formats.filter { item ->
            val mimeType = item.nav("mimeType")?.safeString() ?: ""
            mimeType.startsWith("audio/")
        }

    // Prefer mp4 audio, fall back to any audio format
    return audioFormats.firstOrNull { item ->
        item.nav("mimeType")?.safeString()?.contains("mp4") == true
    }?.nav("url")?.safeString()
        ?: audioFormats.firstOrNull()?.nav("url")?.safeString()
}

// ── Navigation helpers ─────────────────────────────────────────────────────

private fun JsonElement.nav(vararg keys: String): JsonElement? {
    var current: JsonElement = this
    for (key in keys) {
        current =
            when (current) {
                is JsonObject -> current.jsonObject[key] ?: return null
                else -> return null
            }
    }
    return current
}

private fun JsonElement.safeArray(): List<JsonElement>? = runCatching { this.jsonArray }.getOrNull()

private fun JsonElement.safeString(): String? = runCatching { this.jsonPrimitive.contentOrNull }.getOrNull()
