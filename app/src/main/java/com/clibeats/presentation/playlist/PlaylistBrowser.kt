@file:Suppress(
    "FunctionNaming",
    "ktlint:standard:function-naming",
    "LongParameterList",
    "MatchingDeclarationName",
    "MagicNumber",
)

package com.clibeats.presentation.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clibeats.domain.model.Playlist
import com.clibeats.presentation.component.ARTWORK_THUMB_PX
import com.clibeats.presentation.component.artworkUrlFor
import com.clibeats.presentation.component.isLocalFilePlaylist
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBorderInactive
import com.clibeats.presentation.theme.CliBeatsSurfaceVariant
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/** How the Playlists overview is laid out. Saved by name. */
enum class PlaylistViewMode(
    val columns: Int,
    val label: String,
    val icon: ImageVector,
) {
    GRID_SMALL(3, "Small grid", Icons.Outlined.Apps),
    GRID_LARGE(2, "Large grid", Icons.Outlined.GridView),
    LIST(1, "List", Icons.AutoMirrored.Outlined.ViewList),
    DETAILS(1, "Details", Icons.Outlined.ViewAgenda),
    ;

    val isGrid: Boolean get() = columns > 1

    companion object {
        val DEFAULT = GRID_SMALL

        fun fromName(name: String?): PlaylistViewMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** What one playlist entry shows, whatever the view. */
private data class PlaylistEntry(
    val key: String,
    val title: String,
    val count: Int,
    val description: String?,
    val coverUrl: String?,
    val glyph: String,
    val pinned: Boolean = false,
    val local: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

/**
 * The Playlists overview: ♥ Favorites and Most Played pinned at the top, then your own playlists, then
 * playlists read from M3U/PLS files, each under its own header. Shown as a small or large grid, a compact
 * list, or a details list; the choice is remembered.
 */
@Suppress("LongMethod")
@Composable
internal fun PlaylistBrowser(
    state: PlaylistUiState.Success,
    filter: PlaylistFilter,
    viewMode: PlaylistViewMode,
    onViewModeChange: (PlaylistViewMode) -> Unit,
    onFavoritesClick: () -> Unit,
    onMostPlayedClick: () -> Unit,
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit,
    onDeleteClick: (Playlist) -> Unit,
) {
    val (local, app) = state.playlists.partition { it.isLocalFilePlaylist() }
    val pinned =
        listOf(
            PlaylistEntry(
                key = "fav",
                // U+FE0E asks for the plain text heart (accent coloured), not the red emoji some phones draw.
                title = "♥︎ Favorites",
                count = state.favoritesCount,
                description = "Songs you've hearted",
                coverUrl = state.favoritesCover,
                glyph = "♥︎",
                pinned = true,
                onClick = onFavoritesClick,
            ),
            PlaylistEntry(
                key = "most",
                title = "Most Played",
                count = state.mostPlayedCount,
                description = "Your top songs by play count",
                coverUrl = state.mostPlayedCover,
                glyph = "▲",
                pinned = true,
                onClick = onMostPlayedClick,
            ),
        )
    val own =
        app.map { playlist ->
            PlaylistEntry(
                key = playlist.id,
                title = playlist.name,
                count = playlist.trackCount,
                description = playlist.description?.takeIf { it.isNotBlank() },
                coverUrl = state.covers[playlist.id],
                glyph = "♫",
                onClick = { onPlaylistClick(playlist) },
                onLongClick = { onDeleteClick(playlist) },
            )
        }
    val files =
        local.map { playlist ->
            PlaylistEntry(
                key = playlist.id,
                title = playlist.name,
                count = playlist.trackCount,
                description = "M3U/PLS file · read-only",
                coverUrl = state.covers[playlist.id],
                glyph = "♫",
                local = true,
                onClick = { onPlaylistClick(playlist) },
            )
        }
    val total = (if (filter == PlaylistFilter.ALL) pinned.size + own.size else 0) + files.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(viewMode.columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP_DP.dp),
        verticalArrangement = Arrangement.spacedBy(if (viewMode.isGrid) GRID_GAP_DP.dp else 0.dp),
    ) {
        item(key = "toolbar", span = fullSpan) {
            ViewModeBar(total = total, selected = viewMode, onSelect = onViewModeChange)
        }
        if (filter == PlaylistFilter.ALL) {
            header("pinned_h", "PINNED")
            entries(pinned, viewMode)
            header(
                key = "own_h",
                title = "YOUR PLAYLISTS · ${own.size}",
                action = "[ + NEW ]",
                onAction = onCreateClick,
                secondaryAction = "[ IMPORT ]",
                onSecondaryAction = onImportClick,
            )
            if (own.isEmpty()) {
                item(key = "own_empty", span = fullSpan) {
                    Hint("No playlists yet. Tap [ + NEW ], or long-press any song to add it to one.")
                }
            }
            entries(own, viewMode)
            if (viewMode.isGrid && own.isNotEmpty()) {
                item(key = "new") { NewPlaylistTile(onClick = onCreateClick) }
            }
        }
        if (files.isNotEmpty() || filter == PlaylistFilter.LOCAL_M3U) {
            header("local_h", "LOCAL PLAYLISTS · ${files.size}")
            item(key = "local_hint", span = fullSpan) {
                Hint("Read from M3U/PLS files in your music folder. Rescan in Settings → Local Music.")
            }
            entries(files, viewMode)
        }
    }
}

private val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

private fun LazyGridScope.header(
    key: String,
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    item(key = key, span = fullSpan) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = CliBeatsAccent,
                modifier = Modifier.weight(1f),
            )
            if (secondaryAction != null) {
                Text(
                    text = secondaryAction,
                    style = MaterialTheme.typography.labelMedium,
                    color = CliBeatsAccent,
                    modifier =
                        Modifier
                            .clickable(role = Role.Button, onClick = onSecondaryAction)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (action != null) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = CliBeatsAccent,
                    modifier =
                        Modifier
                            .clickable(role = Role.Button, onClick = onAction)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun LazyGridScope.entries(
    list: List<PlaylistEntry>,
    mode: PlaylistViewMode,
) {
    items(list, key = { it.key }) { entry ->
        when (mode) {
            PlaylistViewMode.GRID_SMALL -> PlaylistTile(entry, large = false)
            PlaylistViewMode.GRID_LARGE -> PlaylistTile(entry, large = true)
            PlaylistViewMode.LIST -> PlaylistRow(entry, details = false)
            PlaylistViewMode.DETAILS -> PlaylistRow(entry, details = true)
        }
    }
}

/** "N playlists" and the four view buttons. */
@Composable
private fun ViewModeBar(
    total: Int,
    selected: PlaylistViewMode,
    onSelect: (PlaylistViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (total == 1) "1 playlist" else "$total playlists",
            style = MaterialTheme.typography.labelMedium,
            color = CliBeatsTextSecondary,
            modifier = Modifier.weight(1f),
        )
        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PlaylistViewMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier =
                        Modifier
                            .size(VIEW_BUTTON_DP.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, CliBeatsAccent, RoundedCornerShape(6.dp))
                                } else {
                                    Modifier
                                },
                            ).selectable(selected = isSelected, role = Role.RadioButton) { onSelect(mode) }
                            .semantics { contentDescription = "${mode.label} view" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        tint = if (isSelected) CliBeatsAccent else CliBeatsTextSecondary,
                        modifier = Modifier.size(VIEW_ICON_DP.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Cover(
    entry: PlaylistEntry,
    modifier: Modifier,
    glyphLarge: Boolean,
) {
    val borderColor = if (entry.pinned) CliBeatsAccent else CliBeatsBorderInactive
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CliBeatsSurfaceVariant)
                .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (entry.coverUrl.isNullOrBlank()) {
            Text(
                text = entry.glyph,
                style = with(MaterialTheme.typography) { if (glyphLarge) headlineMedium else titleMedium },
                color = borderColor,
            )
        } else {
            AsyncImage(
                // Same thumbnail URL as song rows: usually already cached, so covers fill instantly.
                model = artworkUrlFor(entry.coverUrl, ARTWORK_THUMB_PX * 2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun countLabel(entry: PlaylistEntry): String {
    val tracks = if (entry.count == 1) "1 track" else "${entry.count} tracks"
    return if (entry.local) "[LOCAL] $tracks" else tracks
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistTile(
    entry: PlaylistEntry,
    large: Boolean,
) {
    Column(
        modifier =
            Modifier.combinedClickable(
                onClick = entry.onClick,
                onLongClick = entry.onLongClick,
                onLongClickLabel = entry.onLongClick?.let { "Delete playlist" },
            ),
    ) {
        Cover(entry, Modifier.fillMaxWidth().aspectRatio(1f), glyphLarge = true)
        Text(
            text = entry.title,
            style = if (large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (entry.pinned) CliBeatsAccent else CliBeatsTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        CountText(entry)
    }
}

/** A list row: compact (cover, name, count) or details (bigger cover, description too). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistRow(
    entry: PlaylistEntry,
    details: Boolean,
) {
    val coverSize: Dp = if (details) DETAILS_COVER_DP.dp else LIST_COVER_DP.dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .combinedClickable(
                    onClick = entry.onClick,
                    onLongClick = entry.onLongClick,
                    onLongClickLabel = entry.onLongClick?.let { "Delete playlist" },
                ).padding(vertical = if (details) 8.dp else 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(entry, Modifier.size(coverSize), glyphLarge = details)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = if (details) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (entry.pinned) CliBeatsAccent else CliBeatsTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (details && entry.description != null) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CountText(entry)
        }
        Text(text = "›", style = MaterialTheme.typography.titleMedium, color = CliBeatsTextSecondary)
    }
}

@Composable
private fun CountText(entry: PlaylistEntry) {
    Text(
        text = countLabel(entry),
        style = MaterialTheme.typography.labelSmall,
        color = CliBeatsTextSecondary,
        maxLines = 1,
    )
}

@Composable
private fun NewPlaylistTile(onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClickLabel = "Create new playlist", onClick = onClick)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, CliBeatsAccent, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineLarge, color = CliBeatsAccent)
        }
        Text(
            text = "New playlist",
            style = MaterialTheme.typography.bodySmall,
            color = CliBeatsAccent,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = CliBeatsTextSecondary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

private const val GRID_GAP_DP = 10
private const val VIEW_BUTTON_DP = 36
private const val VIEW_ICON_DP = 20
private const val LIST_COVER_DP = 44
private const val DETAILS_COVER_DP = 72
