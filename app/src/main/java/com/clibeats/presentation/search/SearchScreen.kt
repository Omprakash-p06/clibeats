@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.AddToPlaylistDialog
import com.clibeats.presentation.component.FavoritesViewModel
import com.clibeats.presentation.component.ProviderSwitcher
import com.clibeats.presentation.component.SongTableHeader
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TrackActionSheet
import com.clibeats.presentation.component.TrackArtwork
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.component.TuiTextField
import com.clibeats.presentation.navigation.AppRoute
import com.clibeats.presentation.navigation.LocalAppRouter
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun SearchScreen(
    focusRequested: Boolean = false,
    onFocusHandled: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    toggleSourceRequested: Boolean = false,
    onToggleSourceHandled: () -> Unit = {},
) {
    LaunchedEffect(toggleSourceRequested) {
        if (toggleSourceRequested) {
            viewModel.toggleSource()
            onToggleSourceHandled()
        }
    }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Double-tapping the Search tab focuses the box and opens the keyboard (one-shot).
    LaunchedEffect(focusRequested) {
        if (focusRequested) {
            runCatching { searchFocus.requestFocus() }
            keyboard?.show()
            onFocusHandled()
        }
    }
    val favorites: FavoritesViewModel = hiltViewModel()
    val query by viewModel.query.collectAsState()
    val state by viewModel.searchResults.collectAsState()
    val providerName by viewModel.activeProviderName.collectAsState()
    val activeProviderId by viewModel.activeProviderId.collectAsState()
    var playlistPickerTrack by remember { mutableStateOf<Track?>(null) }
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    val recentSearches by viewModel.recentSearches.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground)
            .padding(12.dp),
    ) {
        // ── Search input (TUI-styled, focus-activated border) ──────────────
        TuiTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = "Search",
            placeholder = "What do you want to play?",
            modifier = Modifier.semantics { contentDescription = "Search music" },
            focusRequester = searchFocus,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            trailingContent = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.clearQuery()
                            focusManager.clearFocus()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear search",
                            tint = CliBeatsTextSecondary,
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        ProviderSwitcher(
            providers = viewModel.providers,
            activeProviderId = activeProviderId,
            onSelect = viewModel::setActiveProvider,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ── Results area block ───────────────────────────────────────────
        TuiBlock(title = "Search Results — $providerName", modifier = Modifier.weight(1f)) {
            // Empty box: show the songs picked from earlier searches (or the hint when there are none).
            if (query.isBlank()) {
                if (recentSearches.isEmpty()) {
                    SearchIdleState()
                } else {
                    RecentSearchesList(
                        tracks = recentSearches,
                        onTrackClick = { index -> viewModel.onPlayRecent(recentSearches, index) },
                        onClear = viewModel::clearRecentSearches,
                        onAddToQueue = viewModel::onAddToQueue,
                        onFavorite = { track ->
                            favorites.add(track)
                            viewModel.rememberPick(track)
                        },
                        onActionClick = { track -> actionTrack = track },
                    )
                }
                return@TuiBlock
            }
            when (val currentState = state) {
                is SearchUiState.Idle -> SearchIdleState()
                is SearchUiState.Loading -> SearchLoadingState(providerName)
                is SearchUiState.Error -> SearchErrorState(currentState.message)
                is SearchUiState.Success ->
                    if (currentState.tracks.isEmpty()) {
                        SearchNoResultsState()
                    } else {
                        SearchResultsList(
                            tracks = currentState.tracks,
                            onTrackClick = { index -> viewModel.onPlay(currentState.tracks, index) },
                            onAddToQueue = viewModel::onAddToQueue,
                            onFavorite = { track ->
                                favorites.add(track)
                                viewModel.rememberPick(track)
                            },
                            onActionClick = { track -> actionTrack = track },
                        )
                    }
            }
        }
    }

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val router = LocalAppRouter.current

    playlistPickerTrack?.let { track ->
        AddToPlaylistDialog(track = track, onDismiss = { playlistPickerTrack = null })
    }
    actionTrack?.let { track ->
        TrackActionSheet(
            track = track,
            onDismiss = { actionTrack = null },
            onPlayNext = {
                playerViewModel.playNext(track)
                viewModel.rememberPick(track)
            },
            onAddToQueue = {
                viewModel.onAddToQueue(track)
            },
            onAddToPlaylist = {
                playlistPickerTrack = track
                viewModel.rememberPick(track)
            },
            onToggleLike = {
                favorites.add(track)
                viewModel.rememberPick(track)
            },
            onGoToArtist = {
                actionTrack = null
                router.navigate(
                    AppRoute.ArtistDetail(
                        artistName = track.artist,
                        browseId = null,
                        providerId = track.providerId,
                    ),
                )
            },
            onGoToAlbum =
                if (track.album.isNotBlank()) {
                    {
                        actionTrack = null
                        router.navigate(
                            AppRoute.AlbumDetail(
                                albumName = track.album,
                                artistName = track.artist,
                                browseId = null,
                                providerId = track.providerId,
                            ),
                        )
                    }
                } else {
                    null
                },
        )
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun RecentSearchesList(
    tracks: List<Track>,
    onTrackClick: (Int) -> Unit,
    onClear: () -> Unit,
    onAddToQueue: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onActionClick: (Track) -> Unit,
) {
    val accent = LocalAccentColor.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "RECENT SEARCHES", style = MaterialTheme.typography.labelMedium, color = accent)
                Text(
                    text = "[ CLEAR ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = CliBeatsTextSecondary,
                    modifier = Modifier.clickable(onClick = onClear).padding(8.dp),
                )
            }
        }
        itemsIndexed(items = tracks, key = { _, track -> "recent_${track.id}" }) { index, track ->
            SongTableRow(
                trackTitle = track.title,
                artist = track.artist,
                duration = formatDuration(track.durationMs),
                artworkContent = { TrackArtwork(artworkUrl = track.artworkUrl, size = 52) },
                onClick = { onTrackClick(index) },
                onSwipeRight = { onAddToQueue(track) },
                onSwipeLeft = { onFavorite(track) },
                onActionClick = { onActionClick(track) },
                onLongPress = { onActionClick(track) },
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SearchIdleState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "> Type a query to search tracks...",
            style = MaterialTheme.typography.bodyMedium,
            color = CliBeatsTextSecondary,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SearchLoadingState(providerName: String) {
    val accent = LocalAccentColor.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Searching…" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Searching $providerName... [████░░░░]",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SearchErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "> error --$message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SearchNoResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "> No playable tracks found.",
            style = MaterialTheme.typography.bodyMedium,
            color = CliBeatsTextSecondary,
        )
    }
}

@Suppress("FunctionNaming", "LongParameterList", "LongMethod")
@Composable
private fun SearchResultsList(
    tracks: List<Track>,
    onTrackClick: (Int) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onActionClick: (Track) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SongTableHeader(showIndex = false)
        }
        itemsIndexed(items = tracks, key = { _, track -> track.id }) { index, track ->
            SongTableRow(
                trackTitle = track.title,
                artist = buildString {
                    append(track.artist)
                    if (track.album.isNotBlank()) append(" • ${track.album}")
                },
                duration = formatDuration(track.durationMs),
                artworkContent = { TrackArtwork(artworkUrl = track.artworkUrl, size = 52) },
                onClick = { onTrackClick(index) },
                onSwipeRight = { onAddToQueue(track) },
                onSwipeLeft = { onFavorite(track) },
                onActionClick = { onActionClick(track) },
                onLongPress = { onActionClick(track) },
            )
        }
    }
}

/** Format duration milliseconds as "m:ss" or "h:mm:ss". */
internal fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "–"
    val totalSec = durationMs / 1_000L
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
