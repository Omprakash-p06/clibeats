package com.clibeats.core.logging

import android.util.Log
import java.util.UUID

sealed interface StructuredEvent {
    val traceId: String
    val timestamp: Long

    data class SearchRequest(override val traceId: String, val query: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class SearchResponse(override val traceId: String, val count: Int, val durationMs: Long, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class TrackSelected(override val traceId: String, val trackId: String, val title: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class StreamRequest(override val traceId: String, val trackId: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class StreamResolved(override val traceId: String, val trackId: String, val durationMs: Long, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class PlayerPreparing(override val traceId: String, val trackId: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class PlayerReady(override val traceId: String, val trackId: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class PlayerPlaying(override val traceId: String, val trackId: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class PlayerError(override val traceId: String, val stage: String, val error: String, val cause: Throwable? = null, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class NetworkError(override val traceId: String, val endpoint: String, val message: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
    data class GatewayError(override val traceId: String, val code: String, val message: String, override val timestamp: Long = System.currentTimeMillis()) : StructuredEvent
}

object StructuredLogger {
    private const val TAG = "CliBeatsTrace"

    fun generateTraceId(): String = "trace-" + UUID.randomUUID().toString().take(8)

    fun log(event: StructuredEvent) {
        val json = buildString {
            append("{")
            append("\"timestamp\":").append(event.timestamp).append(",")
            append("\"traceId\":\"").append(event.traceId).append("\",")
            append("\"event\":\"").append(event::class.simpleName).append("\",")
            when (event) {
                is StructuredEvent.SearchRequest -> append("\"query\":\"").append(event.query).append("\"")
                is StructuredEvent.SearchResponse -> append("\"count\":").append(event.count).append(",\"durationMs\":").append(event.durationMs)
                is StructuredEvent.TrackSelected -> append("\"trackId\":\"").append(event.trackId).append("\",\"title\":\"").append(event.title.replace("\"", "\\\"")).append("\"")
                is StructuredEvent.StreamRequest -> append("\"trackId\":\"").append(event.trackId).append("\"")
                is StructuredEvent.StreamResolved -> append("\"trackId\":\"").append(event.trackId).append("\",\"durationMs\":").append(event.durationMs)
                is StructuredEvent.PlayerPreparing -> append("\"trackId\":\"").append(event.trackId).append("\"")
                is StructuredEvent.PlayerReady -> append("\"trackId\":\"").append(event.trackId).append("\"")
                is StructuredEvent.PlayerPlaying -> append("\"trackId\":\"").append(event.trackId).append("\"")
                is StructuredEvent.PlayerError -> append("\"stage\":\"").append(event.stage).append("\",\"error\":\"").append(event.error).append("\"")
                is StructuredEvent.NetworkError -> append("\"endpoint\":\"").append(event.endpoint).append("\",\"message\":\"").append(event.message).append("\"")
                is StructuredEvent.GatewayError -> append("\"code\":\"").append(event.code).append("\",\"message\":\"").append(event.message).append("\"")
            }
            append("}")
        }
        runCatching {
            if (event is StructuredEvent.PlayerError) {
                Log.e(TAG, json, event.cause)
            } else {
                Log.i(TAG, json)
            }
        }
    }
}
