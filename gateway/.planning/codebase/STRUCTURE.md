# Codebase Structure

**Analysis Date:** 2026-08-09

## Directory Layout

```
clibeats/gateway/
├── src/                          # Source code (TypeScript)
│   ├── server.ts                 # Process entry point
│   ├── app.ts                    # Fastify app factory + all routes
│   ├── schemas.ts                # JSON Schemas + OpenAPI tag definitions
│   ├── config/
│   │   └── config.ts             # GatewayConfig loader (YAML + env overrides)
│   ├── core/                     # Cross-cutting infrastructure
│   │   ├── cache/
│   │   │   ├── CacheManager.ts   # Facade composing all cache types
│   │   │   ├── RedisCacheBase.ts # Abstract fail-open Redis primitive
│   │   │   └── segregated/       # Type-specific cache implementations
│   │   │       ├── AlbumCache.ts
│   │   │       ├── ArtistCache.ts
│   │   │       ├── ArtworkCache.ts
│   │   │       ├── HealthCache.ts
│   │   │       ├── PlaylistCache.ts
│   │   │       ├── SearchCache.ts
│   │   │       └── SessionCache.ts
│   │   ├── circuit/
│   │   │   └── CircuitBreaker.ts  # CLOSED/OPEN/HALF_OPEN state machine
│   │   ├── events/
│   │   │   └── EventBus.ts        # Typed EventEmitter (globalEventBus singleton)
│   │   ├── health/
│   │   │   └── RedisHealthChecker.ts
│   │   ├── logging/
│   │   │   └── logger.ts          # Pino structured logger
│   │   ├── metrics/
│   │   │   └── metrics.ts         # Prometheus registry + counters/gauges/histograms
│   │   ├── registry/
│   │   │   └── ProviderRegistry.ts     # Adapter storage + priority sorting
│   │   └── selection/
│   │       └── ProviderSelectionEngine.ts  # Scoring + failover orchestration
│   ├── providers/                # Provider plugins (leaf adapters + bootstrap)
│   │   ├── registerProviders.ts  # Config-driven adapter registration
│   │   ├── mock/
│   │   │   └── MockProviderAdapter.ts
│   │   └── youtube/
│   │       ├── YouTubeProviderAdapter.ts
│   │       └── media.ts          # YouTube response normalization helpers
│   └── types/                    # Shared type definitions (leaf, no internal imports)
│       ├── adapter.ts            # ProviderAdapter interface + AdapterHealth
│       ├── capabilities.ts       # ProviderCapabilities boolean flags
│       ├── context.ts            # ProviderContext (country, lang, device, etc.)
│       ├── declarations.d.ts     # ioredis-mock + autocannon ambient modules
│       ├── domain.ts             # Track, Album, Artist, Playlist, Lyrics, StreamResult
│       └── error.ts              # ProviderError hierarchy (9 subclasses)
├── config/                       # Runtime configuration
│   └── gateway.yaml              # Provider/feature flags + TTLs (default path)
├── tests/                        # Test suites
│   ├── architecture/             # Dependency-layer enforcement tests
│   │   └── layers.test.ts
│   ├── contract/                 # OpenAPI spec sync validation
│   │   └── openapi.test.ts
│   ├── integration/              # Fastify-inject HTTP integration tests
│   │   ├── api.test.ts
│   │   ├── failover.test.ts
│   │   ├── health.test.ts
│   │   └── metrics.test.ts
│   ├── load/                     # Autoload performance tests
│   │   └── load-test.ts
│   ├── property/                 # Property-based tests (fast-check)
│   │   └── search-property.test.ts
│   ├── unit/                     # Unit tests for core + providers
│   │   ├── core.test.ts
│   │   ├── mock-provider.test.ts
│   │   ├── redis-cache-resilience.test.ts
│   │   ├── redis-health.test.ts
│   │   └── youtube-adapter.test.ts
├── scripts/                      # Build/CI utilities
│   ├── generate-openapi.ts       # Generates openapi.json from live routes
│   ├── validate-openapi.ts       # Validates committed spec vs. live routes
│   └── validate-phase-a-gateway.ts
├── openapi.json                  # Committed OpenAPI contract (generated)
├── Dockerfile                    # Multi-stage: node:20-alpine builder + runner
├── docker-compose.yml            # gateway + redis:7-alpine services
├── render.yaml                   # Render.com deployment manifest
├── package.json
├── tsconfig.json                 # CommonJS, rootDir=src, tests excluded
├── vitest.config.ts              # Node env, tests/**.test.ts, 70% coverage thresholds
├── .dockerignore
└── .planning/                    # GSD planning artifacts
```

## Directory Purposes

**`src/` — Source code root:**
- Purpose: All TypeScript source; compiled by `tsc` to `dist/` (per `tsconfig.json:6-8`). Entry point at `package.json:main` → `dist/server.js`.
- Contains: `server.ts`, `app.ts`, `schemas.ts`, `config/`, `core/`, `providers/`, `types/`
- Key files: `src/server.ts`, `src/app.ts`, `src/schemas.ts`

**`src/config/` — Configuration management:**
- Purpose: Single configuration loader with typed `GatewayConfig` interface.
- Contains: `config.ts`
- Key files: `src/config/config.ts` — loads `config/gateway.yaml`, applies `PORT`/`REDIS_URL`/`GATEWAY_CONFIG_PATH` env overrides
- Constraint: Depends only on Node built-ins (`fs`, `path`, `yaml`) — enforced by `tests/architecture/layers.test.ts:80`

**`src/core/` — Core infrastructure:**
- Purpose: Reusable cross-cutting concerns shared across the gateway — caching, circuit-breaking, events, health, logging, metrics, provider registry, and selection logic.
- Contains: `cache/`, `circuit/`, `events/`, `health/`, `logging/`, `metrics/`, `registry/`, `selection/`
- Constraint: Must never import from `providers/` — enforced by `tests/architecture/layers.test.ts:41`

**`src/core/cache/` — Caching layer:**
- Purpose: Fail-open Redis cache abstraction with type-segregated sub-caches.
- Contains: `CacheManager.ts` (facade), `RedisCacheBase.ts` (abstract base), `segregated/` (7 type-specific caches)
- Key files: `src/core/cache/CacheManager.ts`, `src/core/cache/RedisCacheBase.ts`
- Pattern: `CacheManager` composes all cache types in its constructor (`src/core/cache/CacheManager.ts:20-29`), reading TTLs from `GatewayConfig` (metadata 86400s, search 3600s, artwork 604800s, etc.)

**`src/core/cache/segregated/` — Segregated cache types:**
- Purpose: One cache class per domain type, each extending `RedisCacheBase` with entity-specific JSON (de)serialization and a distinct Redis namespace for TTL isolation.
- Contains: `AlbumCache`, `ArtistCache`, `ArtworkCache`, `HealthCache`, `PlaylistCache`, `SearchCache`, `SessionCache`
- Pattern: All extend `RedisCacheBase` and override `get`/`set` with typed JSON.parse/stringify. `SessionCache` uses `ttlSeconds: 0` (no auto-expiry) and a colon-separated composite key at `src/core/cache/segregated/SessionCache.ts:10`

**`src/core/circuit/` — Circuit breaker:**
- Purpose: Per-provider circuit-breaking state machine.
- Contains: `CircuitBreaker.ts`
- Key files: `src/core/circuit/CircuitBreaker.ts` — CLOSED (default) → OPEN after `failureThreshold` (default 3) failures → HALF_OPEN after `cooldownSeconds` (default 60) → back to CLOSED on success

**`src/core/events/` — Event bus:**
- Purpose: Centralized typed event dispatch for observability.
- Contains: `EventBus.ts`
- Key files: `src/core/events/EventBus.ts` — exports `EventBus` class and `globalEventBus` singleton; defines 8 typed event payload variants

**`src/core/health/` — Health checking:**
- Purpose: Infrastructure health probes.
- Contains: `RedisHealthChecker.ts`
- Key files: `src/core/health/RedisHealthChecker.ts` — PING-based probe with configurable timeout, returns `RedisHealthResult` with `UP`/`DEGRADED`/`DOWN` status

**`src/core/logging/` — Logging:**
- Purpose: Structured logging facade.
- Contains: `logger.ts`
- Key files: `src/core/logging/logger.ts` — Pino instance with `service: 'clibeats-gateway'` base, also listens to EventBus for event logging

**`src/core/metrics/` — Metrics collection:**
- Purpose: Prometheus metrics registration and EventBus-driven auto-updating.
- Contains: `metrics.ts`
- Key files: `src/core/metrics/metrics.ts` — 8 metrics registered on a single `prom-client` Registry; wildcard EventBus listener at `src/core/metrics/metrics.ts:88` auto-increments counters from events

**`src/core/registry/` — Provider registry:**
- Purpose: In-memory provider adapter storage with priority-sorted access.
- Contains: `ProviderRegistry.ts`
- Key files: `src/core/registry/ProviderRegistry.ts` — `Map<string, ProviderAdapter>` with `register`, `get`, `getAll`, `getSortedByPriority`, `clear`

**`src/core/selection/` — Provider selection engine:**
- Purpose: Provider scoring, health-weighted selection, and failover orchestration.
- Contains: `ProviderSelectionEngine.ts`
- Key files: `src/core/selection/ProviderSelectionEngine.ts` — `computeScore()`, `selectBestProvider()`, `executeWithFailover()`; maintains per-provider CircuitBreakers

**`src/providers/` — Provider plugins:**
- Purpose: Concrete adapter implementations + bootstrap composition.
- Contains: `registerProviders.ts`, `mock/`, `youtube/`
- Key files: `src/providers/registerProviders.ts` — the only providers-layer file allowed to import from `core/` and `config/` (enforced by `tests/architecture/layers.test.ts:54`); reads `config.providers.*` and conditionally instantiates adapters

**`src/providers/mock/` — Mock provider:**
- Purpose: Synthetic data provider for testing/development with configurable failure simulation.
- Contains: `MockProviderAdapter.ts`
- Key files: `src/providers/mock/MockProviderAdapter.ts` — generates 100 tracks across 5 artists/10 albums, 5 playlists; supports 8 `MockProviderState` values for testing failover

**`src/providers/youtube/` — YouTube provider:**
- Purpose: youtubei.js-based adapter for live YouTube Music content.
- Contains: `YouTubeProviderAdapter.ts`, `media.ts`
- Key files: `src/providers/youtube/YouTubeProviderAdapter.ts` — dual-session architecture (MUSIC client for metadata, ANDROID_VR for streaming at `src/providers/youtube/YouTubeProviderAdapter.ts:56-57`); 30s operation timeout via `Promise.race`; error message regex→typed-ProviderError mapping

**`src/types/` — Shared type definitions:**
- Purpose: Leaf-level domain models, interfaces, and error hierarchy with zero internal imports.
- Contains: `adapter.ts`, `capabilities.ts`, `context.ts`, `declarations.d.ts`, `domain.ts`, `error.ts`
- Constraint: Must never import from `core/`, `providers/`, `config/`, or `app` — enforced by `tests/architecture/layers.test.ts:67`

**`tests/` — Test suites:**
- Purpose: Multi-tier testing strategy organized by test type.
- Contains: `architecture/`, `contract/`, `integration/`, `load/`, `property/`, `unit/`
- Pattern: All test files match `tests/**/*.test.ts` (per `vitest.config.ts:6`); compiled `.test.js`/`.test.d.ts` artifacts are excluded from test runs (`vitest.config.ts:7`)

**`tests/architecture/` — Layering enforcement:**
- Purpose: Architectural constraint tests that assert import boundaries at build/CI time.
- Contains: `layers.test.ts`
- Key files: `tests/architecture/layers.test.ts:41` — core→providers ban; `layers.test.ts:54` — providers→core/config/app ban (except registerProviders); `layers.test.ts:67` — types leaf rule; `layers.test.ts:80` — config external-only rule; `layers.test.ts:86` — registerProviders composition check

**`tests/contract/` — OpenAPI contract validation:**
- Purpose: Ensures committed `openapi.json` stays in sync with live route schemas.
- Contains: `openapi.test.ts`
- Key files: `tests/contract/openapi.test.ts` — builds app, generates spec via `app.swagger()`, compares against committed `openapi.json`

**`tests/integration/` — HTTP integration tests:**
- Purpose: End-to-end route testing via Fastify's `inject()` method (no real HTTP socket).
- Contains: `api.test.ts`, `failover.test.ts`, `health.test.ts`, `metrics.test.ts`
- Pattern: Each test file calls `buildApp()` with a partial `GatewayConfig` override (e.g. disables YouTube at `tests/integration/api.test.ts:9`) and uses `app.inject()` for request simulation

**`tests/load/` — Load/performance tests:**
- Purpose: Autoload-based performance benchmarking.
- Contains: `load-test.ts`
- Key files: `tests/load/load-test.ts` — uses `autocannon` to hammer endpoints

**`tests/property/` — Property-based tests:**
- Purpose: Generative testing with `fast-check`.
- Contains: `search-property.test.ts`

**`tests/unit/` — Unit tests:**
- Purpose: Isolated unit tests for core infrastructure and provider adapters.
- Contains: `core.test.ts`, `mock-provider.test.ts`, `redis-cache-resilience.test.ts`, `redis-health.test.ts`, `youtube-adapter.test.ts`
- Pattern: Tests construct core classes directly (e.g. `new ProviderRegistry()` at `tests/unit/core.test.ts:23`) rather than via `buildApp()`, enabling fast isolated runs

**`scripts/` — Build and CI utility scripts:**
- Purpose: Developer and CI tooling for OpenAPI generation/validation and phase verification.
- Contains: `generate-openapi.ts`, `validate-phase-a-gateway.ts`, `validate-openapi.ts`
- Key files: `scripts/generate-openapi.ts` — builds app, extracts Swagger spec, writes to `openapi.json`; `scripts/validate-openapi.ts` — compares committed spec against live route schemas; `scripts/validate-phase-a-gateway.ts` — manual smoke validation suite

**`config/` — Runtime configuration:**
- Purpose: Default gateway configuration consumed by `src/config/config.ts`.
- Contains: `gateway.yaml`

**`dist/` — Compiled output:**
- Purpose: `tsc` build artifact; committed in some states but regenerated by `npm run build`.
- Generated: Yes (`.gitignore` or build pipeline)
- Committed: No (regenerated)

**`coverage/` — Coverage reports:**
- Purpose: Vitest v8 coverage output.
- Generated: Yes
- Committed: No

## Key File Locations

**Entry Points:**
- `src/server.ts` — Process bootstrap; `buildApp()` → `app.listen()` → SIGTERM/SIGINT handlers
- `src/app.ts:66` — `buildApp()` factory; composition root that wires all layers

**Configuration:**
- `src/config/config.ts` — `loadConfig()` loads YAML, applies env overrides, returns typed `GatewayConfig`
- `config/gateway.yaml` — Provider enablement, priorities, TTLs, CORS, stream settings

**Core Logic:**
- `src/core/selection/ProviderSelectionEngine.ts` — Scoring algorithm, failover orchestration, circuit-breaker integration
- `src/core/registry/ProviderRegistry.ts` — Adapter registration and priority-sorted retrieval
- `src/core/cache/CacheManager.ts` — Composed cache facade injected into the Fastify instance
- `src/core/cache/segregated/*.ts` — Per-domain cache types (7 files)

**Routing / Transport:**
- `src/app.ts` — All 11 HTTP route handlers (lines 236–484), lifecycle hooks (lines 113–179), and DI decorations
- `src/schemas.ts` — JSON Schema definitions for every route's request/response validation and OpenAPI documentation

**Provider Plugins:**
- `src/providers/mock/MockProviderAdapter.ts` — Synthetic data provider with 8 failure states
- `src/providers/youtube/YouTubeProviderAdapter.ts` — youtubei.js adapter with dual-client session architecture
- `src/providers/registerProviders.ts` — Config-driven adapter bootstrap

**Type Definitions:**
- `src/types/adapter.ts` — `ProviderAdapter` interface + `AdapterHealth`
- `src/types/domain.ts` — `Track`, `Album`, `Artist`, `Playlist`, `Lyrics`, `StreamResult`
- `src/types/error.ts` — `ProviderError` base + 9 typed subclasses

## Naming Conventions

**Files:**
- PascalCase for classes/types with business logic, e.g. `ProviderSelectionEngine.ts`, `CircuitBreaker.ts`, `YouTubeProviderAdapter.ts`, `MockProviderAdapter.ts`
- Lowercase kebab-case for utilities, e.g. `registerProviders.ts`, `metrics.ts`, `logger.ts`, `config.ts`, `schemas.ts`
- Test files: `*.test.ts` co-suffixed with the module or area, e.g. `core.test.ts`, `layers.test.ts`, `health.test.ts`

**Directories:**
- Lowercase for all directories: `core/`, `providers/`, `types/`, `cache/`, `circuit/`, `events/`, `health/`, `logging/`, `metrics/`, `registry/`, `selection/`, `mock/`, `youtube/`, `segregated/`
- Test subdirectories mirror source concerns: `architecture/`, `integration/`, `unit/`, `contract/`, `load/`, `property/`

**Symbols:**
- Classes are PascalCase, e.g. `ProviderSelectionEngine`, `RedisCacheBase`, `CircuitBreaker`
- Interfaces are PascalCase with optional `I` prefix not used, e.g. `ProviderAdapter`, `ProviderContext`, `GatewayConfig`
- Functions are camelCase, e.g. `loadConfig`, `buildApp`, `registerProviders`, `resolveCdnStreamUrl`
- Constants/types are PascalCase or UPPER_SNAKE, e.g. `ProviderCapabilities` (type), `MOCK_PROVIDER_STATES` (constant), `CircuitState` (type alias)
- Private fields use TypeScript `private` keyword (not `#`), e.g. `CircuitBreaker.ts:6` `private state`, `CircuitBreaker.ts:8` `private failureCount`
- Singletons are exported as `global...`, e.g. `globalEventBus` (`src/core/events/EventBus.ts:24`), `register` in metrics (`src/core/metrics/metrics.ts:4`)

## Where to Add New Code

**New Feature (new resource endpoint):**
- Route handler: Add to `src/app.ts` alongside existing routes (currently all 11 routes live in this single file — see ARCHITECTURE.md "Route handlers grow into app.ts" anti-pattern). Use an existing schema name from `src/schemas.ts` or add a new one following the `nameSchema` pattern.
- Business logic: Add a method to `ProviderSelectionEngine` (`src/core/selection/ProviderSelectionEngine.ts`) only if the feature requires custom failover scoring; otherwise call existing `executeWithFailover()` with a new capability key from `src/types/capabilities.ts`.
- Domain types: Extend `src/types/domain.ts` if a new entity is introduced.
- Cache: Add a new class in `src/core/cache/segregated/` extending `RedisCacheBase` and wire it into `CacheManager` (`src/core/cache/CacheManager.ts:11-28`).

**New Provider Adapter:**
- Implementation: Create `src/providers/<provider-name>/<ProviderName>Adapter.ts` implementing the `ProviderAdapter` interface from `src/types/adapter.ts`. The adapter must depend only on `src/types/` (no core/config/app imports) — see layering constraint.
- Configuration: Add a `providers.<providerName>` block to `config/gateway.yaml` with `enabled` and `priority`.
- Registration: Add a conditional `new <ProviderName>Adapter(...)` block in `src/providers/registerProviders.ts` mirroring the existing YouTube/Mock pattern (config-driven enablement at `src/providers/registerProviders.ts:23-31`).
- Helper modules: Place provider-specific parsing/normalization in `src/providers/<provider-name>/` alongside the adapter (e.g. `media.ts`).

**New Core Infrastructure (cache type, circuit, event, etc.):**
- Create the module under the appropriate `src/core/<layer>/` subdirectory.
- If it emits or consumes events, add the event type to the `GatewayEventPayload` union in `src/core/events/EventBus.ts:3`.
- If it needs Prometheus metrics, register them on the shared `register` Registry in `src/core/metrics/metrics.ts:4` and wire to EventBus if applicable.

**New Test:**
- Place in the appropriate `tests/<category>/` directory based on test type: `unit/` for isolated class tests, `integration/` for `buildApp()` + `app.inject()` tests, `architecture/` for import-constraint tests, `contract/` for API-spec tests, `property/` for fast-check generative tests, `load/` for autoload benchmarks.
- Filename: `descriptive-name.test.ts` matching `vitest.config.ts:6` include pattern.

**New CLI/utility script:**
- Place in `scripts/` as `<name>.ts`, set `process.env.NODE_ENV = 'test'` if it calls `buildApp()` (per `scripts/generate-openapi.ts:5`), and invoke via `ts-node` from an npm script in `package.json:16-17`.

## Special Directories

**`.planning/`:**
- Purpose: GSD workflow planning artifacts — phase plans, specs, roadmaps, codebase maps.
- Generated: No (written by GSD agent workflows)
- Committed: Yes — this directory is tracked in git.

**`dist/`:**
- Purpose: TypeScript compilation output from `tsc`.
- Generated: Yes
- Committed: No (regenerated by `npm run build` or Docker build at `Dockerfile:7`)

**`coverage/`:**
- Purpose: Vitest v8 coverage reports (text, json-summary, lcov).
- Generated: Yes (`npm run test:coverage`)
- Committed: No

**`node_modules/`:**
- Excluded from analysis. Production deps only installed in Docker (`Dockerfile:13`) via `npm ci --omit=dev`.

## Build & Deployment Conventions

**Build:** `tsc` compiles `src/` → `dist/` (`package.json:7`), with `tsconfig.json:8` rootDir=`./src`, `tsconfig.json:16` excluding `tests`.

**CI pipeline** (`package.json:17`): `npm run check` (type-check) → `npm test` (vitest) → `openapi:generate` → `openapi:validate` → `test:load`.

**Deployment:**
- Docker: Multi-stage `node:20-alpine` build (`Dockerfile`) → `node dist/server.js`; healthcheck hits `/health` (`Dockerfile:18-19`)
- docker-compose: `gateway` service on port 8080 + `redis:7-alpine` dependency (`docker-compose.yml`)
- Render.com: Auto-deploys from `Dockerfile` on main branch, healthcheck at `/health` (`render.yaml`)

**Test-time behavior:** When `NODE_ENV=test` (set by test scripts and `vitest.config.ts`), `src/app.ts:44` swaps `ioredis` for `ioredis-mock` so no real Redis is needed for the test suite.

---

*Structure analysis: 2026-08-09*
