// ForbiddenImport: UpdateState is the updater's UI-facing state type (same pattern as ScanStatus).
@file:Suppress("ktlint:standard:function-naming", "ForbiddenImport")

package com.clibeats.presentation.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.data.update.ReleaseInfo
import com.clibeats.data.update.UpdateState
import com.clibeats.presentation.theme.LocalAccentColor

/** Launch pop-up shown when an automatic check finds a newer release. */
@Suppress("FunctionNaming", "LongMethod")
@Composable
fun UpdatePrompt(viewModel: UpdateViewModel = hiltViewModel()) {
    val visible by viewModel.promptVisible.collectAsState()
    val state by viewModel.state.collectAsState()
    if (!visible) return
    val release = state.releaseOrNull() ?: return
    val accent = LocalAccentColor.current
    val busy = state.isWorking

    AlertDialog(
        // Tapping outside hides the prompt dialog without snoozing permanently.
        onDismissRequest = viewModel::hidePrompt,
        modifier = Modifier.border(1.dp, accent),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("> update --available", style = MaterialTheme.typography.titleSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CLIBeats v${release.version} is out. You have v${viewModel.installedVersion}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ReleaseNotesLink(release, accent)
                UpdateProgress(state, accent)
                Text(
                    text = "LATER hides this for a day. You can update any time in Settings → Updates.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.ReadyToInstall ->
                    TextButton(onClick = viewModel::install) { Text("INSTALL", color = accent) }
                is UpdateState.Downloading, is UpdateState.Verifying -> Unit
                is UpdateState.Failed ->
                    TextButton(onClick = { viewModel.download(fromPrompt = true) }) { Text("RETRY", color = accent) }
                else ->
                    TextButton(onClick = { viewModel.download(fromPrompt = true) }) { Text("DOWNLOAD", color = accent) }
            }
        },
        dismissButton = {
            if (busy) {
                TextButton(onClick = viewModel::hidePrompt) {
                    Text("HIDE", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                TextButton(onClick = viewModel::later) {
                    Text("LATER", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    )
}

/** Status line + progress bar for the download / verify / ready / failed states (shared with Settings). */
@Suppress("FunctionNaming")
@Composable
internal fun UpdateProgress(
    state: UpdateState,
    accent: Color,
) {
    when (state) {
        is UpdateState.Downloading -> {
            Text(
                // -1 = checksum fetch, queued, or size not known yet: say so instead of a frozen 0%.
                text = if (state.progress < 0) "Preparing download..." else "Downloading... ${state.progress}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.progress < 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accent)
            } else {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                )
            }
        }
        is UpdateState.Verifying ->
            Text("Checking the file...", style = MaterialTheme.typography.labelMedium, color = accent)
        is UpdateState.ReadyToInstall ->
            Text(
                text = "[ OK ] Downloaded and verified. Tap INSTALL, then confirm the update.",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
        is UpdateState.Failed ->
            Text(
                text = "! ${state.message}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        else -> Unit
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun ReleaseNotesLink(
    release: ReleaseInfo,
    accent: Color,
) {
    if (release.pageUrl.isBlank()) return
    val context = LocalContext.current
    Text(
        text = "release notes",
        style = MaterialTheme.typography.labelMedium,
        color = accent,
        textDecoration = TextDecoration.Underline,
        modifier =
            Modifier
                .clickable(role = Role.Button) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl))) }
                }.padding(vertical = 4.dp),
    )
}

/** A download or checksum check is running. */
internal val UpdateState.isWorking: Boolean
    get() = this is UpdateState.Downloading || this is UpdateState.Verifying

internal fun UpdateState.releaseOrNull(): ReleaseInfo? =
    when (this) {
        is UpdateState.Available -> release
        is UpdateState.Downloading -> release
        is UpdateState.Verifying -> release
        is UpdateState.ReadyToInstall -> release
        is UpdateState.Failed -> release
        else -> null
    }
