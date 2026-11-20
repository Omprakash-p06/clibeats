@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TelemetryViewModel
    @Inject
    constructor(
        private val preferences: AppPreferences,
    ) : ViewModel() {
        val isOptedIn: StateFlow<Boolean> =
            preferences.anonymousUsageOptIn.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000L),
                false,
            )

        private val _promptVisible = MutableStateFlow(false)
        val promptVisible: StateFlow<Boolean> = _promptVisible.asStateFlow()

        init {
            viewModelScope.launch {
                preferences.anonymousUsagePrompted.collect { prompted ->
                    _promptVisible.value = !prompted
                }
            }
        }

        fun onOptIn() {
            _promptVisible.value = false
            viewModelScope.launch {
                preferences.setAnonymousUsageOptIn(true)
            }
        }

        fun onOptOut() {
            _promptVisible.value = false
            viewModelScope.launch {
                preferences.setAnonymousUsageOptIn(false)
            }
        }
    }
