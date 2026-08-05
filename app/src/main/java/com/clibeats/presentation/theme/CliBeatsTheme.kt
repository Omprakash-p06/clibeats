@file:Suppress("ktlint:standard:function-naming")

package com.clibeats.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * CLIBeats Material3 theme.
 *
 * Always dark and uses the fixed brand palette from the UI specification.
 */
@Composable
@Suppress("FunctionNaming")
fun CliBeatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CliBeatsColorScheme,
        typography = CliBeatsTypography,
        shapes = CliBeatsShapes,
        content = content,
    )
}
