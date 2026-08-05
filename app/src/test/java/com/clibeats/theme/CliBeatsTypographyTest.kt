package com.clibeats.theme

import androidx.compose.ui.unit.sp
import com.clibeats.presentation.theme.CliBeatsTypography
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies all typography roles match UI-SPEC sp sizes and font weights.
 * Protects against accidental theme drift in future phases.
 */
class CliBeatsTypographyTest {
    @Test
    fun titleLarge_is_18sp() {
        assertEquals(18.sp, CliBeatsTypography.titleLarge.fontSize)
    }

    @Test
    fun titleMedium_is_16sp() {
        assertEquals(16.sp, CliBeatsTypography.titleMedium.fontSize)
    }

    @Test
    fun bodyLarge_is_14sp() {
        assertEquals(14.sp, CliBeatsTypography.bodyLarge.fontSize)
    }

    @Test
    fun bodyMedium_is_13sp() {
        assertEquals(13.sp, CliBeatsTypography.bodyMedium.fontSize)
    }

    @Test
    fun labelLarge_is_12sp() {
        assertEquals(12.sp, CliBeatsTypography.labelLarge.fontSize)
    }

    @Test
    fun labelMedium_is_11sp() {
        assertEquals(11.sp, CliBeatsTypography.labelMedium.fontSize)
    }

    @Test
    fun labelSmall_is_10sp() {
        assertEquals(10.sp, CliBeatsTypography.labelSmall.fontSize)
    }
}
