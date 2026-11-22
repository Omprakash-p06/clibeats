@file:Suppress("ktlint:standard:function-naming", "MagicNumber")

package com.clibeats.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clibeats.domain.model.LyricLine
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

private val LYRIC_FONT_SIZE = 15.sp
private val TIME_FONT_SIZE = 12.sp
private val ROW_VERTICAL_PADDING = 8.dp

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun LyricsPanel(
    lyrics: List<LyricLine>,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "[ NO SYNCED LYRICS AVAILABLE ]",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = CliBeatsTextSecondary,
            )
        }
        return
    }

    val activeIndex =
        remember(lyrics, positionMs) {
            val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
            if (idx >= 0) idx else 0
        }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 48.dp, horizontal = 12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val rowModifier =
                if (isActive) {
                    Modifier
                        .fillMaxWidth()
                        .background(CliBeatsSurface)
                        .padding(horizontal = 8.dp, vertical = ROW_VERTICAL_PADDING)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSeekTo(line.timeMs) }
                        .padding(horizontal = 8.dp, vertical = ROW_VERTICAL_PADDING)
                }

            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val timeStr = formatTimestamp(line.timeMs)
                Text(
                    text = if (isActive) "> [$timeStr]" else "  [$timeStr]",
                    fontSize = TIME_FONT_SIZE,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) CliBeatsAccent else CliBeatsTextSecondary.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = line.text.ifBlank { "..." },
                    fontSize = LYRIC_FONT_SIZE,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) CliBeatsTextPrimary else CliBeatsTextSecondary,
                )
            }
        }
    }
}

private fun formatTimestamp(timeMs: Long): String {
    val totalSec = timeMs / 1000L
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
