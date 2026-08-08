# External Integrations

**Analysis Date:** 2026-08-09

## APIs & External Services

**Music/Streaming:**
- **YouTube Music** — Primary audio content provider via the youtubei.js library (`youtubei.js` 17.2.0)
  - SDK/Client: `youtubei.js` Innertube client (`src/providers/youtube/YouTubeProviderAdapter.ts:1`)
  - Protocol: Innertube (YouTube Internal API) — `Innertube.create()` session at `src/providers/youtube/YouTubeProviderAdapter.ts:65,76`
  - Client types: `ClientType.MUSIC` for metadata queries (`YouTubeProviderAdapter.ts:56`), `ClientType.ANDROID_VR` for streaming (`YouTubeProviderAdapter.ts:57`)
  - API operations:
    - Search: `yt.music.search()` — `src/providers/youtube/YouTubeProviderAdapter.ts:124`
    - Stream resolution: `yt.getBasicInfo()` — `src/providers/youtube/YouTubeProviderAdapter.ts:167`
    - Album: `yt.music.getAlbum()` — `src/providers/youtube/YouTubeProviderAdapter.ts:211`
    - Artist: `yt.music.getArtist()` — `src/providers/youtube/YouTubeProviderAdapter.ts:257`
    - Playlist: `yt.music.getPlaylist()` — `src/providers/youtube/YouTubeProviderAdapter.ts:278`
    - Health probe: `yt.music.search('a')` — `src/providers/youtube/YouTubeProviderAdapter.ts:314`
  - Error mapping: Regex-based detection of rate-limit, geo-block, auth, and network errors → typed `ProviderError` subclasses (`YouTubeProviderAdapter.ts:103-114`)
  - Diagnostic endpoint: `GET /debug-yt` in `src/app.ts:486-525` probes multiple `ClientType` variants for troubleshooting
  - Authentication: Implicit (anonymous) — no API keys or OAuth tokens required; relies on client fingerprinting via the Innertube session

**Test/Mock:**
- **MockProviderAdapter** — In-process local provider simulating music data
  - Location: `src/providers/mock/MockProviderAdapter.ts`
  - Simulates 5 artists, 100 tracks across 10 albums, 5 playlists with a seeded PRNG (`seed=42`)
  - Supports 8 failure states: `HEALTHY`, `SLOW`, `OFFLINE`, `MALFORMED`, `RATE_LIMITED`, `AUTHENTICATION_FAILED`, `GEO_BLOCKED`, `INTERNAL_ERROR`
  - Stream URLs point to `https://mock-cdn.clibeats.internal/audio/` (internal test CDN)
  - Registered by default with priority 10 (`src/providers/registerProviders.ts:25`)

## Data Storage

**Databases:**
- **Redis** — Primary data store for caching (no other database used)
  - Client: `ioredis` 5.4.2 (`src/app.ts:6`, `src/core/cache/RedisCacheBase.ts:1`)
  - Connection URL: `config.cache.redisUrl` — defaults to `redis://localhost:6379` (`src/config/config.ts:68`); overridable via `REDIS_URL` env var
  - Key naming: namespaced as `clibeats:<namespace>:<key>` (e.g., `clibeats:search:<query>`, `clibeats:albums:<id>`) — `src/core/cache/RedisCacheBase.ts:22`
  - TTL configuration (per namespace from `config/gateway.yaml` lines 27-31):
    | Namespace | TTL | Config field | Cache class |
    |-----------|-----|--------------|-------------|
    | search | 3600s (1h) | `searchTTLSeconds` | `SearchCache` (`src/core/cache/segregated/SearchCache.ts`) |
    | albums | 86400s (24h) | `metadataTTLSeconds` | `AlbumCache` (`src/core/cache/segregated/AlbumCache.ts`) |
    | artists | 86400s (24h) | `metadataTTLSeconds` | `ArtistCache` (`src/core/cache/segregated/ArtistCache.ts`) |
    | playlists | 86400s (24h) | `metadataTTLSeconds` | `PlaylistCache` (`src/core/cache/segregated/PlaylistCache.ts`) |
    | session | 86400s (24h, default) | no TTL config | `SessionCache` (`src/core/cache/segregated/SessionCache.ts`) |
    | artwork | 604800s (7d) | `artworkTTLSeconds` | `ArtworkCache` (`src/core/cache/segregated/ArtworkCache.ts`) |
    | provider-health | 300s (5m) | no config override | `HealthCache` (`src/core/cache/segregated/HealthCache.ts`) |
  - Resilience: All cache operations are fail-open — Redis errors return `null` (cache miss) and emit `CACHE_ERROR` events instead of crashing (`RedisCacheBase.ts:29-60`)
  - Test mode: `ioredis-mock` injected when `NODE_ENV=test` (`src/app.ts:44-46`)
  - Connection config: lazy connect, `maxRetriesPerRequest: 1`, `enableOfflineQueue: false`, retry strategy with max 3 attempts at 500ms — `src/app.ts:48-56`

**File Storage:**
- None — No local or cloud file storage. All media streams are proxied from upstream CDNs (YouTube/CDNs) or mock URLs.

**Caching:**
- Redis (as above) — also used for provider session storage via `SessionCache` (`src/core/cache/segregated/SessionCache.ts`)

## Authentication & Identity

**Auth Provider:**
- Custom gateway-level context extraction — no user-facing auth system
- `ProviderContext.authenticated` derived from presence of `Authorization` header (`src/app.ts:185`)
- `ProviderContext.country` from `X-Country` header, defaults to `US` (`src/app.ts:183`)
- `ProviderContext.language` from `X-Language` header, defaults to `en` (`src/app.ts:184`)
- `ProviderContext.traceId` from `X-Trace-Id` header, auto-generated if absent (`src/app.ts:114-116`)
- YouTube provider uses anonymous Innertube sessions — no stored credentials or OAuth flows

## Monitoring & Observability

**Error Tracking:**
- None — No Sentry, Rollbar, or external error tracking service

**Logging:**
- Pino 9.6.0 structured logger (`src/core/logging/logger.ts:1`)
  - Log level from `LOG_LEVEL` env var, defaults to `info` (`src/core/logging/logger.ts:5`)
  - Service name: `clibeats-gateway` (`src/core/logging/logger.ts:9`)
  - ISO timestamp format (`src/core/logging/logger.ts:10`)
  - Logs: incoming requests (`src/app.ts:118`), request completions (`src/app.ts:128-134`), Redis connection warnings (`src/app.ts:57-62`), all EventBus events (`src/core/logging/logger.ts:14-16`)

**Metrics:**
- **Prometheus** via `prom-client` 15.1.3 (`src/core/metrics/metrics.ts:1`)
  - Endpoint: `GET /metrics` — `src/app.ts:467-470`
  - Registry: custom `client.Registry()` with `collectDefaultMetrics` (`src/core/metrics/metrics.ts:4-5`)
  - Metrics instrumented:
    - `gateway_requests_total` (Counter, per endpoint) — `src/core/metrics/metrics.ts:7-11`
    - `gateway_cache_hits_total` / `gateway_cache_misses_total` (Counter, per namespace) — lines 14-26
    - `gateway_cache_errors_total` (Counter, per namespace + operation) — lines 28-33
    - `gateway_provider_selections_total` (Counter, per provider) — lines 35-40
    - `gateway_provider_failures_total` (Counter, per provider) — lines 42-47
    - `gateway_provider_health` (Gauge, per provider + status) — lines 49-54
    - `gateway_circuit_breaker_state` (Gauge, per provider) — lines 56-61
    - `gateway_search_duration_seconds` (Histogram, cached label) — lines 63-69
    - `gateway_stream_resolution_duration_seconds` (Histogram) — lines 71-76
  - Event-driven: Metrics auto-update from `globalEventBus` events (`src/core/metrics/metrics.ts:88-114`)
  - Circuit breaker state metric updated on state changes (`src/core/circuit/CircuitBreaker.ts:16,25,36,45,53`)

**Health Checks:**
- `GET /health` — Aggregate gateway health (`src/app.ts:447-464`)
- `GET /api/v1/bootstrap` — Provider health + capabilities (`src/app.ts:236-265`)
- `RedisHealthChecker` — PING-based latency check with timeout (`src/core/health/RedisHealthChecker.ts`)
  - Default timeout: 1000ms from `config.health.redisTimeoutMs` (`src/config/config.ts:81`, `src/app.ts:76`)
  - Docker healthcheck: `wget -qO- http://localhost:8080/health` (`Dockerfile` line 19)
  - Render.com: `healthCheckPath: /health` (`render.yaml` line 15)

## CI/CD & Deployment

**Hosting:**
- **Docker** — Multi-stage container build (`Dockerfile`)
- **Render.com** — Declarative deployment (`render.yaml`)

**CI Pipeline:**
- **GitHub Actions** — CI script defined in `package.json` line 17: `npm run check && npm test && npm run openapi:generate && npm run openapi:validate && npm run test:load`
  - `npm run check` — TypeScript type-check (`tsc --noEmit`)
  - `npm test` — Vitest run
  - `npm run openapi:generate` — Generate `openapi.json` via `scripts/generate-openapi.ts`
  - `npm run openapi:validate` — Validate OpenAPI spec via `scripts/validate-openapi.ts`
  - `npm run test:load` — Load test via `tests/load/load-test.ts`
- Note: No `.github/workflows/` directory found in the gateway folder; CI script is defined as the `ci` npm script

**Deployment Artifacts:**
- `Dockerfile` — Multi-stage build (`node:20-alpine`), exposes port 8080, includes HEALTHCHECK
- `render.yaml` — Render.com service config with `PROXY_STREAMING=true`, `PORT=8080`, `NODE_ENV=production`

## Environment Configuration

**Required env vars:**
- `REDIS_URL` — Redis connection string (defaults to `redis://localhost:6379`; used in `src/config/config.ts:68,88` and `docker-compose.yml:12`)
- `PORT` — Server listen port (defaults to 8080; used in `src/config/config.ts:57,87` and `render.yaml:13`)

**Optional env vars:**
- `NODE_ENV` — `'test'` triggers `ioredis-mock` instead of real Redis (`src/app.ts:44`); `'production'` used in Docker/Render
- `HOST` — Server bind host (defaults to `0.0.0.0`; `src/config/config.ts:58`)
- `GATEWAY_CONFIG_PATH` — Override path to YAML config file (`src/config/config.ts:47`)
- `LOG_LEVEL` — Pino log level (defaults to `info`; `src/core/logging/logger.ts:5`)

**Secrets location:**
- No `.env` or `.env.example` files present in the gateway directory
- Environment variables injected directly via Docker (`docker-compose.yml:10-12`), Render.com (`render.yaml:9-15`), or runtime environment
- `.dockerignore` explicitly excludes `.env` (`C:\Users/OM Prakash/Documents/clibeats/gateway/.dockerignore:4`)

## Webhooks & Callbacks

**Incoming:**
- None — No webhook listener endpoints

**Outgoing:**
- None — No outgoing webhooks or callback URLs
- Note: The gateway does make outbound HTTP `fetch` calls to upstream CDN URLs for stream probing and relay (`src/app.ts:206,403`), but these are media URL fetches, not webhook callbacks

---

*Integration audit: 2026-08-09*
