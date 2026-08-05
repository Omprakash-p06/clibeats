<!-- refreshed: 2026-08-05 -->
# Architecture

**Analysis Date:** 2026-08-05

## System Overview

CLIBeats is an Android/Kotlin TUI-inspired music client. It follows **MVVM + Clean Architecture** with a strict three-layer separation (`presentation` → `domain` → `data`), enforced at build time by a Detekt `ForbiddenImport` rule. The project is in active early-milestone construction: the `domain` layer is fully scaffolded, while `presentation` and `data` are empty placeholders awaiting later phases.

```text
┌─────────────────────────────────────────────────────────────┐
│                       MainActivity (.kt)                     │
│        `app/src/main/java/com/clibeats/MainActivity.kt`      │
│   (ComponentActivity + setContent — TUI host wired Phase 2)  │
└──────────────────────────┬──────────────────────────────────┘
                           │ Compose UI (Material3, monochrome TUI theme)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      presentation/  (EMPTY - placeholder)    │
│   `app/src/main/java/com/clibeats/presentation`              │
│   Intended: ViewModels + Compose TUI components/screens     │
└──────────────────────────┬──────────────────────────────────┘
                           │ UseCases / ViewModels
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                domain/  (PURE KOTLIN - populated)            │
│ `app/src/main/java/com/clibeats/domain`                      │
│   model/  (data classes)   provider/  (interfaces)           │
│   MusicProvider  ·  ProviderResult                          │
└──────────────────────────┬──────────────────────────────────┘
                           │ Repository implementations
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                data/  (EMPTY - placeholder)                  │
│ `app/src/main/java/com/clibeats/data`                        │
│   Intended: provider adapters, Room DAOs, cache layer        │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│   External Sources: Music providers, Room DB, Media3 player   │
└─────────────────────────────────────────────────────────────┘
        DI graph (Hilt) rooted at `di/AppModule` (@SingletonComponent)
        `app/src/main/java/com/clibeats/di/AppModule.kt`
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `CLIBeatsApp` | Application entry; Hilt root container (`@HiltAndroidApp`) | `app/src/main/java/com/clibeats/CLIBeatsApp.kt` |
| `MainActivity` | Single Activity hosting Compose content; `@AndroidEntryPoint` | `app/src/main/java/com/clibeats/MainActivity.kt` |
| `Track` | Music track domain model | `app/src/main/java/com/clibeats/domain/model/Track.kt` |
| `Album` | Album aggregation model | `app/src/main/java/com/clibeats/domain/model/Album.kt` |
| `Artist` | Artist identity model | `app/src/main/java/com/clibeats/domain/model/Artist.kt` |
| `Playlist` | Playlist model | `app/src/main/java/com/clibeats/domain/model/Playlist.kt` |
| `PlaybackState` | Player state + `RepeatMode` enum | `app/src/main/java/com/clibeats/domain/model/PlaybackState.kt` |
| `MusicProvider` | Provider abstraction interface (search/track/stream/playlist/queue) | `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt` |
| `ProviderResult` | Sealed result type (Success/Error/Loading) | `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt` |
| `AppModule` | Hilt DI module (`@SingletonComponent`) | `app/src/main/java/com/clibeats/di/AppModule.kt` |

## Pattern Overview

**Overall:** MVVM + Clean Architecture (strict 3-layer), formally decided in `docs/adr/ADR-001-architecture-and-di-strategy.md`.

**Key Characteristics:**
- Dependency direction fixed as `presentation` → `domain` → `data`; `data` never imported by `presentation`.
- `domain` is pure Kotlin (zero Android imports) for testable JUnit-only unit tests without Robolectric/emulators.
- Multiple `MusicProvider` implementations can plug in behind one interface without touching UI code.
- Compile-time DI verification via Hilt 2.51.1 KSP processor.
- `ProviderResult` sealed class standardizes async outcomes across all provider operations (no raw throw/`null` bubbling).

## Layers

**Presentation Layer:**
- Purpose: Compose UI (TUI theme) and ViewModels, plus the `Activity` host.
- Location: `app/src/main/java/com/clibeats/presentation`
- Contains: Currently empty (`presentation/.gitkeep`). Intended: TUI screens, ViewModels, player bar components.
- Depends on: `domain` only (enforced — must NOT import `data`).
- Used by: Android framework (`MainActivity` composes the UI).

**Domain Layer:**
- Purpose: Framework-independent business models and provider contracts.
- Location: `app/src/main/java/com/clibeats/domain`
- Contains: `model/` data classes (`Track`, `Album`, `Artist`, `Playlist`, `PlaybackState`, `RepeatMode`) and `provider/` contracts (`MusicProvider`, `ProviderResult`).
- Depends on: Nothing (pure Kotlin; no Android dependency).
- Used by: `presentation` (planned) and `data` (planned).

**Data Layer:**
- Purpose: Repository implementations, provider network adapters, Room DAOs, offline cache.
- Location: `app/src/main/java/com/clibeats/data`
- Contains: Currently empty (`data/.gitkeep`). Intended: `MusicProvider` impls, Room DAOs, mappers from DTOs to domain models.
- Depends on: `domain` + Android/network libraries.
- Used by: `presentation` via repository interfaces (planned).

**DI Layer:**
- Purpose: Hilt module graph; root scope for singletons.
- Location: `app/src/main/java/com/clibeats/di`
- Contains: `AppModule.kt` (`@Module`, `@InstallIn(SingletonComponent::class)`, currently empty body).
- Depends on: Everything it provides (currently none wired).
- Used by: `CLIBeatsApp` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`).

## Data Flow

### Primary Request Path (contract only — not yet implemented)

1. UI (presentation layer) invokes a repository/use-case for a music operation (search, play, list playlists).
2. Repository (data layer) resolves the target `MusicProvider` implementation.
3. `MusicProvider` performs the async operation, returning a `ProviderResult` — `Success(data)`, `Error(message, cause)`, or `Loading` (`app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`).
4. Result propagates back to the UI, which renders `Success` / `Error` / `Loading` states separately.

### Provider Contract Flow

1. Caller (data layer adapter) invokes one of the five `MusicProvider` suspend functions — `search`, `getTrack`, `stream`, `playlists`, `queue` (`app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`).
2. Provider returns domain `Track`/`Playlist` models wrapped in `ProviderResult`.
3. Models flow to presentation, carrying `providerId` so results remain provider-attributable.

**State Management:**
- No explicit state store yet. Domain contracts model state via `PlaybackState` (immutable data class with `RepeatMode`), intended for a future player ViewModel/state holder in the presentation layer.

## Key Abstractions

**MusicProvider (interface):**
- Purpose: Unifies all music sources (official + custom) behind one contract so the UI/domain is provider-agnostic.
- Example: `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`
- Pattern: Strategy/provider interface with per-instance identity (`providerId`, `displayName`).

**ProviderResult (sealed class):**
- Purpose: Typed success/error/loading outcome for every async provider call, avoiding unchecked exceptions or null-sentinels across the boundary.
- Example: `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`
- Pattern: Kotlin sealed class wrapping `Success<T>`, `Error`, `Loading`.

## Entry Points

**Android Application/Activity:**
- Location: `app/src/main/java/com/clibeats/CLIBeatsApp.kt` and `app/src/main/java/com/clibeats/MainActivity.kt`
- Triggers: OS launch via manifest launcher intent (`app/src/main/AndroidManifest.xml`).
- Responsibilities: `CLIBeatsApp` initializes the Hilt graph; `MainActivity` (single-activity) composes the TUI host (body currently a placeholder comment "Main TUI view host will be wired in Phase 2", `MainActivity.kt:13`).

## Architectural Constraints

- **Layer coupling:** `presentation` MUST NOT import `data` packages — enforced by Detekt `ForbiddenImport` (`value: 'com.clibeats.data.*'`) in `config/detekt/detekt.yml`. Dependency direction is strictly `presentation` → `domain` → `data`.
- **Domain purity:** `domain` must stay pure Kotlin with zero Android framework deps (per ADR-001), keeping it JUnit-testable without Robolectric.
- **Version pinning:** All dependency versions centralized in `gradle/libs.versions.toml`; no project-level repositories allowed (`RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts:9`).
- **Single module:** Currently one Gradle module `:app` (`settings.gradle.kts:16`); no multi-module split yet.

## Anti-Patterns

No anti-patterns observed in current production code — layers `presentation` and `data` are empty scaffolds and `domain` is small and clean. The main risk is architecture drift when `data` and `presentation` implementations land; the Detekt `ForbiddenImport` rule and ADR-001 are the guards against it. Apply the same strict rules when adding new code.

## Error Handling

**Strategy:** Sealed-class result type, not exceptions. All `MusicProvider` operations return `ProviderResult<T>` carrying `Error(message, cause)` instead of throwing, giving callers an explicit, exhaustive branch.

**Patterns:**
- `Success<T>(data)` for successful outcomes.
- `Error(message, cause=null)` for failures with optional underlying throwable.
- `Loading` for in-flight state (`ProviderResult.kt`).

No global exception handlers, custom error types, or `try/catch` scaffolding beyond the contract exist yet.

## Cross-Cutting Concerns

- **Logging:** Not yet implemented (no logger dependency or facade in `libs.versions.toml`).
- **Validation:** Not yet implemented at the application layer; models are plain immutable data classes with nullable optional fields (`artworkUrl`, `streamUrl`, `description`, `year`).
- **Authentication:** Not yet implemented; the `INTERNET` permission is declared in `app/src/main/AndroidManifest.xml:4` anticipating network-backed providers.

---

*Architecture analysis: 2026-08-05*