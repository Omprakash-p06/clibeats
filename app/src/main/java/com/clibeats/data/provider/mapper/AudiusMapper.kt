@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.AudiusArtworkDto
import com.clibeats.data.provider.dto.AudiusTrackDto
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderId

private const val PROVIDER_ID = "audius"
private const val MS_PER_SECOND = 1_000L

/**
 * Map an Audius track DTO to the domain Track model.
 *
 * Audius search responses include a signed stream URL on each track
 * (`stream.url`) which is used as a metadata fallback; the provider overrides
 * `streamUrl` with the verified working `/tracks/{id}/stream` endpoint URL.
 * The track id is collision-safe (`audius:<id>`).
 */
@Suppress("ReturnCount")
fun AudiusTrackDto.toDomainTrack(): Track? {
    val trackId = id ?: return null
    val trackTitle = title ?: return null
    val stream = stream?.url ?: return null

    return Track(
        id = ProviderId.composite(PROVIDER_ID, trackId),
        title = trackTitle,
        artist = user?.name ?: "",
        album = genre ?: "",
        durationMs = (duration ?: 0L) * MS_PER_SECOND,
        artworkUrl = artwork?.preferredUrl(),
        streamUrl = stream,
        providerId = PROVIDER_ID,
    )
}

/** Prefer the highest-resolution artwork variant available. */
private fun AudiusArtworkDto.preferredUrl(): String? = large ?: medium ?: small
