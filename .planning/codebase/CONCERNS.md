# Codebase Concerns

**Analysis Date:** 2026-08-09

## Summary

The gateway is a Fastify-based provider-agnostic audio streaming gateway. The YouTube provider adapter has a partially-implemented PO token attestation flow (`ProviderTokenService`, `poToken/mint.ts`) that is wired into the adapter but blocked in production by an invalid Google WAA API key. Several architectural gaps exist around circuit breaker configuration, cache management, test coverage of the token flow, and stale build artifacts.

---

## Critical Concern

### PO token WAA API key rejected by Google (production blocker)

**What happens:** The `mintPoTokenPair()` function in `src/providers/youtube/poToken/mint.ts` (line 180) calls Google's WAA GenerateIT endpoint via `fetch(buildURL('GenerateIT', true), { headers: getHeaders(), ... })`. The `buildURL` and `getHeaders` helpers from `bgutils-js/utils` (declared in `src/providers/youtube/poToken/bgutils.d.ts` line 75 as `GOOG_API_KEY`) embed an API key that Google rejects with `API_KEY_INVALID` at `waa-pa.googleapis.com`. BotGuard VM execution succeeds (`bgutils-js` loads and runs the challenge bytecode), but the integrity token exchange fails, so no PO token is ever minted. YouTube's `getBasicInfo()` then returns `LOGIN_REQUIRED` with zero adaptive formats, yielding no stream URL.

**Impact:** All YouTube video playback returns `LOGIN_REQUIRED` in production (datacenter IPs). The `stream()` method at `src/providers/youtube/YouTubeProviderAdapter.ts` line 211 detects `playability_status.status === 'LOGIN_REQUIRED'` and throws `PlaybackError` (line 212-215). The retry-on-token-refresh at line 195-199 will repeatedly attempt to re-mint and fail, because the underlying WAA API key is invalid.

**Files:**
- `src/providers/youtube/poToken/mint.ts` — line 180 (WAA GenerateIT call), line 187 (HTTP error throw), line 30 (Platform.shim.eval monkeypatch)
- `src/providers/youtube/YouTubeProviderAdapter.ts` — lines 189-203 (stream PO token flow), lines 211-216 (LOGIN_REQUIRED detection)
- `src/providers/youtube/poToken/bgutils.d.ts` — line 75 (`GOOG_API_KEY` export)

**Fix approach:** Rotate to a valid server-restricted Google API key (set via environment variable, e.g. `GOOGLE_API_KEY`, not hardcoded in `bgutils-js`). Update `getHeaders()` call to inject the environment-sourced key, or fork `buildURL` to accept a key parameter.

---

## High Priority

### PO token service disabled in test environment (zero test coverage)

**What happens:** `registerProviders.ts` line 41 gates PO token service creation on `process.env.NODE_ENV !== 'test'`:
```typescript
if (poTokenConfig.enabled !== false && process.env.NODE_ENV !== 'test') {
  tokenService = new ProviderTokenService(...);
}
```
When `NODE_ENV=test` (all test suites set this), `tokenService` is `undefined`, and `YouTubeProviderAdapter.stream()` at line 189 falls back to `token = undefined`, creating an unauthenticated streaming session. Since most tests disable YouTube entirely (`youtube: { enabled: false }`), the PO token code path (`resolveStream` lines 206-254, `isPoTokenFailure` lines 256-262, `ProviderTokenService` lines 32-103, `mintPoTokenPair` lines 129-205) has **zero coverage**. The `youtube-adapter.test.ts` mocks `Innertube.create` entirely (line 21-26) and never tests PO token injection.

**Files:** `src/providers/registerProviders.ts` line 41; `src/providers/youtube/YouTubeProviderAdapter.ts` lines 189, 206-262; `src/providers/youtube/ProviderTokenService.ts`; `src/providers/youtube/poToken/mint.ts`

**Fix approach:** Add a test-only mint function that returns a synthetic `MintedPoToken` (with a mock PO token + visitor data), inject it into `ProviderTokenService`, and write unit tests that verify `stream()` passes `po_token` to `getBasicInfo`, retries on `LOGIN_REQUIRED`, and caches the token. Gate the real `mintPoTokenPair` import behind a lazy/dynamic import so tests can substitute.

### Unbounded in-memory `cdnUrlCache` (memory leak)

**What happens:** `app.ts` line 191 declares a module-level `Map` with no eviction:
```typescript
const cdnUrlCache = new Map<string, { url: string; total: number; expiresAt: number }>();
```
Entries are written at `app.ts` line 223-227 with an `expiresAt` timestamp, and the read path (`app.ts` line 197-198) checks `cached.expiresAt > Date.now()` — but **expired entries are never deleted**. Every unique `trackId` that flows through the proxy endpoint (`/api/v1/stream/proxy/:trackId`) accumulates an entry indefinitely. Under long-running production load with high track cardinality, this Map grows without bound, causing steady memory growth and eventual OOM.

**Files:** `src/app.ts` lines 191, 197-198, 223-227

**Fix approach:** Add periodic eviction (e.g., a `setInterval` that sweeps expired entries), or replace with an LRU/TTL cache library (e.g., `lru-cache` with `ttl` option), or use Redis with a TTL instead of an in-memory Map.

### CircuitBreaker configuration from gateway.yaml is ignored

**What happens:** The `GatewayConfig` interface (`src/config/config.ts` lines 20-23) defines per-provider `circuitBreaker.failureThreshold` and `circuitBreaker.cooldownSeconds`. The `config/gateway.yaml` file defines these values (lines 15-17 for YouTube: threshold=5, cooldown=60s; lines 22-24 for mock: threshold=3, cooldown=60s). However, `ProviderSelectionEngine.ts` line 18 instantiates circuit breakers with **hardcoded defaults** only:
```typescript
cb = new CircuitBreaker(providerId);  // defaults: failureThreshold=3, cooldownSeconds=60
```
The config values defined in `gateway.yaml` and typed in the config interface are never read, never passed to the `CircuitBreaker` constructor, and never used. The `CircuitBreaker` class (`src/core/circuit/CircuitBreaker.ts` lines 11-15) accepts `failureThreshold` and `cooldownSeconds` constructor parameters but they are always the defaults.

**Files:** `src/core/selection/ProviderSelectionEngine.ts` line 18; `src/config/config.ts` lines 20-23; `config/gateway.yaml` lines 15-17, 22-24; `src/core/circuit/CircuitBreaker.ts` lines 11-15

**Fix approach:** Pass the circuit breaker config from `GatewayConfig` through to `ProviderSelectionEngine`, then into `CircuitBreaker` constructor: `new CircuitBreaker(providerId, config.failureThreshold, config.cooldownSeconds)`.

### PO token / auth failures mapped to wrong error code (501 instead of 401)

**What happens:** `YouTubeProviderAdapter.errorCode()` at line 134 maps login/auth/PO token failures to `UnsupportedError` (HTTP 501 Not Implemented):
```typescript
if (/login|auth|po\s*token|verification/i.test(msg)) throw new UnsupportedError(msg, providerId);
```
`UnsupportedError` (defined in `src/types/error.ts` lines 58-62) maps to code `UNSUPPORTED` with HTTP status 501. This is semantically incorrect — a PO token failure is an authentication/attestation failure (should be `AUTHENTICATION_FAILED` / 401), not a "feature not implemented." The error schema in `src/schemas.ts` (lines 127-138) advertises `AUTHENTICATION_FAILED` as a valid error code, but the YouTube adapter never produces it.

**Files:** `src/providers/youtube/YouTubeProviderAdapter.ts` line 134; `src/types/error.ts` lines 34-62

**Fix approach:** Change the regex match to throw `AuthenticationFailedError` (line 34-38 of `error.ts`, which maps to code `AUTHENTICATION_FAILED`, HTTP 401). Add a dedicated `PO_TOKEN_REQUIRED` error code if finer granularity is needed.

### Global `globalThis` pollution in PO token minting

**What happens:** `src/providers/youtube/poToken/mint.ts` lines 149-158 mutate `globalThis` with DOM-like globals:
```typescript
Object.assign(globalThis, {
  yt: (dom.window as any).yt,
  window: dom.window,
  document: dom.window.document,
  location: dom.window.location,
  origin: dom.window.origin,
});
if (!('navigator' in globalThis)) {
  Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator });
}
```
This pollutes the global scope of the Node.js process with browser-like objects (`window`, `document`, `navigator`, `yt`, `location`, `origin`). If any other code (including `youtubei.js` internals) checks `typeof window !== 'undefined'` to detect a browser environment, it may take unintended code paths. In a multi-request Fastify server, this global state persists across requests and could cause race conditions if multiple concurrent minting operations run.

**Files:** `src/providers/youtube/poToken/mint.ts` lines 149-158, 163

**Fix approach:** Isolate the JSDOM environment in a VM context (`vm.createContext`) instead of polluting `globalThis`. Pass the context as `globalObject` to `BotGuardClient.create()` (line 167-171, which already accepts a `globalObject` option).

### `Platform.shim.eval` monkeypatched at module load time

**What happens:** `src/providers/youtube/poToken/mint.ts` line 30 executes at module import time:
```typescript
Platform.shim.eval = async (data: Types.BuildScriptResult) => new Function(data.output)();
```
This replaces youtubei.js's internal `Platform.shim.eval` for all `Innertube` instances in the process, not just the one used for PO token minting. It's a global side effect triggered by importing `mint.ts` (which is imported by `registerProviders.ts` at line 8). The `new Function(data.output)` call also executes arbitrary generated JavaScript — a security concern if `data.output` is ever tampered with.

**Files:** `src/providers/youtube/poToken/mint.ts` line 30; `src/providers/registerProviders.ts` line 8

**Fix approach:** Move the `Platform.shim.eval` assignment into the `mintPoTokenPair` function body (or a dedicated initialization function) rather than at module top-level. Scope the override to only the session used for minting.

### No `--max-old-space-size=4096` configured anywhere

**What happens:** PO token minting runs YouTube's player JS and BotGuard bytecode in a JSDOM environment via `new Function()` (`mint.ts` line 163). This is memory-intensive and requires `--max-old-space-size=4096` to avoid OOM errors (per the project's own `tmp_gen.js` prototype notes). However, no Node.js memory limit is configured:
- `Dockerfile` line 21: `CMD ["node", "dist/server.js"]` — no `NODE_OPTIONS`
- `package.json` scripts lines 7-9: `dev`, `start`, `test` — no `NODE_OPTIONS`
- `docker-compose.yml` — no `NODE_OPTIONS` env var
- `render.yaml` — no `NODE_OPTIONS` env var

When `registerProviders.ts` lines 51-55 warm the token in the background on startup, production containers may OOM-kill.

**Files:** `Dockerfile` line 21; `package.json` lines 7-9; `docker-compose.yml`; `render.yaml`; `src/providers/youtube/poToken/mint.ts` line 163

**Fix approach:** Add `NODE_OPTIONS=--max-old-space-size=4096` to the Dockerfile `ENV`, docker-compose `gateway` service env, and render.yaml envVars. Add it to the `start` npm script.

### No backoff or circuit breaker on PO token minting retries

**What happens:** `ProviderTokenService.doMint()` (`src/providers/youtube/ProviderTokenService.ts` lines 82-103) catches errors, sets `this.lastError`, and re-throws. There is no exponential backoff, no circuit breaker, and no rate limiting. When `getToken()` is called and the cached token is expired or absent, it calls `forceRefresh()` which calls `doMint()`. If minting fails (e.g., WAA API key invalid), the error propagates to the caller (`YouTubeProviderAdapter.stream()` line 189). On the next request, `getToken()` is called again, `forceRefresh()` is called again, and minting is attempted again — with no backoff. This means every stream request for YouTube triggers a full minting attempt (page fetch + BotGuard execution + WAA API call), hammering the broken WAA endpoint with no throttling.

**Files:** `src/providers/youtube/ProviderTokenService.ts` lines 48-69, 82-103; `src/providers/youtube/YouTubeProviderAdapter.ts` lines 189, 195-198

**Fix approach:** Add an exponential backoff with a max retry count and a failure cooldown. After N consecutive failures, stop attempting minting for a cooldown period and return the last error. Integrate with the `CircuitBreaker` pattern or a dedicated token-minting circuit.

### CircuitBreaker failsafe not checked before health check in `computeScore`

**What happens:** `ProviderSelectionEngine.computeScore()` (`src/core/selection/ProviderSelectionEngine.ts` lines 24-54) calls `cb.isAvailable()` at line 30 to skip providers with an OPEN circuit. But then at lines 42-51, it calls `adapter.healthCheck()` unconditionally and, in the catch block (lines 48-51), decrements the score by 50 without calling `cb.recordFailure()`. This means a health check failure does not contribute to tripping the circuit breaker. A provider that consistently fails health checks will still be selected until enough `executeWithFailover` calls fail the threshold. The health check and execution paths use different failure recording mechanisms.

**Files:** `src/core/selection/ProviderSelectionEngine.ts` lines 29-30, 42-51

**Fix approach:** Call `cb.recordFailure()` in the health check catch block, and avoid calling `healthCheck()` on providers whose circuit is already OPEN (though this is partially mitigated by the `isAvailable()` check at line 30, the `healthCheck()` call at line 43 happens after the score check, for all available providers on every request).

### Health check performs a live API search on every request for every provider

**What happens:** `ProviderSelectionEngine.computeScore()` line 43 calls `adapter.healthCheck()` for EVERY registered adapter on EVERY request (both `selectBestProvider` at line 68 and `executeWithFailover` at line 103 call `computeScore` in a loop). For the YouTube provider, `healthCheck()` (`YouTubeProviderAdapter.ts` lines 366-380) performs a live `yt.music.search('a')` API call to YouTube. This means every search, stream, album, artist, or playlist request triggers at least one live YouTube API round-trip for health scoring — even on cache hits (which return early but still call `executeWithFailover` → `computeScore` → `healthCheck` before the cache is checked... actually, wait — the cache is checked in the route handler before `executeWithFailover` is called).

Actually, let me re-check: in `app.ts` line 275-290, the search route checks `cache.search.get(query)` first. If cached, returns early — `executeWithFailover` is NOT called. If cache miss, calls `executeWithFailover`. For the `/api/v1/stream` route (line 342-371), there is NO cache check — it always calls `executeWithFailover`. So every stream request triggers health checks for all providers.

But even for cache misses on search, every request triggers `adapter.healthCheck()` for all providers. If YouTube is registered, that's a live API call per request to YouTube just for scoring.

**Files:** `src/core/selection/ProviderSelectionEngine.ts` lines 24-54 (computeScore), 67-68 (selectBestProvider loop), 102-103 (executeWithFailover loop); `src/providers/youtube/YouTubeProviderAdapter.ts` lines 366-380 (healthCheck does live search); `src/app.ts` lines 342-371 (stream endpoint has no cache check)

**Fix approach:** Cache health check results with a short TTL (e.g., 30s). Skip `healthCheck()` for providers whose circuit is OPEN. Use the existing `HealthCache` (`src/core/cache/segregated/HealthCache.ts`) that is already defined but never used.

### Stale compiled test artifacts committed alongside TypeScript sources

**What happens:** Compiled `.js` and `.d.ts` artifacts exist in the `tests/` directory alongside their `.ts` sources:
- `tests/integration/failover.test.js` — **missing `await`** on `buildApp()` call (line 11: `app = (0, app_1.buildApp)(...)` vs `.ts` line 12: `app = await buildApp(...)`). This compiled artifact would assign a Promise to `app` and then crash on `app.registry.register(...)`.
- `tests/integration/failover.test.d.ts` — trivial `export {};` (1 line)
- `tests/property/search-property.test.js` — **three divergences** from source: missing `await` on `buildApp()` (line 12), missing config argument (line 12: `buildApp()` vs `.ts` line 10: `buildApp({ providers: {...} })`), `maxLength: 100` vs 50 (`.ts` line 20), `numRuns: 100` vs 10 (`.ts` line 39)
- `tests/property/search-property.test.d.ts`, `tests/unit/core.test.js`, `tests/unit/core.test.d.ts`, `tests/load/load-test.js`, `tests/load/load-test.d.ts` — all compiled artifacts

The `tsconfig.json` line 16 excludes `tests` from compilation (`"exclude": ["node_modules", "dist", "tests"]`), so these artifacts were produced by a different/older configuration and committed. The `vitest.config.ts` line 7 explicitly excludes `*.test.js` from test execution, so they're inert — but their presence causes confusion and drift risk.

**Files:** `tests/integration/failover.test.js` (stale), `tests/integration/failover.test.d.ts` (stale); `tests/property/search-property.test.js` (stale), `tests/property/search-property.test.d.ts` (stale); `tests/unit/core.test.js` (stale), `tests/unit/core.test.d.ts` (stale); `tests/load/load-test.js` (stale), `tests/load/load-test.d.ts` (stale); `tsconfig.json` line 16 (excludes tests); `vitest.config.ts` line 7 (excludes *.test.js)

**Fix approach:** Delete all `.js`, `.d.ts`, and `.js.map` compiled artifacts from the `tests/` directory. Add `tests/**/*.js` and `tests/**/*.d.ts` to `.gitignore`. Add `tests/` to tsconfig `include` or create a separate tsconfig for tests if compilation is needed.

---

## Medium Priority

### `youtube-po-token-generator` declared in package-lock but missing from package.json

**What happens:** `package-lock.json` line 22 lists `"youtube-po-token-generator": "^0.6.0"` as a root dependency. However, `package.json` (lines 19-32) does NOT declare it. The only reference to the package is in the throwaway prototype `tmp_gen.js` (line 2: `require('youtube-po-token-generator/node_modules/jsdom')`), which works around jsdom not being a top-level dependency by reaching into the package's nested `node_modules`. The actual production code in `mint.ts` imports `jsdom` directly (`import { JSDOM } from 'jsdom'`) and uses `bgutils-js` directly — it does NOT use `youtube-po-token-generator` at all. Having the package in the lockfile but not in package.json creates a reproducibility hazard: `npm ci` installs it (from the lockfile), but any workflow that regenerates the lockfile (e.g., `npm install --package-lock-only`) would silently drop it.

**Files:** `package-lock.json` line 22; `package.json` lines 19-32 (no entry); `tmp_gen.js` line 2

**Fix approach:** Either add `youtube-po-token-generator` to package.json (if it's actually a needed dependency) or remove it from package-lock.json and delete `tmp_gen.js`. The package is not imported by any `src/` file.

### `jsdom` not a declared dependency but imported in production code

**What happens:** `src/providers/youtube/poToken/mint.ts` line 25 imports `import { JSDOM } from 'jsdom'`. However, `jsdom` is not listed in `package.json` dependencies or devDependencies. It's only available because it's a transitive dependency of `youtube-po-token-generator` that happens to be hoisted to the top-level `node_modules/`. This is fragile — a future `npm install` could nest `jsdom` under its actual parent package, causing a runtime `MODULE_NOT_FOUND` error when `mint.ts` tries to mint a PO token. The `tmp_gen.js` prototype (line 2) works around this by using the nested path, but `mint.ts` does not.

**Files:** `src/providers/youtube/poToken/mint.ts` line 25; `package.json` lines 19-46 (jsdom absent)

**Fix approach:** Add `jsdom` (or `jsdom` + appropriate types) to `package.json` dependencies. Alternatively, use `youtube-po-token-generator`'s bundled jsdom consistently, or switch to a lighter DOM implementation.

### Shallow config merge wipes provider configurations

**What happens:** `src/app.ts` line 68 performs a shallow spread merge:
```typescript
const config = { ...loadConfig(), ...customConfig };
```
When tests pass `{ providers: { mock: { enabled: false, priority: 0 } } }` (e.g., `failover.test.ts` line 12), the entire `providers` object from `loadConfig()` (which includes both `youtube` and `mock`) is replaced by the override. The YouTube provider config is lost. Tests work around this by explicitly including all providers in the override (e.g., `api.test.ts` line 9: `{ providers: { mock: { enabled: true, priority: 100 }, youtube: { enabled: false } } }`), but this is fragile — a future test that only overrides one provider property would silently wipe all other provider configs.

**Files:** `src/app.ts` line 68; `tests/integration/failover.test.ts` line 12 (works only because mock is fully disabled); `tests/integration/api.test.ts` line 9, `tests/integration/health.test.ts` line 13, `tests/unit/redis-cache-resilience.test.ts` line 14, `tests/integration/metrics.test.ts` line 9, `tests/property/search-property.test.ts` line 10

**Fix approach:** Implement a deep merge for the config object, or merge `providers` specifically: `{ ...loadConfig(), ...customConfig, providers: { ...loadConfig().providers, ...customConfig?.providers } }`.

### Hardcoded `providerId: 'youtube'` in stream proxy error response

**What happens:** In `src/app.ts` line 384, the error response for a failed CDN stream proxy uses a hardcoded provider ID:
```typescript
return reply.code(404).send({
  error: {
    code: 'STREAM_NOT_FOUND',
    message: resolved.message,
    providerId: 'youtube',
  },
});
```
This is returned by `resolveCdnStreamUrl` (line 193-233) which is called from the proxy endpoint (line 374). The `providerId` is always `'youtube'` regardless of which provider actually served the request via `executeWithFailover`. In a failover scenario where the stream was resolved through the mock provider or another provider, this error misattributes the source. Additionally, `resolved.message` at line 383 may contain provider-specific error details from `executeWithFailover`'s `InternalError` (line 144 of `ProviderSelectionEngine.ts`), but the providerId is always hardcoded to `'youtube'`.

**Files:** `src/app.ts` lines 378-386

**Fix approach:** Capture the providerId from the failover result or pass it through `resolveCdnStreamUrl`. The `executeWithFailover` method should return the providerId that succeeded alongside the result.

### Stream resolution endpoint does not use the in-memory CDN cache

**What happens:** The `POST /api/v1/stream` endpoint (`src/app.ts` lines 342-371) always calls `engine.executeWithFailover('playback', ...)` fresh — it does NOT check the in-memory `cdnUrlCache`. Only the proxy endpoint (`/api/v1/stream/proxy/:trackId`, lines 374-444) calls `resolveCdnStreamUrl` which checks `cdnUrlCache`. This creates an inconsistency:
- Direct stream URL requests always hit the provider (and incur PO token checks, health checks, etc.)
- Proxy stream requests benefit from the CDN URL cache

When `config.stream.proxyStreaming` is `true` (as in `gateway.yaml` line 36), the `/api/v1/stream` endpoint rewrites the URL to `/api/v1/stream/proxy/{trackId}` (line 364). So the flow is: POST /stream → resolve stream → rewrite to proxy URL → client calls GET proxy → checks cdnUrlCache. The cache IS used, but only via the proxy path. The POST /stream endpoint itself never caches. If `proxyStreaming` is `false`, the stream URL is returned directly with no caching at all.

**Files:** `src/app.ts` lines 342-371 (POST /stream, no cache check), lines 191-233 (cdnUrlCache used only by `resolveCdnStreamUrl`), line 359 (proxyStreaming config)

**Fix approach:** Use Redis (`cache.session` or a dedicated stream cache) for stream URL caching, or check `cdnUrlCache` in the POST /stream handler as well.

### CircuitBreaker state is not persisted across restarts

**What happens:** The `CircuitBreaker` class (`src/core/circuit/CircuitBreaker.ts` lines 6-57) stores all state in memory: `state`, `failureCount`, `lastStateChangeEpoch`. When the gateway process restarts (e.g., on deploy or crash), all circuit breakers reset to `CLOSED` with `failureCount = 0`. This means a provider that was just tripped (OPEN) will immediately be tried again after a restart, potentially hammering a still-failing upstream. There is no Redis backing store for circuit state, despite Redis being available (`redis: ioredis` is injected throughout the app).

**Files:** `src/core/circuit/CircuitBreaker.ts` lines 6-57 (in-memory only)

**Fix approach:** Persist circuit state to Redis using the existing `RedisCacheBase` pattern. On construction, hydrate state from Redis. On state transitions, write to Redis.

### `HealthCache` is defined but never used

**What happens:** `src/core/cache/segregated/HealthCache.ts` (lines 1-25) defines a cache for provider health records with a 300-second default TTL. It's instantiated in `CacheManager.ts` line 28 (`this.health = new HealthCache(redis, undefined, prefix)`), but `HealthCache` is never read or written anywhere in the codebase. The `ProviderSelectionEngine` (`src/core/selection/ProviderSelectionEngine.ts`) calls `adapter.healthCheck()` directly on every request instead of checking the health cache first. This dead code wastes a cache instance that could have reduced the per-request health check load.

**Files:** `src/core/cache/segregated/HealthCache.ts` (fully implemented but unused); `src/core/cache/CacheManager.ts` line 28 (instantiated); `src/core/selection/ProviderSelectionEngine.ts` lines 42-51 (bypasses cache)

**Fix approach:** In `computeScore`, check `cache.health.getHealth(adapter.id)` before calling `adapter.healthCheck()`. If a recent health record exists (within TTL), use its cached score instead of making a live API call.

### Debug endpoint `/debug-yt` exposed without authentication

**What happens:** `src/app.ts` lines 486-531 defines a `/debug-yt` endpoint marked `{ schema: { hide: true } }` (line 486), meaning it's hidden from the OpenAPI spec but still accessible. It iterates over all `ClientType` values (lines 491-499), makes real `Innertube.create()` and `yt.getBasicInfo()` calls to YouTube (line 503-504), and returns playability status, format counts, and URL availability for each client type. It also exposes PO token service diagnostics (lines 524-528). This endpoint:
- Makes unauthenticated external API calls to YouTube
- Reveals internal provider capabilities and failure modes
- Could be abused for YouTube API quota consumption
- Is accessible without authentication (no auth middleware on this route)

**Files:** `src/app.ts` lines 486-531

**Fix approach:** Either remove the endpoint entirely, gate it behind an admin authentication middleware, or restrict it to localhost/loopback only.

---

## Low Priority

### `validate-phase-a-gateway.ts` sets `NODE_ENV=test` which disables PO token minting

**What happens:** `scripts/validate-phase-a-gateway.ts` line 1 sets `process.env.NODE_ENV = 'test'`. This causes `registerProviders.ts` line 41 to skip PO token service creation. The script (lines 59-70) then attempts to resolve YouTube streams for tracks found via search (queries like 'Wonderwall', 'Believer'). Without a PO token service, YouTube's `stream()` returns `LOGIN_REQUIRED` → `PlaybackError` → failover to mock → mock throws `NotFoundError` (mock doesn't have YouTube track IDs) → all candidates fail → `InternalError` is thrown (line 144 of `ProviderSelectionEngine.ts`) → script assertion at line 68 fails. The validation script cannot verify YouTube streaming end-to-end because the token service is disabled in test mode.

**Files:** `scripts/validate-phase-a-gateway.ts` line 1; `src/providers/registerProviders.ts` line 41; `src/providers/YouTubeProviderAdapter.ts` lines 189-203

**Fix approach:** Use a separate env var (e.g., `SKIP_PO_TOKEN_MINTING`) instead of `NODE_ENV` to gate PO token minting, or inject a mock mint function in the validation script.

### `new Function()` execution of remote BotGuard interpreter (security)

**What happens:** `src/providers/youtube/poToken/mint.ts` line 163:
```typescript
new Function(interpreterJavascript)();
```
This fetches the BotGuard interpreter JavaScript from a URL (`https:${challenge.interpreterUrl}`, line 161) and executes it via `new Function()`. If the interpreter URL is compromised or redirected, arbitrary code execution occurs in the server process. There is no integrity check (SRI), no subresource integrity verification, and no CSP.

**Files:** `src/providers/youtube/poToken/mint.ts` lines 161-163

**Fix approach:** Pin the interpreter URL to a known version, fetch it over HTTPS (it already does), and consider validating a checksum of the expected interpreter content. Alternatively, bundle the interpreter at build time.

### `errorCode` catch blocks rely on `never` return type (fragile pattern)

**What happens:** All public methods in `YouTubeProviderAdapter` end their catch blocks with `this.errorCode(e)` — a bare call without `throw` or `return`:
```typescript
} catch (e) {
  this.errorCode(e);
}
```
The `errorCode` method is declared as `: never` (`YouTubeProviderAdapter.ts` line 127), so TypeScript's control-flow analysis knows it always throws and the method has no missing return path. However, this is fragile: if a future maintainer changes `errorCode`'s return type or makes it return in some edge case (e.g., adding a fallback return for logging), the bare call would silently swallow the error and the method would return `undefined`, which would then be serialized as an HTTP response. The pattern relies entirely on the `never` type annotation rather than an explicit `throw`.

**Files:** `src/providers/youtube/YouTubeProviderAdapter.ts` lines 127 (declared `never`), 158, 202, 306, 327, 361 (bare `this.errorCode(e)` calls in catch blocks)

**Fix approach:** Change all catch sites to `throw this.errorCode(e)` — but since `errorCode` is `never`, TypeScript won't allow `throw` on it. Instead, make `errorCode` always `throw` explicitly (it already does), and add `return` statements or restructure to make the throw explicit. Or better: change the catch blocks to `throw` directly with a descriptive error.

### No `.gitignore` file

**What happens:** There is no `.gitignore` file in the repository root or in `src/`. This means:
- Compiled test artifacts (`.js`, `.d.ts`, `.js.map` in `tests/`) can be committed (they currently are)
- `coverage/` reports can be committed
- `.env` files (if created) can be committed, risking secret leakage
- `node_modules/` can be committed

The `package.json` doesn't define an `engines` field either, so Node.js version is not enforced.

**Files:** Repository root (no `.gitignore` exists)

**Fix approach:** Create a `.gitignore` with entries for `node_modules/`, `coverage/`, `dist/`, `tests/**/*.js`, `tests/**/*.d.ts`, `tests/**/*.js.map`, `.env*`, `*.log`.

### No `engines` field in package.json (Node.js version not pinned)

**What happens:** `package.json` (lines 1-47) has no `engines` field. The `Dockerfile` uses `node:20-alpine` (line 1, 9), and the `bgutils.d.ts` file (line 9) comment states "Node >= 20.19 / 22.x" is required for `require(esm)` support of bgutils-js ESM subpath exports. Without an `engines` field, developers or deployment pipelines could run on an incompatible Node.js version, causing runtime failures when `mint.ts` imports `bgutils-js/botguard` etc.

**Files:** `package.json` lines 1-47; `Dockerfile` lines 1, 9; `src/providers/youtube/poToken/bgutils.d.ts` line 9

**Fix approach:** Add `"engines": { "node": ">=20.19.0" }` to `package.json`. Add `package-lock.json` `"engines"` enforcement via `.npmrc` (`engine-strict=true`).

### MockProvider `'PLAYBACK_ERROR'` state is not in the `MockProviderState` enum

**What happens:** `MockProviderAdapter.ts` line 94 uses a type-unsafe double cast:
```typescript
this.state = value ? 'PLAYBACK_ERROR' as unknown as MockProviderState : 'HEALTHY';
```
`'PLAYBACK_ERROR'` is NOT a member of the `MockProviderState` type (defined at lines 34-42: `HEALTHY | SLOW | OFFLINE | MALFUNCTIONED | RATE_LIMITED | AUTHENTICATION_FAILED | GEO_BLOCKED | INTERNAL_ERROR`). The `as unknown as MockProviderState` cast bypasses TypeScript's type checking. The `simulateFailure()` method (lines 175-196) handles this via the `default` case (line 193-194: `throw new PlaybackError(...)`), but the `healthCheck()` method (lines 270-301) does NOT have a `PLAYBACK_ERROR` case — it checks `state === 'OFFLINE'` (line 271), `state === 'SLOW'` (line 279), and `state !== 'HEALTHY'` (line 288, which covers everything else including the phantom `PLAYBACK_ERROR`). So `PLAYBACK_ERROR` state reports as `DEGRADED` with score 40 via the default catch-all, not as a distinct failure mode.

**Files:** `src/providers/mock/MockProviderAdapter.ts` lines 34-42 (enum), 93-95 (type-unsafe cast), 99-101 (another cast), 193-194 (default case), 270-301 (healthCheck)

**Fix approach:** Add `'PLAYBACK_ERROR'` to the `MockProviderState` enum and handle it explicitly in `simulateFailure()` and `healthCheck()`. Remove the `as unknown as` casts.

### `openapi.test.ts` calls `buildApp()` with default config (YouTube enabled, no PO token in test)

**What happens:** `tests/contract/openapi.test.ts` line 9: `app = await buildApp()` with no custom config or redisClient. This means:
1. `loadConfig()` reads `config/gateway.yaml` → YouTube is enabled with priority 100
2. `registerProviders` creates the `ProviderTokenService` but only if `NODE_ENV !== 'test'` (line 41) — and vitest sets `NODE_ENV=test` by default, so no token service
3. The `/api/v1/stream` endpoint test (lines 64-78) POSTs `{ trackId: 'mock-track-1' }` — this goes through `executeWithFailover('playback', ...)`. YouTube (priority 100) is tried first, fails (no PO token → LOGIN_REQUIRED → PlaybackError → UnsupportedError), then mock (priority 10) is tried with trackId 'mock-track-1' which exists in mock. So the test passes via failover, but YouTube's circuit breaker trips.

The test passes by accident (failover to mock), but it doesn't validate that YouTube streaming works. The `openapi.test.ts` also doesn't test the `/api/v1/stream/proxy/{trackId}` endpoint at all (line 36 lists it in expected paths, but no test exercises it).

**Files:** `tests/contract/openapi.test.ts` lines 9, 64-78

**Fix approach:** Explicitly disable YouTube in test config where YouTube streaming is not the focus, or provide a mock PO token service. Add tests for the proxy endpoint.

---

*Concerns audit: 2026-08-09