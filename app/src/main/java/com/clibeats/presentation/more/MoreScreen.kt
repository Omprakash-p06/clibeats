@file:Suppress(
    "ktlint:standard:function-naming",
    "ktlint:standard:multiline-expression-wrapping",
)

package com.clibeats.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.layout.NavDestination
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/**
 * Secondary navigation menu. Lists every destination that is not part of the
 * primary bottom tabs ([NavDestination.mainTabs]).
 */
@Suppress("FunctionNaming")
@Composable
fun MoreScreen(onNavigate: (NavDestination) -> Unit) {
    val secondaryDestinations =
        NavDestination.all.filterNot { destination -> destination in NavDestination.mainTabs }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CliBeatsBackground),
    ) {
        secondaryDestinations.forEach { destination ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onNavigate(destination) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CliBeatsTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = destination.contentDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
            }
            HorizontalDivider(color = CliBeatsDivider, thickness = 1.dp)
        }
    }
}
