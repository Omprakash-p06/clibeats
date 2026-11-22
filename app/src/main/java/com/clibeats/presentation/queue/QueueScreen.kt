@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.queue

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.component.ReorderableList
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TrackArtwork
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel(),
    onClose: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CliBeatsBackground),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(CliBeatsSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Close queue",
                            tint = CliBeatsTextPrimary,
                        )
                    }
                }
                Text(
                    text = "PLAYBACK QUEUE",
                    style = MaterialTheme.typography.titleMedium,
                    color = CliBeatsTextPrimary,
                )
            }
        }

        when (val currentState = state) {
            is QueueUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CliBeatsTextSecondary,
                    )
                }
            }

            is QueueUiState.Success -> SplitQueueView(currentState, viewModel)
        }
    }
}

@Suppress("FunctionNaming", "LongMethod")
@Composable
private fun SplitQueueView(
    state: QueueUiState.Success,
    viewModel: QueueViewModel,
) {
    val tracks = state.tracks
    val currentIndex = tracks.indexOfFirst { it.id == state.currentTrackId }
    val nowPlayingTrack = if (currentIndex in tracks.indices) tracks[currentIndex] else tracks.firstOrNull()
    val upcomingStartIndex = if (currentIndex >= 0) currentIndex + 1 else 0
    val upcoming = if (currentIndex >= 0) tracks.drop(currentIndex + 1) else emptyList()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
    ) {
        if (nowPlayingTrack != null) {
            TuiBlock(
                title = "Now Playing",
                isActive = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                SongTableRow(
                    trackTitle = nowPlayingTrack.title,
                    artist = nowPlayingTrack.artist,
                    duration = formatDuration(nowPlayingTrack.durationMs),
                    index = if (currentIndex >= 0) currentIndex + 1 else 1,
                    isNowPlaying = true,
                    onClick = {
                        if (currentIndex >= 0) {
                            viewModel.onTrackClick(nowPlayingTrack, currentIndex)
                        }
                    },
                )
            }
        }

        TuiBlock(
            title = "Up Next (${upcoming.size})",
            isActive = false,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val autoplay by viewModel.autoplayEnabled.collectAsState()
                    Text(
                        text = if (autoplay) "[ ∞ AUTOPLAY: ON ]" else "[ ∞ AUTOPLAY: OFF ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (autoplay) CliBeatsAccent else CliBeatsTextSecondary,
                        modifier =
                            Modifier
                                .clickable { viewModel.toggleAutoplay() }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[ SHUFFLE ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = CliBeatsAccent,
                        modifier =
                            Modifier
                                .clickable { viewModel.onShuffle() }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[ CLEAR ]",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (upcoming.isNotEmpty()) {
                                CliBeatsTextSecondary
                            } else {
                                CliBeatsTextSecondary.copy(alpha = 0.5f)
                            },
                        modifier =
                            Modifier
                                .clickable(enabled = upcoming.isNotEmpty()) { viewModel.onClear() }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }

                if (upcoming.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No upcoming tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = CliBeatsTextSecondary,
                        )
                    }
                } else {
                    ReorderableUpcomingList(
                        upcoming = upcoming,
                        startIndex = upcomingStartIndex,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

/**
 * Up Next. Drag a song's ≡ handle, or hold its cover and drag, to move it; the playing song (shown above)
 * stays put. The move is sent to the player once, when the finger lifts, so playback isn't disturbed.
 */
@Suppress("FunctionNaming")
@Composable
private fun ReorderableUpcomingList(
    upcoming: List<com.clibeats.domain.model.Track>,
    startIndex: Int,
    viewModel: QueueViewModel,
) {
    ReorderableList(
        items = upcoming,
        itemId = { it.id },
        onReorder = { from, to, _ -> viewModel.onMove(from + startIndex, to + startIndex) },
    ) { track, index ->
        val queueIndex = index + startIndex
        SongTableRow(
            trackTitle = track.title,
            artist = track.artist,
            duration = formatDuration(track.durationMs),
            index = queueIndex + 1,
            artworkContent = {
                TrackArtwork(
                    artworkUrl = track.artworkUrl,
                    size = 32,
                    modifier = Modifier.artworkHandle { viewModel.onTrackClick(track, queueIndex) },
                )
            },
            onClick = { if (!isAnyDragging) viewModel.onTrackClick(track, queueIndex) },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DragHandle()
                    IconButton(
                        onClick = { viewModel.onRemove(queueIndex) },
                        enabled = !isAnyDragging,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove from queue",
                            tint = CliBeatsTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
        )
    }
}
