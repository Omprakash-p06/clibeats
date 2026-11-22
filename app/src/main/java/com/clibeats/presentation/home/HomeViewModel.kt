@file:Suppress("ForbiddenImport", "ktlint:standard:function-naming")

package com.clibeats.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.network.NetworkMonitor
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.domain.repository.HistoryRepository
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.playback.StreamResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val TRENDING_LIMIT = 25
private const val TRENDING_RETRIES = 2
private const val RETRY_DELAY_MS = 1_500L
private const val NETWORK_WAIT_MS = 15_000L
private const val RECENT_SHOWN = 5
private const val WARM_COUNT = 3
private const val LOCAL_ID = "local"

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val providerRegistry: ProviderRegistry,
        private val appPreferences: AppPreferences,
        private val networkMonitor: NetworkMonitor,
        private val streamResolver: StreamResolver,
        private val historyRepository: HistoryRepository? = null,
        private val playbackRepository: PlaybackRepository? = null,
    ) : ViewModel() {
        val providers: List<MusicProvider> = providerRegistry.providers

        val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

        val activeProviderId: StateFlow<String> =
            appPreferences.activeProviderId
                .map { it ?: ProviderRegistry.DEFAULT_PROVIDER_ID }
                .stateIn(viewModelScope, SharingStarted.Eagerly, ProviderRegistry.DEFAULT_PROVIDER_ID)

        val recentlyPlayed: StateFlow<List<Track>> =
            // Home's [ CLEAR ] hides plays up to that moment without touching the Library's history.
            (
                historyRepository?.let { history ->
                    appPreferences.homeRecentsClearedAt.flatMapLatest { since ->
                        history.getRecentlyPlayedTracksSince(since, RECENT_SHOWN)
                    }
                } ?: flowOf(emptyList())
            )
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

        /** Online provider to return to when switching back from Local. */
        private var lastOnlineProviderId = ProviderRegistry.DEFAULT_PROVIDER_ID

        fun setActiveProvider(id: String) {
            if (id != LOCAL_ID) lastOnlineProviderId = id
            viewModelScope.launch { appPreferences.setActiveProviderId(id) }
        }

        /** Re-tapping the Home tab: Online <-> Local (back to the online provider used last). */
        fun toggleSource() {
            val current = activeProviderId.value
            if (current == LOCAL_ID) {
                setActiveProvider(lastOnlineProviderId)
            } else {
                lastOnlineProviderId = current
                setActiveProvider(LOCAL_ID)
            }
        }

        /** Plays [tracks] from [index]; queues the rest only if "auto-queue related" is on. */
        fun playFromList(
            tracks: List<Track>,
            index: Int,
        ) {
            val repo = playbackRepository ?: return
            viewModelScope.launch {
                if (appPreferences.autoQueueRelated.first()) {
                    repo.setQueue(tracks, index)
                } else {
                    repo.setQueue(listOf(tracks[index]), 0)
                }
            }
        }

        fun addToQueue(track: Track) {
            playbackRepository?.addToQueue(track)
        }

        fun clearRecentlyPlayed() {
            viewModelScope.launch { appPreferences.setHomeRecentsClearedAt(System.currentTimeMillis()) }
        }

        private val activeProvider: kotlinx.coroutines.flow.Flow<MusicProvider> =
            activeProviderId.map { id ->
                providerRegistry.getProvider(id) ?: providerRegistry.defaultProvider()
            }

        val activeProviderName: StateFlow<String> =
            activeProvider
                .map { it.displayName }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = "",
                )

        private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

        /**
         * Trending for the active source. Loaded once per source while the app is open (this ViewModel lives as
         * long as the screen does), so switching tabs never reloads it; [refreshTrending] reloads on request.
         */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        private val _isRefreshing = MutableStateFlow(false)

        /** True while [refreshTrending] reloads a list that's already shown. */
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        private val trendingCache = mutableMapOf<String, List<Track>>()
        private var shownProviderId: String? = null
        private var loadJob: Job? = null

        init {
            viewModelScope.launch { activeProviderId.collect { id -> showTrending(id, force = false) } }
        }

        /** The reload button: fetches the active source's trending again. */
        fun refreshTrending() {
            shownProviderId?.let { showTrending(it, force = true) }
        }

        private fun showTrending(
            providerId: String,
            force: Boolean,
        ) {
            shownProviderId = providerId
            val cached = trendingCache[providerId]
            loadJob?.cancel()
            if (cached != null && !force) {
                _isRefreshing.value = false
                _uiState.value = HomeUiState.Success(cached)
                return
            }
            // A reload keeps the current list on screen (with the button showing progress) until the new one arrives.
            val keepShown = force && _uiState.value is HomeUiState.Success
            loadJob =
                viewModelScope.launch {
                    if (keepShown) _isRefreshing.value = true else _uiState.value = HomeUiState.Loading
                    val provider = providerRegistry.getProvider(providerId) ?: providerRegistry.defaultProvider()
                    when (val result = loadTrending(provider)) {
                        is ProviderResult.Success -> {
                            // Local trending reflects the library, which can change with a rescan: not cached.
                            if (providerId != LOCAL_ID) trendingCache[providerId] = result.data
                            _uiState.value = HomeUiState.Success(result.data)
                            // Pre-resolve the first visible songs so the first tap is instant.
                            streamResolver.warmTracks(result.data, WARM_COUNT)
                        }
                        is ProviderResult.Error -> if (!keepShown) _uiState.value = HomeUiState.Error(result.message)
                        is ProviderResult.Loading -> Unit
                    }
                    _isRefreshing.value = false
                }
        }

        /**
         * Right after the app starts the connection may not be up yet (that's the "fetch error" that went away on
         * its own): on failure, wait for the network if it's down, or briefly if it isn't, and try again.
         */
        private suspend fun loadTrending(provider: MusicProvider): ProviderResult<List<Track>> {
            var result = provider.trending(TRENDING_LIMIT)
            var attempt = 0
            while (result is ProviderResult.Error && attempt < TRENDING_RETRIES) {
                attempt++
                if (!networkMonitor.isOnline.value) {
                    withTimeoutOrNull(NETWORK_WAIT_MS) { networkMonitor.isOnline.first { it } }
                } else {
                    delay(RETRY_DELAY_MS * attempt)
                }
                result = provider.trending(TRENDING_LIMIT)
            }
            return result
        }
    }
