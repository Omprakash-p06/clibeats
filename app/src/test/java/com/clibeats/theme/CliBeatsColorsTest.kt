package com.clibeats.theme

import androidx.compose.ui.graphics.Color
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
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
    fun background_is_0D0D0D() {
        assertEquals(Color(0xFF0D0D0D), CliBeatsBackground)
    }

    @Test
    fun surface_is_151515() {
        assertEquals(Color(0xFF151515), CliBeatsSurface)
    }

    @Test
    fun surfaceVariant_is_1E1E1E() {
        assertEquals(Color(0xFF1E1E1E), CliBeatsSurfaceVariant)
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
    fun textSecondary_is_A0A0A0() {
        assertEquals(Color(0xFFA0A0A0), CliBeatsTextSecondary)
    }

    @Test
    fun textDisabled_is_505050() {
        assertEquals(Color(0xFF505050), CliBeatsTextDisabled)
    }

    @Test
    fun divider_is_2A2A2A() {
        assertEquals(Color(0xFF2A2A2A), CliBeatsDivider)
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
