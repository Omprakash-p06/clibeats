@file:Suppress(
    "ktlint:standard:function-naming",
    "FunctionNaming",
    "LongMethod",
)

package com.clibeats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/**
 * Terminal-native text tab bar component.
 * Replaces Material3 TabRow to match spotify-tui / spicetify-tui (`[ Tracks ]   Artists   Albums`).
 */
@Composable
fun TuiTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CliBeatsBackground)
                .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val labelText = if (isSelected) "[ $title ]" else title
            Text(
                text = labelText,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) CliBeatsAccent else CliBeatsTextSecondary,
                modifier =
                    Modifier
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}
