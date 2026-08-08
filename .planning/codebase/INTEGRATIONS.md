# External Integrations

**Analysis Date:** 2026-08-09

## APIs & External Services

**YouTube (InnerTube):**
- What it's used for: All music search, album/artist/playlist metadata, and stream URL resolution. The gateway is the ONLY component that talks to YouTube — the Android app has zero InnerTube/YouTube code (per ADR-005, ADR-012).
  - SDK/Client: `youtubei.js` v17.2.0 (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts`)
  - Auth: None (anonymous sessions). PO tokens (Proof of Origin) are minted in-process via `bgutils-js` BotGuard/WAA for playback from datacenter IPs (`gateway/src/providers/youtube/poToken/mint.ts`, `ProviderTokenService.ts`). Token + visitorData are bound and refreshed automatically before expiry (default 2h TTL, 30 min refresh buffer).
  - Client types: `ClientType.MUSIC` for metadata; `ClientType.ANDROID_VR` for streaming sessions (chosen in RECOVERY-06 to get unrestricted Range-safe CDN URLs).
  - Rate limits: No API key; subject to YouTube bot detection — the adapter maps rate-limit/geo/login messages to canonical `ProviderError` codes (`RATE_LIMITED`, `GEO_BLOCKED`, `PLAYBACK_ERROR`).

**Gateway ↔ Android REST API:**
- What it's used for: The client consumes the gateway's `/api/v1/*` contract (search, album, artist, playlist, stream, bootstrap, providers) over Retrofit + OkHttp + kotlinx.serialization.
  - Client: `app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt`, DTOs in `data/gateway/dto/GatewayDtos.kt`
  - Contract: JSON Schema in `gateway/src/schemas.ts`, OpenAPI 3 spec generated/validated via `gateway/scripts/generate-openapi.ts` / `validate-openapi.ts`, UI at `/documentation`
  - Headers: `x-trace-id` (correlation), `x-country`, `x-language`, `x-audio-quality`, `x-device` propagated from client context

## Data Storage

**Databases:**
- Room (SQLite) — on-device only. Entities: `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`, `QueueEntity` (`app/src/main/java/com/clibeats/data/local/**`). Schema v1 exported to `app/schemas/`.
  - No cloud DB — the gateway stores no user data.

**Caching:**
- Redis — gateway cache with segregated namespaces: search (1h TTL), metadata: albums/artists/playlists (24h), stream URLs (15 min), artwork (7 days), plus session/health caches (`gateway/src/core/cache/CacheManager.ts` + `cache/segregated/**`).
  - Connection: `REDIS_URL` env var or `gateway/config/gateway.yaml` (`redis://localhost:6379` default).
  - Client: ioredis v5.4.2 with `lazyConnect`, `maxRetriesPerRequest: 1`, fail-open degradation (cache errors never fail the request).
  - Local docker-compose provides `redis:7-alpine`; Render production would need an external Redis add-on (not yet configured).
- In-memory: `cdnUrlCache` Map in `gateway/src/app.ts` for the stream-proxy CDN URL probe results.

## Authentication & Identity

**Auth Provider:**
- None external. The gateway treats requests with an `Authorization` header as `authenticated: true` in context but does not validate tokens (`gateway/src/app.ts` `getContext()`).
- Android stores an `AUTH_TOKEN` in EncryptedSharedPreferences backed by a Keystore `MasterKey` (AES256_GCM), excluded from cloud backup via `res/xml/data_extraction_rules.xml`. It is not currently sent as a real credential.

**OAuth Integrations:**
- None. No user accounts, no Google sign-in.

## Monitoring & Observability

**Metrics:**
- Prometheus — `prom-client` registry in `gateway/src/core/metrics/metrics.ts`, exposed at `/metrics` (requests, cache hits/misses/errors, provider selections/failures, provider health gauge, circuit-breaker state gauge, search/stream latency histograms). Metrics are wired to the internal EventBus so no manual instrumentation at call sites.

**Logs:**
- pino structured JSON logs (`gateway/src/core/logging/logger.ts`), ISO timestamps, `service: clibeats-gateway` base field. Every request carries a `traceId` (client-provided via `x-trace-id` or generated), logged on request and response and echoed back in the `x-trace-id` header and in every error payload.
- Android: `android.util.Log` with diagnostic tags (`PlayerAdapterDiagnostics`, `CLIBeatsApp`), plus an internal `StructuredLogger` (`app/src/main/java/com/clibeats/core/logging/StructuredLogger.kt`) used by telemetry trackers (TimberTelemetryTracker / TimberCrashReporter exist as no-op/sanitized trackers per ADR-010).

**Debug endpoints:**
- `GET /debug-yt` — temporary diagnostics route (hidden from OpenAPI via `{ schema: { hide: true } }`) that probes all youtubei.js client types and PO-token service status; used for Render PO-token investigation (`.planning/debug/yt-po-token-investigation.md`). Not intended for production traffic.

## CI/CD & Deployment

**Hosting:**
- Render.com — `gateway/render.yaml`: Docker web service `clibeats-gateway`, free plan, Oregon region, env `NODE_ENV=production`, `PORT=8080`, `PROXY_STREAMING=true`, health check `/health`. Switched from Railway to Render (`git log` 7e3935d).
- Android — distributed as release APK (v1.0.0), signed with debug keystore; no app store pipeline configured.

**CI Pipeline:**
- GitHub Actions — `.github/workflows/ci.yml`, two jobs on push/PR to main/master/develop:
  1. `quality-and-test` (Android): ktlintCheck → detekt → lintDebug → assembleDebug → testDebugUnitTest, artifacts uploaded.
  2. `gateway-quality-and-test` (Gateway): `npm ci` → `npm run check` (tsc) → `npm test` (Vitest) → `openapi:validate` → Docker build.
  - Secrets: none required beyond standard GitHub token.

## Environment Configuration

**Development:**
- Required env vars: `GATEWAY_URL` for the Android build (debug defaults to `http://192.168.0.106:8080/`); `REDIS_URL` optional for gateway.
- Cleartext HTTP permitted only for dev hosts: `10.0.2.2`, `192.168.0.106`, `127.0.0.1`, `localhost` (`app/src/main/res/xml/network_security_config.xml`).
- Mock services: `mock` provider (deterministic seeded dataset, 8 failure states) is registered alongside YouTube and used for tests and failover demos (`gateway/src/providers/mock/MockProviderAdapter.ts`).

**Staging:**
- N/A — no separate staging environment; Render service doubles as the shared test deployment (PO-token investigation was run against it).

**Production:**
- Secrets management: Render env vars (none sensitive currently). `GATEWAY_URL` must be set to the deployed gateway URL for release builds (release fails fast if missing — no NXDOMAIN fallback, per RECOVERY-02/06).
- Failover/redundancy: single gateway instance; circuit breaker + provider failover is the resilience story; Redis outage degrades to cache-miss (gateway stays up).

## Webhooks & Callbacks

**Incoming:**
- None.

**Outgoing:**
- None. The gateway is a pull-based API only; the only outbound traffic is to YouTube/InnerTube and the CDN for stream relay.

---

*Integrations analysis: 2026-08-09*
*Update when external services change*
