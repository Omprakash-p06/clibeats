@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
    "MagicNumber",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsSurface

/**
 * Reusable track artwork component using Coil.
 *
 * Shows the track's artwork thumbnail from its URL.
 * Falls back to a muted green "♫" placeholder if the URL is null / fails to load.
 *
 * @param artworkUrl  The remote artwork URL. Null shows the placeholder immediately.
 * @param size        Square size in dp (default 40).
 * @param modifier    Optional Modifier.
 */
@Suppress("FunctionNaming")
@Composable
fun TrackArtwork(
    artworkUrl: String?,
    size: Int = 40,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)

    if (artworkUrl.isNullOrBlank()) {
        ArtworkPlaceholder(size = size, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = artworkUrl,
        contentDescription = "Track artwork",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size.dp)
            .clip(shape),
        error = { ArtworkPlaceholder(size = size) },
        loading = { ArtworkPlaceholder(size = size) },
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
