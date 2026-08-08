<!-- refreshed: 2026-08-09 -->
# Architecture

**Analysis Date:** 2026-08-09

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                              HTTP CLIENT                                    │
│                        (Android / iOS / Web)                                │
└──────────────────────────────┬───────────────────────────────────────────────┘
                               │  HTTPS REST + x-trace-id
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FASTIFY HTTP ENTRY POINT                            │
│                       `src/server.ts` · `src/app.ts`                        │
│  - Signal handling (SIGTERM/SIGINT)            - onRequest/onResponse/onSend│
│  - buildApp() factory                           - Trace-ID + structured log  │
│  - Routes: bootstrap, search, album, artist,     - Global error handler      │
│    playlist, stream, stream/proxy, /health,      - Swagger/OpenAPI (tags)    │
│    /metrics, /version, /api/v1/providers         - CORS plugin              │
└──────────────────────────────┬───────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REQUEST DISPATCH (app.ts route handlers)                 │
│  Per-route cache check → engine.executeWithFailover → cache write → reply   │
├─────────────────────────────────────────────────────────────────────────────┤
│                          ProviderSelectionEngine                            │
│                 `src/core/selection/ProviderSelectionEngine.ts`             │
│  - Scores adapters (priority + capability + health + latency)              │
│  - Circuit-breaker gating per provider                                     │
│  - Ordered failover loop with event emission                                │
│  - Throws InternalError / ProviderError on total failure                    │
└──────┬────────────────────────────┬───────────────┬───────────┬──────────────┘
       │                            │               │           │
       ▼                            ▼               ▼           ▼
┌──────────────┐        ┌────────────────┐  ┌────────────┐  ┌─────────────┐
│  CacheLayer   │        │  CircuitBreaker│  │  EventBus   │  │   Metrics    │
│ `CacheManager`│        │ `CircuitBreaker`│  │ `EventBus`  │  │ `metrics.ts` │
│ (Redis-backed)│        │  per provider  │  │(EventEmitter│  │ (prom-client)│
└─┬────────────┘        └────────────────┘  └────────────┘  └─────────────┘
  │
  ▼
┌────────────────────────────────────┐  ┌─────────────────────────────────────────┐
│      Segregated Cache Types         │  │              Redis                       │
│  `cache/segregated/*`               │  │  `redis://localhost:6379` (config)     │
│  - SearchCache (1h TTL)            │  │  ioredis client (fail-open on error)    │
│  - AlbumCache (24h)                 │  │  ioredis-mock in NODE_ENV=test          │
│  - ArtistCache (24h)                │  └─────────────────────────────────────────┘
│  - PlaylistCache (24h)              │
│  - SessionCache (no TTL, explicit)  │
│  - ArtworkCache (7d)                │
│  - HealthCache (5min)               │
└────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PROVIDER ADAPTER INTERFACE                               │
│                   `src/types/adapter.ts`                                    │
│  ProviderAdapter { id, name, priority, capabilities,                      │
│    search(), stream(), album(), artist(), playlist(), healthCheck() }       │
├──────────────────────┬───────────────────────────────────────────────────────┤
│ ProviderRegistry     │                     Provider Plugins                   │
│ `ProviderRegistry`   │   `src/providers/mock/`   `src/providers/youtube/`    │
│  - register()        │   MockProviderAdapter     YouTubeProviderAdapter     │
│  - get() / getAll()  │    (priority 42 default)   (priority 60 default)     │
│  - getSortedByPriority()                                                      │
│  - clear()                                                                      │
├──────────────────────┴───────────────────────────────────────────────────────┤
│                         Config Management                                     │
│                       `src/config/config.ts`                                 │
│   YAML config → `config/gateway.yaml` · env overrides (PORT, REDIS_URL)       │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `server.ts` | Process entry point; creates app, listens, handles SIGTERM/SIGINT | `src/server.ts` |
| `app.ts` | Fastify instance factory; registers routes, plugins, hooks, DI decorations | `src/app.ts` |
| `schemas.ts` | JSON Schema definitions for all route I/O + OpenAPI tags | `src/schemas.ts` |
| `config.ts` | Loads `gateway.yaml`, applies env overrides, returns `GatewayConfig` | `src/config/config.ts` |
| `ProviderRegistry` | Stores and retrieves provider adapters; priority-sorted access | `src/core/registry/ProviderRegistry.ts` |
| `ProviderSelectionEngine` | Scores adapters, gates via circuit breaker, orchestrates failover | `src/core/selection/ProviderSelectionEngine.ts` |
| `CircuitBreaker` | Per-provider CLOSED/OPEN/HALF_OPEN state machine with threshold + cooldown | `src/core/circuit/CircuitBreaker.ts` |
| `CacheManager` | Facade composing all segregated cache types; owns Redis connection | `src/core/cache/CacheManager.ts` |
| `RedisCacheBase` | Abstract fail-open Redis primitive (safeGet/safeSet/safeDel/invalidate) | `src/core/cache/RedisCacheBase.ts` |
| `EventBus` | Global EventEmitter emitting typed `GatewayEventPayload` events | `src/core/events/EventBus.ts` |
| `metrics.ts` | Prometheus registry + counters/gauges/histograms; EventBus listener wiring | `src/core/metrics/metrics.ts` |
| `RedisHealthChecker` | Ping-based Redis health probe with configurable timeout | `src/core/health/RedisHealthChecker.ts` |
| `logger.ts` | Pino structured logger; EventBus listener for event logging | `src/core/logging/logger.ts` |
| `registerProviders.ts` | Bootstraps concrete adapters from config (Mock + YouTube) | `src/providers/registerProviders.ts` |
| `YouTubeProviderAdapter` | youtubei.js-based adapter for search/stream/album/artist/playlist | `src/providers/youtube/YouTubeProviderAdapter.ts` |
| `MockProviderAdapter` | Seeded synthetic provider with configurable failure states | `src/providers/mock/MockProviderAdapter.ts` |
| `media.ts` | YouTube response normalization utilities | `src/providers/youtube/media.ts` |

## Pattern Overview

**Overall:** Hexagonal / Ports-and-Adapters gateway pattern with a layered dependency graph (`types` → `core` → `providers` → `app`). The gateway is a provider-agnostic API facade that translates client requests into adapter-specific calls, shielding clients from provider SDK details.

**Key Characteristics:**
- **Provider-agnostic contract:** The `ProviderAdapter` interface (`src/types/adapter.ts`) is the single port; each provider implements it as a plugin (YouTube, Mock).
- **Fail-open philosophy:** Redis outages degrade to cache-miss instead of crashing requests; circuit breakers trip instead of hanging.
- **Single source of truth at rest:** All domain types and error codes live in `src/types/` as leaf modules with no internal imports.
- **Event-driven cross-cutting concerns:** A single `globalEventBus` (`src/core/events/EventBus.ts`) fans out to both the metrics registrar and the structured logger — observability concerns are decoupled from request handlers.
- **Config-driven provider discovery:** Providers are only registered when their `enabled` flag is not explicitly `false` in `config/gateway.yaml` (`src/providers/registerProviders.ts:24,29`).

## Layers

**`types` (leaf):**
- Purpose: Shared domain models, adapter interface, capabilities, context, and error hierarchy with zero internal imports.
- Location: `src/types/`
- Contains: `adapter.ts` (ProviderAdapter interface + AdapterHealth), `capabilities.ts`, `context.ts`, `domain.ts`, `error.ts` (ProviderError hierarchy + 9 subclasses), `declarations.d.ts`
- Depends on: nothing internal
- Used by: every other layer

**`core` (infrastructure):**
- Purpose: Reusable cross-cutting infrastructure: caching, circuit-breaking, event bus, health, logging, metrics, provider registry, and selection engine.
- Location: `src/core/`
- Contains: `cache/`, `circuit/`, `events/`, `health/`, `logging/`, `metrics/`, `registry/`, `selection/`
- Depends on: `types/`, external packages (`ioredis`, `prom-client`, `pino`)
- Used by: `app.ts`, `registerProviders.ts`
- Constraint (enforced by `tests/architecture/layers.test.ts:41`): **core never imports from providers**.

**`providers` (plugins):**
- Purpose: Concrete provider adapter implementations.
- Location: `src/providers/`
- Contains: `registerProviders.ts` (bootstrap/compose), `mock/MockProviderAdapter.ts`, `youtube/YouTubeProviderAdapter.ts`, `youtube/media.ts`
- Depends on: `types/`, `core/` (only via `registerProviders.ts`; leaf adapters depend only on `types/`)
- Used by: `app.ts` via `registerProviders()`
- Constraint (enforced by `tests/architecture/layers.test.ts:54`): **provider plugins (except registerProviders) never import from core, config, or app** — they are leaf adapters that only depend on `types/`.

**`config`:**
- Purpose: Configuration loading and typing.
- Location: `src/config/config.ts`, runtime config `config/gateway.yaml`
- Depends on: external packages (`fs`, `path`, `yaml`) only — verified by `tests/architecture/layers.test.ts:80`

**`app` (composition root):**
- Purpose: Wires all layers together — creates Redis client, CacheManager, Registry, Engine, HealthChecker; registers Fastify plugins, hooks, schemas, and routes.
- Location: `src/app.ts`
- Depends on: all layers

## Data Flow

### Primary Request Path

1. **HTTP request enters** — `src/server.ts:7` calls `buildApp()`, which calls `app.listen()` at `src/app.ts:13`
2. **onRequest hook (trace ID)** — `src/app.ts:113` generates or echoes `x-trace-id`, logs the incoming request, emits `REQUEST_RECEIVED` event
3. **Route handler** — e.g. `GET /api/v1/search` at `src/app.ts:268` extracts `ProviderContext` via `getContext()` (`src/app.ts:182`), attempts cache read via `cache.search.get()` (`src/app.ts:275`)
4. **Cache hit (fast path)** — emits `CACHE_CHECKED` (hit=true), records `searchLatencyHistogram` with `cached: 'true'`, returns cached result at `src/app.ts:279`
5. **Cache miss** — emits `CACHE_CHECKED` (hit=false) at `src/app.ts:281`, delegates to `engine.executeWithFailover('search', context, ...)` at `src/app.ts:284`
6. **Provider selection + failover** — `ProviderSelectionEngine.executeWithFailover()` (`src/core/selection/ProviderSelectionEngine.ts:94`) iterates all registered adapters, computes scores via `computeScore()`, sorts descending, and tries each in order until one succeeds; each attempt is gated by a per-provider `CircuitBreaker` (`src/core/circuit/CircuitBreaker.ts:54.isAvailable()`)
7. **Circuit recording** — on success `cb.recordSuccess()` at `src/core/selection/ProviderSelectionEngine.ts:126`; on failure `cb.recordFailure()` at `src/core/selection/ProviderSelectionEngine.ts:129`
8. **Event emission** — `PROVIDER_SELECTED`, `PROVIDER_FAILOVER`, `PROVIDER_FAILED`, `CIRCUIT_TRIPPED` events emitted throughout (`src/core/selection/ProviderSelectionEngine.ts:84,117,132` / `src/core/circuit/CircuitBreaker.ts:46`)
9. **Cache write** — successful result cached via `cache.search.set()` / `cache.albums.set()` / `cache.artists.set()` / `cache.playlists.set()` (`src/app.ts:289,305,321,337`)
10. **onResponse hook** — logs completion with trace ID at `src/app.ts:128`
11. **onSend hook** — echoes `x-trace-id` header to client at `src/app.ts:137`

### Stream Proxy Path (direct-to-CDN relay)

1. `POST /api/v1/stream` (`src/app.ts:342`) resolves stream via `engine.executeWithFailover('playback', ...)`, emits `STREAM_RESOLVED` event at `src/app.ts:352`
2. When `config.stream.proxyStreaming` is true (`config/gateway.yaml:36`), response rewrites `streamUrl` to `/api/v1/stream/proxy/:trackId` with `expiresAtEpochSeconds: 0` at `src/app.ts:361`
3. `GET /api/v1/stream/proxy/:trackId` (`src/app.ts:374`) — `resolveCdnStreamUrl()` (`src/app.ts:193`) resolves/caches CDN URL via an in-memory `cdnUrlCache` Map (TTL from `config.cache.streamTTLSeconds`), then proxies binary audio bytes with full `Range` header support (`src/app.ts:389-443`), using an `AbortController` that aborts on client disconnect at `src/app.ts:390`

**State Management:**
- Request-scoped: `ProviderContext` is built per-request from headers at `src/app.ts:182` and passed down to adapters; no global mutable request state.
- Process-scoped singletons: `globalEventBus` (`src/core/events/EventBus.ts:24`), `prometheusRegister` (`src/core/metrics/metrics.ts:4`), `logger` (`src/core/logging/logger.ts:4`).
- Connection-scoped: Redis client, CacheManager, ProviderRegistry, ProviderSelectionEngine, RedisHealthChecker are all instantiated once per `buildApp()` call in `src/app.ts:66-76`.
- In-process (non-distributed) cache: `cdnUrlCache` Map in `app.ts:191` caches resolved CDN stream URLs in memory.

## Key Abstractions

**`ProviderAdapter` interface:**
- Purpose: Defines the contract every provider plugin must implement — five domain operations plus a health check. This is the hexagonal "port."
- Examples: `src/types/adapter.ts:12` (MockProviderAdapter at `src/providers/mock/MockProviderAdapter.ts:55`, YouTubeProviderAdapter at `src/providers/youtube/YouTubeProviderAdapter.ts:29`)
- Pattern: Interface-based plugin — adapters declare `id`, `name`, `priority`, and `capabilities` as readonly fields, then implement `search()`, `stream()`, `album()`, `artist()`, `playlist()`, and `healthCheck()`.

**`ProviderCapabilities` (capability bitmask):**
- Purpose: Boolean feature flags so the selection engine can reject incapable providers before attempting a call.
- Examples: `src/types/capabilities.ts:1` (YouTube enables search/playback/playlists/albums/artists but not recommendations/radio/downloads/lyrics at `src/providers/youtube/YouTubeProviderAdapter.ts:32-42`)
- Pattern: The `computeScore()` method in `ProviderSelectionEngine` returns `-1000` if `adapter.capabilities[requiredCapability]` is falsy (`src/core/selection/ProviderSelectionEngine.ts:38`), filtering the provider out entirely.

**`ProviderError` hierarchy:**
- Purpose: Structured, serializable error types that carry `code`, `providerId`, `statusCode`, and optional `retryAfterSeconds` — mapped directly to the JSON error response shape.
- Examples: `src/types/error.ts:12` (base class with 9 subclasses: `AuthenticationFailedError`→401, `RateLimitedError`→429, `GeoBlockedError`→403, `NotFoundError`→404, `UnsupportedError`→501, `PlaybackError`→502, `NetworkError`→503, `TimeoutError`→504, `InternalError`→500)
- Pattern: The global error handler in `app.ts:143` checks `instanceof ProviderError` first and maps it to the correct HTTP status + structured response body; non-ProviderError 4xx errors get `INVALID_REQUEST`; all else fall through to `INTERNAL_ERROR` at `src/app.ts:171`.

## Entry Points

**`server.ts` (production runtime):**
- Location: `src/server.ts`
- Triggers: `node dist/server.js` (Dockerfile at `Dockerfile:17`) or `npm run dev` → `ts-node-dev` (`package.json:9`)
- Responsibilities: Calls `buildApp()`, reads `app.config.server.port/host`, calls `app.listen()`, registers `SIGTERM`/`SIGINT` handlers for graceful shutdown at `src/server.ts:36-37`

**`buildApp()` factory:**
- Location: `src/app.ts:66`
- Triggers: `server.ts`, OpenAPI generation script (`scripts/generate-openapi.ts:8`), validation script (`scripts/validate-phase-a-gateway.ts:7`), integration tests
- Responsibilities: Creates Fastify instance, loads config, instantiates Redis (mock in test mode), constructs all core singletons, registers CORS + Swagger plugins, calls `registerProviders()`, registers all lifecycle hooks and routes, returns the decorated app

**`registerProviders()`:**
- Location: `src/providers/registerProviders.ts:17`
- Triggers: Called inside `buildApp()` at `src/app.ts:110`
- Responsibilities: Reads `config.providers.*` from `GatewayConfig`, conditionally instantiates `MockProviderAdapter` (always, unless explicitly disabled) and `YouTubeProviderAdapter` (only when `enabled === true`), registers them with the `ProviderRegistry`

## Architectural Constraints

- **Threading:** Node.js single-threaded event loop; no worker threads. Asynchronous operations rely on Promises and async/await throughout. The YouTube adapter uses `Promise.race` with a timeout promise at `src/providers/youtube/YouTubeProviderAdapter.ts:100` for operation-level timeouts.
- **Global state:** Three intentional process-singletons: `globalEventBus` (`src/core/events/EventBus.ts:24`), `prom-client` `register` (`src/core/metrics/metrics.ts:4`), and `pino` `logger` (`src/core/logging/logger.ts:4`). The `cdnUrlCache` Map in `app.ts:191` is instance-scoped (per `buildApp()` call). All carry risk in scaled/multi-instance deployments — see CONCERNS.
- **Circular imports:** A dependency cycle exists: `metrics.ts:2` imports `EventBus`, `logger.ts:2` imports `EventBus`, and `EventBus.ts` imports neither — so metrics and logger both depend on events. `ProviderSelectionEngine` (`src/core/selection/ProviderSelectionEngine.ts:8`) imports both `EventBus` and `metrics`. The `RedisCacheBase` (`src/core/cache/RedisCacheBase.ts:2`) imports `EventBus` for `CACHE_ERROR` events. This is a tree, not a cycle, but metrics/logger coupling to EventBus means event types must not change without updating all listeners.
- **Layering rule:** Enforced by `tests/architecture/layers.test.ts` — `core/` must never import from `providers/`, providers (except `registerProviders.ts`) must never import from `core/`, `config/`, or `app`, and `types/` must be a leaf with no internal imports.
- **Test-time mocking:** When `process.env.NODE_ENV === 'test'`, `app.ts:45` swaps `ioredis` for `ioredis-mock`, avoiding a real Redis dependency in the test suite.

## Anti-Patterns

### Route handlers grow into app.ts (monolithic composition root)

**What happens:** All 11 routes (bootstrap, search, album, artist, playlist, stream, stream/proxy, health, metrics, version, providers, plus a hidden `/debug-yt` test endpoint at `src/app.ts:486`) live in a single file (`src/app.ts`, 528 lines). Route logic, cache checks, failover orchestration, and error handling are interleaved.
**Why it's wrong:** At 528 lines, `app.ts` is the largest source file and mixes HTTP transport concerns (status codes, reply methods) with orchestration logic (cache check → engine call → cache write). Adding a new endpoint with its own caching/failover variant requires touching this ever-growing file, increasing merge conflicts and making individual route logic hard to unit-test in isolation.
**Do this instead:** Extract per-resource route modules (e.g. `src/routes/search.ts`, `src/routes/stream.ts`) that export a `registerRoutes(app, ctx)` function, and compose them from `app.ts`. The existing `getContext()` helper and `resolveCdnStreamUrl()` are already candidates for extraction.

### Debug endpoint committed to source

**What happens:** The `/debug-yt` endpoint at `src/app.ts:486` is a raw debugging tool that uses `youtubei.js` `Innertube.create` with multiple client types and is marked `{ hide: true }` so it doesn't appear in Swagger.
**Why it's wrong:** It requires `youtube-po-token-generator` and live YouTube API calls; executing it without proper credentials causes unhandled errors. It also imports `youtubei.js` client types directly into the entry point rather than through the adapter layer, bypassing the `ProviderAdapter` abstraction. It is not covered by tests.
**Do this instead:** Move to a separate `src/scripts/` debug entrypoint or remove entirely in production builds; gate behind an explicit feature flag.

### In-process CDN URL cache is not shared across instances

**What happens:** `resolveCdnStreamUrl()` at `src/app.ts:193` caches resolved CDN stream URLs in a local `Map` (`cdnUrlCache`) with no cross-instance or cross-process invalidation, only a TTL-based expiry.
**Why it's wrong:** In a multi-instance deployment (e.g. Render scaling beyond free tier, or containerized horizontal scaling), each instance maintains its own cache. A CDN URL that expires/revoke on the upstream side will not be refreshed across instances, and the local Map grows unbounded — there is no eviction strategy, only TTL.
**Do this instead:** Use the Redis-backed `ArtworkCache` pattern (`src/core/cache/segregated/ArtworkCache.ts`) or a dedicated stream cache type for CDN URL storage, with explicit TTL + eviction. This also integrates with the fail-open `RedisCacheBase` and `CACHE_ERROR` event emission.

## Error Handling

**Strategy:** Hierarchical, typed, and mapped at the HTTP boundary. All provider-side errors descend from `ProviderError` (`src/types/error.ts:12`) which carries a canonical `code`, `providerId`, HTTP `statusCode`, and optional `retryAfterSeconds`. The global error handler in `app.ts:143` distinguishes three tiers: (1) `ProviderError` instances are mapped directly to their embedded status code and structured response; (2) Fastify schema validation errors (4xx `statusCode`) become `INVALID_REQUEST`; (3) everything else becomes `INTERNAL_ERROR` (500).

**Patterns:**
- Adapter-level error translation: `YouTubeProviderAdapter.errorCode()` (`src/providers/youtube/YouTubeProviderAdapter.ts:103`) pattern-matches on error message regexes (rate limit, geo-block, auth, network) and throws the appropriate typed `ProviderError` subclass — so the gateway never leaks raw `youtubei.js` error messages to clients.
- Mock-level simulation: `MockProviderAdapter.simulateFailure()` (`src/providers/mock/MockProviderAdapter.ts:175`) throws typed errors per configured `MockProviderState`, enabling failover testing without real upstreams.
- Failover recovery: `ProviderSelectionEngine.executeWithFailover()` (`src/core/selection/ProviderSelectionEngine.ts:141`) re-throws `ProviderError` as-is from the last failed candidate, preserving the original provider context; only when all candidates fail with non-ProviderError exceptions does it wrap in `InternalError`.

## Cross-Cutting Concerns

**Logging:** Pino structured logger (`src/core/logging/logger.ts:4`) with `service: 'clibeats-gateway'` base field and ISO timestamps. Two EventBus wildcard listeners: one in `logger.ts:14` logs every gateway event, one in `app.ts:112` (onRequest) and `app.ts:128` (onResponse) log request lifecycle with trace ID correlation.

**Validation:** Fastify JSON Schema validation at the route level — every route specifies input/output `schema` from `schemas.ts`. Schema validation errors are 4xx and handled by the global error handler as `INVALID_REQUEST`. OpenAPI spec is generated from these schemas and committed as `openapi.json`, validated against live routes in CI via `scripts/validate-openapi.ts`.

**Authentication:** Header-based detection only — `ProviderContext.authenticated` is derived from the presence of an `Authorization` header at `src/app.ts:185`. No token validation occurs in the gateway; the context is passed to adapters which handle their own auth (e.g. YouTube PO token, mock bearer token in stream headers at `src/providers/mock/MockProviderAdapter.ts:230`).

**Tracing:** Trace ID propagation via `x-trace-id` header — generated if absent (`src/app.ts:115`), echoed to client on response (`src/app.ts:139`), and included in all logs and events. Full request lifecycle correlation from onRequest → route handler → error handler → onResponse.

**Metrics:** Prometheus via `prom-client` (`src/core/metrics/metrics.ts`). 8 custom metrics: request counter, cache hit/miss/error counters, provider selection/failure counters, provider health gauge, circuit breaker state gauge, search latency histogram, stream resolution histogram. All wired to EventBus wildcard listener at `src/core/metrics/metrics.ts:88` — no manual `inc()/observe()` calls needed beyond the search latency histogram in `app.ts:278,287`. `/metrics` endpoint serves raw Prometheus text format at `src/app.ts:467`.

---

*Architecture analysis: 2026-08-09*
