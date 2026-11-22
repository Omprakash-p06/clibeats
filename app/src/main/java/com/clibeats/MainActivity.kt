@file:Suppress("ktlint:standard:function-naming")

package com.clibeats

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.home.HomeScreen
import com.clibeats.presentation.layout.MainLayout
import com.clibeats.presentation.library.LibraryScreen
import com.clibeats.presentation.navigation.AppRoute
import com.clibeats.presentation.navigation.AppRouter
import com.clibeats.presentation.navigation.MainTab
import com.clibeats.presentation.onboarding.OnboardingScreen
import com.clibeats.presentation.playlist.PlaylistScreen
import com.clibeats.presentation.queue.QueueScreen
import com.clibeats.presentation.search.SearchScreen
import com.clibeats.presentation.settings.SettingsScreen
import com.clibeats.presentation.theme.CliBeatsTheme
import com.clibeats.presentation.theme.CliBeatsThemeMode
import com.clibeats.presentation.theme.ThemeViewModel
import com.clibeats.presentation.update.UpdatePrompt
import com.clibeats.presentation.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeState by themeViewModel.themeState.collectAsState()
            val onboardingDone by themeViewModel.onboardingDone.collectAsState()
            val isLight = themeState.mode == CliBeatsThemeMode.LIGHT

            // Status/nav bar icons must contrast with the app theme, not the system theme.
            DisposableEffect(isLight) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style =
                    if (isLight) {
                        SystemBarStyle.light(transparent, transparent)
                    } else {
                        SystemBarStyle.dark(transparent)
                    }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            CliBeatsTheme(accent = themeState.accent.color, mode = themeState.mode) {
                when (onboardingDone) {
                    null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    false -> OnboardingScreen(currentTheme = themeState.mode)
                    true -> AppContent()
                }
            }
        }
    }
}

@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
@Composable
private fun AppContent() {
    val router = remember { AppRouter() }
    val stateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = router.canGoBack) { router.popBack() }

    // Re-tapping the tab you're on: Home = switch Online <-> Local, Search = switch source (double-tap
    // opens the search box), Library = switch Online <-> Offline, Playlists = back to the grid.
    var focusSearch by remember { mutableStateOf(false) }
    var toggleSearchSource by remember { mutableStateOf(false) }
    // Search tab: one tap (when already on Search) switches the source after a short wait, which a second
    // tap cancels to open the search box instead, so a double-tap never flips the source first.
    var lastSearchTapAt by remember { mutableLongStateOf(0L) }
    var pendingSearchToggle by remember { mutableStateOf<Job?>(null) }
    var toggleHomeSource by remember { mutableStateOf(false) }
    var toggleLibraryMode by remember { mutableStateOf(false) }
    var showPlaylistGrid by remember { mutableStateOf(false) }
    val onTabClick: (MainTab) -> Unit = { tab ->
        val reTappedAtRoot = router.selectTab(tab)
        if (reTappedAtRoot) {
            when (tab) {
                MainTab.HOME -> toggleHomeSource = true
                MainTab.LIBRARY -> toggleLibraryMode = true
                MainTab.PLAYLISTS -> showPlaylistGrid = true
                MainTab.SEARCH -> Unit // handled below (single vs double tap)
            }
        }
        if (tab == MainTab.SEARCH) {
            val now = SystemClock.uptimeMillis()
            val doubleTap = now - lastSearchTapAt < DOUBLE_TAP_MS
            lastSearchTapAt = now
            pendingSearchToggle?.cancel()
            pendingSearchToggle = null
            when {
                doubleTap -> focusSearch = true
                router.activeTab == MainTab.SEARCH ->
                    pendingSearchToggle =
                        scope.launch {
                            delay(DOUBLE_TAP_MS)
                            toggleSearchSource = true
                        }
            }
        }
    }

    // In-app updater: check GitHub on launch (at most once per chosen interval) and show the pop-up.
    val updateViewModel: UpdateViewModel = hiltViewModel()
    LaunchedEffect(Unit) { updateViewModel.onAppStart() }
    UpdatePrompt(viewModel = updateViewModel)

    MainLayout(
        activeTab = router.activeTab,
        breadcrumb = router.currentRoute.breadcrumb,
        canGoBack = router.canGoBack,
        onBack = { router.popBack() },
        onTabSelected = onTabClick,
        onQueueClick = { router.navigate(AppRoute.Queue) },
        onSettingsClick = { router.navigate(AppRoute.Settings) },
    ) {
        // One route composed at a time; SaveableStateHolder keeps each tab's scroll/input state.
        stateHolder.SaveableStateProvider(router.currentRoute.key) {
            when (router.currentRoute) {
                AppRoute.Home ->
                    HomeScreen(
                        toggleSourceRequested = toggleHomeSource,
                        onToggleSourceHandled = { toggleHomeSource = false },
                    )
                AppRoute.Search ->
                    SearchScreen(
                        focusRequested = focusSearch,
                        onFocusHandled = { focusSearch = false },
                        toggleSourceRequested = toggleSearchSource,
                        onToggleSourceHandled = { toggleSearchSource = false },
                    )
                AppRoute.Library ->
                    LibraryScreen(
                        toggleModeRequested = toggleLibraryMode,
                        onToggleModeHandled = { toggleLibraryMode = false },
                    )
                AppRoute.Playlists ->
                    PlaylistScreen(
                        showGridRequested = showPlaylistGrid,
                        onShowGridHandled = { showPlaylistGrid = false },
                    )
                AppRoute.Queue -> QueueScreen()
                AppRoute.Settings -> SettingsScreen()
                // Playlist detail is selected inside PlaylistScreen for now; artist/album screens
                // arrive with the retention work.
                is AppRoute.PlaylistDetail, is AppRoute.ArtistDetail, is AppRoute.AlbumDetail -> Unit
            }
        }
    }
}

private const val DOUBLE_TAP_MS = 300L
