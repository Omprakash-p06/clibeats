@file:Suppress(
    "ForbiddenImport",
    "ReturnCount",
    "LoopWithTooManyJumpStatements",
    "LongMethod",
    "MaxLineLength",
    "CyclomaticComplexMethod",
)

package com.clibeats.data.provider

import com.clibeats.data.preferences.AppPreferences
import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.NextRequest
import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.dto.SearchRequest
import com.clibeats.data.provider.mapper.chartPlaylistBrowseId
import com.clibeats.data.provider.mapper.playlistTracks
import com.clibeats.data.provider.mapper.toRadioTrackList
import com.clibeats.data.provider.mapper.toTrackList
import com.clibeats.data.provider.youtube.ExtractedStreamInfo
import com.clibeats.data.provider.youtube.NewPipeExtractorResolver
import com.clibeats.data.provider.youtube.PoTokenGenerator
import com.clibeats.data.provider.youtube.PoTokenResult
import com.clibeats.data.provider.youtube.StreamCacheManager
import com.clibeats.data.provider.youtube.StreamUrlDeobfuscator
import com.clibeats.data.provider.youtube.YouTubeClientConfig
import com.clibeats.data.provider.youtube.YouTubeClientStrategy
import com.clibeats.domain.model.PlaybackException
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderId
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.util.DiagnosticLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicProvider
    @Inject
    constructor(
        private val api: InnerTubeApi,
        private val poTokenGenerator: PoTokenGenerator,
        private val streamCacheManager: StreamCacheManager,
        private val newPipeExtractorResolver: NewPipeExtractorResolver,
        private val appPreferences: AppPreferences,
    ) : MusicProvider {
        override val providerId: String = "youtube_music"
        override val displayName: String = "YouTube Music"

        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> {
            val traceId = DiagnosticLogger.generateTraceId()
            DiagnosticLogger.logSearchRequest(traceId, query)

            return runCatching {
                val response = api.search(SearchRequest.forQuery(query))
                val tracks = response.toTrackList().take(limit)
                DiagnosticLogger.logSearchResponse(traceId, tracks.size)
                ProviderResult.Success(tracks)
            }.getOrElse { e ->
                DiagnosticLogger.logError(traceId, "SEARCH_FAILED", e.message ?: "Search request failed")
                ProviderResult.Error(
                    message = e.message ?: "Search failed",
                    cause = e,
                )
            }
        }

        /**
         * The country's chart from YouTube Music's Charts page (daily top music videos, where the phone is),
         * falling back to a "top songs <country>" search when charts aren't available there.
         */
        override suspend fun trending(limit: Int): ProviderResult<List<Track>> {
            val country = com.clibeats.data.provider.dto.UserLocale.gl
            val chart =
                runCatching {
                    val playlistId =
                        api.browse(com.clibeats.data.provider.dto.BrowseRequest.charts(country))
                            .chartPlaylistBrowseId()
                    playlistId?.let {
                        api.browse(com.clibeats.data.provider.dto.BrowseRequest.page(it)).playlistTracks()
                    }
                }.getOrNull()
            if (!chart.isNullOrEmpty()) return ProviderResult.Success(chart.take(limit))
            val name = com.clibeats.data.provider.dto.UserLocale.countryName
            return search(if (name.isBlank()) "trending music" else "top songs $name", limit)
        }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> {
            val rawId = ProviderId.rawSourceId(providerId, trackId)
            val searchRes = search(rawId, 1)
            if (searchRes is ProviderResult.Success && searchRes.data.isNotEmpty()) {
                return ProviderResult.Success(searchRes.data.first())
            }
            return ProviderResult.Error("Track not found: $trackId")
        }

        override suspend fun stream(trackId: String): ProviderResult<String> {
            val rawId = ProviderId.rawSourceId(providerId, trackId)
            val traceId = DiagnosticLogger.generateTraceId()
            DiagnosticLogger.logStreamResolutionStarted(traceId, rawId)

            // 1. Check cache
            val cached = streamCacheManager.get(providerId, rawId)
            if (cached != null) {
                DiagnosticLogger.logStreamUrlResolved(
                    traceId = traceId,
                    host = cached.host,
                    itag = cached.itag,
                    mimeType = cached.mimeType,
                    expiresAtMs = cached.expiresAtMs,
                )
                return ProviderResult.Success(cached.url)
            }

            // 2. Fast path: direct InnerTube /player over the warm HTTP/2 pool. No PO token and a
            // short timeout, because most plays resolve here in ~200-300 ms.
            val highQuality = runCatching { appPreferences.highQualityStreaming.first() }.getOrDefault(true)
            val fast =
                resolveWithInnerTube(
                    rawId = rawId,
                    traceId = traceId,
                    highQuality = highQuality,
                    clients = listOf(YouTubeClientStrategy.PRIMARY),
                    timeoutMs = FAST_PATH_TIMEOUT_MS,
                    poTokenResult = null,
                )
            fast.info?.let { extracted ->
                logAndCache(traceId, rawId, extracted)
                return ProviderResult.Success(extracted.url)
            }

            // 3. Fallback: NewPipeExtractor (watch page + player JS) when InnerTube is gated.
            val newPipeExtracted = newPipeExtractorResolver.resolveStream(rawId, traceId)
            if (newPipeExtracted != null) {
                logAndCache(traceId, rawId, newPipeExtracted)
                return ProviderResult.Success(newPipeExtracted.url)
            }

            // 4. Last resort: the full InnerTube client chain with a PO token.
            val poTokenResult = poTokenGenerator.getPoToken(traceId, rawId)
            val full =
                resolveWithInnerTube(
                    rawId = rawId,
                    traceId = traceId,
                    highQuality = highQuality,
                    clients = YouTubeClientStrategy.FALLBACK_CHAIN,
                    timeoutMs = FULL_PATH_TIMEOUT_MS,
                    poTokenResult = poTokenResult,
                )
            full.info?.let { extracted ->
                logAndCache(traceId, rawId, extracted)
                return ProviderResult.Success(extracted.url)
            }

            val lastErrorMessage = full.lastError.ifBlank { "Unknown player error" }
            DiagnosticLogger.logError(traceId, "STREAM_RESOLUTION_FAILED", lastErrorMessage)
            return ProviderResult.Error(
                message = "Failed to resolve stream for $rawId: $lastErrorMessage",
                cause = PlaybackException.StreamResolutionFailed(lastErrorMessage),
            )
        }

        private data class InnerTubeAttempt(
            val info: ExtractedStreamInfo?,
            val lastError: String,
        )

        /** Tries [clients] in order; returns the first playable audio format and the last error seen. */
        @Suppress("LongParameterList")
        private suspend fun resolveWithInnerTube(
            rawId: String,
            traceId: String,
            highQuality: Boolean,
            clients: List<YouTubeClientConfig>,
            timeoutMs: Long,
            poTokenResult: PoTokenResult?,
        ): InnerTubeAttempt {
            var lastErrorMessage = ""
            val info =
                withTimeoutOrNull(timeoutMs) {
                    for (clientConfig in clients) {
                        DiagnosticLogger.logPlayerRequest(traceId, clientConfig.name, rawId)
                        val requestResult =
                            runCatching {
                                api.player(
                                    PlayerRequest.forClient(
                                        videoId = rawId,
                                        config = clientConfig,
                                        poToken = poTokenResult?.poToken,
                                        visitorData = poTokenResult?.visitorData,
                                    ),
                                )
                            }
                        val playerResp = requestResult.getOrNull()
                        if (playerResp == null) {
                            lastErrorMessage =
                                requestResult.exceptionOrNull()?.message ?: "HTTP failure"
                            DiagnosticLogger.logPlayerResponse(
                                traceId,
                                clientConfig.name,
                                "HTTP_ERROR: $lastErrorMessage",
                            )
                            continue
                        }

                        val status = playerResp.playabilityStatus?.status ?: "OK"
                        DiagnosticLogger.logPlayerResponse(traceId, clientConfig.name, status)
                        if (status == "LOGIN_REQUIRED" || status == "UNPLAYABLE" || status == "ERROR") {
                            lastErrorMessage = playerResp.playabilityStatus?.reason ?: status
                            if (status == "LOGIN_REQUIRED") {
                                DiagnosticLogger.logError(traceId, "BOT_CHECK_FAILED", lastErrorMessage)
                            }
                            continue
                        }

                        val streamingData = playerResp.streamingData
                        if (streamingData == null) {
                            lastErrorMessage = "No streamingData in player response"
                            continue
                        }

                        val extracted =
                            StreamUrlDeobfuscator.extractAudioStream(streamingData, traceId, highQuality)
                        if (extracted != null) return@withTimeoutOrNull extracted
                        lastErrorMessage = "No playable audio format in player response"
                    }
                    null
                }
            return InnerTubeAttempt(info, lastErrorMessage)
        }

        private fun logAndCache(
            traceId: String,
            rawId: String,
            extracted: ExtractedStreamInfo,
        ) {
            DiagnosticLogger.logStreamFormatSelected(
                traceId = traceId,
                itag = extracted.itag,
                mimeType = extracted.mimeType,
                bitrate = extracted.bitrate,
            )
            DiagnosticLogger.logStreamUrlResolved(
                traceId = traceId,
                host = extracted.host,
                itag = extracted.itag,
                mimeType = extracted.mimeType,
                expiresAtMs = extracted.expiresAtMs,
            )
            streamCacheManager.put(providerId, rawId, extracted)
        }

        /** Bitrate (kbps) of the stream extracted for [trackId], if it's still cached. */
        fun cachedBitrateKbps(trackId: String): Int? =
            streamCacheManager.get(providerId, ProviderId.rawSourceId(providerId, trackId))
                ?.bitrate
                ?.takeIf { it > 0 }
                ?.div(BITS_PER_KBIT)

        fun invalidateCache(trackId: String) {
            val rawId = ProviderId.rawSourceId(providerId, trackId)
            streamCacheManager.invalidate(providerId, rawId)
        }

        override suspend fun relatedTracks(
            trackId: String,
            limit: Int,
        ): ProviderResult<List<Track>> {
            val rawId = ProviderId.rawSourceId(providerId, trackId)
            return runCatching {
                val nextJson = api.next(NextRequest.radio(rawId))
                val tracks =
                    nextJson.toRadioTrackList()
                        .filter { ProviderId.rawSourceId(providerId, it.id) != rawId }
                        .take(limit)
                ProviderResult.Success(tracks)
            }.getOrElse { e ->
                ProviderResult.Error(
                    message = "Radio failed for $rawId: ${e.message}",
                    cause = e,
                )
            }
        }

        override suspend fun playlists(): ProviderResult<List<Playlist>> {
            return ProviderResult.Success(emptyList())
        }

        override suspend fun queue(): ProviderResult<List<Track>> {
            return ProviderResult.Success(emptyList())
        }
    }

private const val BITS_PER_KBIT = 1000
private const val FAST_PATH_TIMEOUT_MS = 1_200L
private const val FULL_PATH_TIMEOUT_MS = 20_000L
