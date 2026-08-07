package com.clibeats.domain.model

enum class RepeatMode { OFF, ONE, ALL }

enum class PlaybackStatus {
    IDLE,
    RESOLVING,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
}

data class PlaybackState(
    val currentTrack: Track?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val errorMessage: String? = null,
)
