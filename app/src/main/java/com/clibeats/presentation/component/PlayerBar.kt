@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.theme.CliBeatsSurfaceVariant
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor

/**
 * Persistent bottom player bar — always visible across all main screens.
 *
 * Terminal-style panel:
 * - Rendered as a [TuiBlock] titled "Playing" that highlights when active
 * - Controls: SkipPrevious · PlayArrow/Pause (accent) · SkipNext · QueueMusic
 * - Seekable progress bar with `m:ss / m:ss` timestamp (Spotify TUI style)
 *
 * @param progress     0.0 – 1.0 progress fraction (for display)
 * @param currentMs    Actual current position in ms (for timestamp display)
 * @param totalMs      Track duration in ms (for timestamp display + seek calc)
 * @param onSeek       Called with target position ms when user taps/drags bar
 */
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun PlayerBar(
    trackTitle: String = "Nothing Playing",
    artist: String = "",
    isPlaying: Boolean = false,
    progress: Float = 0f,
    currentMs: Long = 0L,
    totalMs: Long = 0L,
    artworkContent: (@Composable () -> Unit)? = null,
    onPlayPauseClick: () -> Unit = {},
    onSkipNextClick: () -> Unit = {},
    onSkipPreviousClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSeek: (positionMs: Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColor.current
    val playPauseDescription = if (isPlaying) "Pause $trackTitle" else "Play $trackTitle"

    // Track bar width in pixels for seek calculation
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isDragging) dragProgress else progress

    TuiBlock(
        title = "Playing",
        isActive = isPlaying,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork square (36x36dp)
                Box(modifier = Modifier.size(36.dp)) {
                    artworkContent?.invoke()
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Track Title + Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CliBeatsTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artist.isNotBlank()) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = CliBeatsTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Controls
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
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.semantics { contentDescription = playPauseDescription },
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp),
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
                            modifier = Modifier.size(20.dp),
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
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Timestamp + Seekable Progress Bar ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Current position timestamp
                Text(
                    text = formatPlayerTime(currentMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = CliBeatsTextSecondary,
                    modifier = Modifier.padding(end = 6.dp),
                )

                // Seekable progress bar (fills remaining space)
                LinearProgressIndicator(
                    progress = { displayProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(totalMs) {
                            detectTapGestures { offset ->
                                if (totalMs > 0L) {
                                    val fraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                    onSeek((fraction * totalMs).toLong())
                                }
                            }
                        }
                        .pointerInput(totalMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    if (totalMs > 0L) {
                                        onSeek((dragProgress * totalMs).toLong())
                                    }
                                    isDragging = false
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { change, _ ->
                                    val fraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                    dragProgress = fraction
                                },
                            )
                        },
                    color = accent,
                    trackColor = CliBeatsSurfaceVariant,
                )

                // Total duration timestamp
                Text(
                    text = formatPlayerTime(totalMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = CliBeatsTextSecondary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** Format milliseconds as `m:ss` or `h:mm:ss`. */
private fun formatPlayerTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1_000L
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
