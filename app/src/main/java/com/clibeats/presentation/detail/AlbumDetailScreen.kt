@file:Suppress("ktlint:standard:function-naming", "MagicNumber")

package com.clibeats.presentation.detail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.domain.model.Track
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.component.TrackActionSheet
import com.clibeats.presentation.navigation.AppRoute
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun AlbumDetailScreen(
    route: AppRoute.AlbumDetail,
    onBack: () -> Unit,
    viewModel: EntityDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(route.albumName) {
        viewModel.loadAlbum(route.albumName, route.artistName, route.providerId)
    }

    val state by viewModel.uiState.collectAsState()
    var actionTrack by remember { mutableStateOf<Track?>(null) }

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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp).padding(end = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = CliBeatsTextPrimary,
                    )
                }
                Text(
                    text = "ALBUM: ${route.albumName.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CliBeatsTextPrimary,
                )
            }
        }

        when (val current = state) {
            is EntityDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CliBeatsAccent)
                }
            }

            is EntityDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = current.message, color = CliBeatsTextSecondary)
                }
            }

            is EntityDetailUiState.Success -> {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "[ PLAY ALL ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = CliBeatsAccent,
                        modifier =
                            Modifier
                                .clickable { viewModel.playAll(current.tracks) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[ SHUFFLE ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = CliBeatsAccent,
                        modifier =
                            Modifier
                                .clickable { viewModel.shuffleAll(current.tracks) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(current.tracks) { index, track ->
                        SongTableRow(
                            trackTitle = track.title,
                            artist = track.artist,
                            duration = formatDuration(track.durationMs),
                            index = index + 1,
                            onClick = { viewModel.playAll(current.tracks, index) },
                            onActionClick = { actionTrack = track },
                        )
                    }
                }
            }
        }

        actionTrack?.let { track ->
            TrackActionSheet(
                track = track,
                onDismiss = { actionTrack = null },
                onPlayNext = { /* handled by global player */ },
            )
        }
    }
}
