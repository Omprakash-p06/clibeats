@file:Suppress("ForbiddenImport")

package com.clibeats.playback.waveform

import android.content.Context
import android.net.Uri
import com.clibeats.data.cache.CacheManager
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.Track
import com.clibeats.playback.PlayerAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** The waveform of [trackId]: bar heights to draw (0..1), NaN where the loudness isn't known yet. */
data class Waveform(
    val trackId: String,
    val heights: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Waveform) return false
        return other.trackId == trackId && other.heights.contentEquals(heights)
    }

    override fun hashCode(): Int = 31 * trackId.hashCode() + heights.contentHashCode()
}

/**
 * Real loudness for the Now Playing waveform, for whatever is playing:
 *
 * - Saved earlier: shown at once.
 * - Local song, or a song downloaded in full: decoded in the background ([WaveformAnalyzer]), bars filling
 *   in as it goes, then saved.
 * - Streamed song: measured while it plays ([LoudnessTap]); each bar turns real once it has been heard,
 *   and what was heard is saved, so the next play starts with those bars.
 */
@Singleton
class WaveformRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val playerAdapter: PlayerAdapter,
        private val tap: LoudnessTap,
        private val analyzer: WaveformAnalyzer,
        private val cacheManager: CacheManager,
    ) {
        private val store = WaveformStore(File(context.noBackupFilesDir, "waveforms"))
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private val _current = MutableStateFlow<Waveform?>(null)
        val current: StateFlow<Waveform?> = _current.asStateFlow()

        private var trackId: String? = null
        private var saved: FloatArray? = null
        private var complete = false
        private var live = LiveLoudnessTracker()
        private var liveDirty = false
        private var wasPlaying = false
        private var loadJob: Job? = null

        init {
            scope.launch { playerAdapter.playbackState.collect(::onState) }
        }

        private fun onState(state: PlaybackState) {
            val track = state.currentTrack
            if (track?.id != trackId) switchTo(track, state)
            val (sum, count) = tap.drain()
            val duration = state.durationMs.takeIf { it > 0 } ?: track?.durationMs ?: 0L
            if (!complete && track != null &&
                live.onTick(state.positionMs, duration, state.isPlaying, sum, count)
            ) {
                liveDirty = true
            }
            // Keep what was heard when playback stops (the app may be closed while paused).
            if (wasPlaying && !state.isPlaying) saveLive()
            wasPlaying = state.isPlaying
        }

        private fun switchTo(
            track: Track?,
            state: PlaybackState,
        ) {
            saveLive()
            loadJob?.cancel()
            tap.drain() // audio from the previous song
            trackId = track?.id
            saved = null
            complete = false
            live = LiveLoudnessTracker()
            liveDirty = false
            _current.value = null
            if (track == null) {
                return
            }
            val id = track.id
            val durationHint = state.durationMs.takeIf { it > 0 } ?: track.durationMs
            loadJob =
                scope.launch {
                    val stored = withContext(Dispatchers.IO) { store.load(id) }
                    if (trackId != id) return@launch
                    if (stored != null) {
                        saved = stored.levels
                        complete = stored.complete
                        if (complete) {
                            publish()
                        }
                    }
                    if (complete) return@launch
                    val source = sourceFor(track) ?: return@launch
                    analyzer.analyze(source, durationHint).collect { progress ->
                        if (trackId != id) return@collect
                        saved = mergeKnown(progress.levels, saved)
                        if (progress.complete) {
                            saved = progress.levels
                            complete = true
                            withContext(Dispatchers.IO) { store.save(id, progress.levels, complete = true) }
                            publish()
                        }
                    }
                }
        }

        /** A file Android can decode: the local song itself, or the full download of an online song. */
        private suspend fun sourceFor(track: Track): WaveformAnalyzer.Source? {
            val url = track.streamUrl
            if (track.providerId == LOCAL && url != null && url.startsWith("content://")) {
                return WaveformAnalyzer.Source.ContentUri(Uri.parse(url))
            }
            val downloaded = runCatching { cacheManager.getCachedFile(track.id) }.getOrNull()
            return downloaded?.let { WaveformAnalyzer.Source.LocalFile(it) }
        }

        private fun publish() {
            val id = trackId ?: return
            val levels = if (complete) saved ?: emptyLevels() else mergeKnown(live.bars.levels(), saved)
            _current.value = Waveform(id, Loudness.normalize(levels))
        }

        /** Saves bars heard live, on top of what was saved before (unless a full decode already exists). */
        private fun saveLive() {
            val id = trackId ?: return
            if (complete || !liveDirty) return
            liveDirty = false
            val merged = mergeKnown(live.bars.levels(), saved)
            saved = merged
            val allKnown = merged.none { it.isNaN() }
            if (allKnown) {
                complete = true
                publish()
            }
            scope.launch(Dispatchers.IO) { store.save(id, merged, complete = allKnown) }
        }

        private fun emptyLevels() = FloatArray(Loudness.BAR_COUNT) { Float.NaN }

        private companion object {
            const val LOCAL = "local"

            /** Bars known in [primary] win; the rest come from [fallback] (NaN when neither knows). */
            fun mergeKnown(
                primary: FloatArray,
                fallback: FloatArray?,
            ): FloatArray =
                FloatArray(primary.size) { i ->
                    val p = primary[i]
                    if (!p.isNaN()) p else fallback?.getOrNull(i) ?: Float.NaN
                }
        }
    }
