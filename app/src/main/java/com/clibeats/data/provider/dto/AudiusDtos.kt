package com.clibeats.data.provider.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Audius v1 API DTOs.
 *
 * Audius returns flat, stable JSON — unlike legacy provider renderer trees —
 * so strongly-typed @Serializable data classes are the right fit here.
 * All fields are optional with defaults for forward-compatibility.
 */

@Serializable
data class AudiusSearchResponse(
    val data: List<AudiusTrackDto> = emptyList(),
)

@Serializable
data class AudiusTrackResponse(
    val data: AudiusTrackDto? = null,
)

@Serializable
data class AudiusTrackDto(
    val id: String? = null,
    val title: String? = null,
    val duration: Long? = null,
    val genre: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("is_streamable")
    val isStreamable: Boolean? = null,
    val user: AudiusUserDto? = null,
    val artwork: AudiusArtworkDto? = null,
    val stream: AudiusStreamDto? = null,
)

@Serializable
data class AudiusUserDto(
    val name: String? = null,
    val handle: String? = null,
)

@Serializable
data class AudiusArtworkDto(
    @SerialName("150x150")
    val small: String? = null,
    @SerialName("480x480")
    val medium: String? = null,
    @SerialName("1000x1000")
    val large: String? = null,
)

@Serializable
data class AudiusStreamDto(
    val url: String? = null,
)
