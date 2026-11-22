@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
    "LongMethod",
    "FunctionNaming",
    "LongParameterList",
    "MatchingDeclarationName",
)

package com.clibeats.presentation.playlist

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.ARTWORK_THUMB_PX
import com.clibeats.presentation.component.AddToPlaylistDialog
import com.clibeats.presentation.component.FavoritesViewModel
import com.clibeats.presentation.component.ReorderableList
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TrackArtwork
import com.clibeats.presentation.component.artworkUrlFor
import com.clibeats.presentation.component.isLocalFilePlaylist
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsSurfaceVariant
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

enum class PlaylistFilter { ALL, LOCAL_M3U }

@Suppress("CyclomaticComplexMethod")
@Composable
fun PlaylistScreen(
    showGridRequested: Boolean = false,
    onShowGridHandled: () -> Unit = {},
    filter: PlaylistFilter = PlaylistFilter.ALL,
    triggerCreateDialog: Boolean = false,
    onCreateDialogHandled: () -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val favorites: FavoritesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Playlist?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var coverChangePlaylist by remember { mutableStateOf<Playlist?>(null) }
    var pendingClear by remember { mutableStateOf<ClearTarget?>(null) }

    // Re-tapping the Playlists tab while inside a playlist goes back to the grid.
    LaunchedEffect(showGridRequested) {
        if (showGridRequested) {
            viewModel.selectPlaylist(null)
            onShowGridHandled()
        }
    }
    LaunchedEffect(triggerCreateDialog) {
        if (triggerCreateDialog) {
            showCreateDialog = true
            onCreateDialogHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground),
    ) {
        when (val s = state) {
            is PlaylistUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CliBeatsAccent)
                }

            is PlaylistUiState.Success -> {
                val tracks = s.selectedPlaylistTracks
                val selected = s.selectedPlaylist
                when {
                    s.isFavoritesOpen -> {
                        BackHandler { viewModel.selectPlaylist(null) }
                        PlaylistDetailView(
                            title = "♥ FAVORITES",
                            subtitle = "${tracks.size} tracks",
                            tracks = tracks,
                            emptyText = "No favorites yet. Tap ♥ in Now Playing to add songs.",
                            onBackClick = { viewModel.selectPlaylist(null) },
                            onTrackClick = { index -> viewModel.playPlaylist(tracks, index) },
                            onRemoveClick = viewModel::removeFromFavorites,
                            removeLabel = "Remove from favorites",
                            onReorder = viewModel::reorderFavorites,
                            onSwipeRight = viewModel::addToQueue,
                            onSwipeLeft = favorites::add,
                            onLongPress = { addToPlaylistTrack = it },
                            headerAction =
                                clearAllAction(tracks.isNotEmpty()) { pendingClear = ClearTarget.FAVORITES },
                        )
                    }
                    s.isMostPlayedOpen -> {
                        BackHandler { viewModel.selectPlaylist(null) }
                        PlaylistDetailView(
                            title = "MOST PLAYED",
                            subtitle = "${tracks.size} tracks · by play count",
                            tracks = tracks,
                            emptyText = "Nothing played yet. Your most played songs will show up here.",
                            onBackClick = { viewModel.selectPlaylist(null) },
                            onTrackClick = { index -> viewModel.playPlaylist(tracks, index) },
                            onRemoveClick = viewModel::removeFromMostPlayed,
                            removeLabel = "Remove from Most Played",
                            onSwipeRight = viewModel::addToQueue,
                            onSwipeLeft = favorites::add,
                            onLongPress = { addToPlaylistTrack = it },
                            headerAction =
                                clearAllAction(tracks.isNotEmpty()) { pendingClear = ClearTarget.MOST_PLAYED },
                        )
                    }
                    selected != null -> {
                        BackHandler { viewModel.selectPlaylist(null) }
                        val readOnly = selected.isLocalFilePlaylist()
                        PlaylistDetailView(
                            title = selected.name,
                            subtitle =
                                if (readOnly) "[LOCAL] ${tracks.size} tracks · read-only" else "${tracks.size} tracks",
                            tracks = tracks,
                            emptyText =
                                if (readOnly) {
                                    "No matching songs found. Rescan in Settings → Local Music."
                                } else {
                                    "Playlist is empty. Long-press any song to add it."
                                },
                            onBackClick = { viewModel.selectPlaylist(null) },
                            onTrackClick = { index -> viewModel.playPlaylist(tracks, index) },
                            onRemoveClick =
                                if (readOnly) {
                                    null
                                } else {
                                    { track -> viewModel.removeSongFromPlaylist(selected.id, track.id) }
                                },
                            removeLabel = "Remove from playlist",
                            onReorder =
                                if (readOnly) {
                                    null
                                } else {
                                    { reordered -> viewModel.reorderPlaylist(selected.id, reordered) }
                                },
                            onSwipeRight = viewModel::addToQueue,
                            onSwipeLeft = favorites::add,
                            onLongPress = { addToPlaylistTrack = it },
                            headerAction =
                                if (readOnly && tracks.isNotEmpty()) {
                                    "[ SAVE AS APP PLAYLIST ]" to { viewModel.saveAsAppPlaylist(selected, tracks) }
                                } else {
                                    null
                                },
                            coverUrl = s.covers[selected.id],
                            onChangeCoverClick =
                                if (!readOnly) {
                                    { coverChangePlaylist = selected }
                                } else {
                                    null
                                },
                        )
                    }
                    else ->
                        PlaylistBrowser(
                            state = s,
                            filter = filter,
                            viewMode = viewMode,
                            onViewModeChange = viewModel::setViewMode,
                            onFavoritesClick = viewModel::openFavorites,
                            onMostPlayedClick = viewModel::openMostPlayed,
                            onCreateClick = { showCreateDialog = true },
                            onImportClick = { showImportDialog = true },
                            onPlaylistClick = { viewModel.selectPlaylist(it.id) },
                            onDeleteClick = { pendingDelete = it },
                        )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                viewModel.createPlaylist(name, desc)
                showCreateDialog = false
            },
        )
    }
    if (showImportDialog) {
        ImportPlaylistDialog(
            onDismiss = { showImportDialog = false },
            onImport = { url, onComplete ->
                viewModel.importPlaylistFromUrl(url) { success, error ->
                    onComplete(success, error)
                    if (success) showImportDialog = false
                }
            },
        )
    }
    coverChangePlaylist?.let { playlist ->
        ChangeCoverDialog(
            playlist = playlist,
            onDismiss = { coverChangePlaylist = null },
            onPickImage = { uri ->
                viewModel.setPlaylistCoverImage(playlist.id, uri.toString())
                coverChangePlaylist = null
            },
            onSave = { newUrl ->
                viewModel.updatePlaylistArtwork(playlist.id, newUrl)
                coverChangePlaylist = null
            },
        )
    }
    // Clearing a whole list always asks first, so it can't happen by a stray tap.
    pendingClear?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            modifier = Modifier.border(1.dp, CliBeatsAccent),
            title = { Text(if (target == ClearTarget.FAVORITES) "> clear --favorites" else "> clear --most-played") },
            text = {
                Text(
                    if (target == ClearTarget.FAVORITES) {
                        "Remove every song from ♥ Favorites?"
                    } else {
                        "Reset Most Played? It starts counting again from now. Your listening history is kept."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (target == ClearTarget.FAVORITES) viewModel.clearFavorites() else viewModel.clearMostPlayed()
                        pendingClear = null
                    },
                ) { Text("Clear all", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingClear = null }) { Text("Cancel") } },
            containerColor = CliBeatsSurface,
        )
    }
    pendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("> rm \"${playlist.name}\"") },
            text = { Text("Delete this playlist? The songs themselves are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(playlist.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            containerColor = CliBeatsSurface,
        )
    }
    addToPlaylistTrack?.let { track ->
        AddToPlaylistDialog(track = track, onDismiss = { addToPlaylistTrack = null })
    }
}

@Composable
private fun PlaylistDetailView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    emptyText: String,
    onBackClick: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onRemoveClick: ((Track) -> Unit)?,
    removeLabel: String,
    onSwipeRight: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onLongPress: (Track) -> Unit,
    headerAction: Pair<String, () -> Unit>? = null,
    coverUrl: String? = null,
    onChangeCoverClick: (() -> Unit)? = null,
    /** Non-null: rows get a ≡ handle to drag them into a new order (long-press stays "add to playlist"). */
    onReorder: ((List<Track>) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to playlists",
                    tint = CliBeatsTextPrimary,
                )
            }
            if (onChangeCoverClick != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CliBeatsSurfaceVariant)
                        .border(1.dp, CliBeatsAccent, RoundedCornerShape(4.dp))
                        .clickable(onClick = onChangeCoverClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (coverUrl.isNullOrBlank()) {
                        Text(text = "♫", style = MaterialTheme.typography.titleMedium, color = CliBeatsAccent)
                    } else {
                        AsyncImage(
                            model = artworkUrlFor(coverUrl, ARTWORK_THUMB_PX * 2),
                            contentDescription = "Change playlist cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CliBeatsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = CliBeatsTextSecondary)
            }
            if (onChangeCoverClick != null) {
                IconButton(onClick = onChangeCoverClick) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Change playlist cover",
                        tint = CliBeatsAccent,
                    )
                }
            }
        }
        if (headerAction != null) {
            Text(
                text = headerAction.first,
                style = MaterialTheme.typography.labelMedium,
                color = CliBeatsAccent,
                modifier = Modifier
                    .clickable(onClick = headerAction.second)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = emptyText, style = MaterialTheme.typography.bodyMedium, color = CliBeatsTextSecondary)
            }
        } else if (onReorder != null) {
            ReorderableList(
                items = tracks,
                itemId = { it.id },
                onReorder = { _, _, reordered -> onReorder(reordered) },
            ) { track, index ->
                SongTableRow(
                    trackTitle = track.title,
                    artist = track.artist,
                    duration = formatDuration(track.durationMs),
                    index = index + 1,
                    // Hold the cover (or drag ≡) to move the song; a quick tap on the cover still plays it.
                    artworkContent = {
                        TrackArtwork(
                            artworkUrl = track.artworkUrl,
                            size = 52,
                            modifier = Modifier.artworkHandle { onTrackClick(index) },
                        )
                    },
                    onClick = { if (!isAnyDragging) onTrackClick(index) },
                    onSwipeRight = { onSwipeRight(track) },
                    onSwipeLeft = { onSwipeLeft(track) },
                    onLongPress = { onLongPress(track) },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DragHandle()
                            onRemoveClick?.let { remove ->
                                IconButton(onClick = { remove(track) }, enabled = !isAnyDragging) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "$removeLabel: ${track.title}",
                                        tint = CliBeatsTextSecondary,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                itemsIndexed(items = tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    SongTableRow(
                        trackTitle = track.title,
                        artist = track.artist,
                        duration = formatDuration(track.durationMs),
                        index = index + 1,
                        artworkContent = { TrackArtwork(artworkUrl = track.artworkUrl, size = 52) },
                        onClick = { onTrackClick(index) },
                        onSwipeRight = { onSwipeRight(track) },
                        onSwipeLeft = { onSwipeLeft(track) },
                        onLongPress = { onLongPress(track) },
                        actions =
                            onRemoveClick?.let { remove ->
                                {
                                    IconButton(onClick = { remove(track) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "$removeLabel: ${track.title}",
                                            tint = CliBeatsTextSecondary,
                                        )
                                    }
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String, (Boolean, String?) -> Unit) -> Unit,
) {
    var urlText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = { Text(text = "> import --url", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste a Spotify or YouTube Music playlist link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        errorMessage = null
                    },
                    label = { Text("Playlist URL") },
                    singleLine = true,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isImporting = true
                    errorMessage = null
                    onImport(urlText) { success, err ->
                        isImporting = false
                        if (!success) {
                            errorMessage = err ?: "Import failed"
                        }
                    }
                },
                enabled = urlText.isNotBlank() && !isImporting,
            ) {
                Text(if (isImporting) "Importing..." else "Import", color = CliBeatsAccent)
            }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = CliBeatsTextSecondary)
                }
            }
        },
        containerColor = CliBeatsSurface,
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "> new playlist", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create", color = CliBeatsAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CliBeatsTextSecondary)
            }
        },
        containerColor = CliBeatsSurface,
    )
}

@Composable
private fun ChangeCoverDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onPickImage: (Uri) -> Unit,
    onSave: (String?) -> Unit,
) {
    var urlText by remember { mutableStateOf("") }
    // The copy into app storage happens off the main thread in the repository.
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) onPickImage(uri)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, CliBeatsAccent),
        title = { Text(text = "> edit --cover", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Change thumbnail for \"${playlist.name}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = CliBeatsAccent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("CHOOSE FROM DEVICE")
                }
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Image URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Only a custom cover can be reset; otherwise the first song's art is already shown.
                if (!playlist.artworkUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = { onSave(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reset to default cover", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(urlText.trim().ifBlank { null }) },
                enabled = urlText.isNotBlank(),
            ) {
                Text("Save", color = CliBeatsAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CliBeatsTextSecondary)
            }
        },
        containerColor = CliBeatsSurface,
    )
}

private enum class ClearTarget { FAVORITES, MOST_PLAYED }

private fun clearAllAction(
    enabled: Boolean,
    onClick: () -> Unit,
): Pair<String, () -> Unit>? = if (enabled) "[ CLEAR ALL ]" to onClick else null
