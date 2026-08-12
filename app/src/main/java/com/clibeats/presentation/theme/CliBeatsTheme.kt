@file:Suppress("ktlint:standard:function-naming")

package com.clibeats.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * CLIBeats Material3 theme.
 *
 * Provides dynamic [accent] colour and [mode] through [CompositionLocalProvider]
 * so all descendants can read [LocalAccentColor] and [LocalThemeMode].
 *
 * Defaults keep backward compatibility (green / DARK) for previews and tests.
 */
@Composable
@Suppress("FunctionNaming")
fun CliBeatsTheme(
    accent: AccentColor = AccentColor.GREEN,
    mode: CliBeatsThemeMode = CliBeatsThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = buildCliBeatsColorScheme(accent.color, mode)

    CompositionLocalProvider(
        LocalAccentColor provides accent.color,
        LocalThemeMode provides mode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CliBeatsTypography,
            shapes = CliBeatsShapes,
            content = content,
        )
    }
}
