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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.AddToPlaylistDialog
import com.clibeats.presentation.component.FavoritesViewModel
import com.clibeats.presentation.component.ProviderSwitcher
import com.clibeats.presentation.component.SongTableHeader
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TrackArtwork
import com.clibeats.presentation.component.TuiBlock
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
    homeViewModel: HomeViewModel = hiltViewModel(),
    toggleSourceRequested: Boolean = false,
    onToggleSourceHandled: () -> Unit = {},
) {
    // Tapping the Home tab while already on Home switches the source Online <-> Local.
    LaunchedEffect(toggleSourceRequested) {
        if (toggleSourceRequested) {
            homeViewModel.toggleSource()
            onToggleSourceHandled()
        }
    }
    val favorites: FavoritesViewModel = hiltViewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val providerName by homeViewModel.activeProviderName.collectAsState()
    val activeProviderId by homeViewModel.activeProviderId.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val isOnline by homeViewModel.isOnline.collectAsState()
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    addToPlaylistTrack?.let { track ->
        AddToPlaylistDialog(track = track, onDismiss = { addToPlaylistTrack = null })
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
    ) {
        // 1. System Provider Status Block
        item {
            // Local files don't need the internet, so Local mode shows [ LOCAL ] instead of online/offline.
            val isLocal = activeProviderId == "local"
            val isSystemOnline = isLocal || isOnline
            val statusLabel =
                when {
                    isLocal -> "[ LOCAL ]"
                    isOnline -> "[ ONLINE ]"
                    else -> "[ OFFLINE ]"
                }
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
                            color = if (isSystemOnline) CliBeatsAccent else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSystemOnline) CliBeatsAccent else MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PROVIDER : $providerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = CliBeatsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProviderSwitcher(
                        providers = homeViewModel.providers,
                        activeProviderId = activeProviderId,
                        onSelect = homeViewModel::setActiveProvider,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Recently played (from history) with clear
        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "RECENTLY PLAYED",
                    actionLabel = "[ CLEAR ]",
                    onAction = homeViewModel::clearRecentlyPlayed,
                )
            }
            itemsIndexed(recentlyPlayed, key = { _, t -> "recent_${t.id}" }) { index, track ->
                SongTableRow(
                    trackTitle = track.title,
                    artist = track.artist,
                    duration = formatDuration(track.durationMs),
                    artworkContent = { TrackArtwork(artworkUrl = track.artworkUrl, size = 32) },
                    onClick = { homeViewModel.playFromList(recentlyPlayed, index) },
                    onSwipeRight = { homeViewModel.addToQueue(track) },
                    onSwipeLeft = { favorites.add(track) },
                    onLongPress = { addToPlaylistTrack = track },
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // 4. Trending Tracks Block Header
        item {
            SectionHeader(title = "TRENDING / ${providerName.uppercase()}")
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
                    itemsIndexed(state.tracks, key = { _, t -> "trend_${t.id}" }) { index, track ->
                        SongTableRow(
                            trackTitle = track.title,
                            artist = track.artist,
                            duration = formatDuration(track.durationMs),
                            artworkContent = { TrackArtwork(artworkUrl = track.artworkUrl, size = 32) },
                            onClick = { homeViewModel.playFromList(state.tracks, index) },
                            onSwipeRight = { homeViewModel.addToQueue(track) },
                            onSwipeLeft = { favorites.add(track) },
                            onLongPress = { addToPlaylistTrack = track },
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

@Suppress("FunctionNaming")
@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = CliBeatsAccent)
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = CliBeatsTextSecondary,
                modifier = Modifier.clickable(onClick = onAction).padding(8.dp),
            )
        }
    }
}

@Suppress("FunctionNaming")
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
