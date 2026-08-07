@file:Suppress(
    "ktlint:standard:multiline-expression-wrapping",
    "ktlint:standard:function-naming",
)

package com.clibeats.presentation.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.component.PlayerBar
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextSecondary

/**
 * Root layout shell for CLIBeats.
 *
 * Contains:
 * - Adaptive navigation (Rail on compact/medium, Drawer on expanded)
 * - TopAppBar (48dp, flat, no elevation)
 * - Content area slot for screen content
 * - Bottom slot for persistent PlayerBar
 *
 * Per UI-SPEC: Active nav item = CliBeatsAccent. No pill/bubble indicator.
 */
@Suppress("FunctionNaming", "LongMethod")
@Composable
fun MainLayout(
    selectedDestination: NavDestination = NavDestination.Home,
    onDestinationSelected: (NavDestination) -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
    content: @Composable () -> Unit = {},
) {
    val playbackState by playerViewModel.playbackState.collectAsState()
    val trackDuration = playbackState.currentTrack?.durationMs ?: 0L
    val progress =
        if (trackDuration > 0L) {
            (playbackState.positionMs.toFloat() / trackDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            buildNavItems(
                selectedDestination = selectedDestination,
                onDestinationSelected = onDestinationSelected,
            )
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = CliBeatsSurface,
            navigationDrawerContainerColor = CliBeatsSurface,
            navigationBarContainerColor = CliBeatsSurface,
        ),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CliBeatsTopAppBar(
                onSearchClick = { onDestinationSelected(NavDestination.Search) },
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = CliBeatsDivider,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                content()
            }

            // ── Persistent Player Bar ─────────────────────────────────────
            PlayerBar(
                trackTitle = playbackState.currentTrack?.title ?: "Nothing Playing",
                artist = buildString {
                    val track = playbackState.currentTrack
                    if (track != null) {
                        append(track.artist)
                        if (track.album.isNotBlank()) append(" • ${track.album}")
                    } else {
                        append("YouTube Music Provider")
                    }
                },
                isPlaying = playbackState.isPlaying,
                progress = progress,
                artworkContent = playbackState.currentTrack?.artworkUrl?.let { url ->
                    {
                        coil.compose.AsyncImage(
                            model = url,
                            contentDescription = "Artwork for ${playbackState.currentTrack?.title}",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
                onPlayPauseClick = playerViewModel::onPlayPauseClick,
                onSkipNextClick = playerViewModel::onSkipNextClick,
                onSkipPreviousClick = playerViewModel::onSkipPreviousClick,
                onQueueClick = { onDestinationSelected(NavDestination.Queue) },
            )
        }
    }
}

private fun NavigationSuiteScope.buildNavItems(
    selectedDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
) {
    NavDestination.mainTabs.forEach { destination ->
        item(
            selected = destination == selectedDestination,
            onClick = { onDestinationSelected(destination) },
            icon = {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.contentDescription,
                )
            },
            label = { Text(destination.label, maxLines = 1) },
        )
    }
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CliBeatsTopAppBar(onSearchClick: () -> Unit = {}) {
    TopAppBar(
        modifier = Modifier.height(48.dp),
        title = {
            Text(
                text = "CLIBeats",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Open navigation",
                    tint = CliBeatsTextSecondary,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Open search",
                    tint = CliBeatsTextSecondary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CliBeatsSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = null,
    )
}
