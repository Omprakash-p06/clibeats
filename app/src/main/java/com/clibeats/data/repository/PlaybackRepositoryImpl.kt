package com.clibeats.data.repository

import com.clibeats.core.logging.StructuredEvent
import com.clibeats.core.logging.StructuredLogger
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.playback.QueueManager
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.PlayerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        private val musicProvider: MusicProvider,
        private val queueManager: QueueManager,
    ) : PlaybackRepository {
        private val repositoryScope = CoroutineScope(Dispatchers.Main)

        override val playbackState: StateFlow<PlaybackState> = playerAdapter.playbackState

        override val queueState: StateFlow<List<Track>> = queueManager.queue

        override fun playTrack(track: Track) {
            val traceId = StructuredLogger.generateTraceId()
            StructuredLogger.log(StructuredEvent.TrackSelected(traceId, track.id, track.title))
            repositoryScope.launch {
                val start = System.currentTimeMillis()
                StructuredLogger.log(StructuredEvent.StreamRequest(traceId, track.id))
                val resolvedTrack = withContext(Dispatchers.IO) { ensureStreamUrl(track) }
                val duration = System.currentTimeMillis() - start

                if (!resolvedTrack.streamUrl.isNullOrBlank()) {
                    StructuredLogger.log(StructuredEvent.StreamResolved(traceId, track.id, duration))
                    StructuredLogger.log(StructuredEvent.PlayerPreparing(traceId, track.id))
                    playerAdapter.playTrack(resolvedTrack)
                    StructuredLogger.log(StructuredEvent.PlayerPlaying(traceId, track.id))
                } else {
                    StructuredLogger.log(StructuredEvent.PlayerError(traceId, "STREAM_RESOLUTION", "Could not resolve stream URL for track: ${track.id}"))
                }
            }
        }

        override fun setQueue(
            tracks: List<Track>,
            startIndex: Int,
        ) {
            queueManager.setQueue(tracks, startIndex)
            val current = queueManager.currentTrack()
            if (current != null) {
                playTrack(current)
            }
        }

        private suspend fun ensureStreamUrl(track: Track): Track {
            if (!track.streamUrl.isNullOrBlank()) {
                return track
            }
            return when (val result = musicProvider.stream(track.id)) {
                is ProviderResult.Success -> track.copy(streamUrl = result.data)
                else -> track
            }
        }

        override fun moveTrackInQueue(
            fromIndex: Int,
            toIndex: Int,
        ) = queueManager.moveTrack(fromIndex, toIndex)

        override fun removeFromQueue(index: Int) = queueManager.removeTrack(index)

        override fun clearQueue() = queueManager.clearQueue()

        override fun play() = playerAdapter.play()

        override fun pause() = playerAdapter.pause()

        override fun seekTo(positionMs: Long) = playerAdapter.seekTo(positionMs)

        override fun skipToNext() {
            val next = queueManager.nextTrack()
            if (next != null) {
                playTrack(next)
            }
        }

        override fun skipToPrevious() {
            val prev = queueManager.previousTrack()
            if (prev != null) {
                playTrack(prev)
            }
        }

        override fun setRepeatMode(mode: RepeatMode) = playerAdapter.setRepeatMode(mode)

        override fun toggleShuffle() = playerAdapter.toggleShuffle()
    }
