package com.clibeats.domain.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val artworkUrl: String?,
    val trackCount: Int,
    val providerId: String,
)
