package com.clibeats.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val songRepository: SongRepository,
        private val playbackRepository: PlaybackRepository,
    ) : ViewModel() {
        val uiState: StateFlow<LibraryUiState> =
            songRepository.getAllTracksAsFlow()
                .map { tracks ->
                    if (tracks.isEmpty()) {
                        LibraryUiState.Empty
                    } else {
                        val artists = tracks.groupBy { it.artist.ifBlank { "Unknown Artist" } }
                            .map { (artist, list) -> ArtistGroup(artist, list.size) }
                            .sortedBy { it.name }

                        val albums = tracks.groupBy { (it.album.ifBlank { "Unknown Album" }) to it.artist }
                            .map { (key, list) -> AlbumGroup(key.first, key.second, list.size) }
                            .sortedBy { it.title }

                        LibraryUiState.Success(
                            tracks = tracks,
                            artists = artists,
                            albums = albums,
                        )
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = LibraryUiState.Loading,
                )

        fun onTrackClick(track: Track, tracks: List<Track>, index: Int) {
            playbackRepository.setQueue(tracks, index)
        }
    }
