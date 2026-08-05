package com.clibeats.data.repository

import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.PlayerAdapter
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepositoryImpl
    @Inject
    constructor(
        private val playerAdapter: PlayerAdapter,
    ) : PlaybackRepository {
        override val playbackState: StateFlow<PlaybackState> = playerAdapter.playbackState

        override fun playTrack(track: Track) = playerAdapter.playTrack(track)

        override fun setQueue(
            tracks: List<Track>,
            startIndex: Int,
        ) = playerAdapter.setQueue(tracks, startIndex)

        override fun play() = playerAdapter.play()

        override fun pause() = playerAdapter.pause()

        override fun seekTo(positionMs: Long) = playerAdapter.seekTo(positionMs)

        override fun skipToNext() = playerAdapter.skipToNext()

        override fun skipToPrevious() = playerAdapter.skipToPrevious()

        override fun setRepeatMode(mode: RepeatMode) = playerAdapter.setRepeatMode(mode)

        override fun toggleShuffle() = playerAdapter.toggleShuffle()
    }
