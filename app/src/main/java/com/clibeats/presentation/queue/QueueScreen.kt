@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.search.formatDuration
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PLAYBACK QUEUE",
                style = MaterialTheme.typography.titleMedium,
                color = CliBeatsTextPrimary,
            )

            if (state is QueueUiState.Success) {
                OutlinedButton(
                    onClick = viewModel::onClear,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CliBeatsTextSecondary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Clear Queue",
                    )
                    Text(text = " Clear", style = MaterialTheme.typography.bodySmall)
                }
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

            is QueueUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        items = currentState.tracks,
                        key = { index, track -> "${track.id}_$index" },
                    ) { index, track ->
                        SongTableRow(
                            trackTitle = track.title,
                            artist = track.artist,
                            duration = formatDuration(track.durationMs),
                            index = index + 1,
                            isNowPlaying = track.id == currentState.currentTrackId,
                            onClick = { viewModel.onTrackClick(track, index) },
                        )
                    }
                }
            }
        }
    }
}
