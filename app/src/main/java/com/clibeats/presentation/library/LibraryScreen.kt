@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tracks", "Artists", "Albums")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground),
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = CliBeatsSurface,
            contentColor = CliBeatsAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = CliBeatsAccent,
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedTabIndex == index) CliBeatsAccent else CliBeatsTextSecondary,
                        )
                    },
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
                        text = "Library is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CliBeatsTextSecondary,
                    )
                }
            }

            is LibraryUiState.Success -> {
                when (selectedTabIndex) {
                    0 -> TracksList(tracks = currentState.tracks, onTrackClick = { track, index ->
                        viewModel.onTrackClick(track, currentState.tracks, index)
                    })
                    1 -> ArtistsList(artists = currentState.artists)
                    2 -> AlbumsList(albums = currentState.albums)
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun TracksList(
    tracks: List<Track>,
    onTrackClick: (Track, Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = tracks, key = { _, track -> track.id }) { index, track ->
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

@Suppress("FunctionNaming")
@Composable
private fun ArtistsList(artists: List<ArtistGroup>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = artists, key = { _, artist -> artist.name }) { _, artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CliBeatsTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${artist.trackCount} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
            }
            HorizontalDivider(color = CliBeatsDivider, thickness = 1.dp)
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AlbumsList(albums: List<AlbumGroup>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = albums, key = { index, album -> "${album.title}_$index" }) { _, album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CliBeatsTextPrimary,
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = CliBeatsTextSecondary,
                    )
                }
                Text(
                    text = "${album.trackCount} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
            }
            HorizontalDivider(color = CliBeatsDivider, thickness = 1.dp)
        }
    }
}
