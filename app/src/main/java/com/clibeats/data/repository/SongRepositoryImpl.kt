// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.dao.escapeForLike
import com.clibeats.data.local.mapper.toDomain
import com.clibeats.data.local.mapper.toEntity
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Indentation: detekt 1.23.6 misparses ktlint_official @Inject constructor() style (false positive).
// Line length: single-line expression bodies fit ktlint's tolerance; detekt's 120 default flags them.
@Suppress("Indentation", "MaxLineLength", "MaximumLineLength", "Wrapping")
@Singleton
class SongRepositoryImpl
    @Inject
    constructor(
        private val songDao: SongDao,
    ) : SongRepository {
        override fun getAllTracksAsFlow(): Flow<List<Track>> = songDao.getAllAsFlow().map { entities -> entities.map { it.toDomain() } }

        override fun searchTracksAsFlow(query: String): Flow<List<Track>> =
            songDao.searchAsFlow(query.escapeForLike()).map { entities -> entities.map { it.toDomain() } }

        override suspend fun getTrackById(id: String): Track? = songDao.getById(id)?.toDomain()

        override suspend fun upsertTrack(track: Track) {
            val existing = songDao.getById(track.id)
            songDao.upsert(track.toEntity(existing?.localPath, existing?.cachedAt))
        }

        override suspend fun upsertTracks(tracks: List<Track>) {
            val existingById = songDao.getByIds(tracks.map { it.id }).associateBy { it.id }
            songDao.upsertAll(
                tracks.map { track ->
                    val existing = existingById[track.id]
                    track.toEntity(existing?.localPath, existing?.cachedAt)
                },
            )
        }

        override suspend fun deleteTrack(id: String) = songDao.deleteById(id)
    }
