@file:Suppress(
    "ktlint:standard:function-naming",
    "FunctionNaming",
    "LongMethod",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsBorderActive
import com.clibeats.presentation.theme.CliBeatsBorderInactive
import com.clibeats.presentation.theme.CliBeatsSurface

/**
 * Authentic TUI block container with an embedded header title line.
 * Modeled after spotify-tui and spicetify-tui (`┌─ Title ───────────────┐`).
 *
 * @param title Header title text embedded in the top border
 * @param isActive When true, border and title highlight in terminal green (#1DB954)
 * @param modifier Custom modifier for the box
 * @param content Inner content composable
 */
@Composable
fun TuiBlock(
    title: String,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val borderColor: Color = if (isActive) CliBeatsBorderActive else CliBeatsBorderInactive
    val titleColor: Color = if (isActive) CliBeatsAccent else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .border(1.dp, borderColor),
    ) {
        // ── Top Border Header Line with Embedded Title ──────────────────────
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(CliBeatsBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "─ ",
                style = MaterialTheme.typography.labelSmall,
                color = borderColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = titleColor,
            )
            Text(
                text = " ",
                style = MaterialTheme.typography.labelSmall,
                color = borderColor,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = borderColor,
            )
        }

        // ── Inner Block Content ──────────────────────────────────────────────
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
        ) {
            content()
        }
    }
}
