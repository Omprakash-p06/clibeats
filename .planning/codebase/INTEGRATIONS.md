# External Integrations

**Analysis Date:** 2026-08-08

Two runtimes integrate with external systems: the Android app (`app/`) consumes the provider gateway over REST, and the gateway (`gateway/`) talks to YouTube Music's unofficial InnerTube API and Redis. There are no paid API keys or OAuth flows in use.

## APIs & External Services

**YouTube Music (InnerTube, via youtubei.js):**
- Service: YouTube Music unofficial internal API, base `https://music.youtube.com/youtubei/v1/`
- Used for: search, album/artist/playlist metadata, direct-to-CDN stream URL resolution
- SDK/Client: `youtubei.js` 17.2.0 — `Innertube.create({ client_type, lang, country })` in `gateway/src/providers/youtube/YouTubeProviderAdapter.ts`
- Two client sessions:
  - `ClientType.MUSIC` — metadata ops (search, album, artist, playlist, health probe)
  - `ClientType.IOS` — streaming session (`getBasicInfo`, picks highest-bitrate `adaptive_formats` audio URL)
- Auth: **none** — unauthenticated browser client. No API key or quota limits (ADR-005)
- Rate/geo handling: error classifier regexes in `YouTubeProviderAdapter.errorCode()` map messages to `RateLimitedError`, `GeoBlockedError`, `NetworkError`, `UnsupportedError`, `PlaybackError`, `NotFoundError` (`gateway/src/types/error.ts`)
- Risk notes (ADR-005): unofficial API; response shapes change without notice; stream URLs expire (~1h, refreshed on demand; `expiresAtEpochSeconds` returned); PO-token/decipher may be required → surfaces as `PlaybackError`
- Provider disabled by default: `enabled: false` in `gateway/config/gateway.yaml` (mock provider is the default registered adapter, `gateway/src/providers/registerProviders.ts`)

**CliBeats Provider Gateway (self-hosted REST):**
- Service: Fastify server at `http://10.0.2.2:8080/` (Android emulator loopback → host, `BuildConfig.GATEWAY_BASE_URL` in `app/build.gradle.kts`)
- Client: Retrofit 2.11.0 + OkHttp 4.12.0, kotlinx-serialization converter — `app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt`
- Endpoints consumed by the app:
  - `GET /api/v1/search?q=&filterSongs=` → `GatewaySearchResponse`
  - `POST /api/v1/stream` `{ trackId }` → `{ stream: { streamUrl, mimeType, bitrateKbps, expiresAtEpochSeconds } }`
  - `GET /api/v1/album/{id}`, `GET /api/v1/artist/{id}`, `GET /api/v1/playlist/{id}`
- All endpoints defined in `gateway/src/app.ts`; OpenAPI spec generated to `gateway/openapi.json` (`gateway/scripts/generate-openapi.ts`), validated by `gateway/scripts/validate-openapi.ts` and `gateway/tests/contract/openapi.test.ts`
- App-side errors mapped via `app/src/main/java/com/clibeats/data/gateway/mapper/GatewayErrorMapper.kt`

## Data Storage

**Databases:**
- **Room 2.6.1** (on-device SQLite, Android): `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt`
  - Entities (version 1, schema exported via KSP `room.schemaLocation` → `app/schemas`): `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`, `QueueEntity` (`app/src/main/java/com/clibeats/data/local/entity/`)
  - DAOs: `SongDao`, `PlaylistDao`, `HistoryDao`, `CacheIndexDao`, `QueueDao` (`app/src/main/java/com/clibeats/data/local/dao/`)
  - Type converters: `app/src/main/java/com/clibeats/data/local/CliBeatsTypeConverters.kt`
- **Redis 7** (gateway cache): docker `redis:7-alpine` (`gateway/docker-compose.yml`), client `ioredis` (`gateway/src/core/cache/RedisCacheBase.ts`)
  - Keys namespaced `clibeats:<namespace>:<key>` with TTL per namespace
  - Segregated caches (`gateway/src/core/cache/CacheManager.ts` + `segregated/`): search (1h), albums/artists/playlists metadata (24h), artwork (7d), stream (15min), session, health
  - Fail-open: Redis outage degrades to cache miss, never crashes a request (ADR-013)
  - Test mode swaps to `ioredis-mock` when `NODE_ENV=test` (`gateway/src/app.ts` `createRedis()`)

**File Storage:**
- Android local audio cache: `context.cacheDir/audio_cache/<songId>.mp3`, indexed via `CacheIndexDao` — `app/src/main/java/com/clibeats/data/cache/CacheManager.kt`
- Downloads written by OkHttp streaming — `app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt`
- Coil image disk cache: `context.cacheDir/image_cache` (2% of disk) — `app/src/main/java/com/clibeats/di/ImageLoaderModule.kt`
- No external file/blob storage

**Caching:**
- Gateway: Redis (above)
- App: Room-backed cache index + local files; Coil memory (25%) + disk caches

## Authentication & Identity

**Auth Provider:**
- Custom / none. No OAuth, no Google sign-in, no third-party identity provider
- App stores a single `auth_token` in **EncryptedSharedPreferences** (Android Keystore MasterKey, AES256_GCM + AES256_SIV) — `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt`, wired in `app/src/main/java/com/clibeats/di/StorageModule.kt`. Migration to Tink `KeystoreAesGcm` tracked in ADR-003
- Gateway treats the presence of an `authorization` header as `authenticated=true` in `ProviderContext` (`gateway/src/app.ts` `getContext()`); no token validation implemented
- Client context passed via headers: `x-country` (default `US`), `x-language` (default `en`), `x-audio-quality` (default `HIGH`), `x-device` (default `mobile`), `x-trace-id`

## Monitoring & Observability

**Error Tracking:**
- None external. App: custom `CrashReporter`/`TelemetryTracker` interfaces with Timber-style `android.util.Log` impls (`app/src/main/java/com/clibeats/telemetry/`). Gateway: typed error model + error responses with `code`/`traceId` (`gateway/src/types/error.ts`, `gateway/src/app.ts` error handler)

**Logs:**
- Gateway: pino JSON structured logs, `service: clibeats-gateway`, ISO timestamps, level from `LOG_LEVEL`; per-request/incoming logs + response logs keyed by `x-trace-id`; EventBus events auto-logged (`gateway/src/core/logging/logger.ts`, `gateway/src/core/events/EventBus.ts`)
- App: `android.util.Log` under `CLIBeatsTelemetry` tag; OkHttp `HttpLoggingInterceptor` (BODY level, debug builds only)

**Metrics:**
- Gateway: Prometheus via prom-client at `GET /metrics` — counters (`gateway_requests_total`, `gateway_cache_hits/misses/errors_total`, `gateway_provider_selections/failures_total`), gauges (`gateway_provider_health`, `gateway_circuit_breaker_state`), histograms (search + stream resolution latency) (`gateway/src/core/metrics/metrics.ts`)

## CI/CD & Deployment

**Hosting:**
- Gateway: Docker (multi-stage `gateway/Dockerfile`, `node:20-alpine`, port 8080); orchestration via `gateway/docker-compose.yml` (gateway + redis services)
- App: no distribution channel configured yet (ADR-011 covers production release)

**CI Pipeline:**
- GitHub Actions `.github/workflows/ci.yml`:
  - Job 1 (Android): JDK 17 temurin, ktlintCheck, detekt, lintDebug, assembleDebug, testDebugUnitTest
  - Job 2 (Gateway): Node 20, `npm ci`, `npm run check`, `npm test`, `npm run test:coverage` (70% threshold), `npm run openapi:validate`, `npm run test:load` (autocannon), `docker build`

## Environment Configuration

**Required env vars (gateway):**
- `REDIS_URL` (e.g. `redis://localhost:6379`; also set by docker-compose)
- Optional: `PORT`, `HOST`, `GATEWAY_CONFIG_PATH`, `NODE_ENV`, `LOG_LEVEL`
- Config file source of truth: `gateway/config/gateway.yaml`; env vars override file values (`gateway/src/config/config.ts`)

**Secrets location:**
- No `.env` files, no secret stores, no committed credentials. Only secret-adjacent storage is the Android Keystore-backed `clibeats_secure_prefs` (`EncryptedSharedPreferences`). Provider credentials would slot into `gateway.yaml` providers section (none currently).

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None. Gateway is purely request/response; provider failover and cache invalidation are internal (`gateway/src/core/selection/ProviderSelectionEngine.ts`, `gateway/src/core/circuit/CircuitBreaker.ts`)

---

*Integration audit: 2026-08-08*