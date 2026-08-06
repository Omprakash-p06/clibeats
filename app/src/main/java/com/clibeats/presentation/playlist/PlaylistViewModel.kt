package com.clibeats.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
        private val playbackRepository: PlaybackRepository,
    ) : ViewModel() {

        private val _selectedPlaylistId = MutableStateFlow<String?>(null)

        val uiState: StateFlow<PlaylistUiState> =
            playlistRepository.getAllPlaylistsAsFlow()
                .flatMapLatest { playlists ->
                    val selectedId = _selectedPlaylistId.value
                    if (selectedId == null) {
                        flowOf(PlaylistUiState.Success(playlists = playlists))
                    } else {
                        playlistRepository.getSongsForPlaylistAsFlow(selectedId)
                            .map { songs ->
                                val selected = playlists.find { it.id == selectedId }
                                PlaylistUiState.Success(
                                    playlists = playlists,
                                    selectedPlaylist = selected,
                                    selectedPlaylistTracks = songs,
                                )
                            }
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = PlaylistUiState.Loading,
                )

        fun createPlaylist(name: String, description: String?) {
            if (name.isBlank()) return
            viewModelScope.launch {
                val newPlaylist = Playlist(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    description = description?.trim(),
                    artworkUrl = null,
                    trackCount = 0,
                    isOwned = true,
                    providerId = "local",
                )
                playlistRepository.upsertPlaylist(newPlaylist)
            }
        }

        fun deletePlaylist(playlistId: String) {
            viewModelScope.launch {
                if (_selectedPlaylistId.value == playlistId) {
                    _selectedPlaylistId.value = null
                }
                playlistRepository.deletePlaylist(playlistId)
            }
        }

        fun selectPlaylist(playlistId: String?) {
            _selectedPlaylistId.value = playlistId
        }

        fun removeSongFromPlaylist(playlistId: String, songId: String) {
            viewModelScope.launch {
                playlistRepository.removeSongFromPlaylist(playlistId, songId)
            }
        }

        fun playPlaylist(tracks: List<Track>, startIndex: Int = 0) {
            if (tracks.isNotEmpty()) {
                playbackRepository.setQueue(tracks, startIndex)
            }
        }
    }
