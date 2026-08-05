package com.clibeats.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val artworkUrl: String?,
    val trackCount: Int,
    val isOwned: Boolean,
    val providerId: String,
)
