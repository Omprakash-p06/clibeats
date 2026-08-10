@file:Suppress("ktlint:standard:multiline-expression-wrapping")

package com.clibeats.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val CliBeatsBackground = Color(0xFF0C0C0C)
val CliBeatsSurface = Color(0xFF121212)
val CliBeatsSurfaceVariant = Color(0xFF1A1A1A)
val CliBeatsAccent = Color(0xFF1DB954)
val CliBeatsTextPrimary = Color(0xFFFFFFFF)
val CliBeatsTextSecondary = Color(0xFFB3B3B3)
val CliBeatsTextDisabled = Color(0xFF535353)
val CliBeatsDivider = Color(0xFF2A2A2A)
val CliBeatsBorderInactive = Color(0xFF333333)
val CliBeatsBorderActive = Color(0xFF1DB954)
val CliBeatsDestructive = Color(0xFFE53935)
val CliBeatsDestructiveSurface = Color(0xFF3B1515)

val CliBeatsColorScheme = darkColorScheme(
    background = CliBeatsBackground,
    surface = CliBeatsSurface,
    surfaceVariant = CliBeatsSurfaceVariant,
    primary = CliBeatsAccent,
    onPrimary = Color(0xFF000000),
    secondaryContainer = CliBeatsSurfaceVariant,
    onSecondaryContainer = CliBeatsAccent,
    onBackground = CliBeatsTextPrimary,
    onSurface = CliBeatsTextPrimary,
    onSurfaceVariant = CliBeatsTextSecondary,
    outline = CliBeatsDivider,
    error = CliBeatsDestructive,
    errorContainer = CliBeatsDestructiveSurface,
)
