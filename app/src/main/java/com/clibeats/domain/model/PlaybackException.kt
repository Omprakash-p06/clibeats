@file:Suppress("MaxLineLength")

package com.clibeats.domain.model

sealed class PlaybackException(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    class SearchFailed(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("SEARCH_FAILED", message, cause)

    class TrackUnavailable(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("TRACK_UNAVAILABLE", message, cause)

    class PoTokenFailed(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("PO_TOKEN_FAILED", message, cause)

    class PlayerRequestFailed(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("PLAYER_REQUEST_FAILED", message, cause)

    class StreamResolutionFailed(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("STREAM_RESOLUTION_FAILED", message, cause)

    class StreamExpired(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("STREAM_EXPIRED", message, cause)

    class MediaPlaybackFailed(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("MEDIA_PLAYBACK_FAILED", message, cause)

    class BotCheckFailed(
        message: String,
        cause: Throwable? = null,
    ) : PlaybackException("BOT_CHECK_FAILED", message, cause)
}
