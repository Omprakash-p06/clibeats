package com.clibeats.presentation.theme

/**
 * Available background theme modes.
 * DARK  → near-black #0C0C0C (original)
 * AMOLED → pure black #000000 (true OLED black, zero battery on OLED panels)
 */
enum class CliBeatsThemeMode(val displayName: String) {
    DARK("Dark"),
    AMOLED("AMOLED"),
}
