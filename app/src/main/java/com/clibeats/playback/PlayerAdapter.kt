// ForbiddenImport: playback engine implementation imports are legitimate
@file:Suppress("ForbiddenImport", "TooManyFunctions", "ReturnCount", "MagicNumber", "LargeClass")

package com.clibeats.playback

import android.content.Context
import android.content.Intent
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.playback.service.PlaybackService
import com.clibeats.util.ARTWORK_FULL_PX
import com.clibeats.util.DiagnosticLogger
import com.clibeats.util.artworkUrlFor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Singleton
class PlayerAdapter
    @Inject
    constructor(
        private val player: ExoPlayer,
        private val streamResolver: StreamResolver,
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Starts the media session foreground service so playback keeps running
         * in the background and media controls appear in the notification
         * shade / lock screen. The service wraps the same singleton [ExoPlayer].
         */
        private fun startPlaybackService() {
            // Plain start: Media3 promotes the service to foreground itself once playback starts.
            // (startForegroundService would crash if Media3 hadn't posted its notification within 5s.)
            runCatching { context.startService(Intent(context, PlaybackService::class.java)) }
        }

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

        /** One-off messages for the UI, e.g. a song that couldn't be played and was skipped. */
        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = MESSAGE_BUFFER)
        val messages: SharedFlow<String> = _messages

        /** True while the "end of track" sleep timer is armed (the player pauses when the song ends). */
        private val _pauseAtEndOfTrack = MutableStateFlow(false)
        val pauseAtEndOfTrack: StateFlow<Boolean> = _pauseAtEndOfTrack.asStateFlow()

        private val _sleepTimerCountdown = MutableStateFlow<Int?>(null)
        val sleepTimerCountdown: StateFlow<Int?> = _sleepTimerCountdown.asStateFlow()
        private var sleepCountdownJob: Job? = null

        // Songs that failed in a row; stops skipping when a whole queue is unplayable (e.g. offline).
        private var consecutiveFailures = 0

        private val trackList = mutableListOf<Track>()
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var tickerJob: Job? = null

        private fun startPositionTicker() {
            tickerJob?.cancel()
            tickerJob =
                scope.launch {
                    while (player.isPlaying) {
                        updateState()
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }

        private fun stopPositionTicker() {
            tickerJob?.cancel()
            tickerJob = null
        }

        init {
            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            DiagnosticLogger.logMediaPlaying("player")
                            startPositionTicker()
                        } else {
                            stopPositionTicker()
                        }
                        updateState()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            DiagnosticLogger.logMediaReady("player")
                            consecutiveFailures = 0
                        }
                        // The last song ended on its own: the "end of track" timer has done its job.
                        if (playbackState == Player.STATE_ENDED) setPauseAtEndOfTrack(false)
                        updateState()
                    }

                    override fun onPlayWhenReadyChanged(
                        playWhenReady: Boolean,
                        reason: Int,
                    ) {
                        // Paused by pauseAtEndOfMediaItems (works with repeat-one too): disarm the timer.
                        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                            setPauseAtEndOfTrack(false)
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        DiagnosticLogger.logError("player", "MEDIA_ERROR", error.message ?: "ExoPlayer error")
                        if (retryAfterStreamError(error)) return
                        skipAfterError()
                        updateState()
                    }

                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        updateState()
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        retriedMediaId = null
                        val current = player.currentMediaItemIndex
                        if (current in trackList.indices) {
                            resolveTrackBitrate(trackList[current])
                        }
                        updateWakeMode()
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

        fun playTrack(track: Track) {
            startPlaybackService()
            unshuffledOrder = null
            trackList.clear()
            trackList.add(track)
            syncQueue()
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
            startPlaybackService()
            // With shuffle on, a newly started list plays shuffled: the tapped song first, the rest mixed.
            val ordered: List<Track>
            val start: Int
            if (shuffleOn && tracks.size > 1) {
                unshuffledOrder = tracks.toList()
                ordered = shuffledAround(tracks, startIndex.coerceIn(tracks.indices))
                start = 0
            } else {
                unshuffledOrder = null
                ordered = tracks
                start = startIndex
            }
            trackList.clear()
            trackList.addAll(ordered)
            syncQueue()
            val mediaItems = ordered.map { it.toMediaItem() }
            player.setMediaItems(mediaItems, start, 0L)
            player.prepare()
            player.play()
            updateState()
        }

        fun addToQueue(track: Track) {
            startPlaybackService()
            trackList.add(track)
            syncQueue()
            val mediaItem = track.toMediaItem()
            player.addMediaItem(mediaItem)
            if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 1) {
                player.prepare()
            }
            updateState()
        }

        fun addAllToQueue(tracks: List<Track>) {
            if (tracks.isEmpty()) return
            startPlaybackService()
            trackList.addAll(tracks)
            syncQueue()
            val mediaItems = tracks.map { it.toMediaItem() }
            player.addMediaItems(mediaItems)
            if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == tracks.size) {
                player.prepare()
            }
            updateState()
        }

        fun playNext(track: Track) {
            startPlaybackService()
            val targetIndex =
                if (trackList.isEmpty()) {
                    0
                } else {
                    (player.currentMediaItemIndex + 1).coerceIn(0, trackList.size)
                }
            trackList.add(targetIndex, track)
            syncQueue()
            player.addMediaItem(targetIndex, track.toMediaItem())
            if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 1) {
                player.prepare()
            }
            updateState()
        }

        fun restoreQueue(
            tracks: List<Track>,
            startIndex: Int = 0,
            positionMs: Long = 0L,
            repeatMode: RepeatMode = RepeatMode.OFF,
            shuffleEnabled: Boolean = false,
        ) {
            if (tracks.isEmpty()) return
            trackList.clear()
            trackList.addAll(tracks)
            syncQueue()
            val mediaItems = tracks.map { it.toMediaItem() }
            val validIndex = startIndex.coerceIn(tracks.indices)
            player.setMediaItems(mediaItems, validIndex, positionMs.coerceAtLeast(0L))
            player.repeatMode =
                when (repeatMode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            player.shuffleModeEnabled = false
            shuffleOn = shuffleEnabled
            unshuffledOrder = null
            updateState()
        }

        fun play() {
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
                player.prepare()
            }
            // Resuming a restored queue must also start the media service; otherwise there's no
            // notification / lock-screen card and Android may kill background playback.
            if (player.mediaItemCount > 0) startPlaybackService()
            player.play()
        }

        fun pause() {
            player.pause()
        }

        fun seekTo(positionMs: Long) {
            player.seekTo(positionMs)
        }

        fun skipToIndex(index: Int) {
            if (index in trackList.indices) {
                player.seekTo(index, 0L)
                player.play()
            }
        }

        fun skipToNext() {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            }
        }

        /** The track "next" would play, or null at the end of the queue. */
        fun nextTrack(): Track? = trackList.getOrNull(player.nextMediaItemIndex)

        /** The track "previous" would play, or null at the start of the queue. */
        fun previousTrack(): Track? = trackList.getOrNull(player.previousMediaItemIndex)

        /**
         * Plays [track]: jumps to it if it's already in the queue (the copy nearest after the current song),
         * otherwise inserts it right after the current song. Used by "next" to return to a song left via "previous".
         */
        fun playForward(track: Track) {
            val current = player.currentMediaItemIndex
            val existing =
                trackList.indices
                    .filter { trackList[it].id == track.id && it != current }
                    .minByOrNull { if (it > current) it - current else it + trackList.size }
            if (existing != null) {
                player.seekTo(existing, 0L)
            } else {
                startPlaybackService()
                val index = (current + 1).coerceIn(0, trackList.size)
                trackList.add(index, track)
                syncQueue()
                player.addMediaItem(index, track.toMediaItem())
                player.seekTo(index, 0L)
                player.prepare()
            }
            player.play()
            updateState()
        }

        /** Previous item in the queue; at the start of the queue it restarts the current song. */
        fun skipToPrevious() {
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else if (player.mediaItemCount > 0) {
                player.seekTo(0L)
            }
        }

        /** Live playback position of the current song (not the last 500 ms tick). */
        fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

        /** Index of the playing item in the queue, or -1 when the queue is empty. */
        fun currentIndex(): Int = if (trackList.isEmpty()) -1 else player.currentMediaItemIndex

        /**
         * Plays [track] by inserting it just before the current item, so the rest of the queue is kept
         * and "next" returns to the song that was playing. Used by "previous" for songs from earlier queues.
         */
        fun playBeforeCurrent(track: Track) {
            startPlaybackService()
            val index = player.currentMediaItemIndex.coerceIn(0, trackList.size)
            trackList.add(index, track)
            syncQueue()
            player.addMediaItem(index, track.toMediaItem())
            player.seekTo(index, 0L)
            player.prepare()
            player.play()
            updateState()
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

        /**
         * Shuffle reorders the queue itself, so the Queue screen, "next" and "previous" all follow what you see:
         * the playing song moves to the top and every other song is mixed below it. Turning it off puts the
         * songs back in their original order (songs added meanwhile go at the end). The playing song never stops.
         */
        fun toggleShuffle() {
            val current = player.currentMediaItemIndex
            if (current !in trackList.indices || trackList.size < 2) {
                shuffleOn = !shuffleOn
                unshuffledOrder = null
                updateState()
                return
            }
            if (!shuffleOn) {
                unshuffledOrder = trackList.toList()
                applyOrder(shuffledAround(trackList, current), current)
                shuffleOn = true
            } else {
                val original = unshuffledOrder
                if (original != null) {
                    val present = trackList.toMutableList()
                    // Same Track objects as before shuffling (removed ones dropped), then anything added since.
                    val restored =
                        original.filter { o ->
                            val i = present.indexOfFirst { it === o }
                            if (i >= 0) present.removeAt(i)
                            i >= 0
                        }
                    applyOrder(restored + present, current)
                }
                unshuffledOrder = null
                shuffleOn = false
            }
            updateState()
        }

        /** [tracks] with the one at [keep] first and all the others in random order. */
        private fun shuffledAround(
            tracks: List<Track>,
            keep: Int,
        ): List<Track> = listOf(tracks[keep]) + tracks.filterIndexed { i, _ -> i != keep }.shuffled()

        /**
         * Rebuilds the queue in [order] (the same Track objects, playing one included) around the item at
         * [current], which keeps playing: everything else is removed and re-added on both sides of it.
         */
        private fun applyOrder(
            order: List<Track>,
            current: Int,
        ) {
            val playing = trackList[current]
            val at = order.indexOfFirst { it === playing }.takeIf { it >= 0 } ?: return
            if (current + 1 < trackList.size) player.removeMediaItems(current + 1, trackList.size)
            if (current > 0) player.removeMediaItems(0, current)
            val before = order.subList(0, at)
            val after = order.subList(at + 1, order.size)
            if (before.isNotEmpty()) player.addMediaItems(0, before.map { it.toMediaItem() })
            if (after.isNotEmpty()) player.addMediaItems(after.map { it.toMediaItem() })
            trackList.clear()
            trackList.addAll(order)
            syncQueue()
        }

        /** Whether shuffle is on (our own flag: the queue is reordered rather than ExoPlayer shuffling it). */
        private var shuffleOn = false

        /** The queue order from before shuffle was turned on, to put back when it's turned off. */
        private var unshuffledOrder: List<Track>? = null

        fun moveTrack(
            fromIndex: Int,
            toIndex: Int,
        ) {
            if (fromIndex in trackList.indices && toIndex in trackList.indices && fromIndex != toIndex) {
                val track = trackList.removeAt(fromIndex)
                trackList.add(toIndex, track)
                syncQueue()
                player.moveMediaItem(fromIndex, toIndex)
                updateState()
            }
        }

        fun removeFromQueue(index: Int) {
            if (index in trackList.indices) {
                trackList.removeAt(index)
                syncQueue()
                player.removeMediaItem(index)
                updateState()
            }
        }

        /** Clears everything except the song that's playing, so playback continues. */
        fun clearQueue() {
            val current = player.currentMediaItemIndex
            if (current !in trackList.indices) {
                trackList.clear()
                player.clearMediaItems()
            } else {
                if (current + 1 < trackList.size) player.removeMediaItems(current + 1, trackList.size)
                if (current > 0) player.removeMediaItems(0, current)
                val keep = trackList[current]
                trackList.clear()
                trackList.add(keep)
            }
            syncQueue()
            updateState()
        }

        /**
         * Battery: a local file only needs the CPU kept awake; a stream also needs Wi-Fi kept on. Holding the
         * Wi-Fi lock for local playback wastes power, so the lock follows the song that's playing.
         */
        private fun updateWakeMode() {
            val scheme = player.currentMediaItem?.localConfiguration?.uri?.scheme
            val local = scheme == "content" || scheme == "file"
            player.setWakeMode(if (local) C.WAKE_MODE_LOCAL else C.WAKE_MODE_NETWORK)
        }

        private fun syncQueue() {
            _queueFlow.value = trackList.toList()
        }

        // Local files often carry no bitrate in the container; derive it from file size / duration once.
        private val localBitrateKbps = mutableMapOf<String, Int>()

        private fun estimateLocalBitrate(track: Track) {
            val uri = track.streamUrl?.takeIf { track.providerId == "local" } ?: return
            val durationMs = player.duration.takeIf { it > 0 } ?: return
            if (localBitrateKbps.containsKey(track.id)) return
            localBitrateKbps[track.id] = 0
            scope.launch {
                val bytes =
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver
                                .openAssetFileDescriptor(android.net.Uri.parse(uri), "r")
                                ?.use { it.length }
                        }.getOrNull()
                    } ?: return@launch
                if (bytes > 0) {
                    localBitrateKbps[track.id] = (bytes * BITS_PER_BYTE / durationMs).toInt()
                    updateState()
                }
            }
        }

        private val memoizedBitrateKbps = mutableMapOf<String, Int>()

        private fun resolveTrackBitrate(track: Track) {
            if (memoizedBitrateKbps.containsKey(track.id)) return
            val durationMs = player.duration.takeIf { it > 0 } ?: track.durationMs
            scope.launch(Dispatchers.IO) {
                val bitrate = streamResolver.knownBitrateKbps(track, durationMs)
                if (bitrate != null && bitrate > 0) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        memoizedBitrateKbps[track.id] = bitrate
                        updateState()
                    }
                }
            }
        }

        private fun mappedRepeatMode(): RepeatMode =
            when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }

        @Suppress("CyclomaticComplexMethod")
        private fun updateState() {
            val currentIndex = player.currentMediaItemIndex
            val currentTrack = if (currentIndex in trackList.indices) trackList[currentIndex] else null
            val format = runCatching { player.audioFormat }.getOrNull()
            val formatBitrate =
                format?.let { f ->
                    listOf(f.averageBitrate, f.bitrate, f.peakBitrate).firstOrNull { it > 0 }?.div(BITS_PER_KBIT)
                }
            if (format != null && formatBitrate == null && currentTrack != null) estimateLocalBitrate(currentTrack)
            if (formatBitrate == null && currentTrack != null && !memoizedBitrateKbps.containsKey(currentTrack.id)) {
                resolveTrackBitrate(currentTrack)
            }

            _playbackState.value =
                PlaybackState(
                    currentTrack = currentTrack,
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                    repeatMode = mappedRepeatMode(),
                    shuffleEnabled = shuffleOn,
                    codec = format?.sampleMimeType?.let(::codecLabel),
                    bitrateKbps =
                        formatBitrate
                            ?: currentTrack?.let { track ->
                                localBitrateKbps[track.id] ?: memoizedBitrateKbps[track.id]
                            },
                    sampleRateHz = format?.sampleRate?.takeIf { it > 0 },
                    bitDepth = format?.pcmEncoding?.let(::bitDepthOf),
                    durationMs = player.duration.takeIf { it > 0 } ?: (currentTrack?.durationMs ?: 0L),
                )
        }

        private var retriedMediaId: String? = null

        /** Arms or disarms the "end of track" sleep timer: pause when the current song finishes. */
        fun setPauseAtEndOfTrack(enabled: Boolean) {
            player.pauseAtEndOfMediaItems = enabled
            _pauseAtEndOfTrack.value = enabled
        }

        fun startSleepTimer(minutes: Int) {
            sleepCountdownJob?.cancel()
            setPauseAtEndOfTrack(false)
            if (minutes <= 0) {
                _sleepTimerCountdown.value = null
                return
            }
            sleepCountdownJob =
                scope.launch {
                    for (left in minutes downTo 1) {
                        _sleepTimerCountdown.value = left
                        delay(60_000L)
                    }
                    pause()
                    _sleepTimerCountdown.value = null
                }
        }

        fun cancelSleepTimer() {
            sleepCountdownJob?.cancel()
            sleepCountdownJob = null
            _sleepTimerCountdown.value = null
            setPauseAtEndOfTrack(false)
        }

        /**
         * A song that still fails after the retry would leave the queue stuck: tell the user and move
         * on to the next song. Gives up after [MAX_CONSECUTIVE_FAILURES] in a row.
         */
        private fun skipAfterError() {
            val title = trackList.getOrNull(player.currentMediaItemIndex)?.title ?: "this song"
            consecutiveFailures++
            if (player.hasNextMediaItem() && consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                _messages.tryEmit("Couldn't play \"$title\" — skipped")
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            } else {
                _messages.tryEmit("Couldn't play \"$title\"")
            }
        }

        /**
         * Expired/forbidden stream (HTTP 403/410) or a failed resolve: invalidate the cached
         * URL and re-prepare once. The resolving data source fetches a fresh URL.
         */
        private fun retryAfterStreamError(error: androidx.media3.common.PlaybackException): Boolean {
            val retryable =
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED
            val index = player.currentMediaItemIndex
            val track = trackList.getOrNull(index)
            if (!retryable || track == null || retriedMediaId == track.id) return false
            retriedMediaId = track.id
            streamResolver.invalidate(track.providerId, track.id)
            val position = player.currentPosition.coerceAtLeast(0L)
            // Drop any stale direct URL so the placeholder resolver runs again.
            val fresh = track.copy(streamUrl = track.streamUrl?.takeUnless { it.startsWith("http") })
            trackList[index] = fresh
            player.replaceMediaItem(index, fresh.toMediaItem())
            player.seekTo(index, position)
            player.prepare()
            player.play()
            return true
        }

        private fun Track.toMediaItem(): MediaItem {
            // Local files and non-expiring provider URLs play directly. Remote tracks get a placeholder URI that
            // TrackUriResolver resolves (cache file -> fresh stream URL) when ExoPlayer opens it,
            // so queued tracks are playable and expired URLs never reach the network.
            // Signed (expiring) URLs, e.g. googlevideo `expire=`, always go through the resolver.
            val direct = streamUrl?.takeIf { it.isNotBlank() && !it.contains("expire") }
            val uri = direct?.let { android.net.Uri.parse(it) } ?: TrackUriResolver.placeholderUri(this)

            return MediaItem.Builder()
                .setMediaId(id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        // High-res cover for the notification, lock screen and Bluetooth displays.
                        .setArtworkUri(artworkUrlFor(artworkUrl, ARTWORK_FULL_PX)?.let { android.net.Uri.parse(it) })
                        // Original URL, so ArtworkBitmapLoader can fall back to smaller sizes of the same cover.
                        .setExtras(
                            android.os.Bundle().apply {
                                putString(ArtworkBitmapLoader.ARTWORK_SOURCE_KEY, artworkUrl)
                            },
                        ).build(),
                )
                .build()
        }

        companion object {
            private const val POLL_INTERVAL_MS = 500L
            private const val BITS_PER_BYTE = 8L
            private const val BITS_PER_KBIT = 1000
            private const val MESSAGE_BUFFER = 4
            private const val MAX_CONSECUTIVE_FAILURES = 5

            fun codecLabel(mime: String): String =
                when (mime) {
                    androidx.media3.common.MimeTypes.AUDIO_FLAC -> "FLAC"
                    androidx.media3.common.MimeTypes.AUDIO_MPEG -> "MP3"
                    androidx.media3.common.MimeTypes.AUDIO_OPUS -> "OPUS"
                    androidx.media3.common.MimeTypes.AUDIO_AAC -> "AAC"
                    androidx.media3.common.MimeTypes.AUDIO_RAW, androidx.media3.common.MimeTypes.AUDIO_WAV -> "WAV"
                    androidx.media3.common.MimeTypes.AUDIO_VORBIS -> "VORBIS"
                    androidx.media3.common.MimeTypes.AUDIO_ALAC -> "ALAC"
                    else -> mime.substringAfter('/').uppercase()
                }

            fun bitDepthOf(pcmEncoding: Int): Int? =
                when (pcmEncoding) {
                    androidx.media3.common.C.ENCODING_PCM_16BIT -> 16
                    androidx.media3.common.C.ENCODING_PCM_24BIT -> 24
                    androidx.media3.common.C.ENCODING_PCM_32BIT, androidx.media3.common.C.ENCODING_PCM_FLOAT -> 32
                    androidx.media3.common.C.ENCODING_PCM_8BIT -> 8
                    else -> null
                }
        }
    }
