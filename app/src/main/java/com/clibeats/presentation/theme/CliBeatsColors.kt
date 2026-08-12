@file:Suppress("ktlint:standard:multiline-expression-wrapping", "MagicNumber")

package com.clibeats.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Static fallback colours (used before theme is resolved) ────────────────

val CliBeatsBackground = Color(0xFF0C0C0C)
val CliBeatsBackgroundAmoled = Color(0xFF000000)
val CliBeatsSurface = Color(0xFF121212)
val CliBeatsSurfaceAmoled = Color(0xFF0A0A0A)
val CliBeatsSurfaceVariant = Color(0xFF1A1A1A)
val CliBeatsAccent = Color(0xFF1DB954) // default; overridden by LocalAccentColor

val CliBeatsTextPrimary = Color(0xFFFFFFFF)
val CliBeatsTextSecondary = Color(0xFFB3B3B3)
val CliBeatsTextDisabled = Color(0xFF535353)
val CliBeatsDivider = Color(0xFF2A2A2A)
val CliBeatsBorderInactive = Color(0xFF333333)
val CliBeatsBorderActive = Color(0xFF1DB954) // overridden at runtime by LocalAccentColor
val CliBeatsDestructive = Color(0xFFE53935)
val CliBeatsDestructiveSurface = Color(0xFF3B1515)

// ── CompositionLocals ──────────────────────────────────────────────────────

/** Provides the currently active accent [Color] to the composition tree. */
val LocalAccentColor = staticCompositionLocalOf { CliBeatsAccent }

/** Provides the currently active [CliBeatsThemeMode]. */
val LocalThemeMode = staticCompositionLocalOf { CliBeatsThemeMode.DARK }

// ── Dynamic colour scheme builder ──────────────────────────────────────────

/**
 * Builds a Material3 dark colour scheme from the given [accent] and [mode].
 * Call this whenever either changes (theme engine drives it).
 */
fun buildCliBeatsColorScheme(
    accent: Color,
    mode: CliBeatsThemeMode,
) = darkColorScheme(
    background = if (mode == CliBeatsThemeMode.AMOLED) CliBeatsBackgroundAmoled else CliBeatsBackground,
    surface = if (mode == CliBeatsThemeMode.AMOLED) CliBeatsSurfaceAmoled else CliBeatsSurface,
    surfaceVariant = CliBeatsSurfaceVariant,
    primary = accent,
    onPrimary = Color(0xFF000000),
    secondaryContainer = CliBeatsSurfaceVariant,
    onSecondaryContainer = accent,
    onBackground = CliBeatsTextPrimary,
    onSurface = CliBeatsTextPrimary,
    onSurfaceVariant = CliBeatsTextSecondary,
    outline = CliBeatsDivider,
    error = CliBeatsDestructive,
    errorContainer = CliBeatsDestructiveSurface,
)

/** Legacy static scheme (used for previews / tests). */
val CliBeatsColorScheme = buildCliBeatsColorScheme(CliBeatsAccent, CliBeatsThemeMode.DARK)
