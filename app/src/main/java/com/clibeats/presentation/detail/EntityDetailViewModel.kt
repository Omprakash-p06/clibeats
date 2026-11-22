package com.clibeats.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EntityDetailUiState {
    data object Loading : EntityDetailUiState

    data class Success(
        val title: String,
        val subtitle: String?,
        val tracks: List<Track>,
    ) : EntityDetailUiState

    data class Error(val message: String) : EntityDetailUiState
}

@HiltViewModel
class EntityDetailViewModel
    @Inject
    constructor(
        private val providerRegistry: ProviderRegistry,
        private val songRepository: SongRepository,
        private val playbackRepository: PlaybackRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<EntityDetailUiState>(EntityDetailUiState.Loading)
        val uiState: StateFlow<EntityDetailUiState> = _uiState.asStateFlow()

        fun loadArtist(
            artistName: String,
            providerId: String,
        ) {
            viewModelScope.launch {
                _uiState.value = EntityDetailUiState.Loading
                val provider = getProvider(providerId)
                val searchResult = provider?.search(artistName, limit = 30)
                val tracks = (searchResult as? com.clibeats.domain.provider.ProviderResult.Success)?.data.orEmpty()
                val filtered =
                    if (tracks.isNotEmpty()) {
                        tracks.filter { it.artist.contains(artistName, ignoreCase = true) }.ifEmpty { tracks }
                    } else {
                        songRepository.searchTracksAsFlow(artistName).first()
                    }

                _uiState.value =
                    EntityDetailUiState.Success(
                        title = artistName,
                        subtitle = "Artist • ${filtered.size} tracks",
                        tracks = filtered,
                    )
            }
        }

        fun loadAlbum(
            albumName: String,
            artistName: String,
            providerId: String,
        ) {
            viewModelScope.launch {
                _uiState.value = EntityDetailUiState.Loading
                val provider = getProvider(providerId)
                val query = if (artistName.isNotBlank()) "$albumName $artistName" else albumName
                val searchResult = provider?.search(query, limit = 30)
                val tracks = (searchResult as? com.clibeats.domain.provider.ProviderResult.Success)?.data.orEmpty()
                val filtered =
                    if (tracks.isNotEmpty()) {
                        tracks.filter {
                            it.album.contains(albumName, ignoreCase = true) ||
                                it.title.contains(albumName, ignoreCase = true)
                        }.ifEmpty { tracks }
                    } else {
                        songRepository.searchTracksAsFlow(albumName).first()
                    }

                _uiState.value =
                    EntityDetailUiState.Success(
                        title = albumName,
                        subtitle = if (artistName.isNotBlank()) "Album • $artistName" else "Album",
                        tracks = filtered,
                    )
            }
        }

        fun playAll(
            tracks: List<Track>,
            startIndex: Int = 0,
        ) {
            if (tracks.isNotEmpty()) {
                playbackRepository.setQueue(tracks, startIndex)
            }
        }

        fun shuffleAll(tracks: List<Track>) {
            if (tracks.isNotEmpty()) {
                val shuffled = tracks.shuffled()
                playbackRepository.setQueue(shuffled, 0)
            }
        }

        private fun getProvider(providerId: String): MusicProvider? =
            providerRegistry.getProvider(providerId)
                ?: providerRegistry.getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID)
    }
