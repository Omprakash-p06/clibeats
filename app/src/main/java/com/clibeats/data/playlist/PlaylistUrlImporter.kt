// ForbiddenImport: data-layer self-imports are legitimate
@file:Suppress("ForbiddenImport", "TooGenericExceptionCaught", "MagicNumber", "ReturnCount", "MaxLineLength")

package com.clibeats.data.playlist

import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.mapper.toEntity
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistUrlImporter
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val providerRegistry: ProviderRegistry,
        private val playlistRepository: PlaylistRepository,
        private val songDao: SongDao,
    ) {
        suspend fun importPlaylistFromUrl(url: String): Result<String> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val trimmed = url.trim()
                    when {
                        trimmed.contains("spotify.com") -> importSpotifyPlaylist(trimmed)
                        trimmed.contains("youtube.com") || trimmed.contains("youtu.be") ->
                            importYouTubePlaylist(trimmed)
                        else -> error("Unsupported playlist URL. Paste a Spotify or YouTube playlist link.")
                    }
                }
            }

        private suspend fun importSpotifyPlaylist(url: String): String {
            val playlistId = extractSpotifyPlaylistId(url) ?: error("Invalid Spotify playlist URL")
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder().url(embedUrl).build()
            val html =
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Failed to load Spotify playlist (HTTP ${response.code})")
                    response.body?.string().orEmpty()
                }
            val (title, queryList) = parseSpotifyEmbed(html)
            if (queryList.isEmpty()) error("No tracks found in Spotify playlist")

            val matchedTracks = resolveTrackQueries(queryList)
            if (matchedTracks.isEmpty()) error("No matching tracks could be found")

            songDao.upsertAll(matchedTracks.map { it.toEntity() })
            val songIds = matchedTracks.map { it.id }
            return playlistRepository.createAppPlaylist(
                name = title.ifBlank { "Imported Spotify Playlist" },
                songIds = songIds,
            )
        }

        private suspend fun importYouTubePlaylist(url: String): String {
            val listId = extractQueryParam(url, "list") ?: error("No playlist ID found in URL")
            val provider =
                providerRegistry.getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID)
                    ?: error("Default music provider unavailable")

            val result = provider.playlistTracks(listId)
            val tracks =
                (result as? ProviderResult.Success)?.data.orEmpty()
                    .distinctBy { it.id }
                    .take(MAX_IMPORT_TRACKS)
            if (tracks.isEmpty()) {
                val reason = (result as? ProviderResult.Error)?.message ?: "no tracks returned"
                error("Could not load YouTube playlist $listId: $reason")
            }

            songDao.upsertAll(tracks.map { it.toEntity() })
            val songIds = tracks.map { it.id }
            return playlistRepository.createAppPlaylist(
                name = "Imported YouTube Playlist",
                songIds = songIds,
            )
        }

        private fun parseSpotifyEmbed(html: String): Pair<String, List<String>> {
            val titleMatch = Regex("""<title>(.*?)</title>""").find(html)
            val title =
                titleMatch?.groupValues?.getOrNull(1)
                    ?.replace(" | Spotify", "")
                    ?.replace(" - playlist by", " by")
                    ?.trim()
                    .orEmpty()

            val nextDataRegex =
                Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""")
            val nextDataMatch = nextDataRegex.find(html)
            val queries = mutableListOf<String>()

            if (nextDataMatch != null) {
                runCatching {
                    val jsonStr = nextDataMatch.groupValues[1]
                    val root = JSONObject(jsonStr)
                    val props = root.optJSONObject("props")
                    val pageProps = props?.optJSONObject("pageProps")
                    val state = pageProps?.optJSONObject("state")
                    val data = state?.optJSONObject("data")
                    val entity = data?.optJSONObject("entity")
                    val trackList = entity?.optJSONObject("trackList")?.optJSONArray("items")
                    if (trackList != null) {
                        for (i in 0 until trackList.length()) {
                            val item = trackList.optJSONObject(i) ?: continue
                            val name = item.optString("title", "")
                            val subtitle = item.optString("subtitle", "")
                            if (name.isNotBlank()) {
                                queries.add("$name $subtitle".trim())
                            }
                        }
                    }
                }
            }

            if (queries.isEmpty()) {
                val rowMatches = Regex(""""title":"(.*?)","subtitle":"(.*?)"""").findAll(html)
                for (match in rowMatches) {
                    val name = match.groupValues.getOrNull(1).orEmpty()
                    val artist = match.groupValues.getOrNull(2).orEmpty()
                    if (name.isNotBlank()) queries.add("$name $artist".trim())
                }
            }

            return Pair(title, queries)
        }

        private suspend fun resolveTrackQueries(queries: List<String>): List<Track> =
            coroutineScope {
                val provider =
                    providerRegistry.getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID)
                        ?: return@coroutineScope emptyList()

                // YouTube throttles bursts: searching a whole playlist at once gets tracks dropped.
                val gate = Semaphore(MAX_CONCURRENT_SEARCHES)
                val deferreds =
                    queries.take(MAX_IMPORT_TRACKS).map { query ->
                        async(Dispatchers.IO) {
                            gate.withPermit {
                                runCatching {
                                    val res = provider.search(query, limit = 1)
                                    (res as? ProviderResult.Success)?.data?.firstOrNull()
                                }.getOrNull()
                            }
                        }
                    }

                deferreds.awaitAll().filterNotNull().distinctBy { it.id }
            }

        private fun extractSpotifyPlaylistId(url: String): String? {
            val uri = runCatching { URI.create(url) }.getOrNull() ?: return null
            val path = uri.path ?: return null
            val parts = path.split("/").filter { it.isNotBlank() }
            val index = parts.indexOf("playlist")
            return if (index != -1 && index + 1 < parts.size) {
                parts[index + 1].substringBefore("?")
            } else {
                null
            }
        }

        private fun extractQueryParam(
            url: String,
            key: String,
        ): String? {
            val uri = runCatching { URI.create(url) }.getOrNull() ?: return null
            val query = uri.query ?: return null
            return query.split("&")
                .map { it.split("=", limit = 2) }
                .firstOrNull { it.size == 2 && it[0] == key }
                ?.get(1)
        }

        private companion object {
            const val MAX_IMPORT_TRACKS = 50
            const val MAX_CONCURRENT_SEARCHES = 4
        }
    }
