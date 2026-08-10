package com.clibeats.data.provider.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Jamendo API v3.0 DTOs (standard envelope: headers + results).
 */

@Serializable
data class JamendoResponse(
    val headers: JamendoHeaders = JamendoHeaders(),
    val results: List<JamendoTrackDto> = emptyList(),
)

@Serializable
data class JamendoHeaders(
    val status: String? = null,
    val code: Int? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
)

@Serializable
data class JamendoTrackDto(
    val id: String? = null,
    val name: String? = null,
    val duration: Long? = null,
    @SerialName("artist_name")
    val artistName: String? = null,
    @SerialName("album_name")
    val albumName: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val audiodownload: String? = null,
    @SerialName("audiodownload_allowed")
    val audiodownloadAllowed: Boolean? = null,
    @SerialName("license_ccurl")
    val licenseCcurl: String? = null,
)
