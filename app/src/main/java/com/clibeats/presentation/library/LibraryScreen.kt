@file:Suppress(
    "FunctionNaming",
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.AddToPlaylistDialog
import com.clibeats.presentation.component.FavoritesViewModel
import com.clibeats.presentation.component.SongTableHeader
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TrackActionSheet
import com.clibeats.presentation.component.TrackArtwork
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.navigation.AppRoute
import com.clibeats.presentation.navigation.LocalAppRouter
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    toggleModeRequested: Boolean = false,
    onToggleModeHandled: () -> Unit = {},
) {
    val favorites: FavoritesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val mode by viewModel.mode.collectAsState()
    // Tapping the Library tab while already on it flips Online <-> Offline.
    LaunchedEffect(toggleModeRequested) {
        if (toggleModeRequested) {
            viewModel.setMode(if (mode == LibraryMode.ONLINE) LibraryMode.OFFLINE else LibraryMode.ONLINE)
            onToggleModeHandled()
        }
    }
    val localFolderChosen by viewModel.localFolderChosen.collectAsState()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val router = LocalAppRouter.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    addToPlaylistTrack?.let { track ->
        AddToPlaylistDialog(track = track, onDismiss = { addToPlaylistTrack = null })
    }
    actionTrack?.let { track ->
        TrackActionSheet(
            track = track,
            onDismiss = { actionTrack = null },
            onPlayNext = { playerViewModel.playNext(track) },
            onAddToQueue = { playerViewModel.addToQueue(track) },
            onAddToPlaylist = { addToPlaylistTrack = track },
            onToggleLike = { favorites.add(track) },
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
    val tabs = listOf("Tracks", "Artists", "Albums")

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CliBeatsBackground)
                .padding(12.dp),
    ) {
        LibraryModeToggle(mode = mode, onSelect = viewModel::setMode)
        if (mode == LibraryMode.ONLINE && state is LibraryUiState.Success) {
            Text(
                text = "[ CLEAR ]",
                style = MaterialTheme.typography.labelMedium,
                color = CliBeatsTextSecondary,
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .clickable(role = Role.Button, onClick = viewModel::clearPlayedHistory)
                        .padding(8.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        val trackCount = (state as? LibraryUiState.Success)?.tracks?.size ?: 0
        val modeLabel =
            if (mode == LibraryMode.ONLINE) {
                "Online · played"
            } else {
                "Offline · $trackCount ${if (trackCount == 1) "track" else "tracks"}"
            }
        val titleText = "Library — $modeLabel (${tabs[selectedTabIndex]})"
        TuiBlock(
            title = titleText,
            isActive = true,
            modifier = Modifier.weight(1f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = selectedTabIndex == index
                        val label = if (selected) "[● ${title.uppercase()}]" else "[ ${title.uppercase()} ]"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CliBeatsAccent else CliBeatsTextSecondary,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .border(1.dp, if (selected) CliBeatsAccent else CliBeatsDivider)
                                    .clickable(role = Role.Tab) { selectedTabIndex = index }
                                    .padding(vertical = 8.dp),
                        )
                    }
                }

                when (val currentState = state) {
                    is LibraryUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = CliBeatsAccent)
                        }
                    }

                    is LibraryUiState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text =
                                    when {
                                        mode == LibraryMode.ONLINE -> NOTHING_PLAYED
                                        !localFolderChosen -> NO_FOLDER
                                        else -> NO_FILES
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CliBeatsTextSecondary,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    is LibraryUiState.Success -> {
                        when (selectedTabIndex) {
                            0 ->
                                TracksList(
                                    tracks = currentState.tracks,
                                    onTrackClick = { track, index ->
                                        viewModel.onTrackClick(track, currentState.tracks, index)
                                    },
                                    onSwipeRight = viewModel::onSwipeAddToQueue,
                                    onSwipeLeft = favorites::add,
                                    onActionClick = { actionTrack = it },
                                    onLongPress = { actionTrack = it },
                                )
                            1 -> ArtistsList(artists = currentState.artists)
                            2 -> AlbumsList(albums = currentState.albums)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun TracksList(
    tracks: List<Track>,
    onTrackClick: (Track, Int) -> Unit,
    onSwipeRight: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onActionClick: (Track) -> Unit,
    onLongPress: (Track) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SongTableHeader(showIndex = true)
        }
        itemsIndexed(items = tracks, key = { _, track -> track.id }) { index, track ->
            SongTableRow(
                trackTitle = track.title,
                artist = track.artist,
                duration = formatDuration(track.durationMs),
                index = index + 1,
                artworkContent = { TrackArtwork(artworkUrl = track.artworkUrl, size = 52) },
                onClick = { onTrackClick(track, index) },
                onSwipeRight = { onSwipeRight(track) },
                onSwipeLeft = { onSwipeLeft(track) },
                onActionClick = { onActionClick(track) },
                onLongPress = { onLongPress(track) },
            )
        }
    }
}

private const val NOTHING_PLAYED = "> Nothing played yet. Songs you play show up here."
private const val NO_FOLDER = "> No local folder. Settings → Local Music → Choose folder"
private const val NO_FILES = "> No audio files found. Settings → Local Music → Rescan"

@Suppress("FunctionNaming")
@Composable
private fun LibraryModeToggle(
    mode: LibraryMode,
    onSelect: (LibraryMode) -> Unit,
) {
    // Two equal-width segments, matching the Online / Local switch on Home and Search.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryMode.entries.forEach { option ->
            val selected = option == mode
            Text(
                text = if (selected) "[● ${option.name}]" else "[ ${option.name} ]",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) CliBeatsAccent else CliBeatsTextSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { this.selected = selected }
                        .border(1.dp, if (selected) CliBeatsAccent else CliBeatsDivider)
                        .clickable(role = Role.Tab) { onSelect(option) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun ArtistsList(artists: List<ArtistGroup>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = artists, key = { _, item -> item.name }) { index, artist ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}. ${artist.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CliBeatsTextPrimary,
                    maxLines = 1,
                )
                Text(
                    text = "${artist.trackCount} ${if (artist.trackCount == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CliBeatsTextSecondary,
                )
            }
            HorizontalDivider(thickness = 1.dp, color = CliBeatsSurface)
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AlbumsList(albums: List<AlbumGroup>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = albums, key = { _, item -> "${item.artist}_${item.title}" }) { index, album ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}. ${album.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CliBeatsTextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = CliBeatsTextSecondary,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CliBeatsTextSecondary,
                )
            }
            HorizontalDivider(thickness = 1.dp, color = CliBeatsSurface)
        }
    }
}
