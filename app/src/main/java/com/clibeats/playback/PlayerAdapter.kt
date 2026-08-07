// ForbiddenImport: playback engine implementation imports are legitimate
@file:Suppress("ForbiddenImport")

package com.clibeats.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.clibeats.data.cache.CacheManager
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerAdapter
    @Inject
    constructor(
        private val player: ExoPlayer,
        private val cacheManager: CacheManager,
    ) {
        private val adapterScope = CoroutineScope(Dispatchers.Main)
        private var tickerJob: Job? = null

        private val _playbackState =
            MutableStateFlow(
                PlaybackState(
                    currentTrack = null,
                    isPlaying = false,
                    positionMs = 0L,
                    bufferedPositionMs = 0L,
                    repeatMode = RepeatMode.OFF,
                    shuffleEnabled = false,
                ),
            )
        val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

        private val _queueFlow = MutableStateFlow<List<Track>>(emptyList())
        val queueFlow: StateFlow<List<Track>> = _queueFlow.asStateFlow()

        private val trackList = mutableListOf<Track>()

        init {
            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateState()
                        if (isPlaying) {
                            startTicker()
                        } else {
                            stopTicker()
                        }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        updateState()
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int,
                    ) {
                        updateState()
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        updateState()
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        updateState()
                    }
                },
            )
        }

        private fun startTicker() {
            stopTicker()
            tickerJob =
                adapterScope.launch {
                    while (isActive) {
                        delay(500L)
                        updateState()
                    }
                }
        }

        private fun stopTicker() {
            tickerJob?.cancel()
            tickerJob = null
        }

        fun playTrack(track: Track) {
            trackList.clear()
            trackList.add(track)
            val mediaItem = track.toMediaItem()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            updateState()
        }

        fun setQueue(
            tracks: List<Track>,
            startIndex: Int = 0,
        ) {
            trackList.clear()
            trackList.addAll(tracks)
            val mediaItems = tracks.map { it.toMediaItem() }
            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            player.play()
            updateState()
        }

        fun play() {
            player.play()
        }

        fun pause() {
            player.pause()
        }

        fun seekTo(positionMs: Long) {
            player.seekTo(positionMs)
        }

        fun skipToNext() {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            }
        }

        fun skipToPrevious() {
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            }
        }

        fun setRepeatMode(mode: RepeatMode) {
            player.repeatMode =
                when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            updateState()
        }

        fun toggleShuffle() {
            player.shuffleModeEnabled = !player.shuffleModeEnabled
            updateState()
        }

        fun moveTrack(
            fromIndex: Int,
            toIndex: Int,
        ) {
            if (fromIndex in trackList.indices && toIndex in trackList.indices && fromIndex != toIndex) {
                val track = trackList.removeAt(fromIndex)
                trackList.add(toIndex, track)
                player.moveMediaItem(fromIndex, toIndex)
                updateState()
            }
        }

        fun removeFromQueue(index: Int) {
            if (index in trackList.indices) {
                trackList.removeAt(index)
                player.removeMediaItem(index)
                updateState()
            }
        }

        fun clearQueue() {
            trackList.clear()
            player.clearMediaItems()
            updateState()
        }

        private fun updateState() {
            _queueFlow.value = trackList.toList()
            val currentIndex = player.currentMediaItemIndex
            val currentTrack = if (currentIndex in trackList.indices) trackList[currentIndex] else null
            val mappedRepeatMode =
                when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }

            _playbackState.value =
                PlaybackState(
                    currentTrack = currentTrack,
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                    repeatMode = mappedRepeatMode,
                    shuffleEnabled = player.shuffleModeEnabled,
                )
        }

        private fun Track.toMediaItem(): MediaItem {
            val cachedFile = cacheManager.getCachedFileDirect(id)
            val uri =
                if (cachedFile != null && cachedFile.exists()) {
                    android.net.Uri.fromFile(cachedFile)
                } else {
                    streamUrl?.let { android.net.Uri.parse(it) } ?: android.net.Uri.EMPTY
                }

            return MediaItem.Builder()
                .setMediaId(id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
                        .build(),
                )
                .build()
        }
    }
