@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsSurfaceVariant
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/**
 * Persistent bottom player bar — always visible across all main screens.
 *
 * Per UI-SPEC:
 * - Height: 64dp
 * - Background: CliBeatsSurface (#151515)
 * - Top border: 1dp CliBeatsDivider
 * - Progress bar: 2dp, pinned to top edge of bar (above the divider)
 * - Controls: SkipPrevious · PlayArrow/Pause (32dp accent) · SkipNext · QueueMusic
 *
 * @param progress 0.0 to 1.0 progress fraction
 */
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun PlayerBar(
    trackTitle: String = "Not playing",
    artist: String = "",
    isPlaying: Boolean = false,
    progress: Float = 0f,
    artworkContent: (@Composable () -> Unit)? = null,
    onPlayPauseClick: () -> Unit = {},
    onSkipNextClick: () -> Unit = {},
    onSkipPreviousClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val playPauseDescription = if (isPlaying) "Pause $trackTitle" else "Play $trackTitle"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CliBeatsSurface),
    ) {
        // ── 2dp progress bar pinned to top of player bar ──────────────────
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = CliBeatsAccent,
            trackColor = CliBeatsSurfaceVariant,
        )

        // ── 1dp border divider ────────────────────────────────────────────
        HorizontalDivider(
            thickness = 1.dp,
            color = CliBeatsDivider,
        )

        // ── Main player row (64dp) ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork (40x40dp square)
            Box(modifier = Modifier.size(40.dp)) {
                artworkContent?.invoke()
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Track title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CliBeatsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist.isNotBlank()) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = CliBeatsTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Controls: SkipPrevious · Play/Pause · SkipNext · Queue
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onSkipPreviousClick,
                    modifier = Modifier.semantics { contentDescription = "Skip previous" },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = null,
                        tint = CliBeatsTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.semantics { contentDescription = playPauseDescription },
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = CliBeatsAccent,
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(
                    onClick = onSkipNextClick,
                    modifier = Modifier.semantics { contentDescription = "Skip next" },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = null,
                        tint = CliBeatsTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = onQueueClick,
                    modifier = Modifier.semantics { contentDescription = "Open queue" },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                        contentDescription = null,
                        tint = CliBeatsTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
