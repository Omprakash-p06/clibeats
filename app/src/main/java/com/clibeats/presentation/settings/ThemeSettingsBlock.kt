@file:Suppress("ktlint:standard:function-naming", "FunctionNaming", "MagicNumber")

package com.clibeats.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.theme.Accent
import com.clibeats.presentation.theme.AccentColor
import com.clibeats.presentation.theme.CliBeatsThemeMode
import com.clibeats.presentation.theme.LocalAccentColor
import com.clibeats.presentation.theme.ThemePalette

@Suppress("LongParameterList")
@Composable
internal fun ThemeSettingsBlock(
    uiState: SettingsUiState,
    palette: ThemePalette,
    onPaletteChange: (ThemePalette) -> Unit,
    onThemeModeChange: (CliBeatsThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onCustomAccentChange: (Color) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val shownDark = uiState.themeMode.resolve(systemDark) != CliBeatsThemeMode.LIGHT
    // Folded by default: one big heading that shows the current look and opens everything below.
    var expanded by rememberSaveable { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    TuiBlock(title = "Appearance", isActive = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeHeading(palette = palette, uiState = uiState, shownDark = shownDark, expanded = expanded) {
                expanded = !expanded
            }
            if (expanded) {
                ThemeChoices(
                    uiState = uiState,
                    palette = palette,
                    shownDark = shownDark,
                    onPaletteChange = onPaletteChange,
                    onThemeModeChange = onThemeModeChange,
                    onCreate = { creating = true },
                )
                Spacer(Modifier.height(4.dp))
                SectionLabel("ACCENT COLOR — ${uiState.accentColor.displayName.uppercase()}")
                AccentColorPicker(
                    selected = uiState.accentColor,
                    onSelect = onAccentColorChange,
                    onCustom = onCustomAccentChange,
                )
            }
        }
    }
    if (creating) {
        CustomAccentDialog(
            initial = palette.swatch,
            title = "> theme --create (pick its colour)",
            onDismiss = { creating = false },
            onApply = {
                creating = false
                onPaletteChange(ThemePalette.custom(it))
            },
        )
    }
}

/** The big "Theme" heading: the current theme's orb, its name and scheme, and a hint to tap it. */
@Composable
private fun ThemeHeading(
    palette: ThemePalette,
    uiState: SettingsUiState,
    shownDark: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalAccentColor.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(accent.copy(alpha = 0.10f))
                .border(1.dp, accent.copy(alpha = 0.6f), shape)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide themes" else "Change theme") {
                    onClick()
                }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (shownDark) darkOrb(palette) else lightOrb(palette))
                    .border(2.dp, accent, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "THEME", style = MaterialTheme.typography.titleLarge, color = accent)
            Text(
                text = "${palette.displayName} · ${uiState.themeMode.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (expanded) "Tap to close" else "Tap to change colours, themes and accent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = if (expanded) "▾" else "▸", style = MaterialTheme.typography.titleLarge, color = accent)
    }
}

/** Color scheme cards, then the themes (and Create theme). */
@Suppress("LongParameterList")
@Composable
private fun ThemeChoices(
    uiState: SettingsUiState,
    palette: ThemePalette,
    shownDark: Boolean,
    onPaletteChange: (ThemePalette) -> Unit,
    onThemeModeChange: (CliBeatsThemeMode) -> Unit,
    onCreate: () -> Unit,
) {
    val accent = LocalAccentColor.current
    SectionLabel("COLOR SCHEME")
    CliBeatsThemeMode.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { mode ->
                SchemeCard(
                    mode = mode,
                    palette = palette,
                    selected = uiState.themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) { SectionLabel("THEMES") }
        Text(
            text = "[ + Create theme ]",
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.clickable(role = Role.Button, onClick = onCreate).padding(vertical = 6.dp),
        )
    }
    // A custom theme, when one is in use, comes first.
    val options = listOfNotNull(palette.takeIf { it.isCustom }) + ThemePalette.entries
    options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { option ->
                PaletteCard(
                    palette = option,
                    selected = option == palette,
                    shownDark = shownDark,
                    onClick = { if (option.isCustom) onCreate() else onPaletteChange(option) },
                    onPickLight = {
                        onPaletteChange(option)
                        onThemeModeChange(CliBeatsThemeMode.LIGHT)
                    },
                    onPickDark = {
                        onPaletteChange(option)
                        // AMOLED stays AMOLED (it's the dark version on black).
                        if (uiState.themeMode != CliBeatsThemeMode.AMOLED) {
                            onThemeModeChange(CliBeatsThemeMode.DARK)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AccentColorPicker(
    selected: Accent,
    onSelect: (AccentColor) -> Unit,
    onCustom: (Color) -> Unit,
) {
    val currentAccent = LocalAccentColor.current
    var showCustom by remember { mutableStateOf(false) }
    val options: List<AccentColor?> = AccentColor.entries + null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(ACCENTS_PER_ROW).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    if (option != null) {
                        AccentSwatch(
                            fill = SolidColor(option.color),
                            label = option.displayName,
                            isSelected = selected.preset == option,
                            currentAccent = currentAccent,
                            onClick = { onSelect(option) },
                        )
                    } else {
                        val isCustom = selected.preset == null
                        AccentSwatch(
                            fill = if (isCustom) SolidColor(selected.color) else CUSTOM_SWATCH_BRUSH,
                            label = "Custom",
                            isSelected = isCustom,
                            currentAccent = currentAccent,
                            onClick = { showCustom = true },
                        )
                    }
                }
                repeat(ACCENTS_PER_ROW - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
    if (showCustom) {
        CustomAccentDialog(
            initial = selected.color,
            onDismiss = { showCustom = false },
            onApply = {
                showCustom = false
                onCustom(it)
            },
        )
    }
}

private val CUSTOM_SWATCH_BRUSH = Brush.sweepGradient(hueSpectrum())

@Composable
private fun RowScope.AccentSwatch(
    fill: Brush,
    label: String,
    isSelected: Boolean,
    currentAccent: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .weight(1f)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fill)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, currentAccent, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        },
                    ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) currentAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private const val ACCENTS_PER_ROW = 5

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A little picture of the app in that scheme (the System card is half light, half dark). */
@Composable
private fun SchemeCard(
    mode: CliBeatsThemeMode,
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColor.current
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape,
                )
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(6.dp))) {
            when (mode) {
                CliBeatsThemeMode.SYSTEM -> {
                    MiniApp(previewColors(palette, CliBeatsThemeMode.LIGHT), accent, Modifier.weight(1f))
                    MiniApp(previewColors(palette, CliBeatsThemeMode.DARK), accent, Modifier.weight(1f))
                }
                else -> MiniApp(previewColors(palette, mode), accent, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private class PreviewColors(
    val background: Color,
    val panel: Color,
    val line: Color,
)

private fun previewColors(
    palette: ThemePalette,
    mode: CliBeatsThemeMode,
): PreviewColors =
    when (mode) {
        CliBeatsThemeMode.LIGHT ->
            PreviewColors(
                palette.lightBackground,
                palette.lightSurfaceVariant,
                lerp(palette.lightSurfaceVariant, Color.Black, 0.18f),
            )
        CliBeatsThemeMode.AMOLED ->
            PreviewColors(
                Color.Black,
                lerp(palette.darkSurface, Color.Black, 0.4f),
                lerp(palette.darkSurfaceVariant, Color.White, 0.2f),
            )
        else ->
            PreviewColors(
                palette.darkBackground,
                palette.darkSurfaceVariant,
                lerp(palette.darkSurfaceVariant, Color.White, 0.2f),
            )
    }

@Composable
private fun MiniApp(
    colors: PreviewColors,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().background(colors.background).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.line))
        Box(Modifier.fillMaxWidth(0.5f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.line))
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(colors.panel),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Box(Modifier.padding(end = 3.dp).size(8.dp).clip(CircleShape).background(accent))
        }
    }
}

/** A theme: its light and dark versions as two orbs; the one in use has a ring. */
@Suppress("LongParameterList")
@Composable
private fun PaletteCard(
    palette: ThemePalette,
    selected: Boolean,
    shownDark: Boolean,
    onClick: () -> Unit,
    onPickLight: () -> Unit,
    onPickDark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColor.current
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape,
                )
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Orb(
                brush = lightOrb(palette),
                ringed = selected && !shownDark,
                ring = accent,
                label = "${palette.displayName} light",
                onClick = onPickLight,
            )
            Orb(
                brush = darkOrb(palette),
                ringed = selected && shownDark,
                ring = accent,
                label = "${palette.displayName} dark",
                onClick = onPickDark,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = palette.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Orb(
    brush: Brush,
    ringed: Boolean,
    ring: Color,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(54.dp)
                .then(if (ringed) Modifier.border(2.dp, ring, CircleShape) else Modifier)
                .padding(4.dp)
                .clip(CircleShape)
                .background(brush)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable(role = Role.RadioButton, onClickLabel = label, onClick = onClick),
    )
}

private fun lightOrb(palette: ThemePalette): Brush =
    Brush.linearGradient(listOf(Color.White, lerp(Color.White, palette.swatch, 0.45f), palette.lightSurfaceVariant))

private fun darkOrb(palette: ThemePalette): Brush =
    Brush.linearGradient(
        listOf(palette.darkBackground, lerp(palette.darkSurfaceVariant, palette.swatch, 0.55f), Color.Black),
    )
