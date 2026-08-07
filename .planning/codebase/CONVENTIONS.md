---
title: CLIBeats Coding Conventions
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

# Coding Conventions

**Analysis Date:** 2026-08-07

## Lint & Format Tooling

**ktlint** (via `org.jlleitschuh.gradle.ktlint` plugin `12.1.1`, declared in `gradle/libs.versions.toml`):
- No `.editorconfig` in repo; default ktlint rules apply.
- Run: `./gradlew ktlintCheck` — enforced in CI (`.github/workflows/ci.yml`) and `scripts/check-quality-gates.sh`.
- Compose files suppress `ktlint:standard:function-naming` (PascalCase composables) and `ktlint:standard:multiline-expression-wrapping` at file top.

**detekt** `1.23.6` (config: `config/detekt/detekt.yml`):
- `buildUponDefaultConfig = true`, `allRules = false`, `build.maxIssues: 0` (zero-tolerance gate).
- Formatting: `Indentation` disabled (false-positive vs ktlint_official `@Inject constructor` style).
- Complexity: `TooManyFunctions` (files/classes 15, interfaces 10), `LongMethod` threshold 60, `LargeClass` threshold 300.
- Style: `MagicNumber` active — allowed: `-1, 0, 1, 2, 100, 1000`; `ignoreHashCodeFunction` + `ignorePropertyDeclaration` on.
- `ForbiddenImport` active: imports matching `com.clibeats.data.*` are rejected with reason "Clean Architecture Violation: Presentation layer MUST NOT directly import Data layer packages."

**Android Lint** (`app/build.gradle.kts`): `abortOnError = true`, `checkDependencies = true`, `warningsAsErrors = false`. Run: `./gradlew lintDebug`.

## Naming Patterns

**Files:** PascalCase per class; UiState per screen as `{Screen}UiState.kt` (`app/src/main/java/com/clibeats/presentation/search/SearchUiState.kt`).

**Functions:**
- Regular functions/methods: camelCase (`onQueryChange`, `upsertTrack`).
- Composable functions: PascalCase (`PlayerBar`, `SearchScreen`, `TuiBlock`, `SongTableRow`) with `@Suppress("FunctionNaming")` (`app/src/main/java/com/clibeats/presentation/component/PlayerBar.kt:53`).
- Private sub-composables per UI state: `SearchIdleState`, `SearchLoadingState`, `SearchErrorState`, `SearchResultsList` (`app/src/main/java/com/clibeats/presentation/search/SearchScreen.kt:144-201`).
- Mapper extension functions: `toDomain()` / `toEntity()` (`app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt`).

**Variables:**
- camelCase; private mutable backing state prefixed `_`: `_query`, `_selectedPlaylistId` (`app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt:30`, `app/src/main/java/com/clibeats/presentation/playlist/PlaylistViewModel.kt:32`).
- Numeric literals use underscore separators: `259_000L`, `5_000L`, `1_700_000_000_000L`.

**Constants:** `private const val UPPER_SNAKE_CASE` at top of file, before the class: `DEBOUNCE_MS`, `MIN_QUERY_LENGTH` (`SearchViewModel.kt:21-22`), `INNERTUBE_BASE_URL` (`app/src/main/java/com/clibeats/di/NetworkModule.kt:23`).

**Types:** sealed interfaces for UI state; `data class`/`data object` variants (`SearchUiState.kt`, `app/src/main/java/com/clibeats/presentation/library/LibraryUiState.kt`).

## Suppression Convention

`@file:Suppress` appears at the **top of the file, before the package declaration**, and every non-obvious suppression carries an inline justification comment:

```kotlin
// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport")
```
(`app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt:1-2`)

Standard per-layer suppression inventory:
- **Data layer + tests + DI modules:** `"ForbiddenImport"` (self-imports), often `"MaxLineLength"` for DAOs/entities/mappers (`app/src/main/java/com/clibeats/data/local/dao/SongDao.kt:2`).
- **Compose files:** `@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:multiline-expression-wrapping")` plus `@Suppress("FunctionNaming", "LongMethod", "LongParameterList")` on composables (`PlayerBar.kt:1-4,53`).
- **Theme files:** `"ktlint:standard:multiline-expression-wrapping"` (`CliBeatsColors.kt:1`), `"Indentation"` (`CliBeatsTypography.kt:1-4`).
- **Screens:** also `"MagicNumber"` (`SearchScreen.kt:1-5`).
- **Mapping logic:** `"ReturnCount"`, `"LoopWithTooManyJumpStatements"`, `"MagicNumber"` (`app/src/main/java/com/clibeats/data/provider/mapper/TrackMapper.kt:1-6`).
- **Interfaces:** `"TooManyFunctions"` (`app/src/main/java/com/clibeats/domain/repository/PlaybackRepository.kt:1`).
- **ViewModels:** `"ktlint:standard:property-naming"` + `"PropertyName"` (`PlaylistViewModel.kt:1`), `"ktlint:standard:function-naming"` (`SearchViewModel.kt:1`).

## Code Style

**Formatting:** 4-space indent, trailing commas in multi-line argument lists, expression-body style (`=`) for single-expression functions, `when` expression with one branch per line. Multi-line `when` in `when (val result = ...)` captures (`SearchViewModel.kt:43-48`).

**@Inject constructor style (ktlint_official):** constructor parameters indented under a class-level `@Inject`:

```kotlin
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val musicProvider: MusicProvider,
    ) : ViewModel() {
```
(`SearchViewModel.kt:24-29`) — also in `LibraryViewModel.kt`, `TimberTelemetryTracker.kt`, `SongRepositoryImpl.kt`.

## Import Organization

1. Kotlin/Android framework (`androidx.*`, `kotlinx.*`, `android.*`)
2. Own package imports (`com.clibeats.*`)
3. Third-party (`dagger.*`, `okhttp3.*`, `retrofit2.*`, `org.mockito.*`)
4. `javax.inject.*` last

Alphabetical within groups (e.g., `PlayerBar.kt:8-39`). No wildcard imports; path aliases not used (only version catalog aliases in Gradle).

## Architecture-Driven Conventions (Clean Architecture)

- Layers: `presentation` → `domain` → `data`, wired by Hilt in `com.clibeats.di` (8 modules: `AppModule`, `CacheModule`, `DatabaseModule`, `DownloadModule`, `ImageLoaderModule`, `NetworkModule`, `PlaybackModule`, `ProviderModule`, `RepositoryModule`, `StorageModule`, `TelemetryModule`).
- Domain interfaces in `com.clibeats.domain.repository` / `com.clibeats.domain.provider`; implementations in `com.clibeats.data.repository` / `com.clibeats.data.provider` (`MusicProvider.kt`, `YouTubeMusicProvider.kt`).
- **Presentation never imports `com.clibeats.data.*`** — enforced by detekt `ForbiddenImport`; violates only via explicit `@file:Suppress("ForbiddenImport")` with justification.
- Repositories are `@Singleton class XImpl @Inject constructor(...) : XRepository` (`SongRepositoryImpl.kt:20-25`).
- Room entities: `@Entity(tableName = "snake_case")`, `@ColumnInfo(name = "snake_case")` for every field (`app/src/main/java/com/clibeats/data/local/entity/SongEntity.kt`).

## ViewModel Patterns

- `@HiltViewModel` + `@Inject constructor`, injected via `hiltViewModel()` in Compose (`MainActivity.kt:35`).
- One sealed-interface UiState per screen with `data object` for static states (Idle/Loading/Empty) and `data class` for data-bearing states (`SearchUiState.kt`, `LibraryUiState.kt`, `QueueUiState.kt`, `PlaylistUiState.kt`, `SettingsUiState.kt`).
- State exposed as `StateFlow` via `stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000L), initialValue = ...)` (`SearchViewModel.kt:51-55`, `LibraryViewModel.kt:44-48`).
- Event handlers are plain methods delegating to the repository (`onPlayPauseClick`, `onSkipNextClick` in `app/src/main/java/com/clibeats/presentation/player/PlayerViewModel.kt`).

## Error Handling

- Provider boundaries return `ProviderResult<T>` sealed class (`Success<T>` / `Error(message, cause)` / `Loading`) instead of throwing (`app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`).
- Repository/DAO failures surface as UiState `Error(message)` variants consumed by screens (`SearchScreen.kt:129-137`).
- Null-safe JSON navigation with early-skip; parse helpers return `0L` on malformed input (`TrackMapper.kt`, `parseDurationMs`).

## Logging

- `android.util.Log.d("CLIBeatsTelemetry", "event=${event.name} params=${event.params}")` (`app/src/main/java/com/clibeats/telemetry/TimberTelemetryTracker.kt:12`) — the "Timber" naming is historical; the actual logger is `android.util.Log`.
- `HttpLoggingInterceptor` (BODY level) added only when `BuildConfig.DEBUG` (`NetworkModule.kt:43-49`).

## Comments

- KDoc on public composables with `@param` and design-contract notes (`PlayerBar.kt:41-52`), on DAO escape helpers (`SongDao.kt:46-54`), and on mappers (`TrackMapper.kt:25-29`).
- `// ── Section ──` separator comments inside screens (`SearchScreen.kt:71`, `127`).
- Suppression justifications as plain `//` comments directly above `@file:Suppress` (see Suppression Convention).

## Function Design

- Small, single-purpose; expression bodies when possible. Keep composables under detekt `LongMethod` 60-line / `LongParameterList` limits or suppress with justification.
- Long parameter lists on composables get defaults (`PlayerBar.kt:55-65`); ViewModels keep constructor params minimal (repositories only).
- Return `Unit` for side-effect handlers; `StateFlow`/`Flow` for observable state.

## Module Design

- One feature per package under `presentation/` (`home`, `search`, `library`, `playlist`, `queue`, `player`, `settings`, `more`) each containing `{X}Screen.kt`, `{X}ViewModel.kt`, `{X}UiState.kt`.
- Shared components in `presentation/component/` (`PlayerBar.kt`, `SongTableRow.kt`, `TuiBlock.kt`); theme tokens in `presentation/theme/` (`CliBeatsColors.kt`, `CliBeatsTypography.kt`, `CliBeatsShapes.kt`, `CliBeatsTheme.kt`).
- No barrel files; direct imports throughout.
- Gradle deps centralized in `gradle/libs.versions.toml`; `app/build.gradle.kts` references only `libs.*` aliases.

---

*Convention analysis: 2026-08-07*
