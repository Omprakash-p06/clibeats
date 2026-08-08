# Technology Stack

**Analysis Date:** 2026-08-08

This repository contains TWO codebases:

1. **Android app** — Kotlin/Jetpack Compose music client, rooted at `app/`
2. **Provider gateway** — TypeScript Node.js Fastify service, rooted at `gateway/`

They are coupled: the Android app talks to the gateway via Retrofit (`BuildConfig.GATEWAY_BASE_URL`), and the gateway resolves music metadata/streams from YouTube Music via InnerTube. All YouTube-facing logic lives server-side in the gateway.

## Languages

**Primary:**
- Kotlin 2.0.21 — Android app UI, domain, and data layers (`app/src/main/java/com/clibeats/`)
- TypeScript 5.7.3 (declared in `gateway/package.json`; lockfile resolves 5.9.3) — gateway service (`gateway/src/`)
- YAML — gateway runtime config (`gateway/config/gateway.yaml`), docker-compose (`gateway/docker-compose.yml`)

**Secondary:**
- Groovy DSL (`.gradle.kts` is Kotlin DSL, but wrapper `gradlew`/`gradlew.bat` are shell/batch)
- Shell — `scripts/check-quality-gates.sh`
- SQL — via Room DAO annotations (`app/src/main/java/com/clibeats/data/local/dao/`)

## Runtime

**Environment:**
- Android: minSdk 26 (Android 8.0), targetSdk 34, compileSdk 34 (`app/build.gradle.kts`)
- Node.js 20 — production runtime (`gateway/Dockerfile` uses `node:20-alpine`; CI uses `node-version: '20'` in `.github/workflows/ci.yml`). Node 22 locally compatible.

**Package Manager:**
- Android: Gradle wrapper 8.9 (`gradle/wrapper/gradle-wrapper.properties`), version catalog `gradle/libs.versions.toml`
- Gateway: npm (lockfile `gateway/package-lock.json`, lockfileVersion 3, present)

## Frameworks

**Core:**
- Android app:
  - Jetpack Compose, Material 3 via Compose BOM 2024.09.03 (`compose-bom` in `gradle/libs.versions.toml`)
  - Material3 Adaptive Navigation Suite 1.3.1 + `material-icons-extended` — TUI-style navigation (`libs.material3.adaptive.nav`)
  - Hilt / Dagger 2.51.1 — dependency injection (`app/src/main/java/com/clibeats/di/`)
  - AndroidX Media3 1.4.1 — ExoPlayer + MediaSession foreground service (`app/src/main/java/com/clibeats/playback/`)
  - Room 2.6.1 — local persistence (`app/src/main/java/com/clibeats/data/local/`)
  - kotlinx.serialization 1.7.1 — JSON (gateway DTO + Retrofit body parsing in `app/src/main/java/com/clibeats/data/gateway/dto/`)
- Gateway:
  - Fastify 5.x (^5.2.1; lockfile 5.11.2) — HTTP framework (`gateway/src/app.ts`)
  - youtubei.js 17.2.0 — YouTube Music InnerTube client (`gateway/src/providers/youtube/YouTubeProviderAdapter.ts`)
  - ioredis 5.x — Redis client for caching (`gateway/src/core/cache/`)

**Testing:**
- Android: JUnit 4.13.2, Mockito Core 5.12.0 + Mockito-Kotlin 5.4.0, kotlinx-coroutines-test 1.8.1, Room testing, Media3 test utils 1.4.1, OkHttp MockWebServer 4.12.0, Compose UI test (JUnit4), Paparazzi 1.3.4 screenshot regression (`app/src/test/snapshots/`)
- Gateway: Vitest 3.x (lockfile 3.2.7) + @vitest/coverage-v8 3.2.7 (70% threshold in `gateway/vitest.config.ts`), fast-check 3.23.2 property tests, autocannon 8.0.0 load tests (`gateway/tests/load/load-test.ts`), ioredis-mock 5.9.0

**Build/Dev:**
- Android: Gradle 8.9, AGP 8.5.2, KSP 2.0.21-1.0.27, kotlin serialization plugin, Paparazzi, Detekt 1.23.6, ktlint 12.1.1 (org.jlleitschuh.gradle.ktlint)
- Gateway: tsc (`npm run build`), ts-node-dev (`npm run dev`), ts-node for scripts (`gateway/scripts/generate-openapi.ts`, `validate-openapi.ts`), OpenAPI Swagger generation (@fastify/swagger + @fastify/swagger-ui, spec at `gateway/openapi.json`)

## Key Dependencies

**Critical:**
- `youtubei.js` 17.2.0 — sole music-provider client; wraps the unofficial YouTube Music InnerTube API (`https://music.youtube.com/youtubei/v1/`). Used with `ClientType.MUSIC` for metadata and `ClientType.IOS` for streaming (separate sessions, see `gateway/src/providers/youtube/YouTubeProviderAdapter.ts`).
- `androidx.media3` 1.4.1 — audio engine; `DefaultHttpDataSource` streams resolved URLs directly to CDN (`app/src/main/java/com/clibeats/di/PlaybackModule.kt`).
- `retrofit` 2.11.0 + `retrofit2-kotlinx-serialization-converter` 1.0.0 — Android → gateway HTTP (`app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt`).
- `fastify` 5.x — REST + OpenAPI + CORS + error model for gateway (`gateway/src/app.ts`).
- `ioredis` 5.x — Redis caching layer, fail-open design (`gateway/src/core/cache/RedisCacheBase.ts`).

**Infrastructure:**
- `okhttp` 4.12.0 — HTTP client on Android (30s timeouts, debug body logging in `app/src/main/java/com/clibeats/di/NetworkModule.kt`)
- `coil-compose` 2.7.0 — artwork image loading with memory/disk caches (`app/src/main/java/com/clibeats/di/ImageLoaderModule.kt`)
- `androidx.security:security-crypto` 1.1.0-alpha06 — EncryptedSharedPreferences, AES256_GCM via Keystore MasterKey (`app/src/main/java/com/clibeats/di/StorageModule.kt`)
- `androidx.datastore:datastore-preferences` 1.1.1 — settings storage (`app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt`)
- `prom-client` 15.1.3 — Prometheus metrics (`gateway/src/core/metrics/metrics.ts`)
- `pino` 9.6.0 + `pino-pretty` 13.0.0 — structured logging with trace IDs (`gateway/src/core/logging/logger.ts`)
- `yaml` 2.7.0 — gateway YAML config parsing (`gateway/src/config/config.ts`)
- `dotenv` 16.4.7 — env loading for gateway

## Configuration

**Environment:**
- Android: build-time BuildConfig field `GATEWAY_BASE_URL` = `http://10.0.2.2:8080/` (emulator loopback into host gateway) in `app/build.gradle.kts`; network security / manifest perms in `app/src/main/AndroidManifest.xml` (INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK)
- Gateway: `gateway/config/gateway.yaml` (port 8080, host 0.0.0.0, CORS `*`, provider toggles, Redis URL, TTLs). Env overrides: `PORT`, `HOST`, `REDIS_URL`, `GATEWAY_CONFIG_PATH`, `NODE_ENV`, `LOG_LEVEL` (`gateway/src/config/config.ts`, `gateway/src/core/logging/logger.ts`)
- `docker-compose.yml` (`gateway/docker-compose.yml`): overrides `NODE_ENV=production`, `REDIS_URL=redis://redis:6379`
- No `.env` files present in the repo

**Build:**
- Android: `build.gradle.kts` (root, module `app/build.gradle.kts`), `settings.gradle.kts`, `gradle.properties` (`org.gradle.jvmargs=-Xmx2048m`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `android.suppressUnsupportedCompileSdk=35`)
- Gateway: `gateway/tsconfig.json` (ES2022, CommonJS, strict), `gateway/vitest.config.ts`, `gateway/Dockerfile`
- Quality gates script: `scripts/check-quality.sh`

## Platform Requirements

**Development:**
- JDK 17 (Java version pinned in `app/build.gradle.kts` `JavaVersion.VERSION_17`, CI `temurin 17`)
- Android SDK 34, ANDROID_HOME (fallback path `C:\Android\Sdk` set in `app/build.gradle.kts` test config)
- Node.js 20+ for gateway dev (docker images: `node:20-alpine`)
- Local Redis for gateway dev (or let tests use `ioredis-mock` when `NODE_ENV=test`)

**Production:**
- gateway: Docker container (multi-stage `gateway/Dockerfile`), Redis 7 (`redis:7-alpine` in `gateway/docker-compose.yml`), Prometheus scraped `/metrics`
- app: APK sideload/release via Gradle (`app/build.gradle.kts` release build, debug signing for now)

---

*Stack analysis: 2026-08-08*