@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.SongTableHeader
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun SearchScreen(
    onTrackClick: (Track) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val state by viewModel.searchResults.collectAsState()
    val providerName by viewModel.activeProviderName.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground)
            .padding(12.dp),
    ) {
        // ── Search input block ───────────────────────────────────────────
        TuiBlock(title = "Search Prompt", isActive = true) {
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search music" },
                placeholder = {
                    Text(
                        text = "> What do you want to play?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CliBeatsTextSecondary,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = CliBeatsAccent,
                    )
                },
                trailingIcon = {
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CliBeatsSurface,
                    unfocusedContainerColor = CliBeatsSurface,
                    focusedTextColor = CliBeatsTextPrimary,
                    unfocusedTextColor = CliBeatsTextPrimary,
                    focusedIndicatorColor = CliBeatsAccent,
                    unfocusedIndicatorColor = CliBeatsDivider,
                    cursorColor = CliBeatsAccent,
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Results area block ───────────────────────────────────────────
        TuiBlock(title = "Search Results — $providerName", modifier = Modifier.weight(1f)) {
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
                            onTrackClick = onTrackClick,
                        )
                    }
            }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Searching…" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = CliBeatsAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Searching $providerName... [████░░░░]",
                style = MaterialTheme.typography.labelSmall,
                color = CliBeatsAccent,
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

@Suppress("FunctionNaming")
@Composable
private fun SearchResultsList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SongTableHeader()
        }
        itemsIndexed(items = tracks, key = { _, track -> track.id }) { index, track ->
            SongTableRow(
                trackTitle = track.title,
                artist = buildString {
                    append(track.artist)
                    if (track.album.isNotBlank()) append(" • ${track.album}")
                },
                duration = formatDuration(track.durationMs),
                index = index + 1,
                artworkContent = track.artworkUrl?.let {
                    {
                        AsyncImage(
                            model = it,
                            contentDescription = "Artwork for ${track.title}",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
                onClick = { onTrackClick(track) },
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
