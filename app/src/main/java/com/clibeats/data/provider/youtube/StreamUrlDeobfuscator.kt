@file:Suppress("ReturnCount", "MaxLineLength", "MagicNumber")

package com.clibeats.data.provider.youtube

import com.clibeats.util.DiagnosticLogger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLDecoder

data class ExtractedStreamInfo(
    val url: String,
    val host: String,
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val expiresAtMs: Long,
)

object StreamUrlDeobfuscator {
    /**
     * Picks the best playable audio format from a player response and returns its direct URL.
     *
     * YouTube splits audio across `adaptiveFormats` (and sometimes `formats`); only the first
     * array used to be read, so responses with the audio in `formats` failed even though a
     * playable stream was present. Formats that only carry `signatureCipher` are skipped: their
     * signature must be deobfuscated by the player JS, and pretending to do that produced URLs
     * that always failed with 403 at playback time.
     */
    fun extractAudioStream(
        streamingData: JsonElement,
        traceId: String,
        highQuality: Boolean = true,
    ): ExtractedStreamInfo? {
        val obj = runCatching { streamingData.jsonObject }.getOrNull() ?: return null
        val formats =
            (obj["adaptiveFormats"] as? JsonArray).orEmpty() +
                (obj["formats"] as? JsonArray).orEmpty()
        val audio =
            formats.filter { format ->
                format.stringField("mimeType").startsWith("audio/")
            }
        if (audio.isEmpty()) {
            DiagnosticLogger.logError(traceId, "STREAM_RESOLUTION_FAILED", "No audio formats in streamingData")
            return null
        }
        val ordered =
            if (highQuality) {
                audio.sortedWith(
                    compareByDescending<JsonElement> { it.bitrateOrZero() }
                        .thenByDescending { it.stringField("mimeType").contains("mp4") },
                )
            } else {
                // Data saver: lowest known bitrate, unknown bitrates last.
                audio.sortedWith(
                    compareBy<JsonElement> { if (it.bitrateOrZero() <= 0) 1 else 0 }
                        .thenBy { it.bitrateOrZero() },
                )
            }
        ordered.forEach { format ->
            deobfuscateStreamUrl(format, traceId)?.let { return it }
        }
        return null
    }

    fun deobfuscateStreamUrl(
        format: JsonElement,
        traceId: String,
    ): ExtractedStreamInfo? {
        val obj = runCatching { format.jsonObject }.getOrNull() ?: return null
        val itag = obj["itag"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull ?: "audio/unknown"
        val bitrate = obj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0

        val rawUrl = obj["url"]?.jsonPrimitive?.contentOrNull
        if (rawUrl.isNullOrBlank()) {
            val hasCipher =
                !obj["cipher"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() ||
                    !obj["signatureCipher"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()
            val reason =
                if (hasCipher) {
                    "Signature-ciphered format (itag $itag) is not supported"
                } else {
                    "Empty stream URL in format"
                }
            DiagnosticLogger.logError(traceId, "STREAM_RESOLUTION_FAILED", reason)
            return null
        }

        val uri = runCatching { URI.create(rawUrl) }.getOrNull()
        val host = uri?.host ?: "googlevideo.com"
        val expireParam = extractQueryParam(rawUrl, "expire")?.toLongOrNull()
        val expiresAtMs =
            if (expireParam != null) {
                expireParam * 1000L
            } else {
                System.currentTimeMillis() + 4 * 3600 * 1000L
            }

        return ExtractedStreamInfo(
            url = rawUrl,
            host = host,
            itag = itag,
            mimeType = mimeType,
            bitrate = bitrate,
            expiresAtMs = expiresAtMs,
        )
    }

    private fun JsonElement.stringField(name: String): String =
        runCatching { jsonObject[name]?.jsonPrimitive?.contentOrNull }.getOrNull().orEmpty()

    private fun JsonElement.bitrateOrZero(): Int = stringField("bitrate").toIntOrNull() ?: 0

    private fun extractQueryParam(
        url: String,
        paramName: String,
    ): String? {
        val query = url.substringAfter("?", "")
        if (query.isBlank()) return null
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.firstOrNull() == paramName }
            ?.getOrNull(1)
            ?.let { decode(it) }
    }

    private fun decode(str: String): String = runCatching { URLDecoder.decode(str, "UTF-8") }.getOrDefault(str)
}
