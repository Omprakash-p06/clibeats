package com.clibeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.presentation.home.HomeScreen
import com.clibeats.presentation.layout.MainLayout
import com.clibeats.presentation.layout.NavDestination
import com.clibeats.presentation.library.LibraryScreen
import com.clibeats.presentation.more.MoreScreen
import com.clibeats.presentation.player.PlayerViewModel
import com.clibeats.presentation.playlist.PlaylistScreen
import com.clibeats.presentation.queue.QueueScreen
import com.clibeats.presentation.search.SearchScreen
import com.clibeats.presentation.settings.SettingsScreen
import com.clibeats.presentation.theme.CliBeatsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CliBeatsTheme {
                var selectedDestination by remember {
                    mutableStateOf<NavDestination>(NavDestination.Home)
                }
                val playerViewModel: PlayerViewModel = hiltViewModel()

                MainLayout(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = { selectedDestination = it },
                    playerViewModel = playerViewModel,
                ) {
                    when (selectedDestination) {
                        NavDestination.Home ->
                            HomeScreen(
                                onNavigate = { selectedDestination = it },
                                playerViewModel = playerViewModel,
                            )
                        NavDestination.Search ->
                            SearchScreen(
                                onTrackClick = { playerViewModel.playTrack(it) },
                            )
                        NavDestination.Library -> LibraryScreen()
                        NavDestination.More ->
                            MoreScreen(
                                onNavigate = { selectedDestination = it },
                            )
                        NavDestination.Queue -> QueueScreen()
                        NavDestination.Playlists -> PlaylistScreen()
                        NavDestination.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}
