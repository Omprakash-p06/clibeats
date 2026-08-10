@file:Suppress("MaxLineLength", "LongMethod")

package com.clibeats.data.repository

import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.PlayerAdapter
import com.clibeats.playback.StreamResolver
import com.clibeats.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepositoryImpl
    @Inject
    constructor(
        private val playerAdapter: PlayerAdapter,
        private val streamResolver: StreamResolver,
    ) : PlaybackRepository {
        private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override val playbackState: StateFlow<PlaybackState> = playerAdapter.playbackState

        override val queueState: StateFlow<List<Track>> = playerAdapter.queueFlow

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
