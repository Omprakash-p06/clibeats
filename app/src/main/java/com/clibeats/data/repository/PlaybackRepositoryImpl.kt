@file:Suppress("ForbiddenImport", "MaxLineLength", "LongMethod", "TooManyFunctions")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.QueueDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.QueueEntity
import com.clibeats.data.local.mapper.toDomain
import com.clibeats.data.local.mapper.toEntity
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.PlayerAdapter
import com.clibeats.playback.StreamResolver
import com.clibeats.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepositoryImpl
    constructor(
        private val playerAdapter: PlayerAdapter,
        private val streamResolver: StreamResolver,
        private val queueDao: QueueDao,
        private val songDao: SongDao,
        private val appPreferences: AppPreferences,
        private val ioDispatcher: CoroutineDispatcher,
    ) : PlaybackRepository {
        @Inject
        constructor(
            playerAdapter: PlayerAdapter,
            streamResolver: StreamResolver,
            queueDao: QueueDao,
            songDao: SongDao,
            appPreferences: AppPreferences,
        ) : this(
            playerAdapter,
            streamResolver,
            queueDao,
            songDao,
            appPreferences,
            Dispatchers.IO,
        )

        private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

        override val playbackState: StateFlow<PlaybackState> = playerAdapter.playbackState

        override val queueState: StateFlow<List<Track>> = playerAdapter.queueFlow

        init {
            repositoryScope.launch {
                restorePersistentQueue()
            }
            repositoryScope.launch {
                playerAdapter.queueFlow.collect { tracks ->
                    persistQueueItems(tracks)
                }
            }
            repositoryScope.launch {
                playerAdapter.playbackState.collect { state ->
                    val tracks = playerAdapter.queueFlow.value
                    val currentIndex =
                        state.currentTrack?.let { current ->
                            tracks.indexOfFirst { it.id == current.id }
                        } ?: 0
                    appPreferences.saveQueueMetadata(
                        index = if (currentIndex >= 0) currentIndex else 0,
                        positionMs = state.positionMs,
                        repeatMode = state.repeatMode.name,
                        shuffleEnabled = state.shuffleEnabled,
                    )
                }
            }
        }

        private suspend fun restorePersistentQueue() {
            runCatching {
                val songEntities = queueDao.getQueueSongs()
                if (songEntities.isNotEmpty()) {
                    val tracks = songEntities.map { it.toDomain() }
                    val index = appPreferences.lastQueueIndex.first()
                    val positionMs = appPreferences.lastPlaybackPosition.first()
                    val repeatModeStr = appPreferences.savedRepeatMode.first()
                    val shuffleEnabled = appPreferences.savedShuffleEnabled.first()
                    val repeatMode =
                        runCatching { RepeatMode.valueOf(repeatModeStr) }.getOrDefault(RepeatMode.OFF)

                    withContext(Dispatchers.Main) {
                        playerAdapter.restoreQueue(
                            tracks = tracks,
                            startIndex = index,
                            positionMs = positionMs,
                            repeatMode = repeatMode,
                            shuffleEnabled = shuffleEnabled,
                        )
                    }
                }
            }.onFailure { e ->
                DiagnosticLogger.logError("PlaybackRepo", "QUEUE_RESTORE_FAILED", e.message ?: "Restore error")
            }
        }

        private suspend fun persistQueueItems(tracks: List<Track>) {
            runCatching {
                if (tracks.isEmpty()) {
                    queueDao.clearQueue()
                } else {
                    songDao.upsertAll(tracks.map { it.toEntity() })
                    queueDao.replaceQueue(
                        tracks.mapIndexed { index, track ->
                            QueueEntity(position = index, songId = track.id)
                        },
                    )
                }
            }.onFailure { e ->
                DiagnosticLogger.logError("PlaybackRepo", "QUEUE_PERSIST_FAILED", e.message ?: "Persist error")
            }
        }

        override fun playTrack(track: Track) {
            val traceId = DiagnosticLogger.generateTraceId()
            DiagnosticLogger.logTrackSelected(traceId, track.id, track.title)

            repositoryScope.launch {
                runCatching {
                    val resolved = streamResolver.resolve(track, traceId)
                    DiagnosticLogger.logMediaPrepare(traceId, resolved.id)
                    withContext(Dispatchers.Main) {
                        playerAdapter.playTrack(resolved)
                    }
                }.onFailure { e ->
                    DiagnosticLogger.logError(
                        traceId,
                        "MEDIA_PLAYBACK_FAILED",
                        e.message ?: "Failed to resolve stream for playTrack",
                    )
                }
            }
        }

        override fun addToQueue(track: Track) {
            val traceId = DiagnosticLogger.generateTraceId()
            DiagnosticLogger.logTrackSelected(traceId, track.id, track.title)

            repositoryScope.launch {
                runCatching {
                    val resolved = streamResolver.resolve(track, traceId)
                    DiagnosticLogger.logMediaPrepare(traceId, resolved.id)
                    withContext(Dispatchers.Main) {
                        playerAdapter.addToQueue(resolved)
                    }
                }.onFailure { e ->
                    DiagnosticLogger.logError(
                        traceId,
                        "MEDIA_PLAYBACK_FAILED",
                        e.message ?: "Failed to resolve stream for addToQueue",
                    )
                }
            }
        }

        override fun setQueue(
            tracks: List<Track>,
            startIndex: Int,
        ) {
            if (tracks.isEmpty()) return
            val traceId = DiagnosticLogger.generateTraceId()

            repositoryScope.launch {
                runCatching {
                    val targetIndex = startIndex.coerceIn(tracks.indices)
                    val targetTrack = tracks[targetIndex]
                    DiagnosticLogger.logTrackSelected(traceId, targetTrack.id, targetTrack.title)

                    val resolvedTarget = streamResolver.resolve(targetTrack, traceId)
                    val mutableQueue = tracks.toMutableList()
                    mutableQueue[targetIndex] = resolvedTarget

                    DiagnosticLogger.logMediaPrepare(traceId, resolvedTarget.id)
                    withContext(Dispatchers.Main) {
                        playerAdapter.setQueue(mutableQueue, targetIndex)
                    }
                }.onFailure { e ->
                    DiagnosticLogger.logError(
                        traceId,
                        "MEDIA_PLAYBACK_FAILED",
                        e.message ?: "Failed to resolve queue start track",
                    )
                }
            }
        }

        override fun moveTrackInQueue(
            fromIndex: Int,
            toIndex: Int,
        ) = playerAdapter.moveTrack(fromIndex, toIndex)

        override fun removeFromQueue(index: Int) = playerAdapter.removeFromQueue(index)

        override fun clearQueue() = playerAdapter.clearQueue()

        override fun play() = playerAdapter.play()

        override fun pause() = playerAdapter.pause()

        override fun seekTo(positionMs: Long) = playerAdapter.seekTo(positionMs)

        override fun skipToNext() = playerAdapter.skipToNext()

        override fun skipToPrevious() = playerAdapter.skipToPrevious()

        override fun setRepeatMode(mode: RepeatMode) = playerAdapter.setRepeatMode(mode)

        override fun toggleShuffle() = playerAdapter.toggleShuffle()
    }
