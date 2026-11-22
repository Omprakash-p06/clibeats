@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextDisabled
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor

private val ROW_MIN_HEIGHT = 64.dp
private val ROW_VERTICAL_PADDING = 8.dp
private val ROW_HORIZONTAL_PADDING = 12.dp
private val ARTWORK_LIST_SIZE = 52.dp
private val ACTION_TOUCH_TARGET = 44.dp
private val TITLE_FONT_SIZE = 15.sp
private val SUBTITLE_FONT_SIZE = 12.5.sp

/**
 * Ergonomic 64dp song table row — the primary list item component.
 *
 * States:
 * - Normal: CliBeatsBackground background
 * - Now Playing: CliBeatsSurface + 2dp accent left border
 */
@Suppress("FunctionNaming", "LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongTableRow(
    trackTitle: String,
    artist: String,
    duration: String,
    index: Int? = null,
    isNowPlaying: Boolean = false,
    isDownloaded: Boolean = false,
    artworkContent: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSwipeRight: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val semanticsDescription = "$trackTitle by $artist"
    val accent = LocalAccentColor.current
    val rowBackground = if (isNowPlaying) CliBeatsSurface else CliBeatsBackground

    // Swipe right = add to queue, swipe left = add to favorites
    var dragX by remember { mutableFloatStateOf(0f) }
    val swipeModifier =
        if (onSwipeRight == null && onSwipeLeft == null) {
            Modifier
        } else {
            Modifier.pointerInput(onSwipeRight, onSwipeLeft) {
                val threshold = SWIPE_THRESHOLD_DP.dp.toPx()
                val maxRight = if (onSwipeRight != null) threshold * SWIPE_MAX_FACTOR else 0f
                val maxLeft = if (onSwipeLeft != null) -threshold * SWIPE_MAX_FACTOR else 0f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start =
                        awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            val handled = if (over > 0f) onSwipeRight != null else onSwipeLeft != null
                            if (handled) change.consume()
                        } ?: return@awaitEachGesture
                    dragX = 0f
                    val completed =
                        horizontalDrag(start.id) { change ->
                            dragX = (dragX + change.positionChange().x).coerceIn(maxLeft, maxRight)
                            change.consume()
                        }
                    if (completed && dragX > threshold) onSwipeRight?.invoke()
                    if (completed && dragX < -threshold) onSwipeLeft?.invoke()
                    dragX = 0f
                }
            }
        }

    Box(modifier = modifier.fillMaxWidth().then(swipeModifier)) {
        if (dragX != 0f) SwipeHint(dragX)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = dragX },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ROW_MIN_HEIGHT)
                    .background(rowBackground)
                    .semantics { contentDescription = semanticsDescription }
                    .combinedClickable(
                        onClickLabel = "Play",
                        onLongClickLabel = if (onLongPress != null) "More actions" else null,
                        onLongClick = onLongPress ?: onActionClick,
                        onClick = onClick,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ── Now-playing left accent bar (2dp) ─────────────────────────
                if (isNowPlaying) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(ROW_MIN_HEIGHT)
                            .background(accent),
                    )
                } else {
                    Spacer(modifier = Modifier.width(2.dp))
                }

                Spacer(modifier = Modifier.width(ROW_HORIZONTAL_PADDING - 2.dp))

                // ── Optional leading index ────────────────────────────────────
                if (index != null) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CliBeatsTextDisabled,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // ── Artwork (52dp) ───────────────────────────────────────────
                if (artworkContent != null) {
                    Box(
                        modifier = Modifier.size(ARTWORK_LIST_SIZE),
                        contentAlignment = Alignment.Center,
                    ) {
                        artworkContent()
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // ── Track title + artist ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = ROW_VERTICAL_PADDING),
                ) {
                    val displayTitle = if (isNowPlaying) "▸ $trackTitle" else trackTitle
                    Text(
                        text = displayTitle,
                        fontSize = TITLE_FONT_SIZE,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isNowPlaying) accent else CliBeatsTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isDownloaded) {
                            Text(
                                text = "[ ↓ ] ",
                                fontSize = SUBTITLE_FONT_SIZE,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                            )
                        }
                        Text(
                            text = artist,
                            fontSize = SUBTITLE_FONT_SIZE,
                            fontWeight = FontWeight.Normal,
                            color = CliBeatsTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── Duration (labelSmall) ────────────────────────────────────
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = CliBeatsTextSecondary,
                )

                // ── Trailing Actions / single [ ⋮ ] icon button ──────────────
                if (actions != null) {
                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        actions()
                    }
                } else if (onActionClick != null || onLongPress != null) {
                    IconButton(
                        onClick = { (onActionClick ?: onLongPress)?.invoke() },
                        modifier = Modifier.size(ACTION_TOUCH_TARGET),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More actions",
                            tint = CliBeatsTextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(ROW_HORIZONTAL_PADDING))
                }
            }

            // ── 1dp row divider ───────────────────────────────────────────────
            HorizontalDivider(
                thickness = 1.dp,
                color = CliBeatsDivider,
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
fun SongTableHeader(
    modifier: Modifier = Modifier,
    showIndex: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIndex) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelSmall,
                    color = CliBeatsTextSecondary,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
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
                modifier = Modifier.padding(end = ROW_HORIZONTAL_PADDING),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = CliBeatsDivider)
    }
}

private const val SWIPE_THRESHOLD_DP = 72
private const val SWIPE_MAX_FACTOR = 1.5f

/** Revealed behind the row while dragging: "+ QUEUE" (dragging right) or "♥ FAVORITE" (dragging left). */
@Suppress("FunctionNaming")
@Composable
private fun BoxScope.SwipeHint(dragX: Float) {
    val right = dragX > 0f
    Text(
        text = if (right) "+ QUEUE" else "♥ FAVORITE",
        style = MaterialTheme.typography.labelMedium,
        color = CliBeatsAccent,
        modifier =
            Modifier
                .align(if (right) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 16.dp),
    )
}
