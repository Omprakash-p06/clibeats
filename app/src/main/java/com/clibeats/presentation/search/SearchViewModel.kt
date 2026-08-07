@file:Suppress("ktlint:standard:function-naming")

package com.clibeats.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val DEBOUNCE_MS = 300L
private const val MIN_QUERY_LENGTH = 2

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val musicProvider: MusicProvider,
    ) : ViewModel() {
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        @Suppress("OPT_IN_USAGE")
        val searchResults: StateFlow<SearchUiState> =
            _query
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    flow {
                        emit(SearchUiState.Loading)
                        val searchQuery = if (q.length < MIN_QUERY_LENGTH) "" else q
                        emit(
                            when (val result = musicProvider.search(searchQuery)) {
                                is ProviderResult.Success -> SearchUiState.Success(result.data)
                                is ProviderResult.Error -> SearchUiState.Error(result.message)
                                is ProviderResult.Loading -> SearchUiState.Loading
                            },
                        )
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = SearchUiState.Loading,
                )

        val uiState: StateFlow<SearchUiState> = searchResults

        fun onQueryChange(query: String) {
            _query.value = query
        }

        fun clearQuery() {
            _query.value = ""
        }
    }
