@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.cache.CacheManager
import com.clibeats.data.playlist.PlaylistExchangeManager
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.presentation.theme.AccentColor
import com.clibeats.presentation.theme.CliBeatsThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
        private val cacheManager: CacheManager,
        private val providerRegistry: ProviderRegistry,
        private val playlistExchangeManager: PlaylistExchangeManager,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                appPreferences.activeProviderId,
                appPreferences.cacheMaxMb,
                appPreferences.highQualityStreaming,
                appPreferences.authToken,
            ) { providerId, cacheMaxMb, highQuality, token ->
                Triple(
                    Triple(providerId, cacheMaxMb, highQuality),
                    token,
                    Unit,
                )
            }.combine(cacheManager.getAllCachedAsFlow()) { (inner, token, _), cachedEntities ->
                val (providerId, cacheMaxMb, highQuality) = inner
                val totalBytes = cachedEntities.sumOf { it.fileSizeBytes }
                Triple(Triple(providerId, cacheMaxMb, highQuality), token, totalBytes)
            }.combine(appPreferences.themeMode) { (inner, token, totalBytes), modeName ->
                Triple(Triple(inner.first, inner.second, inner.third), Pair(token, totalBytes), modeName)
            }.combine(appPreferences.accentColorName) { (inner, extra, modeName), accentName ->
                val (providerId, cacheMaxMb, highQuality) = inner
                val (token, totalBytes) = extra
                SettingsUiState(
                    activeProviderId = providerId ?: ProviderRegistry.DEFAULT_PROVIDER_ID,
                    providers =
                        providerRegistry.providers.map { provider ->
                            ProviderOption(provider.providerId, provider.displayName)
                        },
                    cacheMaxMb = cacheMaxMb,
                    highQualityStreaming = highQuality,
                    hasAuthToken = !token.isNullOrEmpty(),
                    currentCacheSizeBytes = totalBytes,
                    themeMode =
                        CliBeatsThemeMode.entries.firstOrNull { it.name == modeName }
                            ?: CliBeatsThemeMode.DARK,
                    accentColor =
                        AccentColor.entries.firstOrNull { it.name == accentName }
                            ?: AccentColor.GREEN,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = SettingsUiState(),
            )

        private val _exchangeMessage = MutableStateFlow<String?>(null)
        val exchangeMessage: StateFlow<String?> = _exchangeMessage.asStateFlow()

        fun setActiveProvider(providerId: String) {
            viewModelScope.launch {
                appPreferences.setActiveProviderId(providerId)
            }
        }

        fun setCacheMaxMb(maxMb: Int) {
            viewModelScope.launch {
                appPreferences.setCacheMaxMb(maxMb)
                cacheManager.maxCacheSizeBytes = maxMb * BYTES_IN_MB
                cacheManager.evictLruIfNeeded()
            }
        }

        fun setHighQualityStreaming(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.setHighQualityStreaming(enabled)
            }
        }

        fun setThemeMode(mode: CliBeatsThemeMode) {
            viewModelScope.launch {
                appPreferences.setThemeMode(mode.name)
            }
        }

        fun setAccentColor(accent: AccentColor) {
            viewModelScope.launch {
                appPreferences.setAccentColorName(accent.name)
            }
        }

        fun clearCache() {
            viewModelScope.launch {
                cacheManager.clearAllCache()
            }
        }

        fun clearAuthToken() {
            viewModelScope.launch {
                appPreferences.clearAuthToken()
            }
        }

        fun exportPlaylists() {
            viewModelScope.launch {
                _exchangeMessage.value =
                    playlistExchangeManager.export().fold(
                        onSuccess = { file -> "Exported ${file.absolutePath}" },
                        onFailure = { e -> "Export failed: ${e.message}" },
                    )
            }
        }

        fun importPlaylists() {
            viewModelScope.launch {
                _exchangeMessage.value =
                    playlistExchangeManager.import().fold(
                        onSuccess = { count -> "Imported $count track(s) from clibeats.json" },
                        onFailure = { e -> "Import failed: ${e.message}" },
                    )
            }
        }

        fun clearExchangeMessage() {
            _exchangeMessage.value = null
        }

        companion object {
            private const val BYTES_IN_MB = 1048576L
        }
    }
