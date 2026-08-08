# Codebase Concerns

**Analysis Date:** 2026-08-08

Scope: full repo - Android/Kotlin app (`app/`) and TypeScript provider gateway (`gateway/`). Sources: source inspection, git history (InnerTube stack removal in commit `a7a3f9a`), `.planning/INFRA-FIX-REPORT.md`, `.planning/phases/03-database-local-persistence-layer/deferred-items.md`, `.planning/WINDOWS.md`.

---

## Tech Debt

### Gateway: declared-but-unused packages (`dotenv`, `pino-pretty`)
- Issue: `dotenv` (^16.4.7) and `pino-pretty` (^13.0.0) are in `gateway/package.json` dependencies but have zero imports anywhere in `gateway/src/`. Kept deliberately ("not removed to avoid scope creep") per `.planning/INFRA-FIX-REPORT.md` §3.
- Files: `gateway/package.json:23,27`
- Impact: unnecessary install weight and CI install time; confusion about configuration mechanism (env vars are read via `process.env` directly, see `gateway/src/config/config.ts:56,67,85-87`).
- Fix: remove both from `gateway/package.json`, run `npm install` to prune `package-lock.json`.

**Gateway: dead configuration (`circuitBreaker` per-provider, `streamTTLSeconds`)**
- Issue: `config/gateway.yaml` declares per-provider `circuitBreaker.failureThreshold` / `cooldownSeconds` (mock 3/60, youtube 5/60) but `ProviderSelectionEngine.getCircuitBreaker()` hardcodes `new CircuitBreaker(providerId)` with defaults (3 failures, 60s) - YAML values are never read. Similarly `cache.streamTTLSeconds` (900) is defined but the `/api/v1/stream` route in `gateway/src/app.ts:280-299` performs no caching at all; stream URLs are never Redis-cached and their TTL is never enforced.
- Files: `gateway/config/gateway.yaml:11-25`, `gateway/src/core/selection/ProviderSelectionEngine.ts:15-20`, `gateway/src/core/circuit/CircuitBreaker.ts:12-15`, `gateway/src/config/config.ts:20-22,32`
- Impact: ops tuning knobs silently have no effect; every stream request re-resolves upstream with no dedupe or TTL protection.
- Fix: thread config into `CircuitBreaker` construction (`new CircuitBreaker(providerId, cfg.failureThreshold, cfg.cooldownSeconds)`), and implement stream caching keyed by `trackId` using the existing `RedisCacheBase` infra.

**Gateway: unused cache namespaces (`SessionCache`, `ArtworkCache`, `HealthCache`)**
- Issue: `CacheManager` instantiates `session`, `artwork`, `health` caches (`gateway/src/core/cache/CacheManager.ts:26-28`), but there are no call sites for `cache.session`, `cache.artwork`, or `cache.health` anywhere in `gateway/src/`. The whole session-management feature (ADR-015) is scaffolding: `YouTubeProviderAdapter` memoizes a single anonymous Innertube session (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts:63-83`) and never reads or writes `SessionCache`, and `getContext()` never produces a userId.
- Files: `gateway/src/core/cache/CacheManager.ts:16-18,26-28`, `gateway/src/core/cache/segregated/SessionCache.ts`, `gateway/src/core/cache/segregated/ArtworkCache.ts`, `gateway/src/core/cache/segregated/HealthCache.ts`
- Impact: ad-hoc session API with no consumers; the "session per user" design is unimplemented; provider health records live only in memory (no persistence).
- Fix: wire session usage when YouTube signed-in mode lands, or delete the unused namespaces. Document in ADR-015.

**Gateway: `SessionCache` domain-specific (non-generic) API**
- Issue: `SessionCache` exposes `getSession(providerId, userId)` / `setSession(...)` returning raw `string`, while sibling caches (`SearchCache`, `HealthCache`, etc.) inherit `get`/`set` with JSON decode. Acknowledged as intentional in `.planning/INFRA-FIX-REPORT.md` §3.
- File: `gateway/src/core/cache/segregated/SessionCache.ts:9-20`
- Fix: add a JSON-typed `get`/`set` alongside the domain helpers when sessions get a real data model.

**Gateway: coverage threshold only 70% (current 83%)**
- Issue: vitest thresholds pinned to 70 statements/branches/functions/lines (`gateway/vitest.config.ts:14-19`); CI gates on 70 (`gateway/.github/workflows/ci.yml:91-94`). INFRA report notes actual coverage at 83%.
- Impact: the 13-point slack permits regressions in branching logic (the `withTimeout` helper, `errorCode` regex mapping, failover paths) without failing CI.
- Fix: raise thresholds to ~80-85%.

**Gateway: `ioredis-mock` peer mismatch requires `--legacy-peer-deps`**
- Issue: `ioredis-mock` vs `ioredis` peer range mismatch, documented in `.planning/INFRA-FIX-REPORT.md` §3. The Docker build and CI (`npm ci`) do **not** pass `--legacy-peer-deps`, so fresh installs may fail with `ERESOLVE` depending on npm version.
- Files: `gateway/package.json:25,26,39`, `gateway/.github/workflows/ci.yml:78-79`, `gateway/Dockerfile`
- Impact: setup flakiness on clean checkouts.
- Fix: add `--legacy-peer-deps` to install steps (and document why), or swap `ioredis-mock` for a maintained successor.

**App: over-broad detekt `ForbiddenImport` rule (`com.clibeats.data.*`)**
- Issue: the Phase-0 rule `ForbiddenImport: com.clibeats.data.*` (meant to keep Presentation off Data) flags data-layer self-imports, forcing `@file:Suppress("ForbiddenImport")` in ~15 files, e.g. `app/src/main/java/com/clibeats/di/NetworkModule.kt:1`, `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:1-2`, `app/src/main/java/com/clibeats/data/cache/CacheManager.kt:1`, `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt:1-2`, `app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt:1`. Additionally detekt 1.23.6 misparses the project's ktlint_official `@Inject`-on-own-line constructor style (false-positive `Indentation`), causing a second wave of `@Suppress("Indentation")` (e.g. `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt:18-19`).
- Files: `config/detekt/detekt.yml:9-12,43-46`, `.planning/phases/03-database-local-persistence-layer/deferred-items.md` (D-02, still open), `.planning/WINDOWS.md` item #3 (open deviation)
- Impact: the rule is effectively dead (everyone suppresses it); real Presentation->Data violations pass unnoticed.
- Fix: replace the pattern with an explicit allowlist/denylist of specific imports; disable detekt `Indentation` (ktlint is the formatting authority); refresh `detekt.yml` per the D-02 recommendation.

**App: build/release hardening shortcuts**
- `app/build.gradle.kts:26` hardcodes `GATEWAY_BASE_URL = "http://10.0.2.2:8080/"` (emulator loopback) as the **only** value for all build types - no debug/release differentiation, no `networkSecurityConfig`, plaintext HTTP baked into release APKs.
- `app/build.gradle.kts:36` the release build reuses the **debug signing config**; `isMinifyEnabled = false` (line 31) - no R8 shrinking in release.
- `app/build.gradle.kts:21` `targetSdk = 34` (below the current Play Store requirement level by 2026).
- `gradle/libs.versions.toml` pins `security-crypto = "1.1.0-alpha06"` - an alpha security dependency; legacy `EncryptedSharedPreferences`/`MasterKey` are deprecated in it (comment `app/src/main/java/com/clibeats/di/StorageModule.kt:19-22`); migration to the Tink-based API is deferred per ADR-003.

**Android: half-wired provider selection**
- Issue: `AppPreferences.activeProviderId` is persisted and surfaced in Settings (`app/src/main/java/com/clibeats/presentation/settings/SettingsViewModel.kt:26,48`) offering "YouTube Music (Gateway)" and "Local Device Media" (`app/src/main/java/com/clibeats/presentation/settings/SettingsScreen.kt:53`), but the only `MusicProvider` binding is `GatewayMusicProvider` (`app/src/main/java/com/clibeats/di/ProviderModule.kt:14-16`). Nothing propagates the setting to playback/search; "Local" and the "Trending Hits" defaults are decorative.
- Impact: users selecting "Local" still get the gateway provider; a dead preference adds settings mental overhead.

**Android: AUTH_TOKEN stored but never sent**
- Issue: `AppPreferences.AUTH_TOKEN` (`app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt:35,53,80-88`) persists the token in EncryptedSharedPreferences, but no OkHttp interceptor attaches it to gateway requests - `NetworkModule.provideGatewayOkHttpClient` (`app/src/main/java/com/clibeats/di/NetworkModule.kt:38-53`) only adds a debug logging interceptor. Consequently the gateway's `authenticated` context flag (`gateway/src/app.ts:167`) is always false for real clients.
- Impact: the "authenticated" contract (ADR-015) has no client half; any auth-gated features will silently not engage.

**Android: `GatewayMusicProvider` stubs + hardcoded fallbacks**
- Issue: `getTrack()` returns `ProviderResult.Error("Not implemented in Phase 5")` (`app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt:47-49`); `playlists()`/`queue()` return empty lists (lines 69-74); a blank search is coerced to hardcoded "Trending Hits" (line 36). The gateway has no track-by-id endpoint, so `getTrack` has no backend to call.
- Fix: implement `getTrack` via a new gateway endpoint or remove it from the `MusicProvider` interface.

**README/planning drift**
- `README.md:108-113` links ADR files that do not exist (`ADR-001-clean-architecture-hilt.md`, `ADR-002-music-provider-abstraction.md`) - the real files are `docs/adr/ADR-001-architecture-and-di-strategy.md`, `docs/adr/ADR-002-provider-integration-innertube.md`, etc. Broken links.
- README DoD claims ("Paparazzi screenshot baselines verified", "Cold start <2s") are not enforced in `.github/workflows/ci.yml` - no `verifyPaparazziDebug` step, no cold-start measurement anywhere.
- `.planning/STATE.md` records Phase 3 as current while git history shows Phases 4-11 delivered (through commit `4104a25`) - planning state is stale versus git reality.

## Known Bugs

**Gateway search `filterSongs` type/runtime mismatch**
- Issue: route schema declares `filterSongs: { type: 'boolean' }` (`gateway/src/schemas.ts:196-201`) but the handler reads it as a string and compares `filterSongs !== 'false'` (`gateway/src/app.ts:208,224`). If Fastify coerces "false"->`false`, then `false !== 'false'` is true - YouTube `filterSongs=false` requests always filter to songs anyway, never returning non-song results.
- Fix: type the query param consistently and compare `String(filterSongs) !== 'false'`.

**Corrupt Redis cache entry causes a 500 (JSON.parse without try/catch)**
- `SearchCache.get` / `HealthCache.get` call `JSON.parse(raw)` outside a try/catch (`gateway/src/core/cache/segregated/SearchCache.ts:10-13`, `HealthCache.ts:16-19`). A hand-modified or truncated Redis value (or data written by an older app version) throws, producing a 500 via `app.setErrorHandler` (`gateway/src/app.ts:153-157`) instead of a cache miss. `RedisCacheBase` deliberately fails open on Redis errors - the JSON decode defeats that resilience.
- Fix: wrap the decode in a try and return `null` on failure.

**Corrupted session promise never refreshed**
- `YouTubeProviderAdapter.getSession()` memoizes `sessionPromise` (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts:63-71`), so a failed `Innertube.create` (or expired visitor data) is cached forever; subsequent calls hit the same rejected promise and every request fails with the same error until gateway restart. YouTube visitor data also expires periodically - no refresh path exists.
- Fix: on rejection, reset `this.sessionPromise = undefined` (and `streamingSessionPromise`) before rethrow; add periodic session refresh.

**Android downloads require a pre-resolved streamUrl and use the wrong file extension**
- `TrackDownloadManager.downloadTrack` returns "No stream URL available" unless `track.streamUrl` is populated (`app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt:33-37`), but search results map `streamUrl = null` (`app/src/main/java/com/clibeats/data/gateway/mapper/GatewayMapper.kt:22`); only playback resolution (`PlaybackRepositoryImpl.ensureStreamUrl`, `app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt:65-72`) fills URLs - so a user must play a song before downloading it. Downloads always save to `"$songId.mp3"` (`app/src/main/java/com/clibeats/data/cache/CacheManager.kt:46-48`) even though the gateway `streamResult.mimeType` is `audio/mp4`/webm for real providers.
- Fix: resolve the URL at download start (call `musicProvider.stream()`), and derive the extension from `mimeType`.

**Gateway stream response `headers` dropped by the app**
- `GatewayStreamDto.headers` is parsed (`app/src/main/java/com/clibeats/data/gateway/dto/GatewayDtos.kt:38`) but never used: `PlayerAdapter.toMediaItem` consumes only `streamUrl` (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:251-272`). Mock stream `headers: { Authorization: 'Bearer mock-stream-token' }` (`gateway/src/providers/mock/MockProviderAdapter.ts:230-233`) never reaches the CDN; real YouTube stream signing headers would be lost too.
- Fix: attach headers via a per-request `DataSource.Factory` / `RequestProperties` in `PlayerAdapter`.

**Android Room persists expiring stream URLs with no staleness check**
- `SongEntity.stream_url` (`app/src/main/java/com/clibeats/data/local/entity/SongEntity.kt:16`) persists resolved (TTL-bounded, gateway `streamTTLSeconds` = 900s) URLs, while `PlaybackRepositoryImpl.ensureStreamUrl` reuses any non-blank `streamUrl` regardless of age - a stale persisted URL plays until ExoPlayer gets a 403. The `expiresAtEpochSeconds` field exists in the DTO (`GatewayStreamDto.kt:37`) but is ignored.
- Fix: surface `expiresAtEpochSeconds` into the domain model and re-resolve on an RATING `/stream` when the URL is within the gateway's `stream.urlRefreshBufferSeconds` window.

## Security Considerations

**Gateway is an unauthenticated stream-URL oracle**
- All `/api/v1/*` routes accept anonymous requests; `authenticated` is only `Authorization != null` - a presence check with no verification (`gateway/src/app.ts:167`). The `POST /api/v1/stream` route returns direct googlevideo CDN URLs to anyone who can reach port 8080. Combined with default `host: 0.0.0.0` (`gateway/config/gateway.yaml:5`) and `corsOrigins: ["*"]` (line 6; also fallback `gateway/src/config/config.ts:36,58`), an internet-exposed instance becomes a free stream-resolution API.
- Fix: implement real gateway auth (ADR-015 session tokens), bind the default host to `127.0.0.1` for single-user deployments, and restrict CORS origins.

**Private data leaks into Prometheus labels and logs**
- `requestCounter` (`gateway/src/core/metrics/metrics.ts:7-12`) is labeled with `endpoint: req.url` (`gateway/src/app.ts:105`) - `req.url` includes the query string (e.g. `/api/v1/search?q=...`), so every unique search query creates a Prometheus series (cardinality explosion) and search terms become metric label values.
- The log/event payloads also carry client IPs (`req.ip`), full request URLs (queries), and provider error messages including raw upstream text with URLs (`gateway/src/core/logging/logger.ts:14-15` logs every event; `gateway/src/app.ts:101,113-115`).
- Fix: label with the route pathname only (`req.routeOptions.url`), redact query strings, and never put raw exceptions into `/metrics` output.

**500 handler echoes raw upstream messages to clients**
- The fallback error handler returns `error.message` verbatim (`gateway/src/app.ts:153-157`), and `YouTubeProviderAdapter.errorCode` builds `ProviderError`s from upstream message text that the `ProviderError` branch also sends (lines 128-137). This can leak API internals or Redis URL material on unexpected failure paths.
- Fix: return a stable internal-error message to clients; log details under the trace id.

**No input-length limits on search**
- `searchSchema` `q` has no `maxLength` (`gateway/src/schemas.ts:192-196`), and the query also becomes the Redis cache key (`.toLowerCase().trim()` in `SearchCache.ts:11,17`) - an unbounded `q` produces an unbounded cache key. There is no rate limiting anywhere in the gateway (`RateLimitedError` is only ever generated by upstream providers).
- Fix: `maxLength` (e.g. 200); enforce `minLength` 1 or coerce; add `@fastify/rate-limit` behind auth.

**Swagger UI and metrics are unauthenticated**
- `/documentation` (full schema + config shapes) and `/metrics` (fine-grained routing labels) are served with no auth to anyone able to reach the box (`gateway/src/app.ts:89,322-325`).

**Android cleartext gateway traffic**
- The app speaks HTTP-plaintext to the gateway (`GATEWAY_BASE_URL`) and the release APK embeds the same emulator URL (build.gradle.kts:26); there is no `networkSecurityConfig` pinning that host, so cleartext is allowed.

**Redis default connection has no auth; session keys embed user IDs**
- Default `redis://localhost:6379` (`config.ts:67`) with no password in `config/gateway.yaml`. All cached data (`clibeats:*` prefix) is unsecured; `SessionCache` keys embed the raw userId (`clibeats:session:<providerId>:<userId>`) - PII at rest.
- Fix: enable Redis auth + TLS via env vars, and consider hashing userId before computing the key.

**Client-side sanitization covers only `Bearer` tokens**
- `app/src/main/java/com/clibeats/telemetry/TimberCrashReporter.kt:15` redacts "Bearer ..." in messages, but `PlayerAdapter` logs every media URI to logcat (`[EXOPLAYER_TRANSITION] MediaItem URI: ...` at `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:84-89`, plus `[EXOPLAYER_ERROR] ... message` at lines 111-117) - signed CDN URLs land in adb-visible logcat. `PlaybackEvent.StreamResolved` also carries the resolved URL in its payload (`app/src/main/java/com/clibeats/core/logging/StructuredLogger.kt:10`) even though the current format string prints only the id - any future log-format change can leak it. `NetworkModule` additionally enables `HttpLoggingInterceptor.Level.BODY` in debug builds (`app/src/main/java/com/clibeats/di/NetworkModule.kt:46-51`).
- Fix: log `<track-id>` instead of the URL; sanitize signed URLs when logging is required.

**Performance Bottlenecks**

**Health-probe on every request (provider selection)**
- `ProviderSelectionEngine.computeScore` invokes `adapter.healthCheck()` synchronously inside scoring (`gateway/src/core/selection/ProviderSelectionEngine.ts:41-47`), and `executeWithFailover` rebuilds the full candidate list per request. For the YouTube adapter `healthCheck()` is a live upstream search call (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts:305-319`); with `youtube.enabled = true`, every `/api/v1/search` request would fire an extra YouTube search per provider per request (2x upstream traffic, 30s timeout budget per op, sequential across providers). The `HealthCache` was presumably intended for this but is dead code (see Tech Debt).
- Fix: score from cached health (last attempt + backoff), recompute on a ticker, or reuse one health pass per request.

**Load-test numbers are not representative**
- `.planning/INFRA-FIX-REPORT.md` §4 cites "26k req/s, P99 67ms" from the load test, but `gateway/tests/load/load-test.ts` runs with `NODE_ENV=test` (ioredis-mock in memory), the **mock provider only** (YouTube is disabled by default config), one endpoint and one URL (`/api/v1/search?q=cyber`), 100 connections, 10 seconds, pipelining 1. It measures the mock-in-memory loopback bound, not a real YouTube-backed system against real Redis.
- Fix: add a load profile with real `ioredis` + real Redis and a youtube-enabled lane; publish verified numbers.

**Cold-start budget is unmeasured**
- README DoD #8 promises "Cold start <2s" but no measurement script, APK profile, or CI step exists. The app also performs stream resolution inline in `PlaybackRepositoryImpl.playTrack` (network round-trip before first frame of playback).
- Fix: add a measured cold-start benchmark; move stream resolution off the critical UI path (e.g. resolve in parallel with the player bar appearing, using the gateway refresh buffer).

**Stream URLs are not refreshed client-side**
- The gateway returns `expiresAtEpochSeconds` (and defaults to it) in `MockProviderAdapter.ts:229` and YouTube resolve; the app ignores it - a stale persisted URL (see Room bug above) plays until the CDN rejects it. No prefetch/refresh-ahead of the next queue item exists, so every track change blocks on a `/stream` round-trip.

## Fragile Areas

- **`gateway/src/providers/youtube/media.ts` parser** - built on privately-typed response shapes (flex-columns tables, thumbnail keys) that `youtubei.js` does not stabilize; an Innertube change (e.g. `item_type` semantics, `columns` shape) silently degrades parsing to "Unknown Artist"/empty album. Parsing correctness rides upstream releases (`youtubei.js ^17.2.0`). Add contract property tests against fixtures and a sentinel for "all unknown".
- **`YouTubeProviderAdapter.stream`** - relies on `fmt.url` from `adaptive_formats`; if absent (common for iOS-client sessions), it throws `PlaybackError("... decipher/PO token required)", YouTubeProviderAdapter.ts:172-176`) and gives the client a dead-end with no refresh retry. This is the core playback feature and the most likely production breakpoint.
- **Cross-repo API contract (app <-> gateway)** - `app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt` is a hand-written mirror of `gateway/src/app.ts` routes; there is no shared contract or client codegen (OpenAPI is generated server-side and validated, but never consumed on the app side). Any route change silently breaks the app until both repos are updated in lockstep (ADR-020).
- **Architecture layering test scope** - `gateway/tests/architecture/layers.test.ts` only protects a few rules (core->providers, providers->core/config/app, types leaf, config external-only). It does not scan route handlers for Data/DB access or enforce app-layer import rules; the green signal is easy to outmanoeuvre by adding files in new directories.
- **Telemetry naming mismatch** - `TimberTelemetryTracker`/`TimberCrashReporter` bear the "Timber" name but write to raw `android.util.Log` (`TimberTelemetryTracker.kt:12`, `TimberCrashReporter.kt:16`). No SLA; telemetry is local-only. The names imply a future server-side target that does not exist - document or rename.

## Scaling Limits

- **Metrics cardinality** - the per-query `endpoint` label (see Performance) creates an unbounded label set; a burst of distinct search queries degrades Prometheus scrapping for the whole gateway.
- **Redis key layout** - `keyPrefix` is a single static string (`clibeats`); multi-tenant/versioned sharing on a shared instance requires manual per-deploy config. No TLS/ACL client config supported via `gateway.yaml`.
- **In-process state** - `EventBus`, metrics, and `CircuitBreaker` state live only in memory: events are dropped on crash, and a restart resets every breaker to CLOSED (may momentarily route to a dead provider). No persistence or externalized state (ADR-018 predates a durable bus).

## Dependencies at Risk

- `youtubei.js ^17.2.0` (`gateway/package.json:30`) - fast-moving, reverse-engineered YouTube client; direct-to-YouTube logic tied to undocumented internals (see Fragile Areas). Floating 17.x range; breakage on any Innertube change.
- `security-crypto 1.1.0-alpha06` (`gradle/libs.versions.toml`) - alpha security dependency with deprecated legacy APIs; planned Tink migration pending.
- `compose-bom 2024.09.03` / Kotlin `2.0.21` / AGP `8.5.2` - mid-2024 era versions vs mid-2026; no automated dependency update tooling configured in `.github/`.
- `ioredis-mock` - peer-dependency flakiness on fresh installs (see Tech Debt).

## Missing Critical Features

- **Auth/session flow** - the gateway has no real auth, and the client-side token is stored but never sent (ADR-015 unimplemented end to end).
- **Download resilience** - no resume, no queue, no cancellation of in-flight downloads; downloads depend on a pre-resolved stream URL and land in the app cache dir (evictable by the OS, so "downloads" are not durable).
- **No recommendations/radio/lyrics** - declared unsupported in `YouTubeProviderAdapter.capabilities` (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts:33-42`) and not implemented elsewhere.
- **No offline-first UX** - every search/album/playlist operation passes through the gateway; no offline fallback beyond cached metadata in Room.
- **No library sync** - the gateway exposes no user-library endpoints (`queue()`/`playlists()` return empty in the app provider `GatewayMusicProvider.kt:69-74`).
- **Target SDK drift** - `targetSdk = 34` with no migration path to 35/36 (`app/build.gradle.kts:21`); release signing/minify left at debug/unshrunk defaults.

## Test Coverage Gaps

- **Android instrumented DAO tests never run** - `app/src/androidTest/.../SongDaoTest.kt`, `PlaylistDaoTest.kt`, `HistoryDaoTest.kt`, `CacheIndexDaoTest.kt` exist but are not executed anywhere (no emulator in CI; `.planning/WINDOWS.md` item #2, status open).
- **Gateway unused caches untested** - `SessionCache`, `ArtworkCache`, `HealthCache` have zero test coverage; they become live code only by virtue of the planned session wiring.
- **`YouTubeProviderAdapter` unit tests mock `youtubei.js`** (`gateway/tests/unit/youtube-adapter.test.ts:21-26` via `vi.mock`) with fixture-shaped Innertube responses - no integration/contract test against live or replayed real responses; the parser branch boundaries (unstable shapes) are the vulnerable part, untested against real payloads.
- **No E2E tests** - nothing exercises app -> gateway -> YouTube end to end on a device; the app-side `GatewayMusicProviderTest` uses MockWebServer (`app/src/test/java/com/clibeats/data/gateway/GatewayMusicProviderTest.kt`).
- **Failover exhaustion path** - `executeWithFailover` throwing `InternalError` composed from `lastError.message` (which may embed secrets) is only exercised via mock failure states; there is no native test for all-providers-unhealthy concurrency or half-open circuit tripping races.
- **`gateway/src/server.ts`** excluded from coverage (`gateway/vitest.config.ts:13`); shutdown/bootstrap paths untested.

---

*Concerns audit: 2026-08-08*