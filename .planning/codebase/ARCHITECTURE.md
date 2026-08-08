# Architecture

**Analysis Date:** 2026-08-09

## Pattern Overview

**Overall:** Two-component system — a native Android client (Clean Architecture + MVVM) talking to a provider-agnostic Fastify gateway (plugin/provider architecture). The gateway owns ALL YouTube-facing logic (ADR-012 gateway provider architecture, ADR-013 provider plugin architecture, ADR-014 capability negotiation); the Android app contains zero direct YouTube/InnerTube code.

**Key Characteristics:**
- Provider plugin architecture: adapters registered config-driven, selected by capability score, failover across providers, per-provider circuit breakers
- Layered Clean Architecture on Android: `presentation` → `domain` → `data`, enforced by Detekt `ForbiddenImport` rules (presentation must not import `com.clibeats.data.*`)
- REST contract between client and gateway with JSON Schema/OpenAPI validation
- Event-driven observability on the gateway: internal EventBus feeds both pino logs and Prometheus metrics
- Cache fail-open design: Redis errors never fail requests
- Background media playback via Media3 MediaSessionService with foreground service type mediaPlayback

## Layers

### Android — Presentation Layer
- Purpose: Compose UI, ViewModels, navigation state
- Contains: Screens (`presentation/home|search|library|playlist|queue|settings|more/**`), ViewModels with `UiState` data classes, theme (`CliBeatsColors/Typography/Shapes/Theme`), shared components (`PlayerBar`, `SongTableRow`)
- Location: `app/src/main/java/com/clibeats/presentation/**`
- Depends on: `domain` (models, repositories, playback state) via ViewModels; Hilt-injected
- Used by: `MainActivity` (single-activity Compose app, manual destination switching via `NavDestination` enum — no Navigation library)

### Android — Domain Layer
- Purpose: Pure business models and contracts, no Android/network dependencies
- Contains: `model/` (Track, Album, Artist, Playlist, PlaybackState), `provider/MusicProvider` + `ProviderResult` interface, `repository/` interfaces (Song, Playlist, History, Playback), `playback/QueueManager`
- Location: `app/src/main/java/com/clibeats/domain/**`
- Depends on: nothing external (pure Kotlin)
- Used by: presentation and data layers

### Android — Data Layer
- Purpose: Gateway communication, local persistence, cache, downloads, preferences
- Contains: `gateway/` (Retrofit `GatewayApi`, DTOs, `GatewayMusicProvider` implementation, mappers), `local/` (Room DB, entities, DAOs, mappers), `cache/CacheManager` (disk cache for offline), `download/TrackDownloadManager`, `preferences/AppPreferences`, `network/NetworkMonitor`
- Location: `app/src/main/java/com/clibeats/data/**`
- Depends on: domain contracts; Hilt modules in `di/**`; Retrofit/OkHttp/Room/DataStore
- Used by: domain? No — bound to domain repository interfaces via Hilt

### Android — Playback Engine
- Purpose: Media3 ExoPlayer wrapper exposing reactive state
- Contains: `PlayerAdapter` (singleton wrapper around ExoPlayer, StateFlow of `PlaybackState` + queue), `service/PlaybackService` (MediaSessionService), `QueueManager`
- Location: `app/src/main/java/com/clibeats/playback/**`
- Depends on: domain models, `data/cache/CacheManager` (offline file preference over stream URL)
- Used by: ViewModels (PlayerViewModel) and the player bar

### Gateway — HTTP / API Layer
- Purpose: Fastify server, route handlers, schema validation, error mapping
- Contains: `app.ts` (all route handlers + global hooks/error handler), `schemas.ts` (JSON Schema per route), `server.ts` (bootstrap/shutdown)
- Depends on: core services (registry, engine, cache, health) via Fastify decoration (lightweight DI)
- Used by: Android client, `/documentation` UI, `/health`, `/metrics`

### Gateway — Core Services
- Purpose: Provider lifecycle, selection, caching, resilience, observability
- Contains: `registry/ProviderRegistry`, `selection/ProviderSelectionEngine`, `circuit/CircuitBreaker`, `cache/CacheManager` + segregated caches, `events/EventBus`, `health/RedisHealthChecker`, `metrics/metrics`, `logging/logger`
- Location: `gateway/src/core/**`
- Depends on: provider adapters (via registry), Redis, ioredis, prom-client, pino
- Used by: API routes in `app.ts`

### Gateway — Provider Adapters (plugins)
- Purpose: Translate provider-specific APIs into canonical domain types + capability-negotiated methods
- Contains: `providers/youtube/YouTubeProviderAdapter` (+ `media.ts` raw parsers, `poToken/mint.ts`, `ProviderTokenService.ts`), `providers/mock/MockProviderAdapter` (seeded fake dataset + failure simulators), `registerProviders.ts` (config-driven registration from gateway.yaml)
- Depends on: `types/` (adapter contract, domain models, capabilities, errors, context)
- Used by: ProviderSelectionEngine

## Data Flow

**Search Request (Android → Gateway → YouTube):**
1. User types in `SearchScreen` → `SearchViewModel` calls repository
2. `SongRepositoryImpl` → `GatewayMusicProvider.search()` → Retrofit `GatewayApi.search(q)` with `x-trace-id`
3. Gateway `/api/v1/search` checks Redis search cache; on miss calls `ProviderSelectionEngine.executeWithFailover('search', ...)`
4. Engine scores each registered adapter (priority + capability + health), sorts, then invokes the winner's `search()`
5. `YouTubeProviderAdapter` calls `yt.music.search()` (youtubei.js), parses raw sections into canonical `Track[]`
6. Result cached in Redis (1h), returned as `{ tracks, cached: false }`
7. Android mappers convert DTOs → domain `Track` models for Compose

**Playback Request (Stream Resolution):**
1. User taps a track → `PlayerViewModel.playTrack()` → `GatewayMusicProvider.stream(trackId)` → `POST /api/v1/stream`
2. Gateway resolves stream URL via failover engine (`adapter.stream()`), getting PO token from `ProviderTokenService` (auto-minted via BotGuard/WAA, refreshed before expiry)
3. If `proxyStreaming: true`, the gateway rewrites `streamUrl` to its own `/api/v1/stream/proxy/:trackId` relay
4. Client sets MediaItem; ExoPlayer issues Range requests against the proxy; proxy probes CDN (bytes=0-0) once, caches total size in-memory, then relays bytes 206 partial content
5. `PlayerAdapter` exposes StateFlow updates (position ticker every 500ms) → PlayerBar/PlayerScreen UI

**State Management:**
- Android: `StateFlow`-driven MVVM (`*UiState` data classes); queue + playback state in singleton `PlayerAdapter`
- Gateway: stateless request handling; ephemeral state = Redis cache + in-memory CDN URL probe cache + circuit breaker state (per-process)

## Key Abstractions

**ProviderAdapter (gateway):**
- Purpose: Canonical contract every provider must implement — `search/stream/album/artist/playlist/healthCheck` + `capabilities`
- Examples: `YouTubeProviderAdapter`, `MockProviderAdapter`
- Pattern: Strategy/plugin, registered in `ProviderRegistry` from config (`gateway.yaml`)

**ProviderSelectionEngine:**
- Purpose: Score providers (priority + capability bonus + health) and execute with automatic failover; reuses per-provider `CircuitBreaker`
- Examples: `executeWithFailover('search' | 'playback' | 'albums' | 'artists' | 'playlists', context, op)`
- Pattern: Strategy + Failover loop

**ProviderError hierarchy (gateway):**
- Purpose: Canonical error model (ADR-016) — typed codes (`RATE_LIMITED`, `GEO_BLOCKED`, `NOT_FOUND`, `NETWORK_ERROR`, ...) mapped to HTTP status codes + `retryAfterSeconds`; surface mapping in `app.ts` error handler
- Examples: `RateLimitedError`, `GeoBlockedError`, `NotFoundError`, `PlaybackError`
- Pattern: Typed exception hierarchy

**MusicProvider (Android):**
- Purpose: Domain contract for music capabilities used by the app; implemented by gateway-backed provider
- Example: `GatewayMusicProvider`
- Pattern: Interface + adapter (future providers implement same interface)

**ProviderResult (Android):**
- Purpose: Kotlin sealed result type (`Success`/`Error`) avoiding exceptions across layers; `GatewayErrorMapper` converts gateway errors → user messages
- Pattern: Result monad

**EventBus (gateway):**
- Purpose: Internal pub/sub for lifecycle events (`REQUEST_RECEIVED`, `CACHE_CHECKED`, `PROVIDER_SELECTED`, `CIRCUIT_TRIPPED`, ...) consumed by logger + Prometheus metrics — decouples observability from call sites
- Pattern: Singleton EventEmitter

## Entry Points

**Android:**
- `CLIBeatsApp.onCreate()` — Hilt application init
- `MainActivity` — single activity, Compose `setContent`, destination switching
- `PlaybackService` — Media3 `MediaSessionService`, declared in manifest with `foregroundServiceType="mediaPlayback"`

**Gateway:**
- `gateway/src/server.ts` — `start()` builds app, listens, graceful SIGTERM/SIGINT shutdown
- `gateway/src/app.ts` `buildApp()` — constructs Redis, cache, registry, engine, health checker; registers CORS, Swagger, providers; wires hooks; defines all routes; global error handler
- Docker CMD: `node dist/server.js` (compiled via `npm run build`)

## Error Handling

**Strategy:** Gateway — canonical typed errors thrown from adapters, caught by a single global Fastify error handler that maps to the `{ error: { code, message, providerId, retryAfterSeconds, traceId } }` JSON shape (ADR-016). Android — `ProviderResult.Success/Error` returned across layers; no exceptions leak to UI.

**Patterns:**
- Adapters wrap unknown errors into typed `ProviderError` subclasses via regex classification (`YouTubeProviderAdapter.errorCode()`, `MockProviderAdapter.simulateFailure()`)
- Failover engine: last `ProviderError` propagates; otherwise `InternalError("All provider failover candidates failed...")`
- 4xx schema-validation errors mapped to `INVALID_REQUEST`; everything else 500 `INTERNAL_ERROR`
- Android: `runCatching { }` around network calls with `GatewayErrorMapper` message extraction; graceful `provider_offline` UI state (`HomeScreen`)
- `PlaybackService` releases player on destroy; `PlayerAdapter` logs diagnostics on `PlayerError` with HTTP status extraction

## Cross-Cutting Concerns

**Logging:**
- Gateway: pino JSON logs, ISO timestamps, traceId correlation on request/response hooks (`gateway/src/core/logging/logger.ts`)
- Android: `android.util.Log` diagnostic tags + `StructuredLogger` abstraction for telemetry trackers

**Validation:**
- Gateway: Fastify JSON Schema per route (`schemas.ts`), OpenAPI spec generation + CI validation
- Android: Room DAO constraints, mapper null-safety; UI state validation in ViewModels

**Authentication:**
- None enforced. `Authorization` header presence only flips `context.authenticated`; PO tokens are the anti-bot mechanism for YouTube, not user auth

**Observability:**
- `/metrics` Prometheus (counters/gauges/histograms via EventBus), `/health` aggregate (gateway, redis, providers), `/version`, trace IDs on every response

**Security:**
- EncryptedSharedPreferences (Keystore AES256_GCM) for sensitive tokens; cleartext HTTP restricted to dev hosts via network security config; R8 minification + ProGuard rules in release

---

*Architecture analysis: 2026-08-09*
*Update when major patterns change*
