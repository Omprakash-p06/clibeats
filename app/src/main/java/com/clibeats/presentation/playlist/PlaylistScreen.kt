@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
    "LongMethod",
    "FunctionNaming",
)

package com.clibeats.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun PlaylistScreen(viewModel: PlaylistViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground),
    ) {
        when (val currentState = state) {
            is PlaylistUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CliBeatsAccent)
                }
            }

            is PlaylistUiState.Success -> {
                if (currentState.selectedPlaylist != null) {
                    PlaylistDetailView(
                        playlist = currentState.selectedPlaylist,
                        tracks = currentState.selectedPlaylistTracks,
                        onBackClick = { viewModel.selectPlaylist(null) },
                        onTrackClick = { _, index ->
                            viewModel.playPlaylist(currentState.selectedPlaylistTracks, index)
                        },
                    )
                } else {
                    PlaylistListView(
                        playlists = currentState.playlists,
                        onCreateClick = { showCreateDialog = true },
                        onPlaylistClick = { playlist -> viewModel.selectPlaylist(playlist.id) },
                        onDeleteClick = { playlist -> viewModel.deletePlaylist(playlist.id) },
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
}

@Suppress("FunctionNaming")
@Composable
private fun PlaylistListView(
    playlists: List<Playlist>,
    onCreateClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeleteClick: (Playlist) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PLAYLISTS",
                style = MaterialTheme.typography.titleMedium,
                color = CliBeatsTextPrimary,
            )

            OutlinedButton(
                onClick = onCreateClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CliBeatsAccent),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "New Playlist")
                Text(text = " New", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No playlists found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CliBeatsTextSecondary,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items = playlists, key = { _, playlist -> playlist.id }) { _, playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { onPlaylistClick(playlist) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CliBeatsTextPrimary,
                            )
                            if (!playlist.description.isNullOrBlankCompat()) {
                                Text(
                                    text = playlist.description.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CliBeatsTextSecondary,
                                )
                            }
                        }

                        IconButton(onClick = { onDeleteClick(playlist) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete playlist",
                                tint = CliBeatsTextSecondary,
                            )
                        }
                    }
                    HorizontalDivider(color = CliBeatsDivider, thickness = 1.dp)
                }
            }
        }
    }
}

private fun String?.isNullOrBlankCompat(): Boolean = this == null || this.trim().isEmpty()

@Suppress("FunctionNaming")
@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Track>,
    onBackClick: () -> Unit,
    onTrackClick: (Track, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = CliBeatsTextPrimary,
                )
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = CliBeatsTextPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Playlist is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CliBeatsTextSecondary,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items = tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    SongTableRow(
                        trackTitle = track.title,
                        artist = track.artist,
                        duration = formatDuration(track.durationMs),
                        index = index + 1,
                        onClick = { onTrackClick(track, index) },
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create Playlist", style = MaterialTheme.typography.titleMedium) },
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
