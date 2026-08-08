# Technology Stack

**Analysis Date:** 2026-08-09

## Languages

**Primary:**
- Kotlin 2.0.21 - All Android application code (`app/src/main/java/com/clibeats/**`)
- TypeScript 5.7 - All gateway service code (`gateway/src/**`)

**Secondary:**
- Groovy / Kotlin DSL (`.gradle.kts`) - Gradle build scripts
- JSON Schema (TS objects) - OpenAPI contract in `gateway/src/schemas.ts`
- YAML - Gateway config (`gateway/config/gateway.yaml`), deployment (`gateway/render.yaml`, `docker-compose.yml`)
- XML - Android manifest & resources (`app/src/main/AndroidManifest.xml`, `res/xml/network_security_config.xml`)

## Runtime

**Environment:**
- Android 8.0+ (API 26) to API 34 — `minSdk = 26`, `targetSdk = 34`, JVM target 17 (`app/build.gradle.kts`)
- Node.js 22 (LTS) — gateway, pinned via `node:22-alpine` in `gateway/Dockerfile`; CI uses Node 20 (`setup-node@v4` in `.github/workflows/ci.yml`)
- Java 17 (temurin) — required for the Android Gradle build

**Package Manager:**
- Gradle 8.x with version catalog — `gradle/libs.versions.toml`, wrapper in `gradle/wrapper/` (`gradlew`, `gradlew.bat`)
- npm 10.x — `gateway/package-lock.json` present, `npm ci` in CI

## Frameworks

**Core:**
- Jetpack Compose (BOM 2024.09.03) + Material 3 — Android UI (`app/src/main/java/com/clibeats/presentation/**`), includes Material3 Adaptive Navigation Suite 1.3.1 and material-icons-extended
- AndroidX Media3 1.4.1 (ExoPlayer, MediaSession, MediaSessionService) — playback engine (`app/src/main/java/com/clibeats/playback/**`)
- Fastify 5.2.1 — gateway HTTP framework (`gateway/src/app.ts`, `server.ts`)
- youtubei.js 17.2.0 — YouTube InnerTube client, gateway-side only (no InnerTube code in the Android app)

**Dependency Injection:**
- Hilt 2.51.1 (Dagger) + Hilt Navigation Compose 1.2.0 — Android (`app/src/main/java/com/clibeats/di/**`)
- Fastify instance decoration — gateway DI (`app.decorate(...)` in `gateway/src/app.ts`)

**Persistence:**
- Room 2.6.1 (KSP) — Android local DB (`CliBeatsDatabase`, entities/DAOs in `app/src/main/java/com/clibeats/data/local/**`), schema exported to `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json`
- DataStore Preferences 1.1.1 — non-sensitive settings (`AppPreferences`)
- androidx.security:security-crypto 1.1.0-alpha06 (EncryptedSharedPreferences + Keystore MasterKey AES256_GCM) — AUTH_TOKEN storage
- Redis via ioredis 5.4.2 — gateway cache (`gateway/src/core/cache/**`), `redis:7-alpine` in `docker-compose.yml`

**Testing:**
- JUnit 4.13.2, Mockito 5.x (mockito-kotlin 5.4.0) — Android unit tests (`app/src/test/**`)
- kotlinx-coroutines-test 1.8.1 — coroutine testing
- Paparazzi 1.3.4 — Android screenshot/Compose rendering tests (`app/src/test/**/*ScreenshotTest.kt`)
- MockWebServer (OkHttp 4.12.0) — HTTP mocking
- Vitest 3.0.4 + @vitest/coverage-v8 — gateway tests (`gateway/tests/**`, `gateway/vitest.config.ts`)
- fast-check 3.23.2 — property-based tests (`gateway/tests/property/search-property.test.ts`)
- autocannon 8.0.0 — load testing (`gateway/tests/load/load-test.ts`, `npm run test:load`)
- ioredis-mock 8.9.0 — Redis mocking in gateway tests (`gateway/src/app.ts` lazy-requires it under `NODE_ENV=test`)

**Build/Dev:**
- AGP 8.5.2, Kotlin 2.0.21, Compose compiler plugin, KSP 2.0.21-1.0.27 — Android build
- ktlint 12.1.1 (`org.jlleitschuh.gradle.ktlint`) + Detekt 1.23.6 (`config/detekt/detekt.yml`) + Android Lint — static analysis
- TypeScript 5.7 (tsc), ts-node 10.9.2, ts-node-dev 2.0.0 — gateway build/dev (`gateway/tsconfig.json`, `outDir: ./dist`, CommonJS, ES2022)

## Key Dependencies

**Critical:**
- youtubei.js 17.2.0 - All YouTube search/stream/album/artist/playlist resolution; the most fragile external dependency (schema/bot-detection changes break it)
- ioredis 5.4.2 - Gateway cache (search/metadata/stream/artwork segregated caches, fail-open on errors)
- bgutils-js 4.0.3 - BotGuard/WAA PO-token (Proof of Origin) minting for datacenter playback (`gateway/src/providers/youtube/poToken/mint.ts`)
- Media3 1.4.1 - Background audio playback, media session + notification, audio focus
- Room 2.6.1 - Offline persistence: songs, playlists, history, queue, cache index

**Infrastructure:**
- OkHttp 4.12.0 + Retrofit 2.11.0 + kotlinx-serialization 1.7.1 — Android → gateway HTTP client (`app/src/main/java/com/clibeats/data/gateway/**`)
- Coil 2.7.0 — artwork loading in Compose
- pino 9.6.0 + pino-pretty — gateway structured logging (`gateway/src/core/logging/logger.ts`)
- prom-client 15.1.3 — Prometheus metrics (`gateway/src/core/metrics/metrics.ts`, exposed at `/metrics`)
- @fastify/swagger 9.4.0 + swagger-ui 5.2.0 + yaml 2.7.0 — OpenAPI docs at `/documentation`, generated spec via `scripts/generate-openapi.ts`
- jsdom 29.1.1 — DOM environment used by the PO-token minting flow

## Configuration

**Environment:**
- `GATEWAY_URL` (Gradle property `-PGATEWAY_URL` or env var) — required for release builds; debug falls back to `http://192.168.0.106:8080/` (`app/build.gradle.kts`)
- Gateway: `PORT`, `HOST`, `REDIS_URL`, `NODE_ENV`, `LOG_LEVEL`, `GATEWAY_CONFIG_PATH` (`gateway/src/config/config.ts`), plus `gateway/config/gateway.yaml` (providers, cache TTLs, stream, PO token)
- `ANDROID_HOME` / `ANDROID_SDK_ROOT` — required for tests (injected by `app/build.gradle.kts` test config)

**Build:**
- `gradle/libs.versions.toml` - version catalog (single source of truth for Android deps)
- `gateway/tsconfig.json`, `gateway/vitest.config.ts`, `config/detekt/detekt.yml`, `.github/workflows/ci.yml`
- ProGuard rules in `app/proguard-rules.pro` (R8 enabled for release, `isMinifyEnabled = true`)

## Platform Requirements

**Development:**
- Windows (WINDOWS.md documents emulator + physical device setup) or any OS with JDK 17 + Android SDK + Node.js
- Redis optional locally (gateway degrades to cache-miss when down, `lazyConnect` + fail-open)
- Docker for gateway container builds; `docker-compose.yml` runs gateway + Redis together

**Production:**
- Render.com — `gateway/render.yaml` (web service, Docker, `plan: free`, `region: oregon`, health check `/health`)
- Android APK signed with debug keystore (release buildType uses `signingConfigs.getByName("debug")`)
- No production analytics/CDN; gateway relays CDN audio (Range-safe proxy) when `stream.proxyStreaming: true`

---

*Stack analysis: 2026-08-09*
*Update after major dependency changes*
