<!-- refreshed: 2026-08-09 -->
# Testing Patterns

**Analysis Date:** 2026-08-09

## Test Framework

**Runner:** Vitest 3.0.4 (`package.json:44`)
**Config:** `gateway/vitest.config.ts:4-21`
```ts
export default defineConfig({
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
    exclude: ['tests/**/*.test.js', 'tests/**/*.test.js.map', 'node_modules/**', 'dist/**'],
    coverage: { provider: 'v8', ... },
  },
});
```
**Assertion library:** Vitest built-in `expect` — Jasmine-style API (`toBe`, `toEqual`, `toMatchObject`, `toThrow`, `rejects.toThrow`, `toHaveLength`, `toContain`, `toBeGreaterThan`, `resolves.toBeUndefined`).
**Property testing:** `fast-check` 3.23.2 (`package.json:39`) — `fc.assert`, `fc.asyncProperty`, `fc.fullUnicodeString`.
**Load testing:** `autocannon` 8.0.0 + `@types/autocannon` 7.12.7 (`package.json:34,38`).
**Redis mock:** `ioredis-mock` 8.9.0 + `@types/ioredis-mock` 8.2.8 (`package.json:40,41`).

## Run Commands

```bash
npm run check              # tsc --noEmit — type-check gate (package.json:9)
npm test                   # vitest run — all test suites (package.json:11)
npm run test:watch         # vitest — watch mode (package.json:12)
npm run test:coverage      # vitest run --coverage — with coverage gates (package.json:13)
npm run test:load          # ts-node tests/load/load-test.ts — autocannon (package.json:14)
npm run openapi:generate   # ts-node scripts/generate-openapi.ts (package.json:15)
npm run openapi:validate   # ts-node scripts/validate-openapi.ts (package.json:16)
npm run ci                 # check && test && openapi:generate && openapi:validate && test:load (package.json:17)
```

## Test File Organization

**Location:** `gateway/tests/` — separate from `src/`, organized by concern category (not co-located with source).
**Naming:** `.test.ts` suffix on all test files — matched by `vitest.config.ts:6`: `include: ['tests/**/*.test.ts']`.
**Exclude:** `.test.js` / `.test.js.map` compiled artifacts are excluded (`vitest.config.ts:7`) so stale builds don't double-run.

```
gateway/tests/
├── unit/
│   ├── core.test.ts                     # ProviderRegistry, ServiceSelectionEngine, CircuitBreaker
│   ├── mock-provider.test.ts            # MockProviderAdapter failure matrix (8 states)
│   ├── redis-cache-resilience.test.ts   # Cache namespacing, fail-open, TTL
│   ├── redis-health.test.ts             # RedisHealthChecker UP/DEGRADED/DOWN/timeout
│   └── youtube-adapter.test.ts          # youtubei.js mocking, mapping, error wrap
├── integration/
│   ├── api.test.ts                      # endpoint coverage via app.inject + trace ID
│   ├── failover.test.ts                 # provider failover + circuit breaker trip
│   ├── health.test.ts                   # /health with mocked/failing Redis
│   └── metrics.test.ts                  # /metrics Prometheus output
├── contract/
│   └── openapi.test.ts                  # OpenAPI spec generation + schema validation
├── property/
│   └── search-property.test.ts          # fast-check fuzz on search endpoint
├── architecture/
│   └── layers.test.ts                   # static import-graph layering rules
└── load/
    └── load-test.ts                     # autocannon load test
```

## Test Structure & Suite Organization

**Suite pattern:** `describe(name, () => { ... })` containing `it(name, async () => { ... })` blocks. Imports come from `vitest` directly — no external assertion library:
```ts
// tests/unit/core.test.ts:1-9
import { describe, it, expect, beforeEach } from 'vitest';

describe('Gateway Core Unit Tests', () => {
  let registry: ProviderRegistry;
  let engine: ProviderSelectionEngine;
  let mockAdapter: MockProviderAdapter;
  ...
  beforeEach(() => {
    registry = new ProviderRegistry();
    engine = new ProviderSelectionEngine(registry);
    mockAdapter = new MockProviderAdapter('mock', 42);
    registry.register(mockAdapter);
  });
  it('ProviderRegistry registers and retrieves sorted adapters', () => {
    expect(registry.getAll().length).toBe(1);
    ...
  });
});
```

**Setup / teardown conventions:**
- `beforeEach` + `afterEach` for full app lifecycle — `tests/integration/api.test.ts:8-15`:
```ts
beforeEach(async () => {
  app = await buildApp({ providers: { mock: { enabled: true, priority: 100 }, youtube: { enabled: false } } });
  await app.ready();
});
afterEach(async () => { await app.close(); });
```
- `beforeAll` + `afterAll` for shared external resources (Redis mock) — `tests/unit/redis-cache-resilience.test.ts:12-19`.
- Per-file `const ctx: ProviderContext = { ... }` shared across `it` blocks — `mock-provider.test.ts:13`, `youtube-adapter.test.ts:8`, `core.test.ts:13`.

**Assertion styles observed:**
| Pattern | Example |
|---------|---------|
| Strict equality | `expect(adapter.id).toBe('mock')` |
| Deep equality | `expect(MOCK_PROVIDER_STATES).toEqual([...])` |
| Partial deep | `expect(await mock.healthCheck()).toMatchObject({ status: 'HEALTHY', score: 100 })` |
| Throws (async) | `await expect(mockAdapter.search('x', ctx)).rejects.toThrow(NotFoundError)` |
| Throws (object) | `await expect(adapter.search('x', ctx)).rejects.toMatchObject({ code: 'RATE_LIMITED' })` |
| Collection size | `expect(keys).toHaveLength(1)`, `expect(results.length).toBeGreaterThan(0)` |
| Containment | `expect(stream.streamUrl).toContain('mock-cdn.clibeats.internal')` |
| Resolves | `await expect(broken.set('query', [])).resolves.toBeUndefined()` |
| Numeric range | `expect(score).toBeGreaterThan(100)`, `expect(result.latencyMs).toBeGreaterThanOrEqual(0)` |

## Mocking Strategies

**1. Module mocking — `vi.mock()`:** Replaces an entire module's implementation.

```ts
// tests/unit/youtube-adapter.test.ts:17-26
const mockSearch = vi.fn();
const mockGetBasicInfo = vi.fn();
const mockCreate = vi.fn();
vi.mock('youtubei.js', () => ({
  ClientType: { MUSIC: 'MUSIC', IOS: 'IOS' },
  Innertube: { create: (...args: unknown[]) => mockCreate(...args) },
}));
```
- Mocks are declared with `vi.fn()` **before** `vi.mock()` (hoisting requirement); `vi.clearAllMocks()` runs in `beforeEach` (`youtube-adapter.test.ts:30`).
- Tests assert on the mock via `expect(mockCreate).toHaveBeenCalledWith(expect.objectContaining({ client_type: 'MUSIC' }))` (`youtube-adapter.test.ts:197`).

**2. Redis mocking — `ioredis-mock`:** Full drop-in Redis mock (`package.json:40`) with types declared in `src/types/declarations.d.ts:1-5`.
```ts
// tests/unit/redis-cache-resilience.test.ts:2-15
import RedisMock from 'ioredis-mock';
import { SearchCache } from '../../src/core/cache/segregated/SearchCache';

beforeAll(() => {
  redis = new RedisMock();
  search = new SearchCache(redis as never, 3600, 'clibeats');
});
afterAll(async () => { await redis.quit(); });
```
- `as never` / `as unknown as Redis` casts bridge type differences between the mock and the real `Redis` type.
- **Implicit mock:** `buildApp()` auto-substitutes `ioredis-mock` when `process.env.NODE_ENV === 'test'` (`src/app.ts:44-47`), so integration tests get a real mock backend with zero explicit setup.

**3. Inline fake objects:** Structural fakes for Redis or adapter interfaces.
```ts
// tests/unit/redis-health.test.ts:5-6
const makeMockRedis = (ping: () => Promise<string>) => ({ ping } as any);

// tests/integration/health.test.ts:24-35
const failingRedis = {
  get: async () => { throw new Error('Redis unavailable'); },
  set: async () => { throw new Error('Redis unavailable'); },
  ping: async () => { throw new Error('Redis unavailable'); },
};
const app = await buildApp({}, failingRedis as unknown as Redis);
```

**4. `MockProviderAdapter` as a canonical test double:** The mock adapter itself is the primary double for integration/failover tests. Its `state` property (`'HEALTHY' | 'SLOW' | 'OFFLINE' | ...`) or the legacy `shouldSimulateError` / `simulatedErrorCode` shims configure failure behavior.

**5. Fake timers — `vi.useFakeTimers()`:** Used for the Redis ping timeout path (`redis-health.test.ts:41-48`):
```ts
vi.useFakeTimers();
const pending = checker.check();
await vi.advanceTimersByTimeAsync(60);
const result = await pending;
expect(result.status).toBe('DOWN');
expect(result.message).toContain('timed out');
vi.useRealTimers();
```

**What to mock:** External third-party modules (`youtubei.js`), Redis I/O (`ioredis-mock` or inline fakes), and provider network boundaries (via `MockProviderAdapter` states).

**What NOT to mock:** Core gateway orchestration logic (`ProviderSelectionEngine`, `CircuitBreaker`, `ProviderRegistry`, `CacheManager`) — tested directly with real instances. The `MockProviderAdapter` is used as a real test double rather than being spied on.

## Fixtures & Factories

- **Shared `ProviderContext`:** Each consumer test file declares a module-level `const ctx: ProviderContext = { country: 'US', language: 'en', authenticated: false, preferredAudioQuality: 'HIGH', device: 'mobile', traceId: 'test-trace' }` reused across all `it` blocks (`mock-provider.test.ts:13-20`, `youtube-adapter.test.ts:8-15`, `core.test.ts:13-20`).
- **Deterministic mock data:** `MockProviderAdapter` uses a seeded PRNG (`seed=42`) to generate a stable dataset — 5 artists, 10 albums × 10 tracks (100 tracks), 5 playlists (`MockProviderAdapter.ts:16-29, 103-166`). This eliminates random-data flakiness.
- **No centralized fixture dir:** Test data is constructed inline per-file; there is no shared `fixtures/` or `factories/` directory under `tests/`.

## Coverage

**Tool:** `@vitest/coverage-v8` 3.2.7 (`package.json:37`) — V8-native instrumentation.
**Config:** `gateway/vitest.config.ts:8-19`:
```ts
coverage: {
  provider: 'v8',
  reporter: ['text', 'json-summary', 'lcov'],
  reportsDirectory: './coverage',
  include: ['src/**/*.ts'],
  exclude: ['src/**/declarations.d.ts', 'src/server.ts'],
  thresholds: { statements: 70, branches: 70, functions: 70, lines: 70 },
}
```
**Enforcement:** 70% minimum on statements, branches, functions, and lines. `npm run test:coverage` (`package.json:13`) fails the run if any threshold is unmet.
**Exclusions:** `src/types/declarations.d.ts` (ambient module declarations) and `src/server.ts` (bootstrap-only entry point with signal handling) are excluded from coverage.
**View coverage:** `npm run test:coverage` prints a text summary to stdout and writes `coverage/lcov.info` + `coverage/coverage-summary.json`.

## Test Categories

### Unit Tests (`tests/unit/`)
Pure logic tests with no real HTTP or Redis I/O.

- **`core.test.ts`** — `ProviderSelectionEngine` + `CircuitBreaker` + `ProviderRegistry`. Registers a real `MockProviderAdapter` and asserts: `computeScore() > 100` for a healthy provider, `selectBestProvider` picks the highest scorer, and `CircuitBreaker` state transitions `CLOSED → OPEN` after 3 failures then back to `CLOSED` on success (`core.test.ts:45-60`).
- **`mock-provider.test.ts`** — Failure matrix: iterates all 8 `MockProviderState` values, asserting each throws the correct `ProviderError` subclass and reports the correct `AdapterHealth` (`mock-provider.test.ts:22-96`).
- **`redis-cache-resilience.test.ts`** — Key namespacing (`clibeats:search:hello`), namespace collision avoidance across domains, fail-open on Redis read error (`get` returns `null`), fail-open on write error (resolves without throwing), session TTL behavior, single-key invalidation.
- **`redis-health.test.ts`** — `RedisHealthChecker` via `{ ping: () => Promise<string> }` fakes: UP (PONG), DEGRADED (unexpected reply), DOWN (reject), DOWN on timeout (fake timers).
- **`youtube-adapter.test.ts`** — `vi.mock('youtubei.js')` — search→`Track` mapping (extracts id/title/artist/duration/artwork from `flex_columns`), stream format selection (highest `average_bitrate` audio-only format), `NotFoundError` when no audio format, regex rate-limit→`RateLimitedError` wrap, `healthCheck` UP/DOWN, MUSIC vs IOS client-type selection, and pure helper `media` mappers (`parseRawItem`, `parseSubtitle`, `largestArtworkUrl`).

### Integration Tests (`tests/integration/`)
Boot the full Fastify app via `buildApp(...)` + `await app.ready()`, drive requests with `app.inject({ method, url, payload, headers })`.

- **`api.test.ts`** — Endpoint coverage: `/api/v1/bootstrap` (cold-start context with provider health), `/api/v1/search`, `POST /api/v1/stream` (stream URL resolution), `/health`, `/metrics`, `/version`, `/documentation`. Verifies trace-ID echo: request with `x-trace-id` header → response has matching header (`api.test.ts:100-109`).
- **`failover.test.ts`** — Sets `primaryMock.shouldSimulateError = true`, asserts the secondary provider (`secondary-mock`) serves the request (`tracks[0].providerId === 'secondary-mock'`); then verifies the `CircuitBreaker` opens after 3 failures (`cb.getState() === 'OPEN'`).
- **`health.test.ts`** — Injects a failing Redis (`{ ping: async () => { throw ... } }`) into `buildApp({}, failingRedis)`, asserts `redis: 'DOWN'`, `gateway: 'DEGRADED'`, and the "health does not lie" invariant (HTTP 200 even when Redis is down).
- **`metrics.test.ts`** — Asserts all 9 Prometheus metric names are present in `/metrics` output and that cache hit/miss counters increment after two identical search requests.

### Contract Tests (`tests/contract/`)
- **`openapi.test.ts`** — Validates the live OpenAPI spec generated from route schemas via `app.swagger({ yaml: false })`:
  - Spec version matches `3.x` (`openapi.test.ts:21`).
  - Exact path list contract (11 paths) (`openapi.test.ts:28-40`).
  - Every operation documents `tags`, `description`, `responses` including a 200 and an error/default response.
  - Request validation: missing `trackId` body → HTTP 400; valid payload → 200 (`openapi.test.ts:64-78`).
  - Deterministic / idempotent generation: `JSON.stringify(spec1).toBe(JSON.stringify(spec2))` (`openapi.test.ts:80-84`).

### Property Tests (`tests/property/`)
- **`search-property.test.ts`** — Uses `fast-check` `fc.asyncProperty(fc.fullUnicodeString({ maxLength: 50 }), ...)` to fuzz `/api/v1/search` with 10 random Unicode queries (`{ numRuns: 10 }`), asserting HTTP 200 + schema invariants: every returned track has `id`/`providerId`/`title`/`artist` as strings and `durationSeconds` as a number.

### Architecture Tests (`tests/architecture/`)
- **`layers.test.ts`** — Static analysis: walks `src/`, parses `from '...'` imports via regex, asserts layering rules:
  - `core/` never imports `providers/` (`layers.test.ts:41-52`).
  - `providers/**` (except `registerProviders.ts`) never imports `core/`, `config/`, or `app` (`layers.test.ts:54-65`).
  - `types/` is a leaf — no imports of `core/`, `providers/`, `config/`, or `app` (`layers.test.ts:67-78`).
  - `config/config.ts` depends only on external packages (`layers.test.ts:80-84`).
  - `registerProviders.ts` composes both `core` + concrete adapters, proving the DI wiring is real, not circular (`layers.test.ts:86-91`).

### Load Tests (`tests/load/`)
- **`load-test.ts`** — Sets `NODE_ENV=test`, boots `buildApp()`, listens on ephemeral port, runs `autocannon({ url: \`${address}/api/v1/search?q=cyber\`, connections: 100, duration: 10, pipelining: 1 })`. Logs requests/sec, average/p99 latency, 2xx vs non-2xx counts. Exits non-zero (`process.exit(1)`) if any non-2xx response occurs. Run via `npm run test:load` (`package.json:14`).

## CI Pipeline

**`package.json:17` — `ci` script (full ordered gate):**
```json
"ci": "npm run check && npm test && npm run openapi:generate && npm run openapi:validate && npm run test:load"
```
Execution order: type-check → all test suites → OpenAPI generation → OpenAPI validation → load test.

A typical CI run invokes each stage individually:
```bash
npm run check              # tsc --noEmit
npm test                   # vitest run — unit + integration + contract + property + architecture
npm run openapi:generate   # regenerate openapi.json from live route schemas
npm run openapi:validate   # diff committed openapi.json vs live — fail on drift
npm run test:load          # autocannon against search endpoint
```

---

*Testing analysis: 2026-08-09*
