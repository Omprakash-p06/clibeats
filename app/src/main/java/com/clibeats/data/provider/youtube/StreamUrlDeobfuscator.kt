@file:Suppress("ReturnCount", "MaxLineLength", "MagicNumber")

package com.clibeats.data.provider.youtube

import com.clibeats.util.DiagnosticLogger
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
    fun deobfuscateStreamUrl(
        format: JsonElement,
        traceId: String,
    ): ExtractedStreamInfo? {
        val obj = runCatching { format.jsonObject }.getOrNull() ?: return null
        val itag = obj["itag"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull ?: "audio/unknown"
        val bitrate = obj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0

        var rawUrl = obj["url"]?.jsonPrimitive?.contentOrNull
        val cipherStr = obj["cipher"]?.jsonPrimitive?.contentOrNull ?: obj["signatureCipher"]?.jsonPrimitive?.contentOrNull

        if (rawUrl.isNullOrBlank() && !cipherStr.isNullOrBlank()) {
            rawUrl = parseCipher(cipherStr)
        }

        if (rawUrl.isNullOrBlank()) {
            DiagnosticLogger.logError(traceId, "STREAM_RESOLUTION_FAILED", "Empty stream URL in format")
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

    private fun parseCipher(cipherStr: String): String? {
        val params =
            cipherStr.split("&").associate { param ->
                val parts = param.split("=", limit = 2)
                val key = decode(parts.getOrNull(0) ?: "")
                val value = decode(parts.getOrNull(1) ?: "")
                key to value
            }

        val baseUrl = params["url"] ?: return null
        val signature = params["s"]
        val sigParam = params["sp"] ?: "sig"

        if (signature.isNullOrBlank()) {
            return baseUrl
        }

        val deobfuscatedSig = deobfuscateSignature(signature)
        val delimiter = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl$delimiter$sigParam=$deobfuscatedSig"
    }

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

    private fun decode(str: String): String {
        return runCatching { URLDecoder.decode(str, "UTF-8") }.getOrDefault(str)
    }

    private fun deobfuscateSignature(signature: String): String {
        return signature.reversed()
    }
}
