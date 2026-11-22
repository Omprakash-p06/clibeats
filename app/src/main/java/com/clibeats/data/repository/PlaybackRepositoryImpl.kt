@file:Suppress("ForbiddenImport", "MaxLineLength", "LongMethod", "TooManyFunctions", "LongParameterList")

package com.clibeats.data.repository

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.clibeats.data.local.dao.QueueDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.QueueEntity
import com.clibeats.data.local.mapper.toDomain
import com.clibeats.data.local.mapper.toEntity
import com.clibeats.data.network.NetworkMonitor
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.repository.HistoryRepository
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.PlayerAdapter
import com.clibeats.playback.StreamResolver
import com.clibeats.util.ARTWORK_FULL_PX
import com.clibeats.util.DiagnosticLogger
import com.clibeats.util.artworkUrlFor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        private val historyRepository: HistoryRepository? = null,
        private val imageLoader: dagger.Lazy<ImageLoader>? = null,
        private val appContext: Context? = null,
        private val providerRegistry: ProviderRegistry? = null,
        private val networkMonitor: NetworkMonitor? = null,
    ) : PlaybackRepository {
        @Inject
        constructor(
            playerAdapter: PlayerAdapter,
            streamResolver: StreamResolver,
            queueDao: QueueDao,
            songDao: SongDao,
            appPreferences: AppPreferences,
            historyRepository: HistoryRepository,
            imageLoader: dagger.Lazy<ImageLoader>,
            @ApplicationContext appContext: Context,
            providerRegistry: ProviderRegistry,
            networkMonitor: NetworkMonitor,
        ) : this(
            playerAdapter,
            streamResolver,
            queueDao,
            songDao,
            appPreferences,
            Dispatchers.IO,
            historyRepository,
            imageLoader,
            appContext,
            providerRegistry,
            networkMonitor,
        )

        private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

        /** Songs in the order they played (oldest first, last = playing now), across queues and sources. */
        private val playedHistory = ArrayDeque<Track>()

        /**
         * Songs left by pressing "previous" (last = the one "next" returns to). Next walks back through them in
         * order; playing anything else clears them. Guarded by [playedHistory]'s lock.
         */
        private val forward = ArrayDeque<Track>()

        /** Set while "previous" switches songs, so that switch isn't recorded as a new play. */
        @Volatile private var goingBack = false

        override val playbackState: StateFlow<PlaybackState> = playerAdapter.playbackState

        override val queueState: StateFlow<List<Track>> = playerAdapter.queueFlow

        override val playerMessages: Flow<String> = playerAdapter.messages

        override val pauseAtEndOfTrack: StateFlow<Boolean> = playerAdapter.pauseAtEndOfTrack

        override fun setPauseAtEndOfTrack(enabled: Boolean) = playerAdapter.setPauseAtEndOfTrack(enabled)

        override val sleepTimerCountdown: StateFlow<Int?> = playerAdapter.sleepTimerCountdown

        override fun startSleepTimer(minutes: Int) = playerAdapter.startSleepTimer(minutes)

        override fun cancelSleepTimer() = playerAdapter.cancelSleepTimer()

        private val restored = CompletableDeferred<Unit>()

        override suspend fun awaitRestored() = restored.await()

        init {
            repositoryScope.launch {
                restorePersistentQueue()
                restored.complete(Unit)
                // Start saving only after the restore: the player's empty initial state (no queue, song 0,
                // position 0) would otherwise overwrite the saved queue before it is read back, and the
                // resume card / next launch would start the wrong song from 0:00.
                // drop(1): the queue as it is right now is what was just restored (or nothing, if the
                // restore failed) - don't write it back, only save later changes.
                launch(start = CoroutineStart.UNDISPATCHED) {
                    playerAdapter.queueFlow.drop(1).collect { tracks -> persistQueueItems(tracks) }
                }
                launch(start = CoroutineStart.UNDISPATCHED) { persistPlaybackPosition() }
            }
            repositoryScope.launch {
                // On each new track: record history, warm stream URLs, and trigger infinite autoplay
                playerAdapter.playbackState
                    .map { it.currentTrack }
                    .distinctUntilChanged { a, b -> a?.id == b?.id }
                    .collectLatest { current ->
                        if (current == null) return@collectLatest
                        rememberPlayed(current)
                        runCatching { historyRepository?.recordPlay(current.id, current.providerId) }
                        prefetchAfter(current)
                        checkAutoplay(current)
                    }
            }
        }

        /**
         * Saves the current song, position and modes on meaningful changes (track, play/pause, modes) and
         * every few seconds while playing, so a resume after the app is killed starts close to where it was.
         */
        private suspend fun persistPlaybackPosition() {
            playerAdapter.playbackState
                .distinctUntilChanged { old, new ->
                    old.currentTrack?.id == new.currentTrack?.id &&
                        old.isPlaying == new.isPlaying &&
                        old.repeatMode == new.repeatMode &&
                        old.shuffleEnabled == new.shuffleEnabled &&
                        old.positionMs / POSITION_SAVE_STEP_MS == new.positionMs / POSITION_SAVE_STEP_MS
                }.collect { state ->
                    val current = state.currentTrack ?: return@collect
                    val index = playerAdapter.queueFlow.value.indexOfFirst { it.id == current.id }
                    if (index < 0) return@collect
                    appPreferences.saveQueueMetadata(
                        index = index,
                        positionMs = state.positionMs,
                        repeatMode = state.repeatMode.name,
                        shuffleEnabled = state.shuffleEnabled,
                    )
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
            // Streams resolve lazily inside ExoPlayer (TrackUriResolver); hand off immediately.
            repositoryScope.launch(Dispatchers.Main) { playerAdapter.playTrack(track) }
        }

        override fun addToQueue(track: Track) {
            val traceId = DiagnosticLogger.generateTraceId()
            DiagnosticLogger.logTrackSelected(traceId, track.id, track.title)
            // Streams resolve lazily inside ExoPlayer (TrackUriResolver); hand off immediately.
            repositoryScope.launch(Dispatchers.Main) { playerAdapter.addToQueue(track) }
        }

        override fun playNext(track: Track) {
            val traceId = DiagnosticLogger.generateTraceId()
            DiagnosticLogger.logTrackSelected(traceId, track.id, track.title)
            // Streams resolve lazily inside ExoPlayer (TrackUriResolver); hand off immediately.
            repositoryScope.launch(Dispatchers.Main) { playerAdapter.playNext(track) }
        }

        override fun setQueue(
            tracks: List<Track>,
            startIndex: Int,
        ) {
            if (tracks.isEmpty()) return
            val traceId = DiagnosticLogger.generateTraceId()

            repositoryScope.launch(Dispatchers.Main) {
                val targetIndex = startIndex.coerceIn(tracks.indices)
                DiagnosticLogger.logTrackSelected(traceId, tracks[targetIndex].id, tracks[targetIndex].title)
                playerAdapter.setQueue(tracks, targetIndex)
            }
        }

        private suspend fun prefetchAfter(current: Track) {
            val queue = playerAdapter.queueFlow.value
            val idx = queue.indexOfFirst { it.id == current.id }
            if (idx < 0) return
            queue.drop(idx + 1).take(PREFETCH_COUNT).forEach { next ->
                prefetchArtwork(next)
                if (next.streamUrl.isNullOrBlank() || next.streamUrl.contains("expire")) {
                    runCatching { streamResolver.resolve(next) }
                }
            }
        }

        @Suppress("ReturnCount")
        private suspend fun checkAutoplay(current: Track) {
            val enabled = runCatching { appPreferences.autoplayEnabled?.first() == true }.getOrDefault(false)
            if (!enabled) return
            if (playerAdapter.playbackState.value.repeatMode != RepeatMode.OFF) return
            if (networkMonitor?.isOnline?.value != true) return
            val queue = playerAdapter.queueFlow.value
            val currentIndex = queue.indexOfFirst { it.id == current.id }
            if (currentIndex < 0 || currentIndex < queue.lastIndex - 1) return

            val provider =
                providerRegistry?.getProvider(current.providerId)
                    ?: providerRegistry?.getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID)
                    ?: return
            val result = provider.relatedTracks(current.id)
            val related = (result as? com.clibeats.domain.provider.ProviderResult.Success)?.data.orEmpty()
            if (related.isEmpty()) return

            val recentHistory =
                runCatching {
                    historyRepository?.getRecentlyPlayedTracks(AUTOPLAY_HISTORY_DEDUP_LIMIT)
                        ?.first()
                        ?.map { it.id }
                        ?.toSet()
                }.getOrNull().orEmpty()

            val existingIds = queue.map { it.id }.toSet()
            val candidates = related.filter { it.id !in existingIds && it.id !in recentHistory }
            if (candidates.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerAdapter.addAllToQueue(candidates)
                }
            }
        }

        /** Warms the disk cache with the next tracks' high-res covers so Now Playing shows them instantly. */
        private fun prefetchArtwork(track: Track) {
            val context = appContext ?: return
            val url = artworkUrlFor(track.artworkUrl, ARTWORK_FULL_PX) ?: return
            runCatching {
                imageLoader?.get()?.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(ARTWORK_FULL_PX)
                        .build(),
                )
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

        override fun skipToIndex(index: Int) = playerAdapter.skipToIndex(index)

        /**
         * Any time in a song, "next" goes to the song that comes next: after going back with "previous", that's
         * the song you went back from (and the one before it, and so on); otherwise the next song in the queue.
         * The notification, lock screen and headset buttons come through here too.
         */
        override fun skipToNext() {
            val target = synchronized(playedHistory) { forward.lastOrNull() }
            when {
                target == null -> playerAdapter.skipToNext()
                playerAdapter.nextTrack()?.id == target.id -> playerAdapter.skipToNext()
                else -> playerAdapter.playForward(target)
            }
        }

        /**
         * Like every music player: more than [RESTART_THRESHOLD_MS] into a song, "previous" restarts it;
         * pressed again (near the start) it plays whatever played before, local or online, even if it was
         * in an earlier queue. Falls back to the previous item in the queue when there's no history yet.
         * The notification, lock screen and headset buttons come through here too.
         */
        override fun skipToPrevious() {
            if (playerAdapter.currentPositionMs() > RESTART_THRESHOLD_MS) {
                playerAdapter.seekTo(0L)
                return
            }
            val previous =
                synchronized(playedHistory) {
                    if (playedHistory.size < 2) {
                        null
                    } else {
                        playedHistory.removeLast()
                        playedHistory.last()
                    }
                }
            if (previous == null) {
                playerAdapter.skipToPrevious()
                return
            }
            goingBack = true
            // Remember where we came from, so "next" brings us back here.
            playerAdapter.playbackState.value.currentTrack?.let { leaving ->
                synchronized(playedHistory) { forward.addLast(leaving) }
            }
            // The song before in play order (shuffle-aware) is usually the one that played before: just go back.
            if (playerAdapter.previousTrack()?.id == previous.id) {
                playerAdapter.skipToPrevious()
            } else {
                playerAdapter.playBeforeCurrent(previous)
            }
        }

        private suspend fun rememberPlayed(current: Track) {
            if (goingBack) {
                goingBack = false
                return
            }
            synchronized(playedHistory) {
                // Arrived at the song "next" was meant to return to (by the button or by the previous song
                // ending): drop it from the way forward. Anywhere else means a new direction: forget the way forward.
                if (forward.lastOrNull()?.id == current.id) forward.removeLast() else forward.clear()
            }
            // After a restart, start from the saved listening history so "previous" works straight away.
            val seed =
                if (synchronized(playedHistory) { playedHistory.isEmpty() }) {
                    runCatching { historyRepository?.getRecentlyPlayedTracks(HISTORY_LIMIT)?.first() }
                        .getOrNull()
                        .orEmpty()
                        .asReversed()
                } else {
                    emptyList()
                }
            synchronized(playedHistory) {
                if (playedHistory.isEmpty()) seed.filter { it.id != current.id }.forEach(playedHistory::addLast)
                if (playedHistory.lastOrNull()?.id != current.id) playedHistory.addLast(current)
                while (playedHistory.size > HISTORY_LIMIT) playedHistory.removeFirst()
            }
        }

        override fun setRepeatMode(mode: RepeatMode) = playerAdapter.setRepeatMode(mode)

        override fun toggleShuffle() = playerAdapter.toggleShuffle()

        private companion object {
            const val PREFETCH_COUNT = 2
            const val HISTORY_LIMIT = 100
            const val AUTOPLAY_HISTORY_DEDUP_LIMIT = 25
            const val POSITION_SAVE_STEP_MS = 5_000L
            const val RESTART_THRESHOLD_MS = 3_000L
        }
    }
