package com.clibeats.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.domain.model.AudioOutput
import com.clibeats.domain.model.LyricLine
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.RepeatMode
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.LibraryRepository
import com.clibeats.domain.repository.LyricsRepository
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.domain.repository.PlaylistRepository
import com.clibeats.playback.AudioOutputMonitor
import com.clibeats.presentation.component.isLocalFilePlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sleep timer state: off, a countdown, or "pause when the current track ends". */
sealed interface SleepTimer {
    data object Off : SleepTimer

    data class Countdown(val remainingMin: Int) : SleepTimer

    data object EndOfTrack : SleepTimer
}

@Suppress("TooManyFunctions")
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playbackRepository: PlaybackRepository,
        private val playlistRepository: PlaylistRepository? = null,
        private val libraryRepository: LibraryRepository? = null,
        private val audioOutputMonitor: AudioOutputMonitor? = null,
        private val lyricsRepository: LyricsRepository? = null,
    ) : ViewModel() {
        val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

        /** Where the music is playing (null in previews/tests). */
        val audioOutput: StateFlow<AudioOutput>? = audioOutputMonitor?.current

        fun refreshAudioOutput() {
            audioOutputMonitor?.refresh()
        }

        /** Opens Android's output picker (needs an Activity context). */
        fun openOutputSwitcher(activityContext: android.content.Context) {
            audioOutputMonitor?.openSwitcher(activityContext)
        }

        val queue: StateFlow<List<Track>> = playbackRepository.queueState

        /** Time-synced lyrics for the currently playing track. */
        @Suppress("OPT_IN_USAGE")
        val currentLyrics: StateFlow<List<LyricLine>> =
            playbackState
                .map { state: PlaybackState -> state.currentTrack }
                .distinctUntilChanged { a: Track?, b: Track? -> a?.id == b?.id }
                .flatMapLatest { track: Track? ->
                    if (track == null || lyricsRepository == null) {
                        flowOf(emptyList())
                    } else {
                        flow {
                            val res = lyricsRepository.getLyrics(track).getOrDefault(emptyList())
                            emit(res)
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

        /** Whether the current track is in Favorites (liked songs). */
        @Suppress("OPT_IN_USAGE")
        val isCurrentLiked: StateFlow<Boolean> =
            playbackState
                .map { it.currentTrack?.id }
                .distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id == null || libraryRepository == null) flowOf(false) else libraryRepository.isLiked(id)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

        /** Short label shown just above the pressed button ("ONE" / "ALL" / "OFF", "ON" / "OFF"). */
        private val _hint = MutableStateFlow<ButtonHint?>(null)
        val hint: StateFlow<ButtonHint?> = _hint.asStateFlow()
        private var toastJob: Job? = null

        val playlists: StateFlow<List<Playlist>> =
            (playlistRepository?.getAllPlaylistsAsFlow() ?: flowOf(emptyList()))
                // Local M3U/PLS playlists are read-only, so they aren't add-to targets.
                .map { all -> all.filterNot { it.isLocalFilePlaylist() } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

        val sleepTimer: StateFlow<SleepTimer> =
            combine(
                playbackRepository.sleepTimerCountdown,
                playbackRepository.pauseAtEndOfTrack,
            ) { countdown, endOfTrack ->
                when {
                    countdown != null -> SleepTimer.Countdown(countdown)
                    endOfTrack -> SleepTimer.EndOfTrack
                    else -> SleepTimer.Off
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, SleepTimer.Off)

        /** Short-lived player message (e.g. a song that couldn't be played), shown above the player bar. */
        @Suppress("OPT_IN_USAGE")
        val playerMessage: StateFlow<String?> =
            playbackRepository.playerMessages
                .transformLatest { message ->
                    emit(message)
                    delay(MESSAGE_MS)
                    emit(null)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

        fun playTrack(track: Track) {
            playbackRepository.playTrack(track)
        }

        /** Inserts [track] right after the current one. */
        fun playNext(track: Track) {
            playbackRepository.playNext(track)
        }

        fun addToQueue(track: Track) {
            playbackRepository.addToQueue(track)
        }

        /** Plays [tracks] as the queue from [index], so next/previous and auto-advance work. */
        fun playQueue(
            tracks: List<Track>,
            index: Int,
        ) {
            playbackRepository.setQueue(tracks, index)
        }

        fun onPlayPauseClick() {
            if (playbackState.value.isPlaying) {
                playbackRepository.pause()
            } else {
                playbackRepository.play()
            }
        }

        fun onSkipNextClick() {
            playbackRepository.skipToNext()
        }

        fun onSkipPreviousClick() {
            playbackRepository.skipToPrevious()
        }

        /** Cycles OFF -> ALL (repeat queue) -> ONE (repeat track) -> OFF. */
        fun onRepeatClick() {
            val next =
                when (playbackState.value.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
            playbackRepository.setRepeatMode(next)
            showHint(HintTarget.REPEAT, next.name)
        }

        fun onShuffleClick() {
            val enabling = !playbackState.value.shuffleEnabled
            playbackRepository.toggleShuffle()
            showHint(HintTarget.SHUFFLE, if (enabling) "ON" else "OFF")
        }

        fun toggleLike() {
            val track = playbackState.value.currentTrack ?: return
            viewModelScope.launch { runCatching { libraryRepository?.toggleLike(track) } }
        }

        private fun showHint(
            target: HintTarget,
            text: String,
        ) {
            // Replace any visible hint instantly; hide after TOAST_MS.
            toastJob?.cancel()
            _hint.value = ButtonHint(target, text)
            toastJob =
                viewModelScope.launch {
                    delay(TOAST_MS)
                    _hint.value = null
                }
        }

        fun seekTo(positionMs: Long) {
            playbackRepository.seekTo(positionMs)
        }

        fun addToPlaylist(
            track: Track,
            playlistId: String,
        ) {
            viewModelScope.launch {
                runCatching { playlistRepository?.addTrackToPlaylist(playlistId, track) }
            }
        }

        /** [minutes] > 0 starts a countdown; 0 turns it off; null = pause at end of current track. */
        fun setSleepTimer(minutes: Int?) {
            when {
                minutes == null -> {
                    playbackRepository.cancelSleepTimer()
                    playbackRepository.setPauseAtEndOfTrack(true)
                }
                minutes <= 0 -> {
                    playbackRepository.cancelSleepTimer()
                    playbackRepository.setPauseAtEndOfTrack(false)
                }
                else -> {
                    playbackRepository.startSleepTimer(minutes)
                }
            }
        }

        private companion object {
            const val MINUTE_MS = 60_000L
            const val TOAST_MS = 1_500L
            const val MESSAGE_MS = 3_000L
        }
    }

enum class HintTarget { REPEAT, SHUFFLE }

data class ButtonHint(
    val target: HintTarget,
    val text: String,
)
