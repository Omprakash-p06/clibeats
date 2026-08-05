package com.clibeats.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String?,
    val streamUrl: String?,
    val providerId: String,
)
