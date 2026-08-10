@file:Suppress("ForbiddenImport", "ktlint:standard:function-naming")

package com.clibeats.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val TRENDING_LIMIT = 15

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val providerRegistry: ProviderRegistry,
        private val appPreferences: AppPreferences,
    ) : ViewModel() {
        private val activeProviderId =
            appPreferences.activeProviderId.map { it ?: ProviderRegistry.DEFAULT_PROVIDER_ID }

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

        @Suppress("OPT_IN_USAGE")
        val uiState: StateFlow<HomeUiState> =
            activeProvider
                .flatMapLatest { provider ->
                    flow {
                        emit(HomeUiState.Loading)
                        emit(
                            when (val result = provider.trending(TRENDING_LIMIT)) {
                                is ProviderResult.Success -> HomeUiState.Success(result.data)
                                is ProviderResult.Error -> HomeUiState.Error(result.message)
                                is ProviderResult.Loading -> HomeUiState.Loading
                            },
                        )
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = HomeUiState.Loading,
                )
    }
