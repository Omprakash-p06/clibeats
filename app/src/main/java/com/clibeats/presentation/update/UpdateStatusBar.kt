// ForbiddenImport: UpdateState is the updater's UI-facing state type (same pattern as UpdatePrompt).
@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "ForbiddenImport",
)

package com.clibeats.presentation.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.data.update.ReleaseInfo
import com.clibeats.data.update.UpdateState
import com.clibeats.presentation.theme.CliBeatsBorder
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor

/**
 * In-app persistent update status bar matching the retro TUI aesthetic.
 *
 * Appears below the top app bar whenever an update is downloading, verifying,
 * or ready to install. Allows the user to monitor live download progress without
 * having to keep the modal prompt dialog open.
 */
@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun UpdateStatusBar(
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accent = LocalAccentColor.current
    var dismissedState by remember { mutableStateOf<UpdateState?>(null) }

    val release = state.releaseOrNull()
    val isDownloading = state is UpdateState.Downloading
    val isVerifying = state is UpdateState.Verifying
    val isReady = state is UpdateState.ReadyToInstall

    val shouldShow = (isDownloading || isVerifying || isReady) && release != null && dismissedState != state

    AnimatedVisibility(
        visible = shouldShow,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        if (release == null) return@AnimatedVisibility

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .border(1.dp, CliBeatsBorder)
                .clickable(role = Role.Button) {
                    if (isReady) viewModel.install() else viewModel.showPrompt()
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = "> update: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = statusTextFor(state, release),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isReady) {
                        Text(
                            text = "[INSTALL]",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable(role = Role.Button) { viewModel.install() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    } else {
                        Text(
                            text = "[VIEW]",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable(role = Role.Button) { viewModel.showPrompt() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[×]",
                        style = MaterialTheme.typography.labelSmall,
                        color = CliBeatsTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable(role = Role.Button) { dismissedState = state }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            UpdateProgressBar(state = state, accent = accent)
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun UpdateProgressBar(
    state: UpdateState,
    accent: Color,
) {
    when (state) {
        is UpdateState.Downloading -> {
            val progress = state.progress
            if (progress < 0) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = accent,
                    trackColor = CliBeatsDivider,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = accent,
                    trackColor = CliBeatsDivider,
                )
            }
        }
        is UpdateState.Verifying -> {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = accent,
                trackColor = CliBeatsDivider,
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CliBeatsBorder),
            )
        }
    }
}

private fun statusTextFor(
    state: UpdateState,
    release: ReleaseInfo,
): String =
    when (state) {
        is UpdateState.Downloading -> {
            val progress = state.progress
            val percent = if (progress < 0) "connecting" else "$progress%"
            "v${release.version} downloading... $percent"
        }
        is UpdateState.Verifying -> "v${release.version} verifying..."
        is UpdateState.ReadyToInstall -> "[ OK ] v${release.version} ready"
        else -> "v${release.version}"
    }
