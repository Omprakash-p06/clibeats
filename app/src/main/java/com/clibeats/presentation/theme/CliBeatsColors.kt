@file:Suppress("ktlint:standard:multiline-expression-wrapping")

package com.clibeats.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val CliBeatsBackground = Color(0xFF0D0D0D)
val CliBeatsSurface = Color(0xFF151515)
val CliBeatsSurfaceVariant = Color(0xFF1E1E1E)
val CliBeatsAccent = Color(0xFF1DB954)
val CliBeatsTextPrimary = Color(0xFFFFFFFF)
val CliBeatsTextSecondary = Color(0xFFA0A0A0)
val CliBeatsTextDisabled = Color(0xFF505050)
val CliBeatsDivider = Color(0xFF2A2A2A)
val CliBeatsDestructive = Color(0xFFE53935)
val CliBeatsDestructiveSurface = Color(0xFF3B1515)

val CliBeatsColorScheme = darkColorScheme(
    background = CliBeatsBackground,
    surface = CliBeatsSurface,
    surfaceVariant = CliBeatsSurfaceVariant,
    primary = CliBeatsAccent,
    onPrimary = Color(0xFF000000),
    onBackground = CliBeatsTextPrimary,
    onSurface = CliBeatsTextPrimary,
    onSurfaceVariant = CliBeatsTextSecondary,
    outline = CliBeatsDivider,
    error = CliBeatsDestructive,
    errorContainer = CliBeatsDestructiveSurface,
)
