@file:Suppress("MaxLineLength", "MagicNumber", "TooManyFunctions")

package com.clibeats.util

import android.util.Log

object DiagnosticLogger {
    private const val TAG = "CliBeatsDiagnostic"

    fun generateTraceId(): String = java.util.UUID.randomUUID().toString().take(8)

    private fun safeLog(
        level: Int,
        message: String,
    ) {
        runCatching {
            when (level) {
                Log.ERROR -> Log.e(TAG, message)
                else -> Log.i(TAG, message)
            }
        }.onFailure {
            val prefix = if (level == Log.ERROR) "[ERROR]" else "[INFO]"
            println("$prefix $TAG: $message")
        }
    }

    fun logSearchRequest(
        traceId: String,
        query: String,
    ) {
        safeLog(Log.INFO, "[$traceId] SEARCH_REQUEST: query='$query'")
    }

    fun logSearchResponse(
        traceId: String,
        count: Int,
    ) {
        safeLog(Log.INFO, "[$traceId] SEARCH_RESPONSE: found $count tracks")
    }

    fun logTrackSelected(
        traceId: String,
        trackId: String,
        title: String,
    ) {
        safeLog(Log.INFO, "[$traceId] TRACK_SELECTED: trackId=$trackId, title='$title'")
    }

    fun logStreamResolutionStarted(
        traceId: String,
        trackId: String,
    ) {
        safeLog(Log.INFO, "[$traceId] STREAM_RESOLUTION_STARTED: trackId=$trackId")
    }

    fun logPoTokenStarted(traceId: String) {
        safeLog(Log.INFO, "[$traceId] PO_TOKEN_STARTED")
    }

    fun logPoTokenSuccess(traceId: String) {
        safeLog(Log.INFO, "[$traceId] PO_TOKEN_SUCCESS")
    }

    fun logPlayerRequest(
        traceId: String,
        clientName: String,
        videoId: String,
    ) {
        safeLog(Log.INFO, "[$traceId] PLAYER_REQUEST: client=$clientName, videoId=$videoId")
    }

    fun logPlayerResponse(
        traceId: String,
        clientName: String,
        status: String,
    ) {
        safeLog(Log.INFO, "[$traceId] PLAYER_RESPONSE: client=$clientName, status=$status")
    }

    fun logStreamFormatSelected(
        traceId: String,
        itag: Int,
        mimeType: String,
        bitrate: Int,
    ) {
        safeLog(Log.INFO, "[$traceId] STREAM_FORMAT_SELECTED: itag=$itag, mimeType=$mimeType, bitrate=$bitrate")
    }

    fun logStreamUrlResolved(
        traceId: String,
        host: String,
        itag: Int,
        mimeType: String,
        expiresAtMs: Long,
    ) {
        safeLog(
            Log.INFO,
            "[$traceId] STREAM_URL_RESOLVED: host=$host, itag=$itag, mimeType=$mimeType, expiresAt=$expiresAtMs",
        )
    }

    fun logMediaPrepare(
        traceId: String,
        mediaId: String,
    ) {
        safeLog(Log.INFO, "[$traceId] MEDIA_PREPARE: mediaId=$mediaId")
    }

    fun logMediaReady(traceId: String) {
        safeLog(Log.INFO, "[$traceId] MEDIA_READY")
    }

    fun logMediaPlaying(traceId: String) {
        safeLog(Log.INFO, "[$traceId] MEDIA_PLAYING")
    }

    fun logError(
        traceId: String,
        errorCode: String,
        message: String,
    ) {
        safeLog(
            Log.ERROR,
            "[$traceId] ERROR: code=$errorCode, message=$message",
        )
    }
}
