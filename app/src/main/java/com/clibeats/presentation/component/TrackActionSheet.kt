@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "LongMethod",
    "FunctionNaming",
    "LongParameterList",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clibeats.domain.model.Track
import com.clibeats.presentation.theme.CliBeatsBorder
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor

private val ACTION_ROW_HEIGHT = 48.dp
private val SHEET_HORIZONTAL_PADDING = 16.dp
private val SHEET_VERTICAL_PADDING = 12.dp
private val ACTION_TEXT_SIZE = 14.sp
private val HEADER_GAP = 12.dp

/**
 * TUI bottom sheet displaying actions for a track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    track: Track,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onToggleDownload: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    isLiked: Boolean = false,
    onGoToArtist: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = LocalAccentColor.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CliBeatsSurface,
        modifier = modifier.border(1.dp, CliBeatsBorder),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SHEET_HORIZONTAL_PADDING, vertical = SHEET_VERTICAL_PADDING),
        ) {
            // Header: 52dp art + track info + tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SHEET_VERTICAL_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(artworkUrl = track.artworkUrl, size = 52)
                Spacer(modifier = Modifier.width(HEADER_GAP))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CliBeatsTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = CliBeatsTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "[ ${track.providerId.uppercase()} ]",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = accent,
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = CliBeatsDivider)

            // Action rows (48dp height)
            ActionRow(
                icon = "[ ⏭ ]",
                label = "PLAY NEXT",
                onClick = {
                    onPlayNext()
                    onDismiss()
                },
            )

            ActionRow(
                icon = "[ ≡+ ]",
                label = "ADD TO QUEUE",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                },
            )

            ActionRow(
                icon = "[ + ]",
                label = "ADD TO PLAYLIST",
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                },
            )

            if (onToggleDownload != null) {
                ActionRow(
                    icon = if (isDownloaded) "[ ✓ ]" else "[ ↓ ]",
                    label = if (isDownloaded) "REMOVE DOWNLOAD" else "DOWNLOAD FOR OFFLINE",
                    onClick = {
                        onToggleDownload()
                        onDismiss()
                    },
                )
            }

            if (onToggleLike != null) {
                ActionRow(
                    icon = if (isLiked) "[ ♥ ]" else "[ ♡ ]",
                    label = if (isLiked) "REMOVE FROM FAVORITES" else "ADD TO FAVORITES",
                    onClick = {
                        onToggleLike()
                        onDismiss()
                    },
                )
            }

            if (onGoToArtist != null && track.artist.isNotBlank()) {
                ActionRow(
                    icon = "[ 👤 ]",
                    label = "GO TO ARTIST",
                    onClick = {
                        onGoToArtist()
                        onDismiss()
                    },
                )
            }

            if (onGoToAlbum != null && track.album.isNotBlank()) {
                ActionRow(
                    icon = "[ 💿 ]",
                    label = "GO TO ALBUM",
                    onClick = {
                        onGoToAlbum()
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ACTION_ROW_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = ACTION_TEXT_SIZE,
                fontWeight = FontWeight.SemiBold,
            ),
            color = LocalAccentColor.current,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = ACTION_TEXT_SIZE,
            ),
            color = CliBeatsTextPrimary,
        )
    }
}
