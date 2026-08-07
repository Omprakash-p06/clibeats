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
class YouTubeStreamResolver
    @Inject
    constructor(
        private val api: InnerTubeApi,
        private val signatureDecipher: SignatureDecipher = DefaultSignatureDecipher(),
        private val streamValidator: StreamValidator = StreamValidator(),
    ) : StreamResolver {
        override suspend fun resolve(videoId: String): StreamResult =
            withContext(Dispatchers.IO) {
                val requests =
                    listOf(
                        PlayerRequest(context = InnerTubeContext.tvHtml5(), videoId = videoId),
                        PlayerRequest(context = InnerTubeContext.android(), videoId = videoId),
                        PlayerRequest.forVideoId(videoId),
                    )

                for (request in requests) {
                    val response = runCatching { api.player(request) }.getOrNull()
                    val streamingData = response?.streamingData
                    if (streamingData != null) {
                        val adaptiveFormats =
                            runCatching {
                                streamingData.jsonObject["adaptiveFormats"]?.jsonArray
                            }.getOrNull() ?: emptyList()
                        val formats =
                            runCatching {
                                streamingData.jsonObject["formats"]?.jsonArray
                            }.getOrNull() ?: emptyList()
                        val allFormats = adaptiveFormats + formats

                        for (format in allFormats) {
                            val formatObj = runCatching { format.jsonObject }.getOrNull() ?: continue
                            val directUrl = formatObj["url"]?.jsonPrimitive?.contentOrNull
                            if (!directUrl.isNullOrBlank()) {
                                if (streamValidator.validate(directUrl)) {
                                    return@withContext StreamResult.Success(url = directUrl)
                                }
                            }

                            val cipherString =
                                formatObj["signatureCipher"]?.jsonPrimitive?.contentOrNull
                                    ?: formatObj["cipher"]?.jsonPrimitive?.contentOrNull
                            if (!cipherString.isNullOrBlank()) {
                                val parsed = CipherParser.parse(cipherString)
                                if (parsed != null) {
                                    val sig = parsed.s?.let { signatureDecipher.decipher(it) }
                                    val signedUrl = SignedUrlBuilder.build(parsed, sig)
                                    if (streamValidator.validate(signedUrl)) {
                                        return@withContext StreamResult.Success(url = signedUrl)
                                    }
                                }
                            }
                        }
                    }
                }

                // Piped fallback
                val pipedUrl = fetchPipedStreamUrl(videoId)
                if (!pipedUrl.isNullOrBlank()) {
                    return@withContext StreamResult.Success(url = pipedUrl)
                }

                StreamResult.NoFormats
            }

        private suspend fun fetchPipedStreamUrl(videoId: String): String? =
            withContext(Dispatchers.IO) {
                val instances =
                    listOf(
                        "https://pipedapi.kavin.rocks",
                        "https://api.piped.video",
                        "https://pipedapi.tokhmi.xyz",
                    )
                val client =
                    OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .build()
                val json = Json { ignoreUnknownKeys = true }

                for (baseUrl in instances) {
                    runCatching {
                        val request =
                            Request.Builder()
                                .url("$baseUrl/streams/$videoId")
                                .header("User-Agent", "Mozilla/5.0")
                                .build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val jsonObj = json.parseToJsonElement(bodyString).jsonObject
                            val audioStreams = jsonObj["audioStreams"]?.jsonArray
                            val streamUrl =
                                audioStreams?.firstOrNull()
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
