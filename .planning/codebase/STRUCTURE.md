# Codebase Structure

**Analysis Date:** 2026-08-09

## Directory Layout

```
clibeats/
├── app/                          # Android application module
│   ├── build.gradle.kts          # App build config (SDK, flavors, deps, quality tools)
│   ├── proguard-rules.pro        # R8/ProGuard keep rules
│   ├── schemas/                  # Exported Room DB schemas (v1.json)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/clibeats/   # All Kotlin source (see below)
│       │   └── res/                # XML resources (network config, extraction rules)
│       ├── test/java/com/clibeats/  # JVM unit + Paparazzi screenshot tests
│       └── androidTest/java/com/clibeats/  # Instrumented Room DAO tests
├── gateway/                      # Provider gateway service (TypeScript)
│   ├── src/                      # Source (app, config, core, providers, types, schemas)
│   ├── tests/                    # Vitest suites (unit/integration/contract/property/load)
│   ├── scripts/                  # OpenAPI generate/validate, phase validation
│   ├── config/gateway.yaml       # Gateway runtime config
│   ├── Dockerfile                # node:22-alpine multi-stage build
│   ├── render.yaml               # Render.com deployment config
│   ├── docker-compose.yml        # Local gateway + redis
│   ├── package.json / tsconfig.json / vitest.config.ts
│   └── *.mjs                     # Ad-hoc PO-token diagnostics scripts
├── config/detekt/detekt.yml      # Detekt rules (clean-architecture enforcement)
├── gradle/                       # Version catalog (libs.versions.toml) + wrapper
├── docs/                         # ADRs, architecture, release notes, user guide
├── .planning/                    # GSD planning docs, codebase map, debug sessions
├── .github/workflows/ci.yml      # GitHub Actions CI
└── settings.gradle.kts, build.gradle.kts, gradle.properties
```

## Directory Purposes

**`app/src/main/java/com/clibeats/` (Android package root):**
- Purpose: All Android application code, organized by Clean Architecture layer + cross-cutting concerns
- Contains:
  - `CLIBeatsApp.kt` — `@HiltAndroidApp` Application class
  - `MainActivity.kt` — single Compose activity with destination switching
  - `core/logging/StructuredLogger.kt` — logging abstraction for telemetry
  - `data/` — gateway client, Room local DB (entities/DAOs/mappers), disk cache, downloads, preferences, network monitor, repository impls
  - `di/` — Hilt modules (`AppModule`, `DatabaseModule`, `NetworkModule`, `PlaybackModule`, `ProviderModule`, `RepositoryModule`, `StorageModule`, `CacheModule`, `DownloadModule`, `ImageLoaderModule`, `TelemetryModule`)
  - `domain/` — pure models, `MusicProvider`, repository interfaces, `QueueManager`
  - `playback/` — `PlayerAdapter` (ExoPlayer wrapper), `service/PlaybackService` (MediaSessionService)
  - `presentation/` — Compose screens + ViewModels, theme, shared components
  - `telemetry/` — `AnalyticsEvent`, `CrashReporter`, `TelemetryTracker` interfaces + Timber implementations (ADR-010)

**`app/src/test/` and `app/src/androidTest/`:**
- Purpose: JVM unit/screenshot tests and instrumented tests respectively
- Key files: `.../integration/PlaybackIntegrationTest.kt`, `.../theme/*ScreenshotTest.kt`, `.../data/local/dao/*DaoTest.kt` (androidTest)

**`gateway/src/` (TypeScript package root):**
- Purpose: Gateway service split into types → core → providers → API
- Contains:
  - `app.ts` — Fastify app factory: routes, hooks, DI decoration, error handler, Redis + CDN probe caching
  - `server.ts` — process entry point (listen + graceful shutdown)
  - `schemas.ts` — JSON Schema objects per route (also feeds OpenAPI)
  - `config/config.ts` — YAML/env config loading (`GatewayConfig` interface)
  - `core/cache/` — `CacheManager` + segregated caches (`SearchCache`, `AlbumCache`, `ArtistCache`, `PlaylistCache`, `SessionCache`, `ArtworkCache`, `HealthCache`) over a shared `RedisCacheBase`
  - `core/circuit/CircuitBreaker.ts` — CLOSED/OPEN/HALF_OPEN state machine
  - `core/events/EventBus.ts` — internal pub/sub feeding logs + metrics
  - `core/health/RedisHealthChecker.ts` — Redis ping check
  - `core/logging/logger.ts` — pino singleton
  - `core/metrics/metrics.ts` — Prometheus registry + EventBus wiring
  - `core/registry/ProviderRegistry.ts` — adapter map
  - `core/selection/ProviderSelectionEngine.ts` — scoring, selection, failover
  - `providers/youtube/` — `YouTubeProviderAdapter.ts`, `media.ts` (raw response parsing), `poToken/mint.ts` (BotGuard/WAA), `ProviderTokenService.ts` (token lifecycle)
  - `providers/mock/MockProviderAdapter.ts` — seeded dataset + 8 failure states
  - `providers/registerProviders.ts` — config-driven adapter registration
  - `types/` — `adapter.ts`, `capabilities.ts`, `context.ts`, `domain.ts`, `error.ts`

**`gateway/tests/`:**
- Purpose: Vitest suites organized by level
- Contains: `unit/` (core, mock-provider, provider-token-service, redis-cache-resilience, redis-health, youtube-adapter, youtube-adapter-token), `integration/` (api, failover, health, metrics), `contract/openapi.test.ts`, `property/search-property.test.ts`, `architecture/layers.test.ts`, `load/load-test.ts`

**`gateway/scripts/`:**
- Purpose: Tooling scripts
- Contains: `generate-openapi.ts`, `validate-openapi.ts`, `validate-phase-a-gateway.ts`

**`docs/`:**
- Purpose: Documentation — `adr/` (ADR-001..020), `architecture/` (FINAL_ARCHITECTURE, MASTER_ROADMAP, PROVIDER_STRATEGY, TECHNICAL_DEBT, RISK_REGISTER, ...), `RELEASE_NOTES.md`, `USER_GUIDE.md`, `integration-map.md`, `LICENSES.md`

**`.planning/`:**
- Purpose: GSD workflow state — `STATE.md`, `ROADMAP.md`, `codebase/` (this map), `debug/` (recovery + PO-token investigation sessions), `phases/` (phase plans/summaries)

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/clibeats/MainActivity.kt`: Android UI entry (single activity)
- `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`: Background media entry
- `gateway/src/server.ts`: Gateway process entry
- `gateway/src/app.ts`: Gateway app factory (routes + wiring)

**Configuration:**
- `gradle/libs.versions.toml`: Android version catalog (single source of truth)
- `app/build.gradle.kts`: App build config incl. `GATEWAY_URL` handling (debug/release)
- `gateway/config/gateway.yaml`: Providers, cache TTLs, stream, PO-token config
- `gateway/src/config/config.ts`: Config loader (YAML + env overrides)
- `gateway/render.yaml`, `gateway/Dockerfile`, `gateway/docker-compose.yml`: Deployment
- `config/detekt/detekt.yml`: Static-analysis rules incl. ForbiddenImport enforcement
- `.github/workflows/ci.yml`: CI pipeline

**Core Logic:**
- `gateway/src/core/selection/ProviderSelectionEngine.ts`: failover + scoring
- `gateway/src/core/cache/CacheManager.ts` + `cache/segregated/*.ts`: caching
- `gateway/src/providers/youtube/YouTubeProviderAdapter.ts`: YouTube integration
- `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`: playback state engine
- `app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt`: client-side provider
- `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt`: Room DB definition

**Testing:**
- `gateway/tests/**`: gateway test suites
- `app/src/test/java/com/clibeats/**`: Android unit + screenshot tests
- `app/src/androidTest/java/com/clibeats/**`: instrumented DAO tests

**Documentation:**
- `docs/adr/*.md`: Architecture Decision Records (001-020)
- `docs/architecture/*.md`: Architecture strategy, debt, risk docs
- `docs/RELEASE_NOTES.md`, `docs/USER_GUIDE.md`, `README.md`, `WINDOWS.md`

## Naming Conventions

**Files:**
- Kotlin: `PascalCase.kt`, one class/object per file, `*Test.kt` for unit tests, `*ScreenshotTest.kt` for Paparazzi
- TypeScript: `PascalCase.ts` for classes/modules, `kebab-case` none — camelCase file names for services (`providerTokenService.ts`), `*.test.ts` for tests
- Android resources: `snake_case.xml` (e.g., `network_security_config.xml`, `data_extraction_rules.xml`)

**Directories:**
- Kotlin: lowercase single-context dirs matching package structure (`data/local/dao`, `domain/model`, `presentation/settings`)
- TypeScript: lowercase (`core/cache/segregated`, `providers/youtube/poToken`)

**Special Patterns:**
- Room: entity → `*Entity.kt`, DAO → `*Dao.kt`, cross-ref → `*CrossRef.kt`; database schema exported under `app/schemas/com.clibeats.data.local.CliBeatsDatabase/`
- Hilt: `di/*Module.kt` (object or class with `@InstallIn`)
- Gateway cache: `*Cache.ts` in `core/cache/segregated/` extending `RedisCacheBase`
- Gateway providers: `*ProviderAdapter.ts` implementing `ProviderAdapter`; types in `types/*.ts`

## Where to Add New Code

**New Android feature (e.g., a new screen):**
- UI + ViewModel: `app/src/main/java/com/clibeats/presentation/<feature>/`
- State models: same dir as `*UiState.kt`
- Domain models/contracts: `app/src/main/java/com/clibeats/domain/model` + `domain/repository`
- Data implementation: `app/src/main/java/com/clibeats/data/**`
- DI wiring: `app/src/main/java/com/clibeats/di/`
- Tests: `app/src/test/java/com/clibeats/presentation/<feature>/` (unit) + `app/src/androidTest/` (instrumented)
- Navigation: register destination in `presentation/layout/NavDestination.kt` + `MainActivity.kt`

**New gateway provider:**
- Adapter: `gateway/src/providers/<name>/<Name>ProviderAdapter.ts`
- Registration: `gateway/src/providers/registerProviders.ts` (config-driven from `gateway/config/gateway.yaml`)
- Config: `providers.<name>` block in `gateway.yaml`
- Tests: `gateway/tests/unit/<name>-adapter.test.ts`

**New gateway endpoint:**
- Route + handler: `gateway/src/app.ts`
- Schema: `gateway/src/schemas.ts` (auto-included in OpenAPI)
- Tests: `gateway/tests/integration/api.test.ts`

**Utilities:**
- Shared gateway helpers: `gateway/src/core/**`
- Shared Android helpers: `app/src/main/java/com/clibeats/core/**`

## Special Directories

**`app/schemas/`:**
- Purpose: Exported Room migration schemas
- Source: Auto-generated by Room KSP (`room.schemaLocation` arg in `app/build.gradle.kts`)
- Committed: Yes (needed for verified migrations)

**`gateway/dist/`:**
- Purpose: Compiled gateway JS output
- Source: `tsc` (`npm run build`)
- Committed: No (gitignored; `.dockerignore` too)

**`.planning/debug/`:**
- Purpose: Debug/recovery session docs (RECOVERY-01/02/06, PO-token investigation) + captured evidence (gateway.log, PNGs)
- Committed: Yes (session state)

**`app/src/test` Paparazzi output:**
- Purpose: Screenshot rendering fixtures
- Source: Paparazzi Gradle plugin at test runtime
- Committed: No (build artifacts)

---

*Structure analysis: 2026-08-09*
*Update when directory structure changes*
