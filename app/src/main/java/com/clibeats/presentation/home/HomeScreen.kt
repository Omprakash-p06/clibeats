@file:Suppress(
    "ktlint:standard:function-naming",
    "FunctionNaming",
    "LongMethod",
)

package com.clibeats.presentation.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clibeats.presentation.component.SongTableHeader
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.layout.NavDestination
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/**
 * Home dashboard in the spotify-tui style.
 *
 * Shows a system status block, quick navigation shortcuts, and a live
 * trending tracks feed (Audius) so songs are listed on launch.
 */
@Suppress("FunctionNaming", "LongMethod")
@Composable
fun HomeScreen(
    onNavigate: (NavDestination) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val providerName by homeViewModel.activeProviderName.collectAsState()

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
    ) {
        // 1. System Provider Status Block
        item {
            TuiBlock(title = "System Status", isActive = true) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "● ACTIVE PROVIDER",
                            style = MaterialTheme.typography.labelMedium,
                            color = CliBeatsAccent,
                        )
                        Text(
                            text = "[ ONLINE ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = CliBeatsAccent,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            "PROVIDER : $providerName\n" +
                                "STREAM   : MP3 320 kbps (no account)\n" +
                                "CACHE    : 500 MB (LRU Active)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CliBeatsTextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Quick Navigation Shortcuts
        item {
            TuiBlock(title = "Quick Navigation") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickShortcutButton(
                        label = "[1] SEARCH MUSIC",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(NavDestination.Search) },
                    )
                    QuickShortcutButton(
                        label = "[2] LIBRARY",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(NavDestination.Library) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Trending Tracks Block Header
        item {
            SongTableHeader()
        }

        // 4. Track List Content
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = CliBeatsAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fetching trending tracks...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CliBeatsTextSecondary,
                        )
                    }
                }
            }
            is HomeUiState.Success -> {
                if (state.tracks.isEmpty()) {
                    item {
                        EmptyHomeState()
                    }
                } else {
                    items(state.tracks) { track ->
                        SongTableRow(
                            trackTitle = track.title,
                            artist = track.artist,
                            duration = formatDuration(track.durationMs),
                            artworkContent =
                                track.artworkUrl?.let { url ->
                                    {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Artwork for ${track.title}",
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                },
                            onClick = { playerViewModel.playTrack(track) },
                        )
                    }
                }
            }
            is HomeUiState.Error -> {
                item {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(CliBeatsSurface)
                                .border(1.dp, CliBeatsDivider)
                                .padding(16.dp),
                    ) {
                        Text(
                            text = "> error --provider_offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CliBeatsTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .background(CliBeatsSurface)
                .border(1.dp, CliBeatsDivider)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CliBeatsAccent,
        )
    }
}

@Composable
private fun EmptyHomeState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .border(1.dp, CliBeatsDivider)
                .padding(16.dp),
    ) {
        Text(
            text = "> home --welcome",
            style = MaterialTheme.typography.labelSmall,
            color = CliBeatsAccent,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text =
                "Welcome to CLIBeats Terminal Player.\n" +
                    "Use [Search] to discover music or browse your local [Library].",
            style = MaterialTheme.typography.bodySmall,
            color = CliBeatsTextSecondary,
        )
    }
}
