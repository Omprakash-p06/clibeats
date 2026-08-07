package com.clibeats.domain.playback

import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager
    @Inject
    constructor() {
        private val _queue = MutableStateFlow<List<Track>>(emptyList())
        val queue: StateFlow<List<Track>> = _queue.asStateFlow()

        private var currentIndex: Int = -1
        private var isShuffleEnabled: Boolean = false
        private var repeatMode: RepeatMode = RepeatMode.OFF

        fun setQueue(
            tracks: List<Track>,
            startIndex: Int = 0,
        ) {
            _queue.value = tracks
            currentIndex = if (tracks.isNotEmpty()) startIndex.coerceIn(tracks.indices) else -1
        }

        fun currentTrack(): Track? = _queue.value.getOrNull(currentIndex)

        fun nextTrack(): Track? {
            val q = _queue.value
            if (q.isEmpty() || currentIndex == -1) return null
            if (repeatMode == RepeatMode.ONE) return q.getOrNull(currentIndex)

            val nextIdx = currentIndex + 1
            return if (nextIdx in q.indices) {
                currentIndex = nextIdx
                q[currentIndex]
            } else if (repeatMode == RepeatMode.ALL) {
                currentIndex = 0
                q.getOrNull(0)
            } else {
                null
            }
        }

        fun previousTrack(): Track? {
            val q = _queue.value
            if (q.isEmpty() || currentIndex == -1) return null
            val prevIdx = currentIndex - 1
            return if (prevIdx in q.indices) {
                currentIndex = prevIdx
                q[currentIndex]
            } else {
                q.getOrNull(currentIndex)
            }
        }

        fun moveTrack(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val list = _queue.value.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                _queue.value = list
            }
        }

        fun removeTrack(index: Int) {
            val list = _queue.value.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                _queue.value = list
            }
        }

        fun clearQueue() {
            _queue.value = emptyList()
            currentIndex = -1
        }
    }
