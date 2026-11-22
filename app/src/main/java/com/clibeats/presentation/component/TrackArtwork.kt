@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface

/**
 * Reusable track artwork component using Coil.
 *
 * Shows the track's artwork thumbnail from its URL.
 * Falls back to a muted green "♫" placeholder if the URL is null / fails to load.
 *
 * @param artworkUrl  The remote artwork URL. Null shows the placeholder immediately.
 * @param size        Square size in dp (default 52, the list-row size).
 * @param modifier    Optional Modifier.
 * @param isNowPlaying Draws an accent border instead of the normal divider.
 */
@Suppress("FunctionNaming")
@Composable
fun TrackArtwork(
    artworkUrl: String?,
    size: Int = ARTWORK_LIST_DP,
    modifier: Modifier = Modifier,
    isNowPlaying: Boolean = false,
) {
    val shape = RoundedCornerShape(4.dp)
    val borderColor = if (isNowPlaying) CliBeatsAccent else CliBeatsDivider
    val frame = modifier
        .size(size.dp)
        .border(1.dp, borderColor, shape)
        .clip(shape)

    if (artworkUrl.isNullOrBlank()) {
        ArtworkPlaceholder(size = size, modifier = frame)
        return
    }

    // AsyncImage + painter placeholder: SubcomposeAsyncImage subcomposed every list row (scroll jank).
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    AsyncImage(
        // Shared thumbnail size: one download per cover across all lists and the mini player.
        model = artworkUrlFor(artworkUrl, if (size <= THUMB_MAX_DP) ARTWORK_THUMB_PX else ARTWORK_FULL_PX),
        contentDescription = "Track artwork",
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        modifier = frame,
    )
}

@Suppress("FunctionNaming")
@Composable
private fun ArtworkPlaceholder(
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CliBeatsSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "♫",
            style = MaterialTheme.typography.labelSmall,
            color = CliBeatsAccent.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

private const val THUMB_MAX_DP = 64
private const val ARTWORK_LIST_DP = 52
