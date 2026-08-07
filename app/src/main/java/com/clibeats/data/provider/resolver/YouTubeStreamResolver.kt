package com.clibeats.data.provider.resolver

import com.clibeats.data.provider.api.InnerTubeApi
import com.clibeats.data.provider.dto.InnerTubeContext
import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.resolver.cipher.CipherParser
import com.clibeats.data.provider.resolver.cipher.DefaultSignatureDecipher
import com.clibeats.data.provider.resolver.cipher.SignatureDecipher
import com.clibeats.data.provider.resolver.cipher.SignedUrlBuilder
import com.clibeats.domain.provider.StreamResolver
import com.clibeats.domain.provider.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeStreamResolver @Inject constructor(
    private val api: InnerTubeApi,
    private val signatureDecipher: SignatureDecipher = DefaultSignatureDecipher(),
    private val streamValidator: StreamValidator = StreamValidator(),
) : StreamResolver {

    private var cachedVisitorData: String? = null

    override suspend fun resolve(videoId: String): StreamResult = withContext(Dispatchers.IO) {
        if (cachedVisitorData.isNullOrBlank()) {
            runCatching {
                val visitorRes = api.visitorId()
                cachedVisitorData = visitorRes.responseContext?.visitorData
                logDebug("YouTubeStreamResolverDiagnostics", "[VISITOR_ID_RESOLVED] Fetched visitorData token: $cachedVisitorData")
            }.onFailure { e ->
                logError("YouTubeStreamResolverDiagnostics", "[VISITOR_ID_FAIL] Failed to fetch visitorData: ${e.message}", e)
            }
        }

        val requests = listOf(
            "IOS_STANDALONE" to PlayerRequest(context = InnerTubeContext.iosStandalone(), videoId = videoId),
            "WEB_EMBEDDED_PLAYER" to PlayerRequest(context = InnerTubeContext.webEmbedded(videoId), videoId = videoId),
            "ANDROID_TESTSUITE" to PlayerRequest(context = InnerTubeContext.androidTestSuite(), videoId = videoId),
            "TVHTML5" to PlayerRequest(context = InnerTubeContext.tvHtml5(), videoId = videoId),
            "WEB_REMIX" to PlayerRequest.forVideoId(videoId),
        )

        for ((clientName, request) in requests) {
            logDebug(
                "YouTubeStreamResolverDiagnostics",
                "[PLAYER_REQUEST] Trying client: $clientName for videoId: $videoId",
            )
            val responseResult = runCatching { api.player(request) }
            val response = responseResult.getOrNull()
            if (responseResult.isFailure) {
                logError(
                    "YouTubeStreamResolverDiagnostics",
                    "[PLAYER_RESPONSE_FAIL] Client $clientName failed with exception: ${responseResult.exceptionOrNull()?.message}",
                    responseResult.exceptionOrNull(),
                )
                continue
            }

            val streamingData = response?.streamingData
            val hasStreamingData = streamingData != null
            val playabilityStatusStr = response?.playabilityStatus?.toString() ?: "null"
            val adaptiveFormats = runCatching {
                streamingData?.jsonObject?.get("adaptiveFormats")?.jsonArray
            }.getOrNull() ?: emptyList()
            val formats = runCatching {
                streamingData?.jsonObject?.get("formats")?.jsonArray
            }.getOrNull() ?: emptyList()

            logDebug(
                "YouTubeStreamResolverDiagnostics",
                "[PLAYER_RESPONSE] Client: $clientName, streamingData: $hasStreamingData, playabilityStatus: $playabilityStatusStr, adaptiveFormats count: ${adaptiveFormats.size}, formats count: ${formats.size}",
            )

            val allFormats = adaptiveFormats + formats
            for (format in allFormats) {
                val formatObj = runCatching { format.jsonObject }.getOrNull() ?: continue
                val mimeType = formatObj["mimeType"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                val directUrl = formatObj["url"]?.jsonPrimitive?.contentOrNull

                logDebug(
                    "YouTubeStreamResolverDiagnostics",
                    "[FORMAT_INSPECT] mimeType: $mimeType, directUrl: ${directUrl != null}",
                )

                if (!directUrl.isNullOrBlank()) {
                    logDebug(
                        "YouTubeStreamResolverDiagnostics",
                        "[DIRECT_URL_FOUND] Validating direct URL: $directUrl",
                    )
                    if (streamValidator.validate(directUrl)) {
                        logDebug(
                            "YouTubeStreamResolverDiagnostics",
                            "[VALIDATION_SUCCESS] Direct URL validated successfully for $videoId",
                        )
                        return@withContext StreamResult.Success(url = directUrl)
                    }
                }

                val cipherString = formatObj["signatureCipher"]?.jsonPrimitive?.contentOrNull
                    ?: formatObj["cipher"]?.jsonPrimitive?.contentOrNull
                if (!cipherString.isNullOrBlank()) {
                    logDebug(
                        "YouTubeStreamResolverDiagnostics",
                        "[CIPHER_FOUND] Parsing signatureCipher for $videoId",
                    )
                    val parsed = CipherParser.parse(cipherString)
                    if (parsed != null) {
                        val sig = parsed.s?.let { signatureDecipher.decipher(it) }
                        val signedUrl = SignedUrlBuilder.build(parsed, sig)
                        logDebug(
                            "YouTubeStreamResolverDiagnostics",
                            "[CIPHER_SIGNED] Deciphered & built URL: $signedUrl",
                        )
                        if (streamValidator.validate(signedUrl)) {
                            logDebug(
                                "YouTubeStreamResolverDiagnostics",
                                "[VALIDATION_SUCCESS] Cipher URL validated successfully for $videoId",
                            )
                            return@withContext StreamResult.Success(url = signedUrl)
                        }
                    }
                }
            }
        }

        logWarn(
            "YouTubeStreamResolverDiagnostics",
            "[INNER_TUBE_NO_STREAMS] All InnerTube clients returned no valid streams for $videoId. Trying Piped API fallback...",
        )

        // Piped fallback
        val pipedUrl = fetchPipedStreamUrl(videoId)
        if (!pipedUrl.isNullOrBlank()) {
            logDebug(
                "YouTubeStreamResolverDiagnostics",
                "[PIPED_FALLBACK_SUCCESS] Resolved Piped stream URL: $pipedUrl",
            )
            return@withContext StreamResult.Success(url = pipedUrl)
        }

        logError(
            "YouTubeStreamResolverDiagnostics",
            "[STREAM_RESOLATION_FAILED] No valid streams could be resolved from InnerTube or Piped for $videoId",
        )
        StreamResult.NoFormats
    }

    private suspend fun fetchPipedStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.video",
            "https://pipedapi.tokhmi.xyz",
        )
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val json = Json { ignoreUnknownKeys = true }

        for (baseUrl in instances) {
            runCatching {
                logDebug(
                    "YouTubeStreamResolverDiagnostics",
                    "[PIPED_REQUEST] Querying Piped instance: $baseUrl/streams/$videoId",
                )
                val request = Request.Builder()
                    .url("$baseUrl/streams/$videoId")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val response = client.newCall(request).execute()
                logDebug(
                    "YouTubeStreamResolverDiagnostics",
                    "[PIPED_RESPONSE] Instance: $baseUrl, Code: ${response.code}",
                )
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val jsonObj = json.parseToJsonElement(bodyString).jsonObject
                    val audioStreams = jsonObj["audioStreams"]?.jsonArray
                    val streamUrl = audioStreams?.firstOrNull()
                        ?.jsonObject?.get("url")
                        ?.jsonPrimitive?.contentOrNull
                    if (!streamUrl.isNullOrBlank()) {
                        return@withContext streamUrl
                    }
                }
            }
        }
        null
    }
}

private fun logDebug(tag: String, msg: String) {
    runCatching { android.util.Log.d(tag, msg) }
}

private fun logWarn(tag: String, msg: String) {
    runCatching { android.util.Log.w(tag, msg) }
}

private fun logError(tag: String, msg: String, tr: Throwable? = null) {
    runCatching { android.util.Log.e(tag, msg, tr) }
}
