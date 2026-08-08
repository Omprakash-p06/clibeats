# Codebase Concerns

**Analysis Date:** 2026-08-09

## Tech Debt

**Gateway base URL configuration (DEBT-01):**
- Issue: Android debug builds hardcode `http://192.168.0.106:8080/` as the gateway URL fallback; release builds require `GATEWAY_URL` (fail-fast with no NXDOMAIN fallback since RECOVERY-02/06). The gateway is deployed on Render.com but the app must be pointed at the public URL via a build-time env.
- Why: No app-store distribution or runtime config screen yet; build-time `BuildConfig.GATEWAY_BASE_URL` is the chosen mechanism.
- Impact: APK built without `GATEWAY_URL` is unusable in release; debug builds only work on the developer LAN. Any user/device outside that LAN can't connect.
- Fix approach: Add runtime-configurable gateway URL (settings screen writing to DataStore) and/or deploy a stable public gateway domain (Render free tier gives a `*.onrender.com` URL; a custom domain like `gateway.clibeats.io` was noted as unregistered/NXDOMAIN).

**Single production provider (DEBT-02):**
- Issue: Only `YouTubeProviderAdapter` is a production provider; `mock` is test/demo-only. No runtime dynamic fallback provider exists.
- Why: Milestone-0 scope delivered YouTube first (ADR-012/013/014 define the plugin architecture for adding more).
- Impact: If YouTube changes InnerTube schemas or strengthens bot detection, the whole app loses search/playback with no fallback.
- Fix approach: Implement a secondary adapter (Piped/Invidious per `docs/architecture/TECHNICAL_DEBT.md` priority) registered in `gateway.yaml`; rely on `ProviderSelectionEngine` failover.

**Download manager (DEBT-03):**
- Issue: `TrackDownloadManager` (in `app/src/main/java/com/clibeats/data/download/`) does basic OkHttp byte streaming without native `DownloadManager`/WorkManager pause-resume or network-constraint awareness.
- Why: Offline-first was implemented at MVP level; WorkManager not adopted.
- Impact: Large downloads can fail on flaky networks or drain battery.
- Fix approach: Refactor to WorkManager + `UNMETERED` network constraints, resumable chunked downloads.

**No automated UI/E2E in CI (DEBT-04):**
- Issue: Compose UI and instrumented tests (`connectedDebugAndroidTest`, Maestro flows) are run manually; CI only runs JVM tests, lint, detekt, ktlint, build.
- Why: Emulator-in-CI cost/complexity deferred.
- Impact: UI regressions can slip through between releases; RECOVERY-06 had to do manual emulator validation.
- Fix approach: Add a Gradle Managed Device or Maestro CI job; wire `connectedDebugAndroidTest` into `.github/workflows/ci.yml`.

**Missing features (DEBT-05/06, intentional scope):**
- Lyrics (no LRCLIB integration) and audio effects/EQ (Media3 → output without equalizer/ReplayGain) are unimplemented.
- Fix approach: Future phases; low urgency for v1.0.0.

**ProGuard/R8 fragility (DEBT-07):**
- Issue: Release builds enable R8 (`isMinifyEnabled = true`) with `app/proguard-rules.pro`; Retrofit DTOs/kotlinx.serialization models rely on keep rules. A new DTO without a rule can silently break serialization in release.
- Impact: Serialization crashes in release builds that don't reproduce in debug.
- Fix approach: `@Keep` annotations on DTOs, plus CI release-build smoke test (`assembleRelease` verified at v1.0.0).

**`/debug-yt` endpoint:**
- Issue: Temporary diagnostics route in `gateway/src/app.ts` (hidden from OpenAPI via `{ schema: { hide: true } }`) that instantiates youtubei.js clients and exposes PO-token service status.
- Why: PO-token investigation on Render (`.planning/debug/yt-po-token-investigation.md`).
- Impact: Extra attack surface / maintenance; shouldn't ship to production long-term.
- Fix approach: Remove or gate behind an env flag once the PO-token issue is closed.

**PO-token minting is heavy and fragile:**
- Issue: `poToken/mint.ts` uses `bgutils-js` + jsdom (BotGuard/WAA) in-process; mint failures are non-fatal (lazy retry), but minting requires a DOM environment and can be slow.
- Impact: First stream from a datacenter can stall on token mint; YouTube can rotate/break the WAA flow.
- Fix approach: Cache tokens aggressively (already 2h TTL + 30min buffer), warm at startup (already done), consider external token service if BotGuard changes break minting.

## Known Bugs

**Release build without `GATEWAY_URL` → unusable APK (resolved, watch):**
- Symptoms: `UnknownHostException` → `provider_offline` everywhere.
- Trigger: Building `assembleRelease` without `-PGATEWAY_URL`/env.
- Workaround: Fail-fast config-time error now prevents silent dead APKs (RECOVERY-02); still an operational trap for anyone building releases casually.
- Root cause: Former hardcoded `https://gateway.clibeats.io/` was NXDOMAIN.
- Blocked by: None — resolved; requires discipline on future builds.

**Streaming from datacenter IPs fails with LOGIN_REQUIRED without PO token:**
- Symptoms: `/api/v1/stream` → 500 `INTERNAL_ERROR "All provider failover candidates failed for playback: undefined"`; `/debug-yt` shows all client types `LOGIN_REQUIRED`/0 audio formats on Render.
- Trigger: YouTube streaming from shared datacenter IPs (Render Oregon).
- Workaround: PO-token minting enabled in `gateway.yaml` (`poToken.enabled: true`, refresh buffer 1800s) with auto-refresh + one retry with `forceRefresh` on failure (RECOVERY-10); local/LAN streaming worked with `ANDROID_VR` + Range-safe relay (RECOVERY-06).
- Root cause: YouTube requires Proof-of-Origin for playback; fresh anonymous sessions from datacenter IPs are bot-blocked. Partially mitigated by in-process minting; investigation still ongoing (`yt-po-token-investigation.md`).
- Blocked by: YouTube-side behavior; keep monitoring.

**Mock provider `PLAYBACK_ERROR` legacy knob:**
- Symptoms: `shouldSimulateError`/`simulatedErrorCode` setters cast arbitrary strings into `MockProviderState` (`'PLAYBACK_ERROR' as unknown as MockProviderState`).
- Trigger: Only tests use these; `state` is the supported knob.
- Root cause: Backwards-compat shims from earlier failover tests.
- Impact: Minor type-safety wart; not user-facing.

## Security Considerations

**PO token / BotGuard credentials:**
- Risk: PO tokens and visitor data are anti-bot credentials; leaking or over-minting could trip YouTube's detection or be abused.
- Current mitigation: Tokens live in-memory only (`ProviderTokenService`), never logged; mint failures logged as `po-token: ...` info lines without token values; `registerProviders.ts` disables minting entirely under `NODE_ENV=test`.
- Recommendations: Never log `poToken`/`visitorData` values; add rate limiting on `/api/v1/stream` and `/debug-yt` if exposed publicly; remove `/debug-yt` before general production.

**Cleartext HTTP (dev):**
- Risk: `network_security_config.xml` permits cleartext for `10.0.2.2`, `192.168.0.106`, `127.0.0.1`, `localhost`. If a production base URL is HTTP, traffic is unencrypted.
- Current mitigation: Dev hosts only; release uses `GATEWAY_URL` (should be HTTPS on Render's `*.onrender.com`).
- Recommendations: Enforce HTTPS in the deployment; keep cleartext allowlist dev-only; consider TLS pinning later.

**No auth on gateway:**
- Risk: Any internet client can call `/api/v1/*` (search/stream cost upstream quota and relay bandwidth). `Authorization` header presence only flags context.
- Current mitigation: None for public deployment beyond YouTube's own bot checks.
- Recommendations: Add rate limiting + optional API key if the gateway is exposed publicly (currently `corsOrigins: ["*"]`).

**Encrypted storage:**
- Risk: `AUTH_TOKEN` in EncryptedSharedPreferences is the main credential store.
- Current mitigation: Keystore-backed `MasterKey` AES256_GCM, cloud-backup exclusions (`data_extraction_rules.xml`).
- Recommendations: Rotate key on app upgrade path; audit usage when real auth lands.

## Performance Bottlenecks

**PO-token minting latency:**
- Problem: First playback from datacenter can wait on BotGuard/WAA mint (jsdom + network).
- Measurement: Not profiled; mint can take seconds (slowest path in stream resolution).
- Cause: In-process BotGuard challenge solving.
- Improvement path: Startup warm-up (already implemented), keep-alive, TTL 2h; move to a dedicated mint service if it becomes a bottleneck.

**YouTube metadata sessions:**
- Problem: `MUSIC` client session is created once and reused (`getSession` memoized), but searches + health checks hit YouTube directly on cache miss.
- Measurement: RECOVERY-06 evidence: youtube healthCheck latency 1-2s vs mock 5ms; search ~1-2s per unique query.
- Cause: Live InnerTube calls; Redis search cache (1h) mitigates repeats.
- Improvement path: Metadata caching already in place; consider raising TTLs, prefetching popular queries, or parallelism.

**Stream proxy relay:**
- Problem: `proxyStreaming: true` relays every byte through the gateway (`/api/v1/stream/proxy/:trackId`), doubling bandwidth and adding hop latency vs direct-to-CDN.
- Measurement: Not benchmarked; CDN URL probe (bytes=0-0) happens once per track per 15 min (in-memory cache).
- Cause: Chosen so the app never sees raw googlevideo URLs (privacy) and works behind bot checks.
- Improvement path: Keep proxy for now; re-evaluate `directToCdnStreaming` flag on `/bootstrap` when auth/PO handling stabilizes.

## Fragile Areas

**YouTube adapter (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts`):**
- Why fragile: Depends on youtubei.js raw response shapes (`parseRawItem`, `parseSubtitle`, header casts to `as { title?: { text?: string } }`); YouTube A/B tests and schema changes break parsing silently. Client-type choice (`MUSIC` vs `ANDROID_VR`) affects whether URLs are Range-restricted.
- Common failures: `LOGIN_REQUIRED` (bot check), 403/1MiB Range caps (IOS-signed URLs, fixed via ANDROID_VR in RECOVERY-06), rate limits, schema drift.
- Safe modification: Keep parsing defensive (optional chaining, filters); run `tests/unit/youtube-adapter*.test.ts` + real-query smoke; verify on Render via `/debug-yt` and `/api/v1/stream`.
- Test coverage: Good — `youtube-adapter.test.ts`, `youtube-adapter-token.test.ts`, `media.ts` parsing covered; but no CI live-network test (by design, hermetic).

**PO-token pipeline (`poToken/mint.ts`, `ProviderTokenService.ts`):**
- Why fragile: Relies on bgutils-js WAA flow and YouTube's BotGuard; DOM/jsdom coupling; token-visitor-data binding must stay consistent (session rebuilt on token change).
- Common failures: Mint failure (non-fatal, lazy retry), expiry mid-playback, token rejection → one forced refresh then error.
- Safe modification: Never change token shape without updating session-key logic (`getStreamingSession`); keep mint injectable for tests.
- Test coverage: `provider-token-service.test.ts` covers lifecycle with stub mint; mint itself not unit-tested (network).

**Room schema (`app/schemas/.../1.json`):**
- Why fragile: Schema v1 committed; any entity change must bump version + migration (or users lose data).
- Common failures: Version mismatch crashes; column renames without migrations.
- Safe modification: Follow `room.schemaLocation` export; add migrations per Room docs; run DAO instrumented tests.
- Test coverage: DAO instrumented suites exist but run manually (not CI).

**Build-time URL plumbing:**
- Why fragile: `GATEWAY_URL` resolution in `app/build.gradle.kts` (gradle property → env → fallback) + cleartext allowlist + Render env. Easy to mismatch envs.
- Common failures: Debug on device vs emulator (LAN IP vs 10.0.2.2); release without env.
- Safe modification: Change fallbacks deliberately; keep `network_security_config.xml` in sync with any new dev host.

## Scaling Limits

**Gateway concurrency:**
- Current capacity: Single Fastify process; autocannon load test exists (`tests/load/load-test.ts`) but no published capacity numbers.
- Limit: CPU-bound PO-token minting and youtubei.js sessions per instance; Redis single endpoint.
- Symptoms at limit: YouTube rate limiting (→ circuit breaker trips), search latency growth.
- Scaling path: Multiple instances behind Render's free tier limits are constrained; move Redis to managed service; scale when traffic justifies paid plan.

**Cache sizing:**
- Current capacity: Redis TTLs (search 1h, metadata 24h, stream 15m, artwork 7d) keep keys bounded; in-memory `cdnUrlCache` unbounded Map (potential slow leak on long uptime).
- Limit: Unbounded in-memory Map for CDN probes; Redis eviction at memory limits.
- Symptoms at limit: Memory growth; evicted keys → cache misses (harmless).
- Scaling path: Add LRU/size cap to `cdnUrlCache`; configure Redis maxmemory policy.

## Dependencies at Risk

**youtubei.js 17.2.0:**
- Risk: Actively maintained but YouTube is adversarial; PO-token/bot-check changes can break playback; unmaintained release risk.
- Impact: Total loss of search/streaming (mitigated only by mock).
- Migration plan: Secondary provider (Piped/Invidious) per DEBT-02; monitor releases; keep `media.ts` parsing isolated to ease upgrades.

**bgutils-js 4.0.3:**
- Risk: BotGuard/WAA flow can change without notice; version tied to youtubei.js expectations.
- Impact: PO-token minting breaks → datacenter playback fails.
- Migration plan: Keep minting behind injectable interface (`ProviderTokenService` constructor takes `mint`); swap implementation without touching adapters.

**androidx.security:security-crypto 1.1.0-alpha06:**
- Risk: Alpha version; API may change; MasterKey scheme evolving.
- Impact: Compile/runtime breakage on upgrade; migration work on stable release.
- Migration plan: Track stable releases; EncryptedSharedPreferences usage isolated in `AppPreferences`.

**Node 20 (CI) vs Node 22 (Docker):**
- Risk: Minor version skew between CI (`setup-node@v4` node-version 20) and production image (`node:22-alpine`).
- Impact: Low; language features used are compatible (ES2022 target).
- Migration plan: Align CI to Node 22 for parity.

## Missing Critical Features

**Playlists/queue from gateway:**
- Problem: `GatewayMusicProvider.playlists()` and `queue()` return `emptyList()`; `getTrack()` returns "Not implemented in Phase 5".
- Current workaround: Library/queue features operate on local Room data only; gateway playlist endpoints (`/api/v1/playlist/:id`) exist and are tested but the app doesn't surface remote playlists.
- Blocks: Remote playlist browsing/import.
- Implementation complexity: Low-Medium (client wiring + DTO mapping exists for tracks/albums).

**Downloads resume:**
- Problem: No pause/resume; basic streaming download only (DEBT-03).
- Blocks: Reliable offline listening on mobile networks.
- Implementation complexity: Medium (WorkManager adoption).

**Runtime gateway URL config:**
- Problem: No in-app setting to change gateway URL (must rebuild).
- Blocks: Non-technical users connecting to a different deployment.
- Implementation complexity: Low (DataStore-backed setting + provider re-init).

## Test Coverage Gaps

**Gateway live network paths:**
- What's not tested: Real YouTube calls, PO-token minting, actual stream proxy relay end-to-end in CI (hermetic by design).
- Risk: Schema drift/bot-check changes ship undetected.
- Priority: High.
- Difficulty: High — requires live network or recorded fixtures + VPN/agent network; partially covered by manual `/debug-yt` + Render validation.

**Android instrumented/UI suites in CI:**
- What's not tested: Room DAO tests (`connectedDebugAndroidTest`), Compose UI tests, full playback flow on emulator.
- Risk: Data-layer regression unnoticed; UI breakage between releases.
- Priority: High (DEBT-04).
- Difficulty: Medium — Gradle Managed Devices / Maestro setup.

**YouTube adapter resilience matrix:**
- What's not tested: Full set of youtubei.js parse edge cases across response shapes (only representative fixtures).
- Risk: Partial silent failures (tracks dropped, artwork missing).
- Priority: Medium.
- Difficulty: Medium — capture real responses as fixtures.

---

*Concerns audit: 2026-08-09*
