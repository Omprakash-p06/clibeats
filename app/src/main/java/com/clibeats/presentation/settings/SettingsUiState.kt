package com.clibeats.presentation.settings

data class SettingsUiState(
    val activeProviderId: String = "ytmusic",
    val cacheMaxMb: Int = 512,
    val highQualityStreaming: Boolean = true,
    val hasAuthToken: Boolean = false,
    val currentCacheSizeBytes: Long = 0L,
)
