package com.clibeats.presentation.settings

import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.presentation.theme.AccentColor
import com.clibeats.presentation.theme.CliBeatsThemeMode

data class ProviderOption(
    val id: String,
    val label: String,
)

data class SettingsUiState(
    val activeProviderId: String = ProviderRegistry.DEFAULT_PROVIDER_ID,
    val providers: List<ProviderOption> = emptyList(),
    val cacheMaxMb: Int = 512,
    val highQualityStreaming: Boolean = true,
    val hasAuthToken: Boolean = false,
    val currentCacheSizeBytes: Long = 0L,
    val exchangeMessage: String? = null,
    // Theme engine
    val themeMode: CliBeatsThemeMode = CliBeatsThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.GREEN,
)
