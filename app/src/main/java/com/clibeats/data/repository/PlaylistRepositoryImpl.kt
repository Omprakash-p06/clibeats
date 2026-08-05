package com.clibeats.data.repository

import com.clibeats.data.local.dao.PlaylistDao
import com.clibeats.data.local.entity.PlaylistSongCrossRef
import com.clibeats.data.local.mapper.toDomain
import com.clibeats.data.local.mapper.toEntity
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl
    @Inject
    constructor(
        private val playlistDao: PlaylistDao,
    ) : PlaylistRepository {
        override fun getAllPlaylistsAsFlow(): Flow<List<Playlist>> = playlistDao.getAllAsFlow().map { list -> list.map { it.toDomain() } }

        override suspend fun getPlaylistById(id: String): Playlist? = playlistDao.getById(id)?.toDomain()

        override suspend fun upsertPlaylist(playlist: Playlist) = playlistDao.upsert(playlist.toEntity())

        override suspend fun deletePlaylist(id: String) = playlistDao.deleteById(id)

        override fun getSongsForPlaylistAsFlow(playlistId: String): Flow<List<Track>> =
            playlistDao.getSongsForPlaylistAsFlow(playlistId).map { list ->
                list.map { it.toDomain() }
            }

        override suspend fun addSongToPlaylist(
            playlistId: String,
            songId: String,
            position: Int,
        ) = playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, position))

        override suspend fun removeSongFromPlaylist(
            playlistId: String,
            songId: String,
        ) = playlistDao.removeSongFromPlaylist(playlistId, songId)
    }
