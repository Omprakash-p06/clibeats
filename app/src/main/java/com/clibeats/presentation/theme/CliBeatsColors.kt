@file:Suppress("ktlint:standard:multiline-expression-wrapping")

package com.clibeats.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val CliBeatsBackground = Color(0xFF0C0C0C)
val CliBeatsSurface = Color(0xFF121212)
val CliBeatsSurfaceVariant = Color(0xFF1A1A1A)
val CliBeatsAccent = Color(0xFF1DB954)
val CliBeatsAccentHover = Color(0xFF1ED760)
val CliBeatsAccentPressed = Color(0xFF1AA34A)
val CliBeatsHighlight = Color(0xFF222222)
val CliBeatsSelectedRow = Color(0xFF1A1A1A)
val CliBeatsHoveredRow = Color(0xFF2A2A2A)
val CliBeatsHeader = Color(0xFF535353)
val CliBeatsTextPrimary = Color(0xFFFFFFFF)
val CliBeatsTextSecondary = Color(0xFFB3B3B3)
val CliBeatsSubtext = Color(0xFF808080)
val CliBeatsTextDisabled = Color(0xFF535353)
val CliBeatsNowPlaying = Color(0xFF1DB954)
val CliBeatsDisabled = Color(0xFF404040)
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
