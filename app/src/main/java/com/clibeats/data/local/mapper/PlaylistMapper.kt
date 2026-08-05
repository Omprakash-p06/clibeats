// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local.mapper

import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.domain.model.Playlist

fun PlaylistEntity.toDomain(): Playlist =
    Playlist(
        id = id,
        name = name,
        description = description,
        artworkUrl = artworkUrl,
        trackCount = trackCount,
        isOwned = isOwned,
        providerId = providerId,
    )

fun Playlist.toEntity(
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis(),
): PlaylistEntity =
    PlaylistEntity(
        id = id,
        name = name,
        description = description,
        artworkUrl = artworkUrl,
        trackCount = trackCount,
        isOwned = isOwned,
        providerId = providerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
