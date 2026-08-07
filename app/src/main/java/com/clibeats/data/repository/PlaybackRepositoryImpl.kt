package com.clibeats.data.repository

import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.PlayerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepositoryImpl
    @Inject
    constructor(
        private val playerAdapter: PlayerAdapter,
        private val musicProvider: MusicProvider,
    ) : PlaybackRepository {
        private val repositoryScope = CoroutineScope(Dispatchers.IO)

        override val playbackState: StateFlow<PlaybackState> = playerAdapter.playbackState

        override val queueState: StateFlow<List<Track>> = playerAdapter.queueFlow

        override fun playTrack(track: Track) {
            repositoryScope.launch {
                val resolvedTrack = ensureStreamUrl(track)
                playerAdapter.playTrack(resolvedTrack)
            }
        }

        override fun setQueue(
            tracks: List<Track>,
            startIndex: Int,
        ) {
            repositoryScope.launch {
                val targetIndex = startIndex.coerceIn(tracks.indices)
                val resolvedTracks =
                    tracks.mapIndexed { index, track ->
                        if (index == targetIndex) ensureStreamUrl(track) else track
                    }
                playerAdapter.setQueue(resolvedTracks, targetIndex)
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

        private fun String?.isNullOrBlank(): Boolean = this == null || this.trim().isEmpty()

        private fun String?.isNullOrNotBlank(): Boolean = !this.isNullOrBlank()

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
