@file:Suppress(
    "ForbiddenImport",
    "ReturnCount",
    "LoopWithTooManyJumpStatements",
    "LongMethod",
    "MaxLineLength",
    "CyclomaticComplexMethod",
)

package com.clibeats.data.provider

import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.dto.SearchRequest
import com.clibeats.data.provider.mapper.toTrackList
import com.clibeats.data.provider.youtube.NewPipeExtractorResolver
import com.clibeats.data.provider.youtube.PoTokenGenerator
import com.clibeats.data.provider.youtube.StreamCacheManager
import com.clibeats.data.provider.youtube.StreamUrlDeobfuscator
import com.clibeats.data.provider.youtube.YouTubeClientStrategy
import com.clibeats.domain.model.PlaybackException
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderId
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.util.DiagnosticLogger
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

        override suspend fun trending(limit: Int): ProviderResult<List<Track>> {
            return search("trending music", limit)
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

            // 2. Primary: NewPipeExtractor v0.26.4 maintained extraction
            val newPipeExtracted = newPipeExtractorResolver.resolveStream(rawId, traceId)
            if (newPipeExtracted != null) {
                DiagnosticLogger.logStreamFormatSelected(
                    traceId = traceId,
                    itag = newPipeExtracted.itag,
                    mimeType = newPipeExtracted.mimeType,
                    bitrate = newPipeExtracted.bitrate,
                )
                DiagnosticLogger.logStreamUrlResolved(
                    traceId = traceId,
                    host = newPipeExtracted.host,
                    itag = newPipeExtracted.itag,
                    mimeType = newPipeExtracted.mimeType,
                    expiresAtMs = newPipeExtracted.expiresAtMs,
                )

                streamCacheManager.put(providerId, rawId, newPipeExtracted)
                return ProviderResult.Success(newPipeExtracted.url)
            }

            // 3. Fallback: InnerTube player API with PO-token
            val poTokenResult = poTokenGenerator.getPoToken(traceId, rawId)
            var lastErrorMessage = "Unknown player error"
            for (clientConfig in YouTubeClientStrategy.FALLBACK_CHAIN) {
                DiagnosticLogger.logPlayerRequest(traceId, clientConfig.name, rawId)

                val requestResult =
                    runCatching {
                        val playerReq =
                            PlayerRequest.forClient(
                                videoId = rawId,
                                config = clientConfig,
                                poToken = poTokenResult?.poToken,
                                visitorData = poTokenResult?.visitorData,
                            )
                        api.player(playerReq)
                    }

                val playerResp = requestResult.getOrNull()
                if (playerResp == null) {
                    val err = requestResult.exceptionOrNull()?.message ?: "HTTP failure"
                    DiagnosticLogger.logPlayerResponse(traceId, clientConfig.name, "HTTP_ERROR: $err")
                    lastErrorMessage = err
                    continue
                }

                val status = playerResp.playabilityStatus?.status ?: "OK"
                DiagnosticLogger.logPlayerResponse(traceId, clientConfig.name, status)

                if (status == "LOGIN_REQUIRED" || status == "UNPLAYABLE" || status == "ERROR") {
                    val reason = playerResp.playabilityStatus?.reason ?: status
                    lastErrorMessage = reason
                    if (status == "LOGIN_REQUIRED") {
                        DiagnosticLogger.logError(traceId, "BOT_CHECK_FAILED", reason)
                    }
                    continue
                }

                val streamingData = playerResp.streamingData
                if (streamingData == null) {
                    lastErrorMessage = "No streamingData in player response"
                    continue
                }

                // Extract format
                val extracted = StreamUrlDeobfuscator.deobfuscateStreamUrl(streamingData, traceId)
                if (extracted != null) {
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
                    return ProviderResult.Success(extracted.url)
                }
            }

            DiagnosticLogger.logError(traceId, "STREAM_RESOLUTION_FAILED", lastErrorMessage)
            return ProviderResult.Error(
                message = "Failed to resolve stream for $rawId: $lastErrorMessage",
                cause = PlaybackException.StreamResolutionFailed(lastErrorMessage),
            )
        }

        fun invalidateCache(trackId: String) {
            val rawId = ProviderId.rawSourceId(providerId, trackId)
            streamCacheManager.invalidate(providerId, rawId)
        }

        override suspend fun playlists(): ProviderResult<List<Playlist>> {
            return ProviderResult.Success(emptyList())
        }

        override suspend fun queue(): ProviderResult<List<Track>> {
            return ProviderResult.Success(emptyList())
        }
    }
