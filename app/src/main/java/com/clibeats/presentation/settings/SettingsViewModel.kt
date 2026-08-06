@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.cache.CacheManager
import com.clibeats.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                appPreferences.activeProviderId,
                appPreferences.cacheMaxMb,
                appPreferences.highQualityStreaming,
                appPreferences.authToken,
                cacheManager.getAllCachedAsFlow(),
            ) { providerId, cacheMaxMb, highQuality, token, cachedEntities ->
                val totalBytes = cachedEntities.sumOf { it.fileSizeBytes }
                SettingsUiState(
                    activeProviderId = providerId ?: "ytmusic",
                    cacheMaxMb = cacheMaxMb,
                    highQualityStreaming = highQuality,
                    hasAuthToken = !token.isNullOrEmpty(),
                    currentCacheSizeBytes = totalBytes,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsUiState(),
            )

        fun setActiveProvider(providerId: String) {
            viewModelScope.launch {
                appPreferences.setActiveProviderId(providerId)
            }
        }

        fun setCacheMaxMb(maxMb: Int) {
            viewModelScope.launch {
                appPreferences.setCacheMaxMb(maxMb)
                cacheManager.maxCacheSizeBytes = maxMb * 1024 * 1024L
                cacheManager.evictLruIfNeeded()
            }
        }

        fun setHighQualityStreaming(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.setHighQualityStreaming(enabled)
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
    }
