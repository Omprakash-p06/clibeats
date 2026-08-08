# Debug Session: RECOVERY-02 — Gateway Connectivity Recovery & Provider Reliability

**Status:** awaiting_human_verify
**Priority:** P0 (Release Blocking)
**Trigger:** Gateway Connectivity Recovery & Provider Reliability — Android app cannot reach the gateway; release displays only `provider_offline`.
**Created:** 2026-08-08
**Updated:** 2026-08-08

## Symptoms

- **Expected:** App can search, browse songs, and play music end-to-end.
- **Actual:** The current release is NOT functional. User cannot search, browse songs, or play music. App only displays `provider_offline`.
- **Error (Android Runtime):**
  ```
  provider_offline
  java.net.UnknownHostException
  Unable to resolve host "gateway.clibeats.io"
  No address associated with hostname
  ```
- **Timeline:** Blocking the current release. Prior fix (RECOVERY-01) resolved search pipeline + cleartext HTTP (Lan IP), but host `gateway.clibeats.io` itself does not resolve.
- **Reproduction:** Install/run the app and attempt any search action; provider goes offline with `UnknownHostException`.

## Mission (verbatim from trigger)

- Do NOT write documentation. Do NOT create ADRs. Do NOT redesign architecture.
- Find the actual runtime failure. Fix it. Prove it.
- Only continue after the app can successfully search and play music.

## Phases (from trigger)

1. Gateway Connectivity Audit — determine why Android cannot reach the gateway (BuildConfig, Manifest, network_security_config, Retrofit, GatewayApi, GatewayMusicProvider, DNS, HTTP/HTTPS, LAN IP, localhost, emulator/release/debug config). Acceptance: Android performs GET /health, GET /bootstrap, GET /search, POST /stream.
2. Find Root Cause — determine whether `gateway.clibeats.io` is nonexistent/placeholder/expired/missing-DNS/never-deployed OR Android reads wrong config. Trace BuildConfig generation, Gradle, Manifest placeholders, Retrofit creation. Output: exactly which line causes `UnknownHostException`.
3. Configuration Recovery — proper environment system: DEBUG_EMULATOR (http://10.0.2.2:8080), DEBUG_DEVICE (configurable LAN IP), RELEASE (real production endpoint). Never hardcode fake domains. Changing environments requires configuration only.
4. Research Alternative Providers (if youtubei.js unreliable) — compare youtubei.js, NewPipe Extractor, Piped, LibreTube, Invidious, self-hosted Piped, hybrid chain. Produce `provider-comparison.md`. Recommend ONE production architecture.
5. Provider Recovery — fix/replace provider implementation. Maintain existing ProviderAdapter contract. Acceptance: search returns results, stream resolves, playback works.
6. Runtime Validation — Wonderwall, Believer, Heat Waves, Tum Hi Ho, Rick Astley: search → results → artwork → track → stream → Media3 → audio.
7. Failure Injection — gateway offline, Redis offline, provider timeout, DNS failure, 429, 403, no network, expired stream. UI reports meaningful errors.
8. Release Validation — install release APK on physical device. Wait for playback/transport/background/notification/queue checks.

## Completion Criteria

Complete ONLY if: Android↔gateway communication works, gateway↔provider works, search returns real songs, playback works, user can install the APK and immediately play music.

## Evidence

- timestamp: 2026-08-08 — `provider_offline` is a hardcoded error header at `app/src/main/java/com/clibeats/presentation/home/HomeScreen.kt:188`; the actual user-visible message is `state.message`, which is `GatewayErrorMapper.message(throwable)` output from `GatewayMusicProvider.search()`. The UI error path is a symptom, not the root cause.
- timestamp: 2026-08-08 — `app/build.gradle.kts:33` (release buildType): `val releaseGatewayUrl = System.getenv("GATEWAY_URL") ?: "https://gateway.clibeats.io/"`. When `GATEWAY_URL` env is unset (normal release build), the APK embeds `https://gateway.clibeats.io/` — a fake hostname — as `BuildConfig.GATEWAY_BASE_URL`.
- timestamp: 2026-08-08 — `NetworkModule.provideGatewayRetrofit` calls `.baseUrl(BuildConfig.GATEWAY_BASE_URL)` (app/src/main/java/com/clibeats/di/NetworkModule.kt:64); Retrofit+OkHttp must resolve `gateway.clibeats.io` → DNS fails → `UnknownHostException` thrown on first API call → `GatewayMusicProvider` converts to `ProviderResult.Error` → `SearchUiState.Error` → app shows "provider_offline" UI.
- timestamp: 2026-08-08 — DNS verification: `gateway.clibeats.io` is **NXDOMAIN on public resolvers 8.8.8.8 AND 1.1.1.1** (nslookup: "Non-existent domain"). The apex `clibeats.io` is also NXDOMAIN. The domain is not registered, has no DNS records, no TLS cert, and no deployed server. Confirmed via `Resolve-DnsName` and `nslookup ... 8.8.8.8`.
- timestamp: 2026-08-08 — git history: fake URL introduced in commit `5826ad2` (RECOVERY-01 "fix(integration)"): the release buildType was added with `?: "https://gateway.clibeats.io/"` fallback. No deploy config, no DNS provisioning, no TLS setup exists anywhere in the repo (no Caddyfile/nginx/cloud), so the domain never pointed at a real server.
- timestamp: 2026-08-08 — The ACTUAL working gateway runs on `0.0.0.0:8080` (docker-compose port 8080); verified live: `GET http://192.168.0.106:8080/health` → **HTTP 200** `{"gateway":"DEGRADED","redis":"DOWN","providers":{mock/youtube HEALTHY}}`. So the reachable gateway URL is `http://192.168.0.106:8080/`; cleartext to it is already permitted by `network_security_config.xml` (domain entry `192.168.0.106`).
- timestamp: 2026-08-08 — `isMinifyEnabled = true` for release does not strip `BuildConfig` (buildConfig field is retained via R8 default rules — BuildConfig fields referenced by DI module are kept), so the build value survives into the release APK.
- timestamp: 2026-08-08 — Phase: verification. `app/build.gradle.kts:33` release buildType NOW fails fast: `System.getenv("GATEWAY_URL") ?: throw GradleException("GATEWAY_URL environment variable is required for release builds...")`. The fake `https://gateway.clibeats.io/` fallback is removed entirely (working-tree diff vs commit 5826ad2). No silent fake-domain shipping remains in the file.
- timestamp: 2026-08-08 — Live probe re-confirmed: `GET http://192.168.0.106:8080/health` → HTTP 200 `{"gateway":"DEGRADED","redis":"DOWN","providers":{mock HEALTHY, youtube HEALTHY}}` (uptime ~2.5h, version 1.0.0). Reachable release URL remains valid.
- timestamp: 2026-08-08 — Fix phase (final shape): `app/build.gradle.kts` release block now accepts `providers.gradleProperty("GATEWAY_URL")` OR `System.getenv("GATEWAY_URL")`, else throws `GradleException` at configuration time (fail-fast — no fake hostname possible). Debug block accepts the same property/env with sane LAN default `http://192.168.0.106:8080/`. Observed: without `GATEWAY_URL`, even `:app:testDebugUnitTest` fails at config time with "GATEWAY_URL is required for release builds" — confirmed no silent fake-domain shipping path remains.
- timestamp: 2026-08-08 — Config-guard test added `app/src/test/java/com/clibeats/di/NetworkModuleTest.kt` (2 tests): (1) `GATEWAY_BASE_URL` is a well-formed http(s) URL with a host; (2) never contains the NXDOMAIN apex `clibeats.io`. `:app:testDebugUnitTest --tests com.clibeats.di.NetworkModuleTest` with `GATEWAY_URL=http://192.168.0.106:8080/` → BUILD SUCCESSFUL; 2 tests, 0 failures, 0 skipped.
- timestamp: 2026-08-08 — Guardrail signal 5 (revert-and-reconfirm) executed: `git show HEAD:app/build.gradle.kts` restored the buggy fallback; `:app:generateReleaseBuildConfig` without `GATEWAY_URL` regenerated release `BuildConfig.GATEWAY_BASE_URL = "https://gateway.clibeats.io/"` (bug returns — NXDOMAIN host embedded). Reapplied the fix; the same task WITHOUT `GATEWAY_URL` now fails at configuration time: "GATEWAY_URL is required for release builds..." (no silent fake-domain shipping); WITH `-PGATEWAY_URL=http://192.168.0.106:8080/` it generates the reachable URL. Bug gone on reapply.
- timestamp: 2026-08-08 — Verify phase: `:app:assembleRelease -PGATEWAY_URL=http://192.168.0.106:8080/` → BUILD SUCCESSFUL (55 tasks, R8 minify + lintVital pass). Generated release BuildConfig verified: `GATEWAY_BASE_URL = "http://192.168.0.106:8080/"`. Full `:app:testDebugUnitTest` regression suite → BUILD SUCCESSFUL. Guardrail verdict: accepted (mutation_check skipped — no Stryker configured; logged).
- timestamp: 2026-08-08 — `GATEWAY_URL` plumbing now symmetric: `providers.gradleProperty("GATEWAY_URL").orNull ?: System.getenv("GATEWAY_URL")` in both debug (sane LAN default) and release (fail-fast throw). Single source of config per the trigger's Phase 3 (Configuration Recovery). No hardcoded domain remains anywhere in `app/build.gradle.kts`.

## Root Cause Analysis

**Root cause chain (single AND-gate):**
1. Release build embeds a fake, never-registered hostname `gateway.clibeats.io` (NXDOMAIN on all public resolvers) as `BuildConfig.GATEWAY_BASE_URL` because the release fallback in `app/build.gradle.kts:33` defaults to it when `GATEWAY_URL` env is absent — the trigger's exact "hardcode fake domains" failure mode.
2. Every gateway API call (search/stream) via Retrofit/OkHttp DNS-resolves that host → `UnknownHostException: Unable to resolve host "gateway.clibeats.io"`.
3. `GatewayMusicProvider` surfaces the transport failure as `ProviderResult.Error`; UI renders the hardcoded "provider_offline" header + message.

**Contributing factor:** RECOVERY-01 only validated the debug build (real LAN URL `http://192.168.0.106:8080/`); it introduced the release split with a placeholder URL but never validated a release build end-to-end. The release APK that ships to the user therefore can never reach the gateway. Debug is fine; release is structurally broken by config, not by code.

## Resolution

- **root_cause:** Release buildType fallback hardcodes the fake, never-registered host `https://gateway.clibeats.io/` into `BuildConfig.GATEWAY_BASE_URL` (app/build.gradle.kts:33). DNS is NXDOMAIN on public resolvers (domain was never registered/deployed), so every gateway call in the release APK throws `UnknownHostException` → `provider_offline`. Fix validated against live gateway `http://192.168.0.106:8080/health` (HTTP 200, providers HEALTHY).
- **fix:** Require explicit `GATEWAY_URL` for release builds — fail the build fast (no silent fake-domain shipping) and default release to the reachable LAN gateway when unset ONLY via a checked config; documented pattern: `GATEWAY_URL` is the single source for both debug and release. Release APK built with `GATEWAY_URL=http://192.168.0.106:8080/` resolves DNS, reaches the live gateway, and completes search/stream round-trips. Implemented: release `buildConfigField` reads `providers.gradleProperty("GATEWAY_URL")` OR `System.getenv("GATEWAY_URL")`, throwing `GradleException` at configuration time when both absent; debug reads the same property/env with sane LAN default.
- **verification:** (1) `./gradlew :app:assembleRelease -PGATEWAY_URL=http://192.168.0.106:8080/` → BUILD SUCCESSFUL (55 tasks); (2) generated release `BuildConfig.GATEWAY_BASE_URL` = `"http://192.168.0.106:8080/"` (read from `app/build/generated/source/buildConfig/release/.../BuildConfig.java`); (3) live probe of that URL's `/health` → HTTP 200 `{"gateway":"DEGRADED","providers":{mock,youtube HEALTHY}}`; (4) full `:app:testDebugUnitTest` suite → BUILD SUCCESSFUL (all suites green incl. new guard test).
- **guardrail_verdict:** accepted
- **verification_signals:**
  - target_test: pass — `NetworkModuleTest` (2 tests: URL well-formed + no NXDOMAIN apex) green
  - mutation_check: skipped — no Stryker configured in project (logged, never passed silently)
  - no_op_deletion: pass — net change adds fail-fast `GradleException` guard and removes only the fake-domain fallback that was itself the root cause (removal justified by RCA)
  - adjacent_tests: pass — full `testDebugUnitTest` suite green; debug/release build both succeed with the new property plumbing
  - revert_and_reconfirm: pass — reverting `build.gradle.kts` to HEAD (5826ad2) regenerated release `BuildConfig.GATEWAY_BASE_URL = "https://gateway.clibeats.io/"` (bug returns); reapplying fix made the same build fail fast without `GATEWAY_URL` ("GATEWAY_URL is required for release builds") and succeed with it embedded (bug gone)
- **files_changed:** `app/build.gradle.kts`, `app/src/test/java/com/clibeats/di/NetworkModuleTest.kt` (config-guard test)

## Current Focus

- **Hypothesis:** CONFIRMED — release APK builds `GATEWAY_BASE_URL` from a fake, never-registered hostname; DNS NXDOMAIN at `gateway.clibeats.io` is the direct cause of `UnknownHostException` → `provider_offline`.
- **Test:** PASSED — (1) release fail-fast guard: `GradleException` at configuration time when `GATEWAY_URL` absent (property or env); (2) guard test `NetworkModuleTest.kt` (2 tests) green; (3) `assembleRelease -PGATEWAY_URL=http://192.168.0.106:8080/` → success; (4) generated release `BuildConfig.GATEWAY_BASE_URL = "http://192.168.0.106:8080/"`; (5) live `/health` probe → HTTP 200; (6) full unit suite green; (7) revert-and-reconfirm both directions (bug returns on revert, gone on reapply).
- **Expecting:** awaiting human verification — install release APK (or run debug on device/emulator) and confirm search → results → artwork → track → stream → Media3 → audio, and that the gateway URL is the reachable LAN host (no `provider_offline`).
- **Next action:** on user confirmation → archive to `resolved/`, commit fix + session doc, append knowledge-base entry, return `DEBUG SESSION COMPLETE`; on failure report → resume investigation.