package com.clibeats.data.gateway.dto

import kotlinx.serialization.Serializable

/** Track object returned by the Provider Gateway (TrackSchema). */
@Serializable
data class GatewayTrackDto(
    val id: String,
    val providerId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0L,
    val artworkUrl: String? = null,
    val explicit: Boolean = false,
)

@Serializable
data class GatewaySearchResponse(
    val tracks: List<GatewayTrackDto> = emptyList(),
    val cached: Boolean = false,
)

@Serializable
data class GatewayStreamRequest(
    val trackId: String,
)

/** StreamResultSchema returned by the gateway on /api/v1/stream. */
@Serializable
data class GatewayStreamDto(
    val trackId: String,
    val providerId: String,
    val streamUrl: String,
    val mimeType: String? = null,
    val bitrateKbps: Long? = null,
    val expiresAtEpochSeconds: Long = 0L,
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
data class GatewayStreamResponse(
    val stream: GatewayStreamDto,
)

@Serializable
data class GatewayAlbumDto(
    val id: String,
    val providerId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val trackCount: Int = 0,
    val releaseYear: Long? = null,
    val tracks: List<GatewayTrackDto> = emptyList(),
)

@Serializable
data class GatewayArtistDto(
    val id: String,
    val providerId: String,
    val name: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
)

@Serializable
data class GatewayPlaylistDto(
    val id: String,
    val providerId: String,
    val title: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val trackCount: Int = 0,
    val tracks: List<GatewayTrackDto> = emptyList(),
)

/** Error body produced by the gateway global error handler (ErrorResponseSchema). */
@Serializable
data class GatewayErrorResponse(
    val error: GatewayErrorDto,
)

@Serializable
data class GatewayErrorDto(
    val code: String,
    val message: String,
    val providerId: String,
    val retryAfterSeconds: Long? = null,
    val traceId: String? = null,
)
