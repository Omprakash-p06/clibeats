@file:Suppress(
    "ForbiddenImport",
    "ReturnCount",
    "LoopWithTooManyJumpStatements",
    "MagicNumber",
)

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.BrowseResponse
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

/**
 * The chart playlist to show as "trending" from a Charts page: the daily top music videos for the country
 * (real songs), else the top 100, else the trending list, else the first chart playlist. Returns its
 * browse id (`VL...`).
 */
fun BrowseResponse.chartPlaylistBrowseId(): String? {
    val playlists =
        findAll(contents ?: return null, "musicTwoRowItemRenderer").mapNotNull { item ->
            val title = item.nav("title", "runs")?.safeArray()?.getOrNull(0)?.nav("text")?.safeString()
            val id = item.nav("navigationEndpoint", "browseEndpoint", "browseId")?.safeString()
            if (title != null && id != null && id.startsWith("VL")) title to id else null
        }
    val preferences = listOf("daily top music videos", "top 100 music videos", "top songs", "trending")
    return preferences.firstNotNullOfOrNull { wanted ->
        playlists.firstOrNull { (title, _) -> title.lowercase().startsWith(wanted) }?.second
    } ?: playlists.firstOrNull()?.second
}

/** Songs of a playlist page (e.g. a chart), in order; items without a video id are skipped. */
fun BrowseResponse.playlistTracks(): List<Track> {
    val root = contents ?: return emptyList()
    return findAll(root, "musicResponsiveListItemRenderer")
        .mapNotNull { it.parseTrack() }
        .distinctBy { it.id }
}

/**
 * Parse InnerTube next (radio) response into a list of Track domain objects.
 * Looks for playlistPanelVideoRenderer items.
 */
fun JsonElement.toRadioTrackList(): List<Track> {
    return findAll(this, "playlistPanelVideoRenderer").mapNotNull { item ->
        val videoId = item.nav("videoId")?.safeString() ?: return@mapNotNull null
        val title =
            item.nav("title", "runs")?.safeArray()?.firstOrNull()?.nav("text")?.safeString()
                ?: item.nav("title", "simpleText")?.safeString()
                ?: return@mapNotNull null
        val artist =
            item.nav("shortBylineText", "runs")?.safeArray()?.firstOrNull()?.nav("text")?.safeString()
                ?: item.nav("longBylineText", "runs")?.safeArray()?.firstOrNull()?.nav("text")?.safeString()
                ?: "Unknown Artist"
        val durationText =
            item.nav("lengthText", "runs")?.safeArray()?.firstOrNull()?.nav("text")?.safeString()
                ?: item.nav("lengthText", "simpleText")?.safeString()
                ?: ""
        val thumbnails = item.nav("thumbnail", "thumbnails")?.safeArray()
        val artworkUrl = thumbnails?.lastOrNull()?.nav("url")?.safeString()

        Track(
            id = "$PROVIDER_ID:$videoId",
            title = title,
            artist = artist,
            album = "",
            durationMs = parseDurationMs(durationText),
            artworkUrl = artworkUrl,
            streamUrl = null,
            providerId = PROVIDER_ID,
        )
    }.distinctBy { it.id }
}

/** Every value stored under [key] anywhere in the tree, in document order. */
private fun findAll(
    root: JsonElement,
    key: String,
): List<JsonElement> {
    val found = mutableListOf<JsonElement>()

    fun walk(element: JsonElement) {
        when (element) {
            is JsonObject ->
                element.forEach { (k, v) ->
                    if (k == key) found += v else walk(v)
                }
            is kotlinx.serialization.json.JsonArray -> element.forEach(::walk)
            else -> Unit
        }
    }
    walk(root)
    return found
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
    // Playlists (charts) put the duration in a fixed column instead of the artist line.
    val fixedDuration =
        this.nav("fixedColumns")?.safeArray()?.getOrNull(0)
            ?.nav("musicResponsiveListItemFixedColumnRenderer", "text", "runs")
            ?.safeArray()?.getOrNull(0)?.nav("text")?.safeString()
    val durationMs = parseDurationMs(durationStr).takeIf { it > 0 } ?: parseDurationMs(fixedDuration ?: "")

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
