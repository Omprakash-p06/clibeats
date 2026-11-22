@file:Suppress("ReturnCount")

package com.clibeats.presentation.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Per-tab back stacks plus one global utility stack for Queue and Settings.
 *
 * Plain Kotlin (no Navigation-Compose dependency), so back behaviour is unit-testable and the
 * root tabs keep their own history: switching tabs never clears where you were.
 */
class AppRouter(
    startTab: MainTab = MainTab.HOME,
) {
    private val stacks: Map<MainTab, MutableList<AppRoute>> =
        MainTab.entries.associateWith { mutableStateListOf<AppRoute>(it.route) }

    private val utilityStack = mutableStateListOf<AppRoute>()

    var activeTab: MainTab by mutableStateOf(startTab)
        private set

    /** The route currently drawn: a utility overlay wins, else the active tab's top. */
    val currentRoute: AppRoute
        get() = utilityStack.lastOrNull() ?: stack(activeTab).last()

    val canGoBack: Boolean
        get() = utilityStack.isNotEmpty() || stack(activeTab).size > 1 || activeTab != MainTab.HOME

    /** True when the current tab is at its root, i.e. a second tap should run the tab's own action. */
    fun isAtTabRoot(tab: MainTab = activeTab): Boolean = utilityStack.isEmpty() && stack(tab).size == 1

    /**
     * Switches tabs. Tapping the active tab while inside it pops back to its root; the caller gets
     * `true` so it can also run the tab's re-tap action (flip source, show the playlist grid, ...).
     */
    fun selectTab(tab: MainTab): Boolean {
        if (tab != activeTab) {
            activeTab = tab
            return false
        }
        if (utilityStack.isNotEmpty()) {
            utilityStack.clear()
            return false
        }
        val stack = stack(tab)
        if (stack.size > 1) {
            stack.subList(1, stack.size).clear()
            return false
        }
        return true
    }

    fun navigate(route: AppRoute) {
        when (route) {
            is AppRoute.Tab -> {
                val tab = MainTab.entries.first { it.route == route }
                activeTab = tab
            }

            AppRoute.Queue, AppRoute.Settings -> {
                if (utilityStack.lastOrNull() != route) utilityStack.add(route)
            }

            else -> {
                val stack = stack(activeTab)
                if (stack.lastOrNull() != route) stack.add(route)
            }
        }
    }

    /** Returns false when the system should handle back (exit the activity). */
    fun popBack(): Boolean {
        when {
            utilityStack.isNotEmpty() -> {
                utilityStack.removeAt(utilityStack.lastIndex)
                return true
            }

            stack(activeTab).size > 1 -> {
                val stack = stack(activeTab)
                stack.removeAt(stack.lastIndex)
                return true
            }

            activeTab != MainTab.HOME -> {
                activeTab = MainTab.HOME
                return true
            }

            else -> return false
        }
    }

    private fun stack(tab: MainTab): MutableList<AppRoute> = stacks.getValue(tab)
}

val LocalAppRouter = compositionLocalOf<AppRouter> { error("No AppRouter provided") }
