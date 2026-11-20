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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.home.HomeScreen
import com.clibeats.presentation.layout.MainLayout
import com.clibeats.presentation.layout.NavDestination
import com.clibeats.presentation.library.LibraryScreen
import com.clibeats.presentation.onboarding.OnboardingScreen
import com.clibeats.presentation.search.SearchScreen
import com.clibeats.presentation.settings.SettingsScreen
import com.clibeats.presentation.telemetry.TelemetryPrompt
import com.clibeats.presentation.theme.CliBeatsTheme
import com.clibeats.presentation.theme.CliBeatsThemeMode
import com.clibeats.presentation.theme.ThemeViewModel
import com.clibeats.presentation.update.UpdatePrompt
import com.clibeats.presentation.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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

            CliBeatsTheme(accent = themeState.accent, mode = themeState.mode) {
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
    val tabs = NavDestination.mainTabs
    // Saveable page: tab survives rotation/process death.
    var savedPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = savedPage) { tabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) { savedPage = pagerState.currentPage }

    // Bumped on every "open Playlists" request, so a repeat request still switches the sub-tab.
    var openPlaylistsRequest by rememberSaveable { mutableIntStateOf(0) }

    val navigate: (NavDestination) -> Unit = { destination ->
        if (destination == NavDestination.Playlists) {
            openPlaylistsRequest++
        }
        val resolved =
            when (destination) {
                NavDestination.Playlists -> NavDestination.Library
                else -> destination
            }
        val target = tabs.indexOf(resolved)
        if (target >= 0) {
            scope.launch {
                // Neighbours slide; far jumps snap so we don't compose every page in between.
                if (abs(target - pagerState.currentPage) == 1) {
                    pagerState.animateScrollToPage(target)
                } else {
                    pagerState.scrollToPage(target)
                }
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0) { navigate(NavDestination.Home) }

    // Re-tapping the tab you're on: Home / Search = switch Online <-> Local (double-tap Search = search box),
    // Library = switch Online <-> Offline.
    var focusSearch by remember { mutableStateOf(false) }
    var toggleSearchSource by remember { mutableStateOf(false) }
    // Search tab: one tap (when already on Search) switches the source after a short wait, which a second
    // tap cancels to open the search box instead, so a double-tap never flips the source first.
    var lastSearchTapAt by remember { mutableLongStateOf(0L) }
    var pendingSearchToggle by remember { mutableStateOf<Job?>(null) }
    var toggleHomeSource by remember { mutableStateOf(false) }
    var toggleLibraryMode by remember { mutableStateOf(false) }
    val onTabClick: (NavDestination) -> Unit = { destination ->
        if (destination == tabs.getOrNull(pagerState.currentPage)) {
            when (destination) {
                NavDestination.Home -> toggleHomeSource = true
                NavDestination.Search -> Unit // handled below (single vs double tap)
                NavDestination.Library -> toggleLibraryMode = true
                else -> Unit
            }
        }
        if (destination == NavDestination.Search) {
            val now = SystemClock.uptimeMillis()
            val doubleTap = now - lastSearchTapAt < DOUBLE_TAP_MS
            lastSearchTapAt = now
            pendingSearchToggle?.cancel()
            pendingSearchToggle = null
            when {
                doubleTap -> focusSearch = true
                tabs[pagerState.currentPage] == NavDestination.Search ->
                    pendingSearchToggle =
                        scope.launch {
                            delay(DOUBLE_TAP_MS)
                            toggleSearchSource = true
                        }
            }
        }
        navigate(destination)
    }

    // In-app updater: check GitHub on launch (at most once per chosen interval) and show the pop-up.
    val updateViewModel: UpdateViewModel = hiltViewModel()
    LaunchedEffect(Unit) { updateViewModel.onAppStart() }
    UpdatePrompt(viewModel = updateViewModel)
    TelemetryPrompt()

    MainLayout(
        selectedDestination = tabs[pagerState.currentPage],
        onDestinationSelected = onTabClick,
    ) {
        // Swipe left/right between tabs in bottom-bar order.
        HorizontalPager(
            state = pagerState,
            key = { tabs[it].route },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (tabs[page]) {
                NavDestination.Home ->
                    HomeScreen(
                        toggleSourceRequested = toggleHomeSource,
                        onToggleSourceHandled = { toggleHomeSource = false },
                    )
                NavDestination.Search ->
                    SearchScreen(
                        focusRequested = focusSearch,
                        onFocusHandled = { focusSearch = false },
                        toggleSourceRequested = toggleSearchSource,
                        onToggleSourceHandled = { toggleSearchSource = false },
                    )
                NavDestination.Library ->
                    LibraryScreen(
                        toggleModeRequested = toggleLibraryMode,
                        onToggleModeHandled = { toggleLibraryMode = false },
                        openPlaylistsRequest = openPlaylistsRequest,
                    )
                NavDestination.Settings -> SettingsScreen()
                else -> Unit
            }
        }
    }
}

private const val DOUBLE_TAP_MS = 300L
