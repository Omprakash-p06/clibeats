---
title: CLIBeats Architecture
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

<!-- refreshed: 2026-08-07 -->
# Architecture

**Analysis Date:** 2026-08-07

## System Overview

```text
┌──────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION (Compose UI)                       │
│  MainActivity ── MainLayout ── Screen(s)  ◄── ViewModel(s)           │
│  `presentation/layout/MainLayout.kt`     `presentation/*/*ViewModel` │
└──────────────┬───────────────────────────────────────────────────────┘
               │ StateFlow / events
               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          DOMAIN (pure Kotlin)                        │
│  Models: `domain/model/*.kt`                                         │
│  Abstractions: `domain/repository/*.kt`, `domain/provider/*.kt`      │
└──────────────┬───────────────────────────────────────────────────────┘
               │ interfaces implemented via Hilt @Binds
               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          DATA (implementations)                      │
│  Repos: `data/repository/*Impl.kt`   Provider: `data/provider/`       │
│  Room:  `data/local/`                Network: `data/network/`         │
│  Cache: `data/cache/`                Prefs:   `data/preferences/`     │
│  Downloads: `data/download/`                                          │
└──────────────┬───────────────────────────────────────────────────────┘
               │ ExoPlayer + Media3
               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     PLAYBACK ENGINE (Media3)                          │
│  `playback/PlayerAdapter.kt` → ExoPlayer ← `playback/service/`        │
└──────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `MainActivity` | Single-activity host; owns `NavDestination` state; top-level `PlayerViewModel` | `app/src/main/java/com/clibeats/MainActivity.kt` |
| `MainLayout` | Adaptive shell: `NavigationSuiteScaffold` (rail/drawer), TopAppBar, persistent `PlayerBar` | `app/src/main/java/com/clibeats/presentation/layout/MainLayout.kt` |
| `NavDestination` | Sealed nav graph (7 destinations, 4 main tabs) | `app/src/main/java/com/clibeats/presentation/layout/NavDestination.kt` |
| `MusicProvider` | Provider abstraction (search/stream/playlists/queue) | `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt` |
| `YouTubeMusicProvider` | InnerTube-backed provider implementation | `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt` |
| Repositories (domain) | Interfaces owned by domain | `app/src/main/java/com/clibeats/domain/repository/*.kt` |
| Repository impls (data) | Room/engine-backed implementations | `app/src/main/java/com/clibeats/data/repository/*Impl.kt` |
| `PlayerAdapter` | Singleton ExoPlayer wrapper; publishes `PlaybackState`/queue `StateFlow`s | `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt` |
| `PlaybackService` | Media3 `MediaSessionService` for background audio | `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt` |
| DI modules | All Hilt bindings/provides | `app/src/main/java/com/clibeats/di/*.kt` |
| Telemetry | `TelemetryTracker` / `CrashReporter` (Timber impls) | `app/src/main/java/com/clibeats/telemetry/*.kt` |

## Pattern Overview

**Overall:** MVVM + Clean Architecture (per `docs/adr/ADR-001-architecture-and-di-strategy.md`) — one `:app` module, package-by-layer.

**Key Characteristics:**
- **Dependency rule:** `presentation` → `domain` → `data`; `data` implements `domain` interfaces; `domain` is pure Kotlin (no Android imports).
- **State:** ViewModels expose immutable `StateFlow<UiState>`; screens `collectAsState()` and emit events via ViewModel methods.
- **DI:** Hilt 2.51.1 with `@Binds` for interface→impl mapping (`di/RepositoryModule.kt`, `di/ProviderModule.kt`, `di/TelemetryModule.kt`).
- **Single shared playback engine:** one `@Singleton ExoPlayer` + `@Singleton PlayerAdapter` flow exposed through `PlaybackRepository`.
- **Navigation:** manual state-based navigation via sealed class + `when`, NOT Navigation Compose.

## Layers

**Presentation:**
- Purpose: Compose UI + ViewModels + UiState
- Location: `app/src/main/java/com/clibeats/presentation/`
- Contains: per-feature packages (`home/`, `search/`, `library/`, `playlist/`, `queue/`, `settings/`, `player/`, `more/`), shared `component/` (TUI-style blocks, table rows, `PlayerBar`), `layout/`, `theme/` (CliBeats colors/typography/shapes)
- Depends on: `domain` (repositories, models, provider), Hilt for injection
- Used by: `MainActivity`

**Domain:**
- Purpose: business models, repository contracts, provider abstraction
- Location: `app/src/main/java/com/clibeats/domain/`
- Contains: `model/` (`Track`, `Playlist`, `Artist`, `Album`, `PlaybackState` + `RepeatMode`), `repository/` (`SongRepository`, `PlaylistRepository`, `HistoryRepository`, `PlaybackRepository`), `provider/` (`MusicProvider`, `ProviderResult`)
- Depends on: nothing (pure Kotlin; `ProviderResult` is a sealed class with `Success`/`Error`/`Loading`)
- Used by: presentation ViewModels, data implementations

**Data:**
- Purpose: all concrete implementations — Room, network, provider, cache, downloads, preferences
- Location: `app/src/main/java/com/clibeats/data/`
- Contains: `repository/` (impls), `local/` (DB + `dao/` + `entity/` + `mapper/`), `provider/` (`YouTubeMusicProvider`, `api/` `InnerTubeApi` + `InnerTubeHeaderInterceptor`, `dto/`, `mapper/` `TrackMapper`), `network/` (`NetworkMonitor`), `preferences/` (`AppPreferences`), `cache/` (`CacheManager`), `download/` (`TrackDownloadManager`)
- Depends on: `domain`, Room, Retrofit/OkHttp, DataStore, security-crypto, Media3 types (via `PlayerAdapter`)
- Used by: `di/` modules to fulfill domain contracts

**Playback (supporting package):**
- Purpose: Media3 engine integration outside the three core layers
- Location: `app/src/main/java/com/clibeats/playback/`
- Contains: `PlayerAdapter.kt` (ExoPlayer wrapper), `service/PlaybackService.kt` (MediaSessionService)
- Depends on: `data/cache/CacheManager`, `domain/model/Track`, Media3

## Data Flow

### Primary Request Path — Search

1. User types in `SearchScreen` → `SearchViewModel.onQueryChange()` (`app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt`)
2. `_query` `MutableStateFlow` → `debounce(300ms)` → `distinctUntilChanged` → `flatMapLatest`
3. `musicProvider.search(query)` → `YouTubeMusicProvider.search()` (`app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`) → `InnerTubeApi.search()` POST (`app/src/main/java/com/clibeats/data/provider/api/InnerTubeApi.kt`)
4. `SearchResponse.toTrackList()` walks the JSON tree null-safely (`app/src/main/java/com/clibeats/data/provider/mapper/TrackMapper.kt`)
5. Result wrapped in `ProviderResult.Success/Error` → mapped to `SearchUiState` → `stateIn(viewModelScope, WhileSubscribed(5_000L))`

### Primary Request Path — Playback

1. `HomeScreen`/`SearchScreen` calls `playerViewModel.playTrack(track)` (`app/src/main/java/com/clibeats/presentation/player/PlayerViewModel.kt`)
2. `PlaybackRepository.playTrack()` → `PlaybackRepositoryImpl` → `PlayerAdapter.playTrack()` (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`)
3. `Track.toMediaItem()` picks cached file (via `CacheManager`) → else `streamUrl` → else `Uri.EMPTY`; `player.setMediaItem` + `prepare()` + `play()`
4. ExoPlayer `Player.Listener` callbacks (`onIsPlayingChanged`, `onMediaItemTransition`, etc.) → `updateState()` → `_playbackState` / `_queueFlow`
5. `MainLayout` collects `playerViewModel.playbackState` → renders `PlayerBar` progress/artwork (`app/src/main/java/com/clibeats/presentation/layout/MainLayout.kt`)

### Secondary Flow — Library (Room-backed)

1. `LibraryViewModel` calls `songRepository.getAllTracksAsFlow()` (`app/src/main/java/com/clibeats/presentation/library/LibraryViewModel.kt`)
2. `SongRepositoryImpl` → `SongDao.getAllAsFlow()` → `Flow<List<SongEntity>>` → `toDomain()` mapper (`app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt`)
3. Groups by artist/album → `LibraryUiState.Success` → `stateIn`

**State Management:**
- ViewModel-scoped: `StateFlow<UiState>` with `SharingStarted.WhileSubscribed(5_000L)` per screen
- App-scoped singletons: `PlayerAdapter` (`playbackState`, `queueFlow`), `AppPreferences`, `CacheManager`, `TrackDownloadManager` — all `@Singleton` StateFlows
- Nav state: plain `mutableStateOf<NavDestination>` held in `MainActivity`

## Key Abstractions

**`MusicProvider` (domain/provider):**
- Purpose: multi-provider streaming abstraction — search, getTrack, stream, playlists, queue
- Interface: `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`
- Impl: `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt` (bound in `di/ProviderModule.kt`)
- Result type: `ProviderResult<T>` sealed class (`app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`)

**Repository interfaces (`domain/repository`):**
- Purpose: domain-owned contracts for songs, playlists, history, playback
- Examples: `PlaybackRepository.kt`, `SongRepository.kt`, `PlaylistRepository.kt`, `HistoryRepository.kt`
- Bound to `*Impl` in `di/RepositoryModule.kt`

**`PlayerAdapter` (playback):**
- Purpose: single facade over ExoPlayer; maps Media3 states to domain `PlaybackState`; resolves cached/local URIs
- File: `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`
- Pattern: `@Singleton` with injected `ExoPlayer` + `CacheManager`; exposes `StateFlow`s; shares the ExoPlayer with `PlaybackService`'s `MediaSession`

**Mappers:**
- `data/local/mapper/SongMapper.kt`, `PlaylistMapper.kt` — Room entity ↔ domain model
- `data/provider/mapper/TrackMapper.kt` — InnerTube JSON/DTO ↔ domain `Track` (incl. `extractStreamUrl()`)

**Telemetry:**
- `TelemetryTracker` / `CrashReporter` interfaces in `app/src/main/java/com/clibeats/telemetry/`, Timber impls bound in `di/TelemetryModule.kt`

## Entry Points

**`CLIBeatsApp`:**
- Location: `app/src/main/java/com/clibeats/CLIBeatsApp.kt`
- `@HiltAndroidApp` Application — Hilt root

**`MainActivity`:**
- Location: `app/src/main/java/com/clibeats/MainActivity.kt`
- `@AndroidEntryPoint`; `enableEdgeToEdge()`; `setContent { CliBeatsTheme { MainLayout(...) } }`; renders destination via `when`
- Declared in `app/src/main/AndroidManifest.xml` as launcher activity

**`PlaybackService`:**
- Location: `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`
- `MediaSessionService` (foreground `mediaPlayback`) with injected `ExoPlayer`; builds a `MediaSession` on the shared player; registered in `AndroidManifest.xml`

## Architectural Constraints

- **Threading:** ExoPlayer callbacks on main thread; `PlayerAdapter.updateState()` mutates StateFlows from listener callbacks; `TrackDownloadManager` uses its own `CoroutineScope(Dispatchers.IO)`; Room queries via suspend/Flow; `TrackMapper` JSON parsing is synchronous on caller thread.
- **Global state:** `PlayerAdapter` (`playbackState`, `queueFlow`), `AppPreferences`, `CacheManager` (cache dir + index), `TrackDownloadManager`, `NetworkMonitor`, `YouTubeMusicProvider` — all `@Singleton`.
- **Layer coupling:** `domain` must not import `android.*` (ADR-001). `@file:Suppress("ForbiddenImport")` comments mark legitimate cross-layer imports in `data/`, `playback/`, `di/` (custom detekt rule).
- **DI scope:** everything app-scoped in `SingletonComponent`; no `ActivityComponent`/`ViewModelComponent` bindings exist yet.
- **No Navigation Compose:** navigation is a sealed-class `when` in `MainActivity`; screens receive `onNavigate` lambdas; `PlayerViewModel` is created once at activity scope and passed down.

## Anti-Patterns

### runBlocking inside MediaItem construction

**What happens:** `PlayerAdapter.toMediaItem()` calls `runBlocking { cacheManager.getCachedFile(id) }` (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:190`), blocking the calling thread to hit the Room-backed `CacheIndexDao`.
**Why it's wrong:** can block main thread during playback start; breaks coroutine discipline.
**Do this instead:** resolve the cache lookup asynchronously (suspend) and set media items once the URI is known, or cache a pre-resolved URI map.

### Provider stubs & unused stream path

**What happens:** `MusicProvider.getTrack()`, `playlists()`, `queue()` return `ProviderResult.Error("Not implemented in Phase 5")` / empty lists (`app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`); `MusicProvider.stream()` is never called by `PlayerAdapter` — `TrackMapper` sets `streamUrl = null` for search results, so `toMediaItem()` falls back to `Uri.EMPTY` for uncached tracks.
**Why it's wrong:** search-result playback yields an empty media URI; provider contract is half-wired.
**Do this instead:** resolve `streamUrl` via `MusicProvider.stream(trackId)` before enqueueing, or pre-fill via `getTrack()`.

### Over-broad detekt `ForbiddenImport` suppressions

**What happens:** many files carry `@file:Suppress("ForbiddenImport")`; comments state the Phase 0 `com.clibeats.data.*` pattern is over-broad.
**Why it's wrong:** weakens the layer rule the suppression was meant to enforce.
**Do this instead:** narrow the detekt rule to specific import allowlists (`config/detekt/detekt.yml`).

## Error Handling

**Strategy:** functional result types + `runCatching`.

**Patterns:**
- `ProviderResult<T>` (`Success`/`Error`/`Loading`) returned by provider and mapped to UiState in ViewModels (`SearchViewModel.kt:43-48`)
- `runCatching { ... }.getOrElse` in `YouTubeMusicProvider` and `CacheManager`
- Downloads track per-track `DownloadStatus` state machine (`data/download/DownloadStatus.kt`)
- No global error handler; failures surface as `UiState.Error` messages in screens

## Cross-Cutting Concerns

**Logging:** Timber via `telemetry/TimberTelemetryTracker.kt` + `TimberCrashReporter.kt` (bound in `di/TelemetryModule.kt`); OkHttp `HttpLoggingInterceptor` at `BODY` level in debug builds only (`di/NetworkModule.kt`)
**Validation:** minimal — query length guard in `SearchViewModel` (`MIN_QUERY_LENGTH = 2`); null-safe JSON navigation in `TrackMapper`
**Authentication:** `auth_token` stored in `EncryptedSharedPreferences` (`di/StorageModule.kt`, `data/preferences/AppPreferences.kt`); currently unused by `InnerTubeHeaderInterceptor`
**Image loading:** Coil `AsyncImage` with custom `ImageLoader` (memory 25%, disk 2%, shared OkHttp) from `di/ImageLoaderModule.kt`

---

*Architecture analysis: 2026-08-07*
