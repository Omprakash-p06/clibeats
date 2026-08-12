@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ThemeState(
    val mode: CliBeatsThemeMode = CliBeatsThemeMode.DARK,
    val accent: AccentColor = AccentColor.GREEN,
)

/**
 * Survives the Activity and keeps the resolved [ThemeState] as a [StateFlow].
 * MainActivity reads this and feeds it into [CliBeatsTheme].
 */
@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
    ) : ViewModel() {
        val themeState: StateFlow<ThemeState> =
            combine(
                appPreferences.themeMode,
                appPreferences.accentColorName,
            ) { modeName, accentName ->
                ThemeState(
                    mode =
                        CliBeatsThemeMode.entries.firstOrNull { it.name == modeName }
                            ?: CliBeatsThemeMode.DARK,
                    accent =
                        AccentColor.entries.firstOrNull { it.name == accentName }
                            ?: AccentColor.GREEN,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ThemeState(),
            )
    }
