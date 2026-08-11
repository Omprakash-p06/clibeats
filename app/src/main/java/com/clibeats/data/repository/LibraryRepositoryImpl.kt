// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.LikedSongDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.SongEntity
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
