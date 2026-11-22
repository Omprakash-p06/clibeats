package com.clibeats.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** The four music-first tabs in the bottom bar. Queue and Settings are utility routes. */
enum class MainTab(
    val route: AppRoute.Tab,
    val label: String,
    val icon: ImageVector,
) {
    HOME(AppRoute.Home, "HOME", Icons.Outlined.Home),
    SEARCH(AppRoute.Search, "SEARCH", Icons.Outlined.Search),
    LIBRARY(AppRoute.Library, "LIBRARY", Icons.Outlined.LibraryMusic),
    PLAYLISTS(AppRoute.Playlists, "PLAYLISTS", Icons.AutoMirrored.Outlined.PlaylistPlay),
}

/**
 * Every screen the app can show, with the TUI breadcrumb printed in the top bar.
 *
 * A [Tab] route is a bottom-bar root; everything else is pushed onto a tab's stack or the
 * global utility stack (Queue, Settings).
 */
sealed interface AppRoute {
    val breadcrumb: String

    /** Stable key for [androidx.compose.runtime.saveable.SaveableStateHolder]. */
    val key: String
        get() = breadcrumb

    sealed interface Tab : AppRoute

    data object Home : Tab {
        override val breadcrumb: String = "~/home"
    }

    data object Search : Tab {
        override val breadcrumb: String = "~/search"
    }

    data object Library : Tab {
        override val breadcrumb: String = "~/library"
    }

    data object Playlists : Tab {
        override val breadcrumb: String = "~/playlists"
    }

    data class PlaylistDetail(
        val playlistId: String,
        val title: String,
    ) : AppRoute {
        override val breadcrumb: String = "~/playlists/${title.slug()}"
    }

    data class ArtistDetail(
        val artistName: String,
        val browseId: String?,
        val providerId: String,
    ) : AppRoute {
        override val breadcrumb: String = "~/artist/${artistName.slug()}"
    }

    data class AlbumDetail(
        val albumName: String,
        val artistName: String,
        val browseId: String?,
        val providerId: String,
    ) : AppRoute {
        override val breadcrumb: String = "~/album/${albumName.slug()}"
    }

    data object Queue : AppRoute {
        override val breadcrumb: String = "~/queue"
    }

    data object Settings : AppRoute {
        override val breadcrumb: String = "~/settings"
    }
}

/** `Gym Mix` -> `gym_mix`, `AC/DC` -> `ac_dc`; keeps breadcrumbs one line. */
private fun String.slug(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "untitled" }
