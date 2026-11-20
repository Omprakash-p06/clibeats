@file:Suppress(
    "ktlint:standard:multiline-expression-wrapping",
    "ktlint:standard:function-naming",
)

package com.clibeats.presentation.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.imageLoader
import coil.request.ImageRequest
import com.clibeats.presentation.component.ARTWORK_FULL_PX
import com.clibeats.presentation.component.PlayerBar
import com.clibeats.presentation.component.TrackArtwork
import com.clibeats.presentation.component.artworkUrlFor
import com.clibeats.presentation.player.HintTarget
import com.clibeats.presentation.player.NowPlayingScreen
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.queue.QueueScreen
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsBorder
import com.clibeats.presentation.theme.CliBeatsDestructive
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor
import com.clibeats.presentation.update.UpdateStatusBar
import com.clibeats.presentation.update.UpdateViewModel

/**
 * Root layout shell for CLIBeats.
 *
 * Contains:
 * - TopAppBar (48dp, flat, no elevation)
 * - Content area slot for screen content
 * - Persistent PlayerBar
 * - Custom TUI bottom navigation bar with sharp 1.dp borders and terminal styling
 */
@Suppress("FunctionNaming", "LongMethod")
@Composable
fun MainLayout(
    selectedDestination: NavDestination = NavDestination.Home,
    onDestinationSelected: (NavDestination) -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    content: @Composable () -> Unit = {},
) {
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showQueue) { showQueue = false }
    BackHandler(enabled = showNowPlaying && !showQueue) { showNowPlaying = false }

    val handleDestinationSelected: (NavDestination) -> Unit = { destination ->
        if (destination == NavDestination.Queue) {
            showQueue = true
        } else {
            onDestinationSelected(destination)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            CliBeatsTopAppBar(
                onSearchClick = { handleDestinationSelected(NavDestination.Search) },
                onQueueClick = { showQueue = true },
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = CliBeatsDivider,
            )

            UpdateStatusBar(viewModel = updateViewModel)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                content()
            }

            // ── Persistent Player Bar (own scope: 500ms ticks don't recompose the screen)
            PlayerBarHost(
                playerViewModel = playerViewModel,
                onExpandClick = { showNowPlaying = true },
                onQueueClick = { showQueue = true },
            )

            // ── Custom TUI Bottom Navigation Bar
            TuiBottomBar(
                destinations = NavDestination.mainTabs,
                selectedDestination = selectedDestination,
                onSelect = handleDestinationSelected,
            )
        }

        // ── Full-screen Now Playing overlay (slides up over everything) ──
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(tween(NOW_PLAYING_ANIM_MS)) { it },
            exit = slideOutVertically(tween(NOW_PLAYING_ANIM_MS)) { it },
        ) {
            NowPlayingScreen(
                viewModel = playerViewModel,
                onClose = { showNowPlaying = false },
                onOpenQueue = {
                    showQueue = true
                },
            )
        }

        // ── Full-screen Queue overlay (slides up) ──
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(tween(NOW_PLAYING_ANIM_MS)) { it },
            exit = slideOutVertically(tween(NOW_PLAYING_ANIM_MS)) { it },
        ) {
            QueueScreen(
                onClose = { showQueue = false },
            )
        }
    }
}

/**
 * Drop-in TUI bottom bar composable matching TuiBlock frames and sharp 1.dp rectangular aesthetic.
 */
@Suppress("FunctionNaming")
@Composable
fun TuiBottomBar(
    destinations: List<NavDestination>,
    selectedDestination: NavDestination,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColor.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CliBeatsBackground)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = CliBeatsBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                val isSelected = destination == selectedDestination
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(destination) }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (isSelected) accent else CliBeatsTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val labelText =
                            if (isSelected) {
                                "[${destination.label.uppercase()}]"
                            } else {
                                destination.label.uppercase()
                            }
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) accent else CliBeatsTextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                if (index < destinations.lastIndex) {
                    VerticalDivider(
                        thickness = 1.dp,
                        color = CliBeatsBorder,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private const val NOW_PLAYING_ANIM_MS = 250

@Suppress("FunctionNaming")
@Composable
private fun PlayerBarHost(
    playerViewModel: PlayerViewModel,
    onExpandClick: () -> Unit,
    onQueueClick: () -> Unit = {},
) {
    val playbackState by playerViewModel.playbackState.collectAsState()
    val hint by playerViewModel.hint.collectAsState()
    val playerMessage by playerViewModel.playerMessage.collectAsState()
    PlayerMessageLine(playerMessage)
    // Prefetch the high-res cover as soon as a track starts, so Now Playing opens with it ready.
    val context = LocalContext.current
    val artworkUrl = playbackState.currentTrack?.artworkUrl
    LaunchedEffect(artworkUrl) {
        val hiRes = artworkUrlFor(artworkUrl, ARTWORK_FULL_PX) ?: return@LaunchedEffect
        context.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(hiRes)
                .size(ARTWORK_FULL_PX)
                .build(),
        )
    }
    // Player-reported duration first: local files may have no duration in metadata.
    val trackDuration = playbackState.durationMs.takeIf { it > 0 } ?: playbackState.currentTrack?.durationMs ?: 0L
    val progress =
        if (trackDuration > 0L) {
            (playbackState.positionMs.toFloat() / trackDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    PlayerBar(
        trackTitle = playbackState.currentTrack?.title ?: "Not playing",
        artist = playbackState.currentTrack?.artist ?: "",
        isPlaying = playbackState.isPlaying,
        progress = progress,
        currentMs = playbackState.positionMs,
        totalMs = trackDuration,
        artworkContent = playbackState.currentTrack?.artworkUrl?.let { url ->
            {
                TrackArtwork(artworkUrl = url, size = 36)
            }
        },
        onPlayPauseClick = playerViewModel::onPlayPauseClick,
        onSkipNextClick = playerViewModel::onSkipNextClick,
        onSkipPreviousClick = playerViewModel::onSkipPreviousClick,
        onSeek = playerViewModel::seekTo,
        repeatMode = playbackState.repeatMode,
        onRepeatClick = playerViewModel::onRepeatClick,
        repeatHint = hint?.takeIf { it.target == HintTarget.REPEAT }?.text,
        onExpandClick = onExpandClick,
        onQueueClick = onQueueClick,
    )
}

/** One-line notice above the player bar, e.g. a song that couldn't be played and was skipped. */
@Suppress("FunctionNaming")
@Composable
private fun PlayerMessageLine(message: String?) {
    // Keep the last text while fading out so the line doesn't empty mid-animation.
    val last = remember { arrayOf("") }
    message?.let { last[0] = it }
    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = "! " + last[0],
            style = MaterialTheme.typography.labelMedium,
            color = CliBeatsDestructive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .background(CliBeatsSurface)
                    .border(1.dp, CliBeatsDestructive)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CliBeatsTopAppBar(
    onSearchClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
) {
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
            IconButton(onClick = onQueueClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                    contentDescription = "Open queue",
                    tint = CliBeatsTextSecondary,
                )
            }
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
