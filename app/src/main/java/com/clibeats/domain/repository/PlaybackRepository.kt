@file:Suppress("TooManyFunctions")

package com.clibeats.domain.repository

import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackRepository {
    val playbackState: StateFlow<PlaybackState>

    val queueState: StateFlow<List<Track>>

    fun playTrack(track: Track)

    fun setQueue(
        tracks: List<Track>,
        startIndex: Int = 0,
    )

    fun moveTrackInQueue(
        fromIndex: Int,
        toIndex: Int,
    )

    fun removeFromQueue(index: Int)

    fun clearQueue()

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun skipToNext()

    fun skipToPrevious()

    fun setRepeatMode(mode: RepeatMode)

    fun toggleShuffle()
}
