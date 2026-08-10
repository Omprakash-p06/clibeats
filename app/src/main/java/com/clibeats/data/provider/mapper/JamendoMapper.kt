@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.JamendoTrackDto
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderId

private const val PROVIDER_ID = "jamendo"
private const val MS_PER_SECOND = 1_000L

/**
 * Maps a Jamendo track DTO to the domain model.
 *
 * `audiodownload` is the direct MP3 file URL (Range-capable); `audio` is the
 * CDN stream URL used as a fallback.
 */
@Suppress("ReturnCount")
fun JamendoTrackDto.toDomainTrack(): Track? {
    val trackId = id ?: return null
    val title = name ?: return null
    val url = audiodownload ?: audio ?: return null

    return Track(
        id = ProviderId.composite(PROVIDER_ID, trackId),
        title = title,
        artist = artistName ?: "",
        album = albumName ?: "",
        durationMs = (duration ?: 0L) * MS_PER_SECOND,
        artworkUrl = image,
        streamUrl = url,
        providerId = PROVIDER_ID,
    )
}
