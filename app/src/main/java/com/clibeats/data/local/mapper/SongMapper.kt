package com.clibeats.data.local.mapper

import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Track

fun SongEntity.toDomain(): Track =
    Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        providerId = providerId,
    )

fun Track.toEntity(): SongEntity =
    SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        providerId = providerId,
    )
