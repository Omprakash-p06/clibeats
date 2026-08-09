# Phase 2 Research: TUI Design System & Navigation Layout

**Researched:** 2026-08-05
**Phase Goal:** Implement the monochrome TUI design system, JetBrains Mono typography, top app bar, navigation, and persistent bottom player.

---

## Finding 1: JetBrains Mono — Bundle as Asset, NOT Downloadable Font

**Question:** Can JetBrains Mono be loaded via Android's Downloadable Fonts API (`Fonts.GoogleFonts`)?

**Answer: NO.**
JetBrains Mono is listed on the Google Fonts website but is NOT registered in the Android Downloadable Fonts provider. Attempting `Fonts.GoogleFonts("JetBrains Mono")` will fail at runtime.

**Required approach:** Bundle `.ttf` font files directly in `app/src/main/res/font/`.

**Files to download and bundle:**
- `jetbrains_mono_regular.ttf` — FontWeight.Normal (400)
- `jetbrains_mono_medium.ttf` — FontWeight.Medium (500)
- `jetbrains_mono_semibold.ttf` — FontWeight.SemiBold (600)
- `jetbrains_mono_bold.ttf` — FontWeight.Bold (700)

**Source:** [JetBrains/JetBrainsMono](https://github.com/JetBrains/JetBrainsMono/releases) — License: SIL Open Font License 1.1 (free for commercial use).

**Kotlin declaration:**
```kotlin
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)
```

---

## Finding 2: Adaptive Navigation — Use `NavigationSuiteScaffold`

**Question:** How to implement Rail (phones) ↔ Drawer (tablets) adaptive navigation without manual if/else size checks?

**Answer:** Use `NavigationSuiteScaffold` from `androidx.compose.material3:material3-adaptive-navigation-suite`.

This component automatically renders:
- `NavigationBar` (bottom bar) on compact-width screens (phones portrait)
- `NavigationRail` on medium-width screens (phones landscape, small tablets)
- `NavigationDrawer` on expanded-width screens (large tablets)

**Dependency to add:**
```toml
# gradle/libs.versions.toml
adaptive-nav = "1.3.1"

[libraries]
material3-adaptive-nav = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite", version.ref = "adaptive-nav" }
```

**Important caveat:** Do NOT wrap `NavigationSuiteScaffold` in a standard `Scaffold` — it manages its own layout. Place `TopAppBar` inside the content slot using a `Column`.

**Per UI-SPEC:** For phones (compact), the UI-SPEC mandates NavRail (64dp) not NavigationBar. Override `layoutType`:
```kotlin
NavigationSuiteScaffold(
    layoutType = NavigationSuiteType.NavigationRail, // force rail on phone
    navigationSuiteItems = { ... }
) { content }
```
Or detect window size class manually and pass the appropriate `NavigationSuiteType`.

---

## Finding 3: Compose UI Visual Testing — Paparazzi (JVM Screenshot Tests)

**Question:** Best approach for "Compose UI visual component tests" as required by the roadmap?

**Answer:** Paparazzi v1.3.1 — renders Compose components on JVM, no emulator required.

**Why Paparazzi over `compose-ui-test`:**
- No emulator or device needed → runs in CI without AVD
- Pixel-perfect screenshot diffs catch visual regressions
- Dramatically faster than instrumented tests

**Dependency:**
```toml
# gradle/libs.versions.toml
paparazzi = "1.3.4"

[plugins]
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
```

**Test pattern:**
```kotlin
class CliBeatsThemeTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoTitleBar"
    )

    @Test fun songRowDefault() {
        paparazzi.snapshot {
            CliBeatsTheme {
                SongTableRow(
                    trackTitle = "Midnight City",
                    artist = "M83",
                    duration = "4:02",
                    artworkUrl = null
                )
            }
        }
    }
}
```

**Tasks:** `./gradlew recordPaparazziDebug` (generate baselines), `./gradlew verifyPaparazziDebug` (CI gate).

---

## Finding 4: Material3 ColorScheme — Dark Theme Setup

For `MaterialTheme(colorScheme = ...)` with pure dark scheme (no dynamic color):

```kotlin
val CliBeatsColorScheme = darkColorScheme(
    background      = Color(0xFF0D0D0D),
    surface         = Color(0xFF151515),
    surfaceVariant  = Color(0xFF1E1E1E),
    primary         = Color(0xFF1DB954),
    onPrimary       = Color(0xFF000000),
    onBackground    = Color(0xFFFFFFFF),
    onSurface       = Color(0xFFFFFFFF),
    onSurfaceVariant= Color(0xFFA0A0A0),
    outline         = Color(0xFF2A2A2A),
    error           = Color(0xFFE53935),
)
```

Use `darkColorScheme()` (not `lightColorScheme()`) — this suppresses any light-mode overrides. Do NOT use `dynamicDarkColorScheme()` — that would override the brand palette with wallpaper-derived colors.

---

## Finding 5: Package Structure for Presentation Layer

Based on Phase 1 scaffold, the `presentation/` package is currently empty (`.gitkeep`). Phase 2 will create:

```
presentation/
├── theme/
│   ├── CliBeatsTheme.kt        # MaterialTheme wrapper
│   ├── CliBeatsColors.kt       # Color tokens
│   ├── CliBeatsTypography.kt   # Typography with JetBrains Mono
│   └── CliBeatsShapes.kt       # Shape tokens (0dp radius for TUI)
├── layout/
│   ├── MainLayout.kt           # NavigationSuiteScaffold + TopAppBar shell
│   └── NavDestination.kt       # Sealed class for nav items
└── component/
    ├── SongTableRow.kt          # 48dp dense row component
    └── PlayerBar.kt             # Persistent bottom player component
```

---

## Validation Architecture (Nyquist)

Tests needed to validate this phase:

| Test Type | What to test | Tool |
|-----------|-------------|------|
| Unit | Color token values match spec | JUnit (pure Kotlin) |
| Unit | Typography sp/weight values match spec | JUnit |
| Screenshot | `CliBeatsTheme` dark background renders correctly | Paparazzi |
| Screenshot | `SongTableRow` all states (normal, hover, now-playing) | Paparazzi |
| Screenshot | `PlayerBar` idle + playing states | Paparazzi |
| Screenshot | `MainLayout` with NavRail + content | Paparazzi |
| Compile | No `android.*` imports in `theme/` package | Detekt/ktlint CI gate |

## RESEARCH COMPLETE
