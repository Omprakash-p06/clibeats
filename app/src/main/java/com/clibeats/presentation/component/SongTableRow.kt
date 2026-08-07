@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextDisabled
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/**
 * Dense 48dp song table row — the primary list item component.
 *
 * States:
 * - Normal: CliBeatsBackground background
 * - Focused/Hovered: CliBeatsSurfaceVariant background
 * - Now Playing: CliBeatsSurface + 2dp CliBeatsAccent left border
 *
 * Per UI-SPEC: 48dp height, 32x32dp square artwork, start-aligned text.
 */
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun SongTableRow(
    trackTitle: String,
    artist: String,
    duration: String,
    index: Int? = null,
    isNowPlaying: Boolean = false,
    artworkContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val semanticsDescription = "$trackTitle by $artist"
    val rowBackground = when {
        isNowPlaying -> CliBeatsSurface
        else -> CliBeatsBackground
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(rowBackground)
                .semantics { contentDescription = semanticsDescription }
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Now-playing left accent bar (2dp) ─────────────────────────
            if (isNowPlaying) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(CliBeatsAccent),
                )
            } else {
                Spacer(modifier = Modifier.width(2.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // ── Track index or artwork ─────────────────────────────────────
            if (artworkContent != null) {
                Box(
                    modifier = Modifier.size(32.dp),
                ) {
                    artworkContent()
                }
            } else {
                // Index number fallback when no artwork
                Text(
                    text = index?.toString() ?: "–",
                    style = MaterialTheme.typography.labelMedium,
                    color = CliBeatsTextDisabled,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── Track title + artist (fills remaining space) ──────────────
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isNowPlaying) CliBeatsAccent else CliBeatsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = CliBeatsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── Duration (end-aligned, tabular) ───────────────────────────
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium,
                color = CliBeatsTextSecondary,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        // ── 1dp row divider ───────────────────────────────────────────────
        HorizontalDivider(
            thickness = 1.dp,
            color = CliBeatsDivider,
        )
    }
}

@Composable
fun SongTableHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#",
                style = MaterialTheme.typography.labelSmall,
                color = CliBeatsTextSecondary,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TITLE & ARTIST",
                style = MaterialTheme.typography.labelSmall,
                color = CliBeatsTextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "TIME",
                style = MaterialTheme.typography.labelSmall,
                color = CliBeatsTextSecondary,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = CliBeatsDivider)
    }
}
