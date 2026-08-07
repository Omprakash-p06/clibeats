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
import androidx.compose.foundation.layout.height
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "SYSTEM NAVIGATION & MORE",
            style = MaterialTheme.typography.titleMedium,
            color = CliBeatsAccent,
        )

        Spacer(modifier = Modifier.height(16.dp))

        MoreNavigationCard(
            title = "Playback Queue",
            description = "View, reorder, or clear active queue items",
            icon = Icons.AutoMirrored.Outlined.QueueMusic,
            onClick = { onNavigate(NavDestination.Queue) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Playlists",
            description = "Manage and create custom audio playlists",
            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
            onClick = { onNavigate(NavDestination.Playlists) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        MoreNavigationCard(
            title = "Settings",
            description = "Configure audio provider, cache limits & developer options",
            icon = Icons.Outlined.Settings,
            onClick = { onNavigate(NavDestination.Settings) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // System Developer Status Card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(CliBeatsSurface)
                    .border(1.dp, CliBeatsDivider)
                    .padding(12.dp),
        ) {
            Text(
                text = "> system --status",
                style = MaterialTheme.typography.labelSmall,
                color = CliBeatsAccent,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text =
                    "ACTIVE PROVIDER : YouTube Music (InnerTube API v1)\n" +
                        "CACHE STORAGE   : 500 MB (LRU Active)\n" +
                        "SYSTEM STATUS   : CONNECTED & OPERATIONAL",
                style = MaterialTheme.typography.bodySmall,
                color = CliBeatsTextSecondary,
            )
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
