# Coding Conventions

**Analysis Date:** 2026-08-05

## Naming Patterns

**Files:**
- PascalCase for Kotlin types; files match their single top-level type: `Track.kt`, `Playlist.kt`, `PlaybackState.kt`.
- Android XML sample files (e.g. `AndroidManifest.xml`) follow Android defaults.
- Test files match the class they test with a `Test` suffix: `TrackTest.kt`.

**Functions:**
- camelCase for most functions and methods: `search`, `getTrack`, `stream`, `playlists`, `queue` in `MusicProvider.kt`.
- Test function names use descriptive **snake_case** scenarios (underscore-separated, behavior-driven): `track_construction_succeeds_with_valid_fields`, `track_equality_is_structural`, `track_copy_updates_single_field`, `playbackState_defaults_to_not_playing` in `app/src/test/java/com/clibeats/domain/model/TrackTest.kt`.

**Variables:**
- camelCase; nullable properties use a fluent suffix rather than prefix: `artworkUrl`, `streamUrl`, `description`, `year` (all nullable `?`) in `app/src/main/java/com/clibeats/domain/model/*.kt`.
- Boolean properties use `isX` prefix: `isPlaying`, `shuffleEnabled`, `isOwned` (note `isOwned` — prefix "is" retains the verb).
- Domain property names use camelCase with units/qualifiers included: `durationMs`, `positionMs`, `bufferedPositionMs`, `trackCount`.

**Types:**
- PascalCase (Kotlin default): `Track`, `Album`, `Artist`, `Playlist`, `PlaybackState`, `RepeatMode`, `MusicProvider`, `ProviderResult`.
- Enums use PascalCase members: `RepeatMode { OFF, ONE, ALL }` in `app/src/main/java/com/clibeats/domain/model/PlaybackState.kt`.

## Code Style

The project is a Kotlin/Android app (Compose + Hilt). Style is enforced by two tools wired in `app/build.gradle.kts`:
- **ktlint** (12.1.1 via `org.jlleitschuh.gradle.ktlint`) — `ktlintCheck`
- **Detekt 1.23.6** — `detekt`, also wired to `detekt-formatting` plugin (ktlint formatting rules run under detekt)

**Formatting settings observed:**
- 4-space indentation in Gradle Kotlin DSL (`app/build.gradle.kts`); Kotlin source uses ktlint defaults (2-space standard body indent, configurable).
- **Trailing commas** used in multi-line argument/parameter lists (e.g. data class `Track(...)` trailing commas, `MainActivity` setContent block, proguardFiles list).
- No semicolon terminators; Kotlin idiomatic syntax.

**Linting (Android Lint) config** in `app/build.gradle.kts`:
- `abortOnError = true` — lint errors fail the build.
- `warningsAsErrors = false` — warnings do not fail the build.
- `checkDependencies = true` — lint checks library module sources too.

## Import Organization

**Order:**
1. Kotlin standard library / platform imports (`android.os.Bundle`, `android.app.Application`).
2. androidx / third-party imports (`androidx.activity.ComponentActivity`, `com.google.dagger...`).
3. Same-project imports last, in package order (`com.clibeats.domain.model.Playlist`, `com.clibeats.domain.model.Track`).

Order and grouping are enforced by ktlint/detekt-formatting and are not hand-tuned.

## Error Handling

**Result-type pattern (recommended):** Error states are modeled as a sealed result type rather than exceptions. The established project pattern is `ProviderResult<out T>` in `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`:

```kotlin
sealed class ProviderResult<out T> {
    data class Success<T>(val data: T) : ProviderResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ProviderResult<Nothing>()
    data object Loading : ProviderResult<Nothing>()
}
```

**Guidance for new code:**
- Provider/repository operations should return `ProviderResult<T>` (with `Success` / `Error` / `Loading`) — matching `MusicProvider` in `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`.
- Use `ProviderResult.Error(message, cause)` to carry an optional `Throwable` cause — do not throw checked-style exceptions across layer boundaries.
- `Loading` is an explicit state, enabling Compose UI to render loading states without null hacks.

## Logging

**Framework:** No logging framework dependency is declared (`gradle/libs.versions.toml` has no timber/slf4j/logcat artifact). No logging calls or channel exists in source yet. Android `Log` (android.util.Log) is the expected default until a framework is introduced — treat logging as a future addition rather than an established convention.

## Comments

**When to Comment:**
- KDoc/Javadoc is NOT currently used on any existing type or function.
- Minimal inline comments. The only comments present are build-config explanations: e.g. `// Main TUI view host will be wired in Phase 2` in `app/src/main/java/com/clibeats/MainActivity.kt`, and `// The root build config declares plugin versions and top-level quality tools.` in `build.gradle.kts`.
- Project docs (architecture, decisions) live in `docs/adr/*.md` (see `docs/adr/ADR-001-architecture-and-di-strategy.md`) rather than inline.
- Add concise comments to explain *why* (non-obvious rationale), not *what*.

## Function Design

**Size:** Detekt enforces complexity limits in `config/detekt/detekt.yml`:
- `LongMethod` threshold: **60 lines**.
- `LargeClass` threshold: **300 lines**.
- `TooManyFunctions` thresholds: **15** per class/file, **15** per class, **10** per interface.

**Parameters:** Default arguments are preferred over overloads. Example: `search(query: String, limit: Int = 20)` in `MusicProvider.kt`.

**Return Values:** Functions on the provider boundary return `ProviderResult<T>`. Suspending functions (`suspend`) are used for asynchronous provider operations.

## Module Design

**Exports:** Types are declared `open` to their package only by default; data classes and interfaces are the primary public surface. There are no explicit `internal`/`public` modifiers used yet — Kotlin defaults are relied upon.

**Barrel Files:** Not used. Each type lives in its own file (e.g. `domain/model/Track.kt`).

## Architecture Constraints (enforced)

**Clean Architecture layering** recorded in `docs/adr/ADR-001-architecture-and-di-strategy.md`:
- Layers: `presentation` → `domain` → `data` (each under `app/src/main/java/com/clibeats/`).
- **Domain layer must be pure Kotlin (zero Android dependencies).**
- Detekt `ForbiddenImport` in `config/detekt/detekt.yml` blocks `com.clibeats.data.*` imports from the presentation layer:
  ```yaml
  ForbiddenImport:
    imports:
      - value: 'com.clibeats.data.*'
        reason: 'Clean Architecture Violation: Presentation layer MUST NOT directly import Data layer packages.'
  ```

**Dependency Injection:** Hilt.
- `@HiltAndroidApp` on `CLIBeatsApp` (`app/src/main/java/com/clibeats/CLIBeatsApp.kt`).
- `@AndroidEntryPoint` on `MainActivity` (`app/src/main/java/com/clibeats/MainActivity.kt`).
- Modules annotated `@Module @InstallIn(SingletonComponent::class)` as an `object` — see `app/src/main/java/com/clibeats/di/AppModule.kt`.

**Placeholders / not-yet-built:** `presentation/`, `data/`, and `domain/` directories currently only contain `.gitkeep` markers (`app/src/main/java/com/clibeats/presentation/.gitkeep`, `app/src/main/java/com/clibeats/data/.gitkeep`, `app/src/main/java/com/clibeats/domain/.gitkeep`). No use cases, ViewModels, repositories, or Compose UI screens exist yet — conventions below extrapolate from the established model/provider/DI patterns.

---

*Convention analysis: 2026-08-05*