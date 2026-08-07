package com.clibeats.telemetry

sealed class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap()) {
    data class PlaybackStarted(val trackId: String, val providerId: String) :
        AnalyticsEvent("playback_started", mapOf("track_id" to trackId, "provider_id" to providerId))

    data class PlaybackPaused(val trackId: String) :
        AnalyticsEvent("playback_paused", mapOf("track_id" to trackId))

    data class SearchExecuted(val providerId: String) :
        AnalyticsEvent("search_executed", mapOf("provider_id" to providerId))

    data class CacheCleared(val freedBytes: Long) :
        AnalyticsEvent("cache_cleared", mapOf("freed_bytes" to freedBytes.toString()))
}
