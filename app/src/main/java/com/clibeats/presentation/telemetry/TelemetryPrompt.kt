@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.clibeats.presentation.telemetry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/** One-time prompt asking whether the user wants to report usage anonymously. */
@Composable
fun TelemetryPrompt(viewModel: TelemetryViewModel = hiltViewModel()) {
    val visible by viewModel.promptVisible.collectAsState()
    if (!visible) return

    AlertDialog(
        onDismissRequest = { viewModel.onOptOut() },
        modifier = Modifier.border(1.dp, CliBeatsAccent),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "> telemetry --opt-in",
                style = MaterialTheme.typography.titleSmall,
                color = CliBeatsAccent,
            )
        },
        text = {
            Column {
                Text(
                    text =
                        "Would you like to report your usage anonymously to github so that our " +
                            "real time user data update goes up? none of your data is collected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CliBeatsTextPrimary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::onOptIn) {
                Text("[ YES ]", color = CliBeatsAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onOptOut) {
                Text("[ NO ]", color = CliBeatsTextSecondary)
            }
        },
    )
}
