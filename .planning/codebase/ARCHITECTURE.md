<!-- refreshed: 2026-08-08 -->
# Architecture

**Analysis Date:** 2026-08-08

## System Overview

The repository contains **two independent codebases** wired together over a REST contract. The Android app (`app/`) is the CLI-style music player; it delegates all music-provider traffic (search, stream resolution, metadata) to the TypeScript provider gateway (`gateway/`). There is no in-app YouTube scraping — the app only talks to the gateway via Retrofit, and the gateway owns the provider logic (Mock + YouTube Music via `youtubei.js`).

```text
┌──────────────────────────────────────────────────────────────────────┐
│                     ANDROID APP (app/) — Kotlin + Compose              │
│                                                                        │
│   PRESENTATION (com.clibeats.presentation)                             │
│   `app/src/main/java/com/clibeats/presentation/`                      │
│   Screens · ViewModels · UiState · TUI components · MainLayout        │
│                              │                                        │
│                              ▼                                        │
│   DOMAIN (com.clibeats.domain)                                        │
│   `app/src/main/java/com/clibeats/domain/`                            │
│   models · repository interfaces · MusicProvider · QueueManager       │
│                              │                                        │
│                              ▼                                        │
│   DATA (com.clibeats.data)                                            │
│   `app/src/main/java/com/clibeats/data/`                              │
│   Room (local/) · Retrofit (gateway/) · CacheManager · Downloads      │
│                              │                                        │
│   PLAYBACK (com.clibeats.playback) — Media3 ExoPlayer                 │
│   `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`          │
│   `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`│
└──────────────────────────────┬───────────────────────────────────────┘
                               │  HTTP/JSON (Retrofit ↔ Fastify)
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│      PROVIDER GATEWAY (gateway/) — Node.js + TypeScript + Fastify     │
│                                                                      │
│   LAYER 1 · TRANSPORT     `gateway/src/app.ts` routes + hooks         │
│                              │                                        │
│                              ▼                                        │
│   LAYER 2 · ORCHESTRATION  ProviderSelectionEngine (failover/select)  │
│                              │ `core/selection/ProviderSelectionEngine.ts`
│                              ▼                                        │
│   LAYER 3 · CORE SERVICES   ProviderRegistry · CircuitBreaker ·       │
│                             CacheManager (Redis segregated) ·         │
│                             EventBus · metrics · health               │
│                              │                                        │
│                              ▼                                        │
│   LAYER 4 · PROVIDERS       ProviderAdapter implementations           │
│                              `providers/mock/` · `providers/youtube/`  │
└──────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| CLIBeatsApp | Hilt `Application` root | `app/src/main/java/com/clibeats/CLIBeatsApp.kt` |
| MainActivity | Single Compose entry point; holds nav state | `app/src/main/java/com/clibeats/MainActivity.kt` |
| MainLayout | Adaptive nav shell (rail/drawer), TopAppBar, persistent PlayerBar | `app/src/main/java/com/clibeats/presentation/layout/MainLayout.kt` |
| ViewModels (`*ViewModel`) | Convert UI intents into repository/provider calls; expose `StateFlow` UiState | `app/src/main/java/com/clibeats/presentation/**/` |
| QueueManager | In-memory queue order, repeat/next/prev logic | `app/src/main/java/com/clibeats/domain/playback/QueueManager.kt` |
| MusicProvider | Domain contract for search/stream/playlists | `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt` |
| GatewayMusicProvider | `MusicProvider` impl backed by the gateway REST API | `app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt` |
| GatewayApi | Retrofit interface mirroring gateway routes | `app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt` |
| Repositories (`domain/repository/*`) | Domain interfaces over local + remote data | `app/src/main/java/com/clibeats/domain/repository/` |
| Repository Impls (`data/repository/*` | Room/IO-backed implementations | `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt` |
| CliBeatsDatabase | Room database: 6 entities, 5 DAOs | `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt` |
| CacheManager | Offline audio cache (`audio_cache/`) + index in Room | `app/src/main/java/com/clibeats/data/cache/CacheManager.kt` |
| TrackDownloadManager | Stream → disk downloads with per-track status flow | `app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt` |
| PlayerAdapter | ExoPlayer wrapper: queue, transport controls, `PlaybackState` flow | `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt` |
| PlaybackService | Foreground `MediaSessionService` hosting the MediaSession | `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt` |
| buildApp | Fastify app factory: plugins, DI decoration, routes, hooks, error handler | `gateway/src/app.ts` |
| ProviderRegistry | In-memory `Map<string, ProviderAdapter>`; priority sort | `gateway/src/core/registry/ProviderRegistry.ts` |
| ProviderSelectionEngine | Scores providers (priority, capability, health, circuit state); failover executor | `gateway/src/core/selection/ProviderSelectionEngine.ts` |
| CircuitBreaker | Per-provider CLOSED → OPEN → HALF_OPEN state machine | `gateway/src/core/circuit/CircuitBreaker.ts` |
| CacheManager | Owns 7 segregated Redis-backed caches | `gateway/src/core/cache/CacheManager.ts` |
| RedisCacheBase | Fail-open Redis primitive; `clibeats:<ns>:<key>` namespacing | `gateway/src/core/cache/RedisCacheBase.ts` |
| EventBus | Singleton `EventEmitter`; decouples metrics/logging from flow | `gateway/src/core/events/EventBus.ts` |
| RedisHealthChecker | PING-with-timeout health probe | `gateway/src/core/health/RedisHealthChecker.ts` |
| Provider adapters | `ProviderAdapter` impls: `MockProviderAdapter`, `YouTubeProviderAdapter` | `gateway/src/providers/mock/`, `gateway/src/providers/youtube/` |

## Pattern Overview

**Android side:** Clean Architecture (MVVM) — one package tree per layer (`presentation` / `domain` / `data`), domain interfaces defined at `domain/repository` and `domain/provider`, boundaries crossed only via interfaces bound by Hilt. UI is single-activity Compose with a hand-rolled "TUI" aesthetic (`presentation/component/TuiBlock.kt`, `TuiTabBar.kt`). State flows are `StateFlow` exposed by ViewModels and repositories; the playback state is a `StateFlow<PlaybackState>` surfaced through `PlayerAdapter`.

**Gateway side:** Fastify plugin composition + layered "core" services with a config-driven provider plugin system. Every provider implements the same `ProviderAdapter` interface (`gateway/src/types/adapter.ts`), so the transport layer (`app.ts`) never touches provider internals — it goes through `ProviderSelectionEngine.executeWithFailover` which handles scoring, circuit breaking, and failover. Cross-cutting concerns (metrics, logging) subscribe to a shared `EventBus` instead of being called inline — the bus is the only non-imperative coupling.

**Key Characteristics:**
- Two independent module trees; the Android `data` layer is the only consumer of the gateway HTTP contract (`app/src/main/AndroidManifest.xml` declares INTERNET + media-playback foreground service).
- Dependency inversion is enforced by convention + a custom detekt `ForbiddenImport` rule; many files carry `@file:Suppress("ForbiddenImport")` with explanatory comments because the rule's default wildcard scope (`com.clibeats.data.*`) is over-broad.
- Full-stack seam: Android `MusicProvider` ↔ `GatewayApi` (Retrofit) ↔ Fastify routes ↔ `ProviderAdapter`.

## Layers

### Android — Presentation
- **Purpose:** React to user intent, render UI state
- **Location:** `app/src/main/java/com/clibeats/presentation/`
- **Contains:** `home/`, `search/`, `library/`, `playlist/`, `queue/`, `player/`, `settings/`, `more/`, plus `component/` (TUI primitives), `layout/` (shell + nav), `theme/` (CliBeats color/type/shape)
- **Depends on:** `domain` interfaces + `PlayerViewModel` (via `PlaybackRepository`)
- **Never touches:** `data` classes directly — ViewModels inject repository/provider interfaces

### Android — Domain
- **Package:** `app/src/main/java/com/clibeats/domain/`
- **Contains:** Pure Kotlin models (`model/Track.kt`, `PlaybackState.kt`, ...), repository interfaces (`repository/`), provider contract (`provider/MusicProvider.kt` + `ProviderResult` sealed class), queue state machine (`playback/QueueManager.kt`)
- **Depends on:** nothing infrastructure-level (one deliberate exception: `HistoryRepository` returns the Room entity `HistoryEntity` — see `HistoryRepository.kt`)

### Android — Data
- **Package:** `app/src/main/java/com/clibeats/data/`
- **Contains:** `repository/` impls, `local/` (Room DAO/entity/mapper), `gateway/` (Retrofit API + DTOs + mappers), `cache/` (file cache), `download/`, `network/` (NetworkMonitor), `preferences/` (DataStore + encrypted prefs)
- **Depends on:** Retrofit/OkHttp, Room, DataStore

### Android — Playback
- **Package:** `app/src/main/java/com/clibeats/playback/`
- **Contains:** `PlayerAdapter.kt` (ExoPlayer wrapper, injectable singleton), `service/PlaybackService.kt` (MediaSessionService bridging ExoPlayer to the OS media notification)
- **Depends on:** Media3, `data/cache/CacheManager`, `domain/model`, Hilt (`lateinit var player: ExoPlayer` injected in service)

### Gateway — Routing (Thin)
- **Location:** `gateway/src/app.ts`
- **Purpose:** Decorations expose `config`, `registry`, `engine`, `cache`, `health` on the Fastify instance; every segment route is a generic case of "check supported cache → fall through to `engine.executeWithFailover(...)` → set cache → respond"

### Gateway — Orchestration
- **Location:** `gateway/src/core/selection/ProviderSelectionEngine.ts`
- **Purpose:** The only entry point adapters are invoked through. Computes a numeric score per provider = priority + capability bonus + health score + latency adjust (with a hard `-1000` for open circuit or missing capability), ranks, then executes candidates in order, tracking `recordSuccess()/recordFailure()` on the winning/losing circuit breakers and emitting `PROVIDER_*` events

### Gateway — Core Services
- **Location:** `gateway/src/core/`
- **Contains:** `registry/`, `circuit/`, `cache/` + `cache/segregated/*Cache.ts`, `events/EventBus.ts`, `health/RedisHealthChecker.ts`, `logging/logger.ts`, `metrics/metrics.ts`

### Gateway — Providers
- **Location:** `gateway/src/providers/`
- **Contains:** one directory per provider (`mock/`, `youtube/`), each exposing a class implementing `ProviderAdapter`; registration is centralized in `gateway/src/providers/registerProviders.ts` and driven by `gateway/config/gateway.yaml` (`providers.<id>.enabled|priority`)

## Data Flow

### Primary Request Path (Android — Playback-driven)

1. **UI intent** — `MainActivity.kt` passes `onTrackClick` so `HomeScreen`/`SearchScreen` call `playerViewModel.playTrack(track)` (`MainActivity.kt:50`, `PlayerViewModel.kt:40`).
2. **ViewModel bridge** — `PlayerViewModel` delegates to `PlaybackRepository.playTrack()` (`app/src/main/java/com/clibeats/presentation/player/PlayerViewModel.kt:40`).
3. **PlaybackRepository** — `PlaybackRepositoryImpl.playTrack()` (`app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt:35`) launches a Main coroutine, calls `ensureStreamUrl(track)` (delegates to `MusicProvider.stream(trackId)` if URL is blank — `PlaybackRepositoryImpl.kt:65`) and then `playerAdapter.playTrack(resolvedTrack)`.
4. **PlayerAdapter → ExoPlayer** — `PlaybackRepositoryImpl.kt:46` → `PlaybackAdapter.playTrack()` (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:141`): builds `MediaItem` via `Track.toMediaItem()` (prefers cached file from `CacheManager.getCachedFileDirect(id)`; else `streamUrl`), `player.setMediaItem/prepare/play`.
5. **State propagation** — ExoPlayer listener + 500ms ticker coroutine drive `updateState()` (`PlayerAdapter.kt:229`) which writes `_playbackState` (position, buffered, repeat, shuffle) and `_queueFlow`. Consumers collect via `PlayerViewModel.playbackState` and `MainLayout`'s `PlayerBar` (`MainLayout.kt:58`).
6. **Offline route** — downloaded audio bypasses the gateway: `TrackDownloadManager` (`data/download/TrackDownloadManager.kt`) streams over OkHttp into `CacheManager`, which next time makes `toMediaItem()` use the local file.

### External Request Path (Android → Gateway)

1. **SearchViewModel** (`app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt:34`) `debounce(300).flatMapLatest` → `MusicProvider.search()`.
2. `GatewayMusicProvider.search()` (`app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt:32`) calls `api.search(query, filterSongs=true)` on the Retrofit `GatewayApi` (`data/gateway/api/GatewayApi.kt:21`).
3. Retrofit (base `http://10.0.2.2:8080/` from `BuildConfig.GATEWAY_BASE_URL` in `app/build.gradle.kts:26`) hits `GET /api/v1/search`.
4. **Gateway** (`gateway/src/app.ts:207`): `cache.search.get(query)` → on miss `engine.executeWithFailover('search', context, adapter => adapter.search(...))` → `searchLatencyHistogram` → `cache.search.set(...)` → returns `{ tracks, cached:false }`.
5. DTOs map back in `data/gateway/mapper/GatewayMapper.kt` (`toDomainTrack`) and flow into `SearchUiState.Success`.

### Gateway — Stream Resolution

- `POST /api/v1/stream` — `app.ts:281`: `engine.executeWithFailover('playback', context, (a) => a.stream(trackId, context))`; emits `STREAM_RESOLVED`. Backed by `ProviderStreamResolver` in `providers/youtube/YouTubeProviderAdapter.stream()` (adaptive-format pick, descrambled URL, expiry).

### Gateway — Failover

- **Path:** `ProviderSelectionEngine.executeWithFailover` (`gateway/src/core/selection/ProviderSelectionEngine.ts:94`): compute per-candidate score → try candidate; on failure `cb.recordFailure()` + emit `PROVIDER_FAILED`, move to next; if all fail and last error is `ProviderError` rethrow it, else throw `InternalError`. Failover to next provider is emitted as `PROVIDER_FAILOVER`.

## State Management

- **Android:** Most state is in-memory `StateFlow` (ViewModels, `PlayerAdapter`, `QueueManager`). Persisted state: Room (`CliBeatsDatabase`) for songs/playlists/history/cache index; DataStore (`data/preferences/AppPreferences.kt`) for settings; EncryptedSharedPreferences for auth token (`data/di/StorageModule.kt`).
- **Gateway:** Stateless beyond Redis caches; the only process memory is `ProviderRegistry` adapter instances and per-provider `CircuitBreaker` instances inside `ProviderSelectionEngine` (keyed by providerId).

## Key Abstractions

**`MusicProvider` (Android):**
- Purpose: decouples search/stream from provider implementation
- Examples: `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt` (interface), `GatewayMusicProvider.kt` (only impl)
- Pattern: `suspend fun` returning `ProviderResult<T>` sealed Success/Error/Loading

**`ProviderAdapter` (Gateway):**
- Purpose: the canonical provider contract; every provider exposes identical operations
- Examples: `gateway/src/types/adapter.ts` (interface), `gateway/src/providers/mock/MockProviderAdapter.ts`, `gateway/src/providers/youtube/YouTubeProviderAdapter.ts`
- Pattern: `search | stream | album | artist | playlist | healthCheck`, all returning gateway domain types (Track/Album/Artist/Playlist/StreamResult — `gateway/src/types/domain.ts`)

**`PlaybackRepository` (both):**
- Purpose: normalize transport (ExoPlayer) behind the domain contract
- Examples: interface `app/src/main/java/com/clibeats/domain/repository/PlaybackRepository.kt`, impl `app/.../data/repository/PlaybackRepositoryImpl.kt`

**EventBus (Gateway):**
- Purpose: fire-and-forget domain events consumed by metrics + logger
- Examples: `gateway/src/core/events/EventBus.ts`, subscribers in `gateway/src/core/metrics/metrics.ts` and `gateway/src/core/logging/logger.ts`
- Pattern: `emitEvent(payload)` → `emit(type)` + `emit('*')`; types in `GatewayEventPayload` union

## Entry Points

**Android:**
- `app/src/main/AndroidManifest.xml` — declares `MainActivity` (launcher) and `PlaybackService` (MediaSessionService, foregroundType `mediaPlayback`)
- `app/src/main/java/com/clibeats/CLIBeatsApp.kt` — `@HiltAndroidApp`
- `app/src/main/java/com/clibeats/MainActivity.kt` — Compose root

**Gateway:**
- `gateway/src/server.ts` — bootstrap: `buildApp()`, listen, SIGTERM/SIGINT graceful shutdown
- `gateway/src/app.ts` — `buildApp()` factory used by both production and tests (`tests/integration/api.test.ts` builds in the same way)
- Root scripts: `npm run dev` (ts-node-dev), `npm start` (dist), `npm run openapi:generate/validate`

## Architectural Constraints

- **Android UI:** single-activity, no Compose NavController — navigation is manual `selectedDestination` state switched in `MainActivity.kt` (`mutableStateOf<NavDestination>(NavDestination.Home)`) through `MainLayout` (`MainLayout.kt:67` `NavigationSuiteScaffold`); `NavDestination` sealed class in `layout/NavDestination.kt`.
- **Threading (app):** Main-safe only: all ExoPlayer/high-level calls on main dispatcher (repositoryScope), IO for network via `withContext(Dispatchers.IO)`; `TrackDownloadManager` uses its own IO scope for OkHttp streaming.
- **Gateway:** single-threaded event loop (Fastify); heavy work happens inside provider promises (youtubei.js). `CircuitBreaker` state is in-memory mutable — restart resets it.
- **Config-driven providers:** an adapter is only registered when `gateway.yaml` says so (registerProvider flow). Provider defaults: Mock enabled priority 100, YouTube disabled (must set `enabled: true`).
- **Global state:** `globalEventBus` (singleton) is process-wide; `PlayerAdapter` is `@Singleton` (Android). Any other global mutable state is accidental.
- **Error contract:** all gateway failures are `ProviderError` subtypes (types/error.ts) mapped to HTTP status codes by the singleton error handler in `app.ts` (`setErrorHandler`), including a `INVALID_REQUEST` branch for schema validation failures and a generic `INTERNAL_ERROR`.

## Anti-Patterns

### ForbiddenImport Suppression as a Norm

**What happens:** `@file:Suppress("ForbiddenImport")` appears widely (data, DI, playback, cache folders) because a detekt rule flags legitimate `com.clibeats.data.*` imports.

**Why it's wrong:** file-level suppression per-file loses the rule's protection for accidental cross-layer imports in the file's remainder.

**Do this instead:** If a violation is intentional, prefer targeted annotations/import-blacklist refinement in `config/detekt/detekt.yml` over file-wide suppression.

### Placeholder Providers

**What happens:** `GatewayMusicProvider.getTrack()` returns a placeholder Error and `playlists()`/`queue()` return `emptyList()` (`app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt:47-75`). Nothing fails, but features silently "work" with empty/error results.

**Why it's wrong:** silent partial API surface; callers can't distinguish "empty" from "unimplemented".

**Do this instead:** mark those methods with `TODO`/UnsupportedOperationException or remove until wired to gateway endpoints (`/api/v1/playlist` exists on the gateway already).

### Fastify "DI" Tightly Bound in the Root App

**What happens:** `app.decorate('config'|'registry'|'engine'|'cache'|'health')` + a module-augmentation in `app.ts` makes the whole app depend on `buildApp`'s composition root; any new core service requires editing `app.ts`.

**Why it's wrong:** limits testability (each suite must fully re-assemble) and IDE type safety relies on re-declaration.

**Do it (alternative):** prefer passing services into route handlers via function closures or an explicit `AppServices` object rather than relying on `FastifyInstance` string decoration.

### `Auto Refresh` Stream URL Refresh Not Enforced

**What happens:** `StreamResult.expiresAtEpochSeconds` is emitted, and `config.stream.urlRefreshBufferSeconds` exists, but the app never re-resolves a URL from the gateway before expiry (PlaybackRepository only uses a `MusicProvider.stream` URL once at play time — `PlaybackRepositoryImpl.ensureStreamUrl`).

**Impact:** long-running listen sessions can play a redirecting/expired URL ($ admin → playback failure mid-track).

**Fix direction:** `Player` should schedule a refresh before `expiresAtEpochSeconds - urlRefreshBufferSeconds` and re-resolve via `MusicProvider.stream(track.id)` (mirroring the gateway feature flag `streamUrlAutoRefresh: true` in `GET /api/v1/bootstrap`).

## Error Handling

**Android:** Wrapper-sealed `ProviderResult` (Success/Error/Loading). Repository functions are the fail-safe: network errors surface inside `ProviderResult.Error` (mapped via `GatewayErrorMapper` — `data/gateway/mapper/GatewayErrorMapper.kt`) rather than throwing; playback failures go through `StructuredLogger` (`core/logging/StructuredLogger.kt`) with a `PlaybackEvent.Failure` event. HttpException body parsing is guarded by `runCatching`.

**Gateway:** Every failure from adapters or core is an `ProviderError` subtype (`types/error.ts`) — `RateLimitedError(429)`, `GeoBlockedError(403)`, `NotFoundError(404)`, `NetworkError(503)`, `TimeoutError(504)`, etc. The single `setErrorHandler` in `app.ts` serializes them into the `{error:{code,message,providerId,retryAfterSeconds,traceId}}` envelope. Non-`ProviderError` 4xx → `INVALID_REQUEST`, everything else → `INTERNAL_ERROR` 500. `CircuitBreaker.recordFailure()` + failover covers provider-level outage.

## Cross-Cutting Concerns

**Logging (Android):** `TimberTelemetryTracker` + `StructuredLogger`, telemetry interfaces in `com.clibeats.telemetry`; `PlaybackEvent` sealed events for stage breakdown.
**Logging (Gateway):** pino JSON logger (`core/logging/logger.ts`), tagged `service: clibeats-gateway`, trace IDs from `x-trace-id` header auto-added by `onRequest` hook and echoed back in `onSend`.
**Metrics (Gateway):** prom-client `register`; counters for requests/cache-hits/misses, provider selections/failures; gauges for provider health + circuit breaker state; histograms for search & stream latency. Auto-derived from EventBus events.
**Validation:** Gateway uses Fastify JSON-schema validation (`schemas.ts`, 9 schemas: bootstrap/search/album/artist/playlist/stream/health/version/providers/metrics). Android uses `kotlinx.serialization` with `ignoreUnknownKeys`.
**Auth/Bootstrap:** No gateway auth token guard (only `authenticated: authorization header != null` in `getContext`). The `/api/v1/bootstrap` endpoint returns gateway/API versions + provider healths + `minimumAndroidVersion` + streaming feature flags (ADR-020).

---

*Architecture analysis: 2026-08-08*