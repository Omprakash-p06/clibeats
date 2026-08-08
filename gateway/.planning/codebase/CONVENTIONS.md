<!-- refreshed: 2026-08-09 -->
# Coding Conventions

**Analysis Date:** 2026-08-09

## Languages

**Primary:**
- TypeScript 5.7.3 — entire `gateway/src/` codebase (compiled via `tsconfig.json`)
- YAML 1.2 — `gateway/config/gateway.yaml` for runtime configuration (parsed by `yaml` package)

## TypeScript Configuration

**Strict mode** enforced via `gateway/tsconfig.json:2-14`:
- `"target": "ES2022"`
- `"module": "CommonJS"`, `"moduleResolution": "node"`
- `"strict": true` — enables `strictNullChecks`, `strictFunctionTypes`, `strictBindCallApply`, `alwaysStrict`, `noImplicitAny`, `noImplicitThis`, `useUnknownInCatchVariables`
- `"esModuleInterop": true` — enables default imports for CommonJS interop (e.g. `import Redis from 'ioredis'`)
- `"skipLibCheck": true` — skips type-checking of `node_modules/*.d.ts`
- `"forceConsistentCasingInFileNames": true`
- `"declaration": true`, `"sourceMap": true`
- `"outDir": "./dist"`, `"rootDir": "./src"`
- `"include": ["src/**/*"]`; excludes `node_modules`, `dist`, `tests`

**Type-checking gate:** `npm run check` runs `tsc --noEmit` (`package.json:9`) — this is the sole static-analysis gate. No ESLint, Prettier, or Biome configuration exists in the gateway.

### Type Usage Patterns

- **Interfaces** for structural contracts — `ProviderAdapter` (`src/types/adapter.ts:12`), `GatewayConfig` (`src/config/config.ts:5`), `ProviderContext` (`src/types/context.ts:1`), `ProviderCapabilities` (`src/types/capabilities.ts:1`), domain models (`src/types/domain.ts`).
- **`as` casts** at untyped boundaries — e.g. `(res.contents ?? []) as any[]` in `YouTubeProviderAdapter.ts:127`; `JSON.parse(raw) as Track[]` in `SearchCache.ts:13`.
- **Type-only imports** via `import type { ... }` — `src/server.ts:3` and `src/providers/registerProviders.ts:1` (`import type { FastifyInstance }`).
- **Union / discriminated-union types** for state — `MockProviderState` string union (`MockProviderAdapter.ts:34`), `CircuitState` (`CircuitBreaker.ts:4`), `GatewayEventPayload` discriminated union (`EventBus.ts:3-11`).
- **`never` return** in error-mapping helpers — `YouTubeProviderAdapter.errorCode()` (`YouTubeProviderAdapter.ts:103`) returns `never` (always throws).
- **`readonly` fields** for immutable identity — `ProviderAdapter` interface fields (`adapter.ts:13-16`), implemented in `MockProviderAdapter.ts:56` and `YouTubeProviderAdapter.ts:30-31`.

## Code Style

| Aspect | Convention | Evidence |
|--------|-----------|----------|
| Indentation | 2 spaces, no tabs | all `src/` and `tests/*.ts` files |
| Semicolons | Always | statement terminators throughout |
| Quotes | Single quotes | imports and string literals (`'fastify'`, `'HEALTHY'`) |
| Braces | 1TBS (`{` same line, `}`/else/catch own line) | `app.ts`, `MockProviderAdapter.ts` |
| Line length | No enforced maximum | long lines split manually |

**Formatting / Linting:** No ESLint or Prettier configuration exists. The only automated code-quality gate is `tsc --noEmit` (`npm run check`, `package.json:9`). Conventions are enforced by review.

## Naming Conventions

**Files:** `PascalCase.ts` for class/modules — `CacheManager.ts`, `ProviderSelectionEngine.ts`, `MockProviderAdapter.ts`, `RedisCacheBase.ts`, `YouTubeProviderAdapter.ts`, `CircuitBreaker.ts`, `RedisHealthChecker.ts`, `ProviderRegistry.ts`, `EventBus.ts`, `config.ts`.

**Directories:** lowercase singular/plural or camelCase — `core/`, `types/`, `providers/`, `config/`, `cache/`, `circuit/`, `events/`, `logging/`, `metrics/`, `registry/`, `selection/`, `health/`, `segregated/`, `mock/`, `youtube/`.

**Functions:** `camelCase` — `loadConfig`, `buildApp`, `registerProviders`, `computeScore`, `selectBestProvider`, `executeWithFailover`, `resolveCdnStreamUrl`, `parseRawItem`, `parseSubtitle`, `largestArtworkUrl`, `safeGet`, `safeSet`, `safeDel`.

**Variables:** `camelCase` — `failureCount`, `lastStateChangeEpoch`, `cdnUrlCache`, `streamResult`, `mockConfig`, `youtubeConfig`.

**Classes:** `PascalCase` — `ProviderRegistry`, `CircuitBreaker`, `RedisHealthChecker`, `CacheManager`, `ProviderSelectionEngine`, `MockProviderAdapter`, `YouTubeProviderAdapter`.

**Module-level constants:** `UPPER_SNAKE_CASE` — `YOUTUBE_PROVIDER_ID` (`media.ts:1`), `OPERATION_TIMEOUT_MS` (`YouTubeProviderAdapter.ts:27`).

**Parameters:** `camelCase`; unused params prefixed with leading underscore `_` — e.g. `_context` in `YouTubeProviderAdapter.search()` (`YouTubeProviderAdapter.ts:118`), `stream()` (`:162`), `album()` (`:209`), `artist()` (`:255`), `playlist()` (`:276`).

**Test files:** `.test.ts` suffix — every test file ends in `.test.ts` (matched by `vitest.config.ts:6`: `include: ['tests/**/*.test.ts']`).

## Import Organization

**Order:** Two-group style — external/package imports first, then internal relative imports. No blank line separation between groups observed.

Example from `src/app.ts:1-30`:
```ts
import { Readable } from 'stream';
import fastify, { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';
import Redis from 'ioredis';
import { loadConfig, GatewayConfig } from './config/config';
import { ProviderRegistry } from './core/registry/ProviderRegistry';
// ...
```

**Path aliases:** None. All internal references use explicit relative paths (`../`, `../../`) without file extensions — e.g. `'./config/config'`, `'../../types/adapter'`, `'../../../types/domain'`.

**Type-only imports:** `import type { ... }` used when only the type is consumed at compile time (`server.ts:3`, `registerProviders.ts:1`). Regular `import { ... }` is used otherwise.

## Error Handling

**Strategy:** Throw typed `ProviderError` subclasses on failure; map upstream exceptions to canonical errors at adapter boundaries; translate to HTTP status centrally in the Fastify error handler.

**Domain error hierarchy** — `src/types/error.ts`:
- Base class `ProviderError` (line 12) carries: `code: ProviderErrorCode`, `providerId: string`, `statusCode: number`, optional `retryAfterSeconds?: number`.
- `ProviderErrorCode` union (`error.ts:1-10`): `'AUTHENTICATION_FAILED' | 'RATE_LIMITED' | 'GEO_BLOCKED' | 'NOT_FOUND' | 'UNSUPPORTED' | 'PLAYBACK_ERROR' | 'NETWORK_ERROR' | 'TIMEOUT_ERROR' | 'INTERNAL_ERROR'`.
- Typed subclasses (each hardcodes a canonical `code` + HTTP `statusCode`):

| Class | code | HTTP | Defined |
|-------|------|------|---------|
| `AuthenticationFailedError` | `AUTHENTICATION_FAILED` | 401 | `error.ts:34` |
| `RateLimitedError` | `RATE_LIMITED` | 429 | `error.ts:40` |
| `GeoBlockedError` | `GEO_BLOCKED` | 403 | `error.ts:46` |
| `NotFoundError` | `NOT_FOUND` | 404 | `error.ts:52` |
| `UnsupportedError` | `UNSUPPORTED` | 501 | `error.ts:58` |
| `PlaybackError` | `PLAYBACK_ERROR` | 502 | `error.ts:64` |
| `NetworkError` | `NETWORK_ERROR` | 503 | `error.ts:70` |
| `TimeoutError` | `TIMEOUT_ERROR` | 504 | `error.ts:76` |
| `InternalError` | `INTERNAL_ERROR` | 500 | `error.ts:82` |

**Result / Either types:** **Not used.** The codebase throws exceptions rather than returning `Result<T>` / `Either<E, A>`. Failures surface via rejected promises.

**Central error handler** — `src/app.ts:142-179` (`app.setErrorHandler`):
- `ProviderError` → structured body `{ error: { code, message, providerId, retryAfterSeconds, traceId } }` using the error's own `statusCode`.
- Fastify schema-validation 4xx errors → `{ error: { code: 'INVALID_REQUEST', message, providerId: 'gateway', traceId } }`.
- All other errors → `{ error: { code: 'INTERNAL_ERROR', message, providerId: 'gateway', traceId } }` with HTTP 500.

**Upstream error mapping** — `YouTubeProviderAdapter.errorCode()` (`YouTubeProviderAdapter.ts:103-114`): regex-matches upstream exception messages and re-throws as canonical `ProviderError` subclasses (rate-limit → `RateLimitedError`, geo → `GeoBlockedError`, auth → `UnsupportedError`, network → `NetworkError`, missing audio → `NotFoundError`). Existing `ProviderError` instances are re-thrown unchanged. Always returns `never` (always throws).

**Fail-open / graceful-degradation pattern (errors that do NOT throw):** Producers never throw for cache misses (`null`) or empty results:
- `RedisCacheBase` is fail-open — read failures return `null`, write failures are swallowed and emit a `CACHE_ERROR` event (`RedisCacheBase.ts:29-60`).
- `MockProviderAdapter.search('')` returns the first 20 tracks instead of erroring (`MockProviderAdapter.ts:207`).

## Async Patterns

**Style:** `async`/`await` throughout. All adapter methods, route handlers, and cache helpers return `Promise<T>`.

**Fan-out concurrency:** `Promise.all` for independent parallel calls — `app.ts:237` (`/health` provider checks) and `app.ts:449` (`/health` per-provider loop is sequential by design, but `/api/v1/bootstrap` at `app.ts:237` uses `Promise.all`).

**Timeout handling:** `Promise.race` against a `setTimeout`-based reject — `YouTubeProviderAdapter.withTimeout()` (`YouTubeProviderAdapter.ts:85-101`). Timers use `timer.unref?.()` (`YouTubeProviderAdapter.ts:98`) so they do not keep the process alive.

**Manual promise/timeout with cleanup:** `RedisHealthChecker.pingWithTimeout()` (`RedisHealthChecker.ts:55-71`) — `new Promise((resolve, reject) => { const timer = setTimeout(...); ... })` with explicit `clearTimeout(timer)` on both resolve and reject paths.

**Graceful async degradation:** `RedisCacheBase.safeGet` / `safeSet` / `safeDel` (`RedisCacheBase.ts:29-60`) wrap Redis calls in try/catch; on failure they emit `CACHE_ERROR` to the event bus and return `null`/`void` instead of throwing.

## Provider Adapter Contract

**Interface:** `ProviderAdapter` in `src/types/adapter.ts:12-24`. Every provider adapter implements this contract via `implements ProviderAdapter` (`MockProviderAdapter.ts:55`, `YouTubeProviderAdapter.ts:29`).

Required shape:
```ts
interface ProviderAdapter {
  readonly id: string;
  readonly name: string;
  readonly priority: number;
  readonly capabilities: ProviderCapabilities;
  search(query: string, context: ProviderContext, filterSongs?: boolean): Promise<Track[]>;
  stream(trackId: string, context: ProviderContext): Promise<StreamResult>;
  album(albumId: string, context: ProviderContext): Promise<Album>;
  artist(artistId: string, context: ProviderContext): Promise<Artist>;
  playlist(playlistId: string, context: ProviderContext): Promise<Playlist>;
  healthCheck(): Promise<AdapterHealth>;
}
```

**Capabilities** — `src/types/capabilities.ts:1-10`: `ProviderCapabilities` with boolean flags (`search`, `playback`, `playlists`, `albums`, `artists`, `recommendations`, `radio`, `downloads`, `lyrics`). Adapters declare their support matrix as a `public readonly` object (`MockProviderAdapter.ts:56`, `YouTubeProviderAdapter.ts:32`).

**Context** — `src/types/context.ts:1-7`: `ProviderContext` (`country`, `language`, `authenticated`, `preferredAudioQuality`, `device`, `traceId`) is threaded through every adapter call for request-scoped context and trace propagation.

**Health result** — `AdapterHealth` (`adapter.ts:5-10`): `{ status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY', score: number, latencyMs: number, message?: string }`.

**Capability-based selection:** `ProviderSelectionEngine.computeScore()` (`ProviderSelectionEngine.ts:24-54`) checks `adapter.capabilities[requiredCapability]` and returns `-1000` if incapable, gating provider selection on declared capabilities.

## Dependency Injection & Provider Registration

**Manual DI (no IoC container):** All core dependencies are constructed and wired manually inside `buildApp()` (`src/app.ts:66-83`):
```ts
const redis = redisClient || createRedis(config);
const cache = new CacheManager(redis, config);
const registry = new ProviderRegistry();
const engine = new ProviderSelectionEngine(registry);
const healthChecker = new RedisHealthChecker(redis, { timeoutMs: config.health?.redisTimeoutMs });
app.decorate('config', config);
app.decorate('registry', registry);
app.decorate('engine', engine);
app.decorate('cache', cache);
app.decorate('health', healthChecker);
```

**Fastify type augmentation:** `declare module 'fastify' { interface FastifyInstance { ... } }` in `app.ts:33-41` extends `FastifyInstance` with typed `config`, `registry`, `engine`, `cache`, `health` decorators.

**Provider registration** — `src/providers/registerProviders.ts:17-33`:
- Config-driven: an adapter registers only when its config `enabled` flag is not explicitly `false`.
- Signature: `registerProviders(app, registry, config, override?: ProviderRegistrationOverride)`.
- Mock: registered by default (`mockConfig == null || mockConfig.enabled !== false`) at priority 100 (`registerProviders.ts:24-25`).
- YouTube: registered only when `youtubeConfig.enabled === true` (`registerProviders.ts:28-31`).
- `ProviderRegistrationOverride` (`registerProviders.ts:7-10`) allows tests to force-enable/disable providers.

**Registry** — `src/core/registry/ProviderRegistry.ts`:
- Backing store: `Map<string, ProviderAdapter>` keyed by `adapter.id` (`ProviderRegistry.ts:4`).
- Methods: `register(adapter)` (`ProviderRegistry.ts:6`), `get(id)` (`ProviderRegistry.ts:10`), `getAll()` (`ProviderRegistry.ts:14`), `getSortedByPriority()` — sorts descending by `priority` (`ProviderRegistry.ts:18-20`), `clear()` (`ProviderRegistry.ts:22`).

## Layered Architecture (enforced)

```
types/          ← leaf: domain models, errors, adapter interface, capabilities, context
  ↑
config/         ← leaf: loads gateway.yaml + env overrides; depends only on externals
  ↑
core/           ← cache, circuit, events, health, logging, metrics, registry, selection
  ↑
providers/      ← mock/ + youtube/ adapters (leaf plugins) + registerProviders (composition)
  ↑
app.ts          ← composition root: builds Fastify app, registers plugins, defines routes
server.ts       ← entry point: buildApp() + listen + signal handling
```
Layer rules are enforced by `tests/architecture/layers.test.ts`.

## Code Generation (OpenAPI)

**Generation** — `scripts/generate-openapi.ts` (run via `npm run openapi:generate`, `package.json:15`): boots `buildApp()`, calls `app.swagger({ yaml: false })`, writes the JSON spec to `openapi.json`.

**Validation** — `scripts/validate-openapi.ts` (run via `npm run openapi:validate`, `package.json:16`): compares committed `openapi.json` path list against the live-generated spec; fails CI if paths drift (missing or stale).

**Route schemas** — all exported as named consts from `src/schemas.ts` (`bootstrapSchema`, `searchSchema`, `albumSchema`, `artistSchema`, `playlistSchema`, `streamSchema`, `healthSchema`, `versionSchema`, `providersSchema`, `metricsSchema`, `streamProxySchema`); routes reference them via `{ schema: ... }` in `app.ts`.

**Reusable schema subtypes** in `src/schemas.ts:1-149` — `TrackSchema`, `AlbumSchema`, `ArtistSchema`, `PlaylistSchema`, `StreamResultSchema`, `AdapterHealthSchema`, `ProviderCapabilitiesSchema`, `ProviderInfoSchema`, `ErrorResponseSchema`.

**Schema-driven validation:** Fastify validates request bodies/params/querystrings against the JSON-Schema before the handler runs — e.g. `POST /api/v1/stream` with empty payload returns 400 (`tests/contract/openapi.test.ts:64-78`).

## Response & Middleware Conventions

**Trace-ID propagation** — `app.ts:112-140`: an `onRequest` hook generates/echoes `x-trace-id` (from header or random), an `onResponse` hook logs completion with trace, and an `onSend` hook echoes the header back to the client.

**Structured logging** — `src/core/logging/logger.ts`: pino logger with `LOG_LEVEL` env (`logger.ts:5`), ISO timestamps, base `{ service: 'clibeats-gateway' }`. Context objects passed as the first argument: `logger.info({ traceId, method, url }, 'incoming request')` (`app.ts:118`).

**Event bus** — `src/core/events/EventBus.ts:3-22`: `GatewayEventPayload` discriminated union drives observability; `logger` and `metrics` both subscribe via `onEvent('*', ...)` (`app.ts` wiring at `logger.ts:14`, `metrics.ts:88`).

**Prometheus metrics** — `src/core/metrics/metrics.ts`: counters/gauges/histograms registered on a shared `prom-client.Registry`; metrics updated via `EventBus` listeners (`metrics.ts:4, 88-114`). `/metrics` route serves `register.metrics()` (`app.ts:467-470`).

---

*Convention analysis: 2026-08-09*
