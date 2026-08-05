package com.clibeats.presentation.layout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All navigable destinations in the CLIBeats app.
 * Ordered as they appear in the navigation rail/drawer.
 */
sealed class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    data object Home : NavDestination(
        route = "home",
        label = "Home",
        icon = Icons.Outlined.Home,
        contentDescription = "Home",
    )

    data object Search : NavDestination(
        route = "search",
        label = "Search",
        icon = Icons.Outlined.Search,
        contentDescription = "Search",
    )

    data object Library : NavDestination(
        route = "library",
        label = "Library",
        icon = Icons.Outlined.LibraryMusic,
        contentDescription = "Library",
    )

    data object Playlists : NavDestination(
        route = "playlists",
        label = "Playlists",
        icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
        contentDescription = "Playlists",
    )

    data object Queue : NavDestination(
        route = "queue",
        label = "Queue",
        icon = Icons.AutoMirrored.Outlined.QueueMusic,
        contentDescription = "Queue",
    )

    data object Settings : NavDestination(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Settings,
        contentDescription = "Settings",
    )

    companion object {
        val all: List<NavDestination> = listOf(Home, Search, Library, Playlists, Queue, Settings)
    }
}
