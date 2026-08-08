package com.clibeats.data.gateway.mapper

import com.clibeats.data.gateway.dto.GatewayAlbumDto
import com.clibeats.data.gateway.dto.GatewayArtistDto
import com.clibeats.data.gateway.dto.GatewayPlaylistDto
import com.clibeats.data.gateway.dto.GatewayTrackDto
import com.clibeats.domain.model.Album
import com.clibeats.domain.model.Artist
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track

private const val MS_PER_SECOND = 1_000L

fun GatewayTrackDto.toDomainTrack(): Track =
    Track(
        id = id,
        title = title,
        artist = artist,
        album = album.orEmpty(),
        durationMs = durationSeconds * MS_PER_SECOND,
        artworkUrl = artworkUrl,
        streamUrl = null,
        providerId = providerId,
    )

fun List<GatewayTrackDto>.toDomainTracks(): List<Track> = map { it.toDomainTrack() }

fun GatewayAlbumDto.toDomainAlbum(): Album =
    Album(
        id = id,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        trackCount = trackCount,
        year = releaseYear?.toInt(),
        providerId = providerId,
    )

fun GatewayArtistDto.toDomainArtist(): Artist =
    Artist(
        id = id,
        name = name,
        artworkUrl = avatarUrl,
        providerId = providerId,
    )

fun GatewayPlaylistDto.toDomainPlaylist(): Playlist =
    Playlist(
        id = id,
        name = title,
        description = description,
        artworkUrl = artworkUrl,
        trackCount = trackCount,
        isOwned = false,
        providerId = providerId,
    )
