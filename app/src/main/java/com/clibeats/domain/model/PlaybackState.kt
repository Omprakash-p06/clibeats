package com.clibeats.domain.model

enum class RepeatMode { OFF, ONE, ALL }

data class PlaybackState(
    val currentTrack: Track?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
    // Audio format of the current stream (null until the player knows it).
    val codec: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val durationMs: Long = 0L,
    val activeOutputDevice: AudioOutputDevice? = null,
)
