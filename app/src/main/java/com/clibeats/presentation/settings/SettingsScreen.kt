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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsSurface

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "[ SETTINGS ]",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Section: Active Music Provider
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CliBeatsSurface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVE MUSIC PROVIDER",
                    style = MaterialTheme.typography.titleMedium,
                    color = CliBeatsAccent,
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf("ytmusic" to "YouTube Music", "local" to "Local Device Media").forEach { (id, label) ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setActiveProvider(id) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.activeProviderId == id,
                            onClick = { viewModel.setActiveProvider(id) },
                            colors = RadioButtonDefaults.colors(selectedColor = CliBeatsAccent),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Section: Audio Cache Limit
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CliBeatsSurface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DISK CACHE LIMIT",
                    style = MaterialTheme.typography.titleMedium,
                    color = CliBeatsAccent,
                )
                Text(
                    text = "Current Usage: ${uiState.currentCacheSizeBytes / BYTES_PER_MB} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(256, 512, 1024, 2048).forEach { mb ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCacheMaxMb(mb) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.cacheMaxMb == mb,
                            onClick = { viewModel.setCacheMaxMb(mb) },
                            colors = RadioButtonDefaults.colors(selectedColor = CliBeatsAccent),
                        )
                        Text(
                            text = "$mb MB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Section: Streaming Quality
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CliBeatsSurface),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "HIGH QUALITY STREAMING",
                        style = MaterialTheme.typography.titleMedium,
                        color = CliBeatsAccent,
                    )
                    Text(
                        text = "Prefer 256kbps AAC audio streams",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.highQualityStreaming,
                    onCheckedChange = { viewModel.setHighQualityStreaming(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = CliBeatsAccent),
                )
            }
        }

        // Section: Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CliBeatsSurface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "MAINTENANCE ACTIONS",
                    style = MaterialTheme.typography.titleMedium,
                    color = CliBeatsAccent,
                )
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
