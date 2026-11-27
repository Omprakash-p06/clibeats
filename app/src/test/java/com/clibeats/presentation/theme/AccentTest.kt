package com.clibeats.presentation.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccentTest {
    @Test
    fun `presets are saved and read back by name`() {
        val accent = Accent.fromStored("MAGENTA")
        assertThat(accent.preset).isEqualTo(AccentColor.MAGENTA)
        assertThat(accent.storedName).isEqualTo("MAGENTA")
    }

    @Test
    fun `custom colours round-trip as hex`() {
        val accent = Accent.fromStored("#12AB9F")
        assertThat(accent.preset).isNull()
        assertThat(accent.color).isEqualTo(Color(0xFF12AB9F))
        assertThat(accent.storedName).isEqualTo("#12AB9F")
        assertThat(accent.displayName).isEqualTo("Custom #12AB9F")
    }

    @Test
    fun `people who picked a retired preset keep it as a custom colour`() {
        val blue = Accent.fromStored("BLUE")
        assertThat(blue.preset).isNull()
        assertThat(Accent.toHex(blue.color)).isEqualTo("#2979FF")
        assertThat(Accent.toHex(Accent.fromStored("PINK").color)).isEqualTo("#E91E63")
        assertThat(Accent.fromStored("RED").preset).isNull()
    }

    @Test
    fun `missing or broken values fall back to the default accent`() {
        assertThat(Accent.fromStored(null)).isEqualTo(Accent.DEFAULT)
        assertThat(Accent.fromStored("#XYZ")).isEqualTo(Accent.DEFAULT)
        assertThat(Accent.fromStored("NOPE")).isEqualTo(Accent.DEFAULT)
        assertThat(Accent.parseHex("1db954")).isEqualTo(Color(0xFF1DB954))
        assertThat(Accent.DEFAULT.storedName).isEqualTo("WHITE")
    }
}
