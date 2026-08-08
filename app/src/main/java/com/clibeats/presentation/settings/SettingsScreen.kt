@file:Suppress("ktlint:standard:function-naming")

package com.clibeats.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.theme.CliBeatsAccent

private const val BYTES_PER_MB = 1048576L

@Suppress("FunctionNaming", "LongMethod", "MagicNumber")
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section: Active Music Provider
        TuiBlock(title = "Active Provider", isActive = true) {
            Column {
                listOf("ytmusic" to "YouTube Music (Gateway)", "local" to "Local Device Media").forEach { (id, label) ->
                    val isSelected = uiState.activeProviderId == id
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setActiveProvider(id) }
                                .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isSelected) "(•) " else "( ) ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) CliBeatsAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Section: Audio Cache Limit
        TuiBlock(title = "Disk Cache Limit") {
            Column {
                Text(
                    text = "Current Usage: ${uiState.currentCacheSizeBytes / BYTES_PER_MB} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                listOf(256, 512, 1024, 2048).forEach { mb ->
                    val isSelected = uiState.cacheMaxMb == mb
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCacheMaxMb(mb) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isSelected) "(•) " else "( ) ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) CliBeatsAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$mb MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Section: Streaming Quality
        TuiBlock(title = "Audio Quality") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setHighQualityStreaming(!uiState.highQualityStreaming) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "HIGH QUALITY STREAMING",
                        style = MaterialTheme.typography.titleSmall,
                        color = CliBeatsAccent,
                    )
                    Text(
                        text = "Prefer 256kbps AAC audio streams",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (uiState.highQualityStreaming) "[ ON ]" else "[ OFF ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.highQualityStreaming) CliBeatsAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Section: Actions
        TuiBlock(title = "Maintenance Actions") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { viewModel.clearCache() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("CLEAR ALL AUDIO CACHE")
                }
                if (uiState.hasAuthToken) {
                    Button(
                        onClick = { viewModel.clearAuthToken() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Text("CLEAR SESSION CREDENTIALS")
                    }
                }
            }
        }
    }
}
