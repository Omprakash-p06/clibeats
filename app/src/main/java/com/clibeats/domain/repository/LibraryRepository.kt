package com.clibeats.domain.repository

import com.clibeats.domain.model.Album
import com.clibeats.domain.model.Artist
import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun getLikedSongs(): Flow<List<Track>>

    fun isLiked(trackId: String): Flow<Boolean>

    suspend fun toggleLike(track: Track)

    fun getSavedAlbums(): Flow<List<Album>>

    fun isAlbumSaved(albumId: String): Flow<Boolean>

    suspend fun toggleSaveAlbum(album: Album)

    fun getSavedArtists(): Flow<List<Artist>>

    fun isArtistSaved(artistId: String): Flow<Boolean>

    suspend fun toggleSaveArtist(artist: Artist)
}
