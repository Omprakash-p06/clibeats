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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
import com.clibeats.presentation.navigation.MainTab
import com.clibeats.presentation.player.HintTarget
import com.clibeats.presentation.player.NowPlayingScreen
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.theme.CliBeatsDestructive
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.update.UpdateStatusBar

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
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun MainLayout(
    activeTab: MainTab = MainTab.HOME,
    breadcrumb: String = activeTab.route.breadcrumb,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onTabSelected: (MainTab) -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
    content: @Composable () -> Unit = {},
) {
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }

    Box(modifier = Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                buildNavItems(
                    activeTab = activeTab,
                    onTabSelected = onTabSelected,
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
                    breadcrumb = breadcrumb,
                    canGoBack = canGoBack,
                    onBack = onBack,
                    onQueueClick = onQueueClick,
                    onSettingsClick = onSettingsClick,
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = CliBeatsDivider,
                )

                // Update download progress (only while an update is downloading or ready to install).
                UpdateStatusBar()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    content()
                }

                // ── Persistent Player Bar (own scope: 500ms ticks don't recompose the screen)
                PlayerBarHost(
                    playerViewModel,
                    onExpandClick = { showNowPlaying = true },
                )
            }
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
                    showNowPlaying = false
                    onQueueClick()
                },
            )
        }
    }
}

private const val NOW_PLAYING_ANIM_MS = 250

@Suppress("FunctionNaming")
@Composable
private fun PlayerBarHost(
    playerViewModel: PlayerViewModel,
    onExpandClick: () -> Unit,
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
        context.imageLoader.enqueue(ImageRequest.Builder(context).data(hiRes).build())
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

private fun NavigationSuiteScope.buildNavItems(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    MainTab.entries.forEach { tab ->
        item(
            selected = tab == activeTab,
            onClick = { onTabSelected(tab) },
            icon = {
                // Label below already names the tab; a description here would be read twice.
                Icon(imageVector = tab.icon, contentDescription = null)
            },
            // Four tabs: single-line labels so "PLAYLISTS" fits at 360dp.
            label = {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                )
            },
        )
    }
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CliBeatsTopAppBar(
    breadcrumb: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        modifier = Modifier.height(48.dp),
        title = {
            Text(
                text = if (canGoBack) breadcrumb else "> clibeats",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (canGoBack) {
                Text(
                    text = "< cd ..",
                    style = MaterialTheme.typography.labelMedium,
                    color = CliBeatsTextSecondary,
                    modifier =
                        Modifier
                            .clickable(role = Role.Button, onClick = onBack)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Open settings",
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
