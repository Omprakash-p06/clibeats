@file:Suppress(
    "ForbiddenImport",
    "ReturnCount",
    "TooGenericExceptionCaught",
    "SwallowedException",
    "MagicNumber",
    "MaxLineLength",
)

package com.clibeats.data.provider.youtube

import com.clibeats.util.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Request as OkHttpRequest
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse

@Singleton
class NewPipeExtractorResolver
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
    ) {
        private val isInitialized = AtomicBoolean(false)

        private fun ensureInitialized() {
            if (isInitialized.compareAndSet(false, true)) {
                NewPipe.init(
                    object : Downloader() {
                        override fun execute(request: NPRequest): NPResponse {
                            val reqBuilder = OkHttpRequest.Builder().url(request.url())
                            reqBuilder.header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                            )
                            reqBuilder.header("Accept-Language", "en-US,en;q=0.9")

                            request.headers().forEach { (k, values) ->
                                values.forEach { v -> reqBuilder.addHeader(k, v) }
                            }

                            val dataToSend = request.dataToSend()
                            if (dataToSend != null) {
                                val body = dataToSend.toRequestBody("application/json".toMediaType())
                                reqBuilder.post(body)
                            }

                            val response = okHttpClient.newCall(reqBuilder.build()).execute()
                            val responseBodyStr = response.body?.string() ?: ""
                            return NPResponse(
                                response.code,
                                response.message,
                                response.headers.toMultimap(),
                                responseBodyStr,
                                request.url(),
                            )
                        }
                    },
                )
            }
        }

        suspend fun resolveStream(
            rawId: String,
            traceId: String,
        ): ExtractedStreamInfo? =
            withContext(Dispatchers.IO) {
                ensureInitialized()
                try {
                    DiagnosticLogger.logPlayerRequest(traceId, "NEWPIPE_EXTRACTOR_V26", rawId)
                    val watchUrl = "https://www.youtube.com/watch?v=$rawId"
                    val extractor = ServiceList.YouTube.getStreamExtractor(watchUrl) as YoutubeStreamExtractor
                    extractor.fetchPage()

                    val audioStreams = extractor.audioStreams
                    if (audioStreams.isEmpty()) {
                        DiagnosticLogger.logPlayerResponse(traceId, "NEWPIPE_EXTRACTOR_V26", "NO_AUDIO_STREAMS")
                        return@withContext null
                    }

                    // Prefer M4A/AAC format with highest bitrate, or highest available audio stream
                    val selectedStream = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                    val streamUrl = selectedStream.content
                    if (streamUrl.isBlank()) {
                        DiagnosticLogger.logPlayerResponse(traceId, "NEWPIPE_EXTRACTOR_V26", "EMPTY_STREAM_URL")
                        return@withContext null
                    }

                    DiagnosticLogger.logPlayerResponse(traceId, "NEWPIPE_EXTRACTOR_V26", "OK")

                    val formatName = selectedStream.getFormat()?.name?.lowercase() ?: "m4a"
                    val mimeType =
                        when (formatName) {
                            "m4a" -> "audio/mp4"
                            "webma_opus" -> "audio/webm"
                            else -> "audio/mp4"
                        }

                    val uriHost = runCatching { java.net.URI(streamUrl).host }.getOrNull() ?: "googlevideo.com"
                    val expiresAtMs = parseExpiryFromUrl(streamUrl)

                    ExtractedStreamInfo(
                        url = streamUrl,
                        mimeType = mimeType,
                        bitrate = selectedStream.averageBitrate * 1000,
                        itag = selectedStream.itag,
                        host = uriHost,
                        expiresAtMs = expiresAtMs,
                    )
                } catch (e: Exception) {
                    DiagnosticLogger.logPlayerResponse(traceId, "NEWPIPE_EXTRACTOR_V26", "ERROR: ${e.message}")
                    null
                }
            }

        private fun parseExpiryFromUrl(url: String): Long {
            val now = System.currentTimeMillis()
            val defaultExpiry = now + 5 * 60 * 60 * 1000L // 5 hours default
            return try {
                val expireParam = url.substringAfter("expire=", "").substringBefore("&")
                if (expireParam.isNotBlank()) {
                    val expireSeconds = expireParam.toLong()
                    expireSeconds * 1000L
                } else {
                    defaultExpiry
                }
            } catch (e: Exception) {
                defaultExpiry
            }
        }
    }
