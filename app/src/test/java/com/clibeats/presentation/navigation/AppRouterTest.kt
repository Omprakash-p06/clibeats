@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppRouterTest {
    private val detail = AppRoute.PlaylistDetail(playlistId = "p1", title = "Gym Mix")

    @Test
    fun `starts on Home with nothing to go back to`() {
        val router = AppRouter()

        assertThat(router.activeTab).isEqualTo(MainTab.HOME)
        assertThat(router.currentRoute).isEqualTo(AppRoute.Home)
        assertThat(router.canGoBack).isFalse()
    }

    @Test
    fun `selecting another tab switches the root route`() {
        val router = AppRouter()

        val reTapped = router.selectTab(MainTab.LIBRARY)

        assertThat(reTapped).isFalse()
        assertThat(router.activeTab).isEqualTo(MainTab.LIBRARY)
        assertThat(router.currentRoute).isEqualTo(AppRoute.Library)
    }

    @Test
    fun `tab stacks are isolated and survive switching away`() {
        val router = AppRouter()
        router.selectTab(MainTab.PLAYLISTS)
        router.navigate(detail)

        router.selectTab(MainTab.SEARCH)
        assertThat(router.currentRoute).isEqualTo(AppRoute.Search)

        router.selectTab(MainTab.PLAYLISTS)
        assertThat(router.currentRoute).isEqualTo(detail)
    }

    @Test
    fun `back pops the active tab detail before leaving the tab`() {
        val router = AppRouter()
        router.selectTab(MainTab.PLAYLISTS)
        router.navigate(detail)

        assertThat(router.popBack()).isTrue()
        assertThat(router.currentRoute).isEqualTo(AppRoute.Playlists)
        assertThat(router.popBack()).isTrue()
        assertThat(router.activeTab).isEqualTo(MainTab.HOME)
        assertThat(router.popBack()).isFalse()
    }

    @Test
    fun `re-tapping the active tab pops to its root, then reports the re-tap`() {
        val router = AppRouter()
        router.selectTab(MainTab.PLAYLISTS)
        router.navigate(detail)

        assertThat(router.selectTab(MainTab.PLAYLISTS)).isFalse()
        assertThat(router.currentRoute).isEqualTo(AppRoute.Playlists)
        assertThat(router.selectTab(MainTab.PLAYLISTS)).isTrue()
    }

    @Test
    fun `queue and settings overlay the current tab and pop first`() {
        val router = AppRouter()
        router.selectTab(MainTab.LIBRARY)
        router.navigate(AppRoute.Queue)

        assertThat(router.currentRoute).isEqualTo(AppRoute.Queue)
        assertThat(router.popBack()).isTrue()
        assertThat(router.currentRoute).isEqualTo(AppRoute.Library)

        router.navigate(AppRoute.Settings)
        assertThat(router.currentRoute).isEqualTo(AppRoute.Settings)
        assertThat(router.popBack()).isTrue()
        assertThat(router.activeTab).isEqualTo(MainTab.LIBRARY)
    }

    @Test
    fun `navigating to a tab route activates that tab`() {
        val router = AppRouter()

        router.navigate(AppRoute.Playlists)

        assertThat(router.activeTab).isEqualTo(MainTab.PLAYLISTS)
        assertThat(router.currentRoute).isEqualTo(AppRoute.Playlists)
    }
}
