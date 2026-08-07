@file:Suppress(
    "ktlint:standard:function-naming",
    "FunctionNaming",
    "LongMethod",
)

package com.clibeats.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.layout.NavDestination
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextSecondary

@Composable
fun MoreScreen(onNavigate: (NavDestination) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TuiBlock(title = "Navigation", isActive = true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoreNavigationCard(
                    title = "Playback Queue",
                    description = "View, reorder, or clear active queue items",
                    icon = Icons.AutoMirrored.Outlined.QueueMusic,
                    onClick = { onNavigate(NavDestination.Queue) },
                )

                MoreNavigationCard(
                    title = "Playlists",
                    description = "Manage and create custom audio playlists",
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    onClick = { onNavigate(NavDestination.Playlists) },
                )

                MoreNavigationCard(
                    title = "Settings",
                    description = "Configure audio provider, cache limits & options",
                    icon = Icons.Outlined.Settings,
                    onClick = { onNavigate(NavDestination.Settings) },
                )
            }
        }

        // Developer Hub Card
        TuiBlock(title = "Developer Hub") {
            Column {
                Text(
                    text =
                        "Developer    : Omprakash Panda\n" +
                            "Version      : v0.2.0-beta\n" +
                            "Architecture : MVVM + Clean Architecture\n" +
                            "Provider     : YouTube Music (InnerTube API)\n" +
                            "Build        : Production Release\n" +
                            "Engine       : Media3 ExoPlayer\n" +
                            "Cache        : 500 MB LRU Storage\n" +
                            "Status       : Operational",
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MoreNavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(CliBeatsSurface)
                .border(1.dp, CliBeatsDivider)
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CliBeatsAccent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CliBeatsTextSecondary,
                )
            }
        }
        Text(
            text = ">",
            style = MaterialTheme.typography.titleMedium,
            color = CliBeatsAccent,
        )
    }
}
