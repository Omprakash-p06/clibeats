package com.clibeats.theme

import androidx.compose.ui.graphics.Color
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsBorderActive
import com.clibeats.presentation.theme.CliBeatsBorderInactive
import com.clibeats.presentation.theme.CliBeatsDestructive
import com.clibeats.presentation.theme.CliBeatsDestructiveSurface
import com.clibeats.presentation.theme.CliBeatsDivider
import com.clibeats.presentation.theme.CliBeatsSurface
import com.clibeats.presentation.theme.CliBeatsSurfaceVariant
import com.clibeats.presentation.theme.CliBeatsTextDisabled
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies every color token matches the UI-SPEC hex values exactly.
 * If any of these tests fail, the theme has drifted from the design contract.
 */
class CliBeatsColorsTest {
    @Test
    fun background_is_0C0C0C() {
        assertEquals(Color(0xFF0C0C0C), CliBeatsBackground)
    }

    @Test
    fun surface_is_121212() {
        assertEquals(Color(0xFF121212), CliBeatsSurface)
    }

    @Test
    fun surfaceVariant_is_1A1A1A() {
        assertEquals(Color(0xFF1A1A1A), CliBeatsSurfaceVariant)
    }

    @Test
    fun accent_is_1DB954() {
        assertEquals(Color(0xFF1DB954), CliBeatsAccent)
    }

    @Test
    fun textPrimary_is_FFFFFF() {
        assertEquals(Color(0xFFFFFFFF), CliBeatsTextPrimary)
    }

    @Test
    fun textSecondary_is_B3B3B3() {
        assertEquals(Color(0xFFB3B3B3), CliBeatsTextSecondary)
    }

    @Test
    fun textDisabled_is_535353() {
        assertEquals(Color(0xFF535353), CliBeatsTextDisabled)
    }

    @Test
    fun divider_is_2A2A2A() {
        assertEquals(Color(0xFF2A2A2A), CliBeatsDivider)
    }

    @Test
    fun borderInactive_is_333333() {
        assertEquals(Color(0xFF333333), CliBeatsBorderInactive)
    }

    @Test
    fun borderActive_is_1DB954() {
        assertEquals(Color(0xFF1DB954), CliBeatsBorderActive)
    }

    @Test
    fun destructive_is_E53935() {
        assertEquals(Color(0xFFE53935), CliBeatsDestructive)
    }

    @Test
    fun destructiveSurface_is_3B1515() {
        assertEquals(Color(0xFF3B1515), CliBeatsDestructiveSurface)
    }
}
