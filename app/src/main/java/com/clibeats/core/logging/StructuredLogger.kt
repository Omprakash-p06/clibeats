package com.clibeats.core.logging

sealed interface PlaybackEvent {
    data class Search(val query: String) : PlaybackEvent

    data class TrackSelected(val trackId: String, val title: String) : PlaybackEvent

    data class PlayerRequest(val videoId: String) : PlaybackEvent

    data class StreamResolved(val videoId: String, val url: String, val durationMs: Long) : PlaybackEvent

    data class Validated(val url: String, val durationMs: Long) : PlaybackEvent

    data class Buffering(val trackId: String) : PlaybackEvent

    data class Playing(val trackId: String) : PlaybackEvent

    data class Failure(val stage: String, val reason: String, val cause: Throwable? = null) : PlaybackEvent
}

object StructuredLogger {
    private const val TAG = "CliBeatsPlayback"

    fun log(event: PlaybackEvent) {
        val message =
            when (event) {
                is PlaybackEvent.Search -> "[SEARCH] Query: ${event.query}"
                is PlaybackEvent.TrackSelected -> "[TRACK_SELECTED] Id: ${event.trackId}, Title: ${event.title}"
                is PlaybackEvent.PlayerRequest -> "[PLAYER_REQUEST] VideoId: ${event.videoId}"
                is PlaybackEvent.StreamResolved -> "[STREAM_RESOLVED] VideoId: ${event.videoId}, Took: ${event.durationMs}ms"
                is PlaybackEvent.Validated -> "[VALIDATED] URL Validated in ${event.durationMs}ms"
                is PlaybackEvent.Buffering -> "[BUFFERING] TrackId: ${event.trackId}"
                is PlaybackEvent.Playing -> "[PLAYING] TrackId: ${event.trackId}"
                is PlaybackEvent.Failure -> "[FAILURE] Stage: ${event.stage}, Reason: ${event.reason}"
            }
        runCatching { android.util.Log.d(TAG, message, (event as? PlaybackEvent.Failure)?.cause) }
    }
}
