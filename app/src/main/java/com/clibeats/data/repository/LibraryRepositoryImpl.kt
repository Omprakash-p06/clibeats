// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.LikedSongDao
import com.clibeats.data.local.dao.SavedAlbumDao
import com.clibeats.data.local.dao.SavedArtistDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SavedAlbumEntity
import com.clibeats.data.local.entity.SavedArtistEntity
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Album
import com.clibeats.domain.model.Artist
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl
    @Inject
    constructor(
        private val songDao: SongDao,
        private val likedSongDao: LikedSongDao,
        private val savedAlbumDao: SavedAlbumDao,
        private val savedArtistDao: SavedArtistDao,
    ) : LibraryRepository {
        override fun getLikedSongs(): Flow<List<Track>> =
            likedSongDao.getLikedSongsAsFlow().map { entities ->
                entities.map { it.toDomainTrack() }
            }

        override fun isLiked(trackId: String): Flow<Boolean> = likedSongDao.isLikedFlow(trackId)

        override suspend fun toggleLike(track: Track) {
            val currentlyLiked = likedSongDao.isLiked(track.id)
            if (currentlyLiked) {
                likedSongDao.unlikeSong(track.id)
            } else {
                songDao.upsert(track.toEntity())
                likedSongDao.likeSong(track.id)
            }
        }

        override fun getSavedAlbums(): Flow<List<Album>> =
            savedAlbumDao.getAllAsFlow().map { entities ->
                entities.map { it.toDomainAlbum() }
            }

        override fun isAlbumSaved(albumId: String): Flow<Boolean> = savedAlbumDao.isSavedFlow(albumId)

        override suspend fun toggleSaveAlbum(album: Album) {
            val isSaved = savedAlbumDao.isSaved(album.id)
            if (isSaved) {
                savedAlbumDao.deleteById(album.id)
            } else {
                savedAlbumDao.upsert(album.toEntity())
            }
        }

        override fun getSavedArtists(): Flow<List<Artist>> =
            savedArtistDao.getAllAsFlow().map { entities ->
                entities.map { it.toDomainArtist() }
            }

        override fun isArtistSaved(artistId: String): Flow<Boolean> = savedArtistDao.isSavedFlow(artistId)

        override suspend fun toggleSaveArtist(artist: Artist) {
            val isSaved = savedArtistDao.isSaved(artist.id)
            if (isSaved) {
                savedArtistDao.deleteById(artist.id)
            } else {
                savedArtistDao.upsert(artist.toEntity())
            }
        }
    }

private fun SongEntity.toDomainTrack(): Track =
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

private fun Track.toEntity(): SongEntity =
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

private fun SavedAlbumEntity.toDomainAlbum(): Album =
    Album(
        id = id,
        title = title,
        artist = artist,
        year = year,
        artworkUrl = artworkUrl,
        trackCount = trackCount,
        providerId = providerId,
    )

private fun Album.toEntity(): SavedAlbumEntity =
    SavedAlbumEntity(
        id = id,
        title = title,
        artist = artist,
        year = year,
        artworkUrl = artworkUrl,
        trackCount = trackCount,
        providerId = providerId,
    )

private fun SavedArtistEntity.toDomainArtist(): Artist =
    Artist(
        id = id,
        name = name,
        artworkUrl = artworkUrl,
        providerId = providerId,
    )

private fun Artist.toEntity(): SavedArtistEntity =
    SavedArtistEntity(
        id = id,
        name = name,
        artworkUrl = artworkUrl,
        providerId = providerId,
    )
