package com.clibeats.domain.model

enum class RepeatMode { OFF, ONE, ALL }

data class PlaybackState(
    val currentTrack: Track?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
)
