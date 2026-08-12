package com.clibeats.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Preset accent colours the user can choose from in Settings.
 *
 * Each entry carries a [Color] used for borders, progress bars, highlights,
 * and the [displayName] shown in the UI.
 */
@Suppress("MagicNumber")
enum class AccentColor(
    val color: Color,
    val displayName: String,
) {
    GREEN(Color(0xFF1DB954), "Green"),
    BLUE(Color(0xFF2979FF), "Blue"),
    AMBER(Color(0xFFFFC107), "Amber"),
    WHITE(Color(0xFFFFFFFF), "White"),
    PINK(Color(0xFFE91E63), "Pink"),
    PURPLE(Color(0xFF9C27B0), "Purple"),
    ;

    companion object {
        val default: AccentColor = GREEN

        /** Resolve from the hex string stored in DataStore; falls back to [default]. */
        fun fromHex(hex: String): AccentColor =
            entries.firstOrNull { it.color.value.toString(16).uppercase() == hex.uppercase() }
                ?: default
    }
}
