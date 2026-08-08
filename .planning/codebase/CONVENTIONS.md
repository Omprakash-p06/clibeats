# Coding Conventions

**Analysis Date:** 2026-08-08

This repo contains TWO codebases with distinct conventions:

- **Android/Kotlin app** under `app/` — Clean Architecture, Hilt DI, Compose UI
- **TypeScript gateway** under `gateway/` — Fastify HTTP service, layered core/providers

---

## Android / Kotlin Conventions

### Naming Patterns

**Files:**
- PascalCase per Kotlin class name: `SongRepositoryImpl.kt`, `SearchViewModel.kt`, `GatewayMusicProvider.kt`
- Suffixes encode role: `*RepositoryImpl` (data impl), `*Dao` (Room), `*Entity`, `*Dto`, `*Mapper`/`*Model` suffixless (domain), `*ViewModel`, `*UiState`, `*Screen`, `*Module` (Hilt), `*Test` (tests, e.g. `SearchViewModelTest.kt`)
- Mapper files use `*Mapper.kt` naming with free extension functions: `SongMapper.kt`, `PlaylistMapper.kt`, `GatewayMapper.kt`, `GatewayErrorMapper.kt`

**Functions:**
- camelCase; suspend functions used for I/O (`app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt`)
- Expression-body style (`=`) used heavily for single-expression functions, e.g. `override fun getTrackById(id: String): Track? = songDao.getById(id)?.toDomain()` in `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt`
- Test names use **backtick sentence style**: `fun \`search returns Success state on successful provider call\`()` (see `app/src/test/java/com/clibeats/presentation/search/SearchViewModelTest.kt`)

**Variables:**
- camelCase; `private val` for injected dependencies
- Private `MutableStateFlow` named `_foo` exposed as non-null `StateFlow<T>` named `foo` — `_query` / `query` in `app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt`
- Named arguments used pervasively in constructors/map-copies for readability (`Track(id = ..., title = ...)` in `app/src/test/java/com/clibeats/presentation/component/PlayerBarTest.kt`)
- Numeric literals like `5_000L`, `259_000L`, `1_700_000_000_000L` use Kotlin underscore separators

**Types:**
- Domain models are plain `data class` (e.g. `Track` in `app/src/main/java/com/clibeats/domain/model/Track.kt`)
- UI state uses `sealed interface` with `data object`/`data class` variants: `Loading`/`Empty`/`Success` in `app/src/main/java/com/clibeats/presentation/library/LibraryUiState.kt`
- Result wrapper is a sealed class with `data object Loading`: `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`

### Package Structure (Clean Architecture)

Feature-by-layer, root package `com.clibeats` (set in `app/build.gradle.kts`, `namespace = "com.clibeats"`):

| Layer | Location | Contents |
|-------|----------|----------|
| Presentation | `app/src/main/java/com/clibeats/presentation/**` | Compose screens, `*ViewModel`, `*UiState`, theme, components |
| Domain | `app/src/main/java/com/clibeats/domain/**` | Pure models, `*Repository` interfaces, `MusicProvider`, `QueueManager` |
| Data | `app/src/main/java/com/clibeats/data/**` | `repository/*Impl`, `local` (Room), `gateway` (Retrofit + DTOs + mappers), `cache`, `download`, `network`, `preferences` |
| DI | `app/src/main/java/com/clibeats/di/**` | All Hilt modules |
| Playback | `app/src/main/java/com/clibeats/playback/**` | Media3 `PlayerAdapter`, foreground `PlaybackService` |
| Core/Telemetry | `app/src/main/java/com/clibeats/core/**`, `telemetry/**` | StructuredLogger, Timber crash reporting |

**Layer rule (enforced by detekt):** Presentation must not import `com.clibeats.data.*`. The `ForbiddenImport` rule in `config/detekt/detekt.yml` blocks this; the exception pattern is in DI modules and data-internal files, which carry `@file:Suppress("ForbiddenImport")` with an explanatory comment (e.g. `app/src/main/java/com/clibeats/di/RepositoryModule.kt`, `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt`).

### Dependency Injection (Hilt)

**Injection style:** constructor injection with `@Inject`. Singletons annotated `@Singleton`, ViewModels `@HiltViewModel`.

- App class: `@HiltAndroidApp` on `CLIBeatsApp` in `app/src/main/java/com/clibeats/CLIBeatsApp.kt`
- Repository bindings: abstract `@Module @InstallIn(SingletonComponent::class)` with `@Binds` in `app/src/main/java/com/clibeats/di/RepositoryModule.kt`
- Creator modules: `object` module with `@Provides @Singleton` — e.g. Retrofit/OkHttp/Json in `app/src/main/java/com/clibeats/di/NetworkModule.kt`
- Qualified bindings use `@Named("gateway")` for the OkHttpClient/Retrofit pair (see `app/src/main/java/com/clibeats/di/NetworkModule.kt`)
- ViewModels take only repository/provider interfaces, never concrete data classes: `@HiltViewModel class SearchViewModel @Inject constructor(private val musicProvider: MusicProvider)` in `app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt`

### ViewModel / StateFlow Pattern

- Private `MutableStateFlow` for imperative input; `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), initialValue)` for declarative derivation (`LibraryViewModel.kt`, `SearchViewModel.kt`)
- Query streams compose `debounce` → `distinctUntilChanged` → `flatMapLatest`, emitting `Loading` before each attempt; `app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt` uses `DEBOUNCE_MS = 300L` and `MIN_QUERY_LENGTH = 2`
- ViewModels expose `val uiState: StateFlow<XUiState>` only; UI never calls repository directly

### Repository Pattern

- Interface in `domain/repository/`, impl in `data/repository/` (suffix `Impl`)
- Impls hold DAOs/providers; expose `Flow`-returning reads (`getAllTracksAsFlow`) and `suspend` writes; mapper extension functions convert entity↔domain (`app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt`)
- `escapeForLike()` extension on `String` for SQL `LIKE` wildcard escaping in `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt`

### Networking Conventions

- Retrofit interface `GatewayApi` in `app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt` — `suspend` functions, `@Query`/`@Path`/`@Body`, kotlinx.serialization converter
- `Json { ignoreUnknownKeys = true; isLenient = true }` in `app/src/main/java/com/clibeats/di/NetworkModule.kt`
- Timeouts: connect/read/call all 30s via `OkHttpClient.Builder()`
- OkHttp logging only in `BuildConfig.DEBUG` (`HttpLoggingInterceptor.Level.BODY`)

### Room Conventions

- One database: `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt` with `exportSchema = true` (schemas exported to `app/schemas/` via `ksp { arg("room.schemaLocation", ...) }` in `app/build.gradle.kts`)
- `@TypeConverters(CliBeatsTypeConverters::class)`; entities in `data/local/entity/`, DAOs in `data/local/dao/`
- DAO style: `@Insert(onConflict = OnConflictStrategy.REPLACE)` for upsert; `Flow<List<X>>` for reactive reads; `@Query("DELETE ...")` for deletes (`SongDao.kt`, `PlaylistDao.kt`, `CacheIndexDao.kt`, `HistoryDao.kt`, `QueueDao.kt`)

### Error Handling (Android)

**Result wrapper:** All provider/repository suspend calls resolve to `ProviderResult<T>` sealed class (`Success`/`Error(message, cause)`/`Loading`) — see `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt` and usage in `app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt`.

**HTTP error mapping:**
`app/src/main/java/com/clibeats/data/gateway/mapper/GatewayErrorMapper.kt` converts `Retrofit HttpException` + gateway error body into user-facing messages grouped by gateway code (`RATE_LIMITED`, `GEO_BLOCKED`, `NOT_FOUND`, `AUTHENTICATION_FAILED`, `PLAYBACK_ERROR`, `TIMEOUT_ERROR`, `NETWORK_ERROR`), falling back to `throwable.message`. Tests cover each code in `app/src/test/java/com/clibeats/data/gateway/mapper/GatewayErrorMapperTest.kt`.

**Transport pattern:**
`runCatching { ... }.getOrElse { ProviderResult.Error(GatewayErrorMapper.message(e), e) }` wrapped in `withContext(Dispatchers.IO)` — see `app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt`.

**Playback logging:**
`StructuredLogger` in `app/src/main/java/com/clibeats/core/logging/StructuredLogger.kt`: `object` with `PlaybackEvent` sealed interface; `log(PlaybackEvent.Failure(stage, reason, cause))` wrapped in `runCatching { Log.d(TAG, ...) }` so logging never crashes playback.

### Logging (Android)

- `StructuredLogger.log(...)` for playback lifecycle (`Search`, `TrackSelected`, `PlayerRequest`, `StreamResolved`, `Buffering`, `Playing`, `Failure`) — `app/src/main/java/com/clibeats/core/logging/StructuredLogger.kt`
- Timber-based `TimberCrashReporter` / `TimberTelemetryTracker` in `app/src/main/java/com/clibeats/telemetry/` (token redaction expected per `app/src/test/java/com/clibeats/telemetry/TimberCrashReporterTest.kt`)

### Code Style (Android)

**Formatting/Linting:**
- **ktlint** plugin `org.jlleitschuh.gradle.ktlint` v12.1.1 (root `build.gradle.kts`; run `./gradlew ktlintCheck`)
- **detekt** v1.23.6 with `config/detekt/detekt.yml`: `maxIssues: 0`, complexity rules — `TooManyFunctions` (files 15 / classes 15 / interfaces 10), `LongMethod` (threshold 60), `LargeClass` (300), `MagicNumber` (ignores -1,0,1,2,100,1000), `ForbiddenImport` for `com.clibeats.data.*`
- `@file:Suppress(...)` at the top of a file is the accepted escape hatch **with a comment why** — e.g. `@file:Suppress("ForbiddenImport", "MaxLineLength")` with "data-layer self-imports are legitimate" on `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt`; `@Suppress("Indentation", "MaxLineLength", "MaximumLineLength", "Wrapping")` for detekt parse issues in `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt`
- Compose screens suppress lint noise via `@file:Suppress(...)` and ktlint-specific suppressions like `"ktlint:standard:function-naming"` at the top of `app/src/main/java/com/clibeats/presentation/search/SearchViewModel.kt` and `"ktlint:standard:property-naming"` in `app/src/main/java/com/clibeats/presentation/playlist/PlaylistViewModel.kt`

**Android Lint:** `lint { abortOnError = true; checkDependencies = true; warningsAsErrors = false }` in `app/build.gradle.kts`

---

## Gateway TypeScript Conventions (`gateway/`)

### Tooling

- **TypeScript 5.7.3** with `tsconfig.json`: `"strict": true`, `target ES2022`, `module CommonJS`, `declaration`, `sourceMap`. `check` script is `tsc --noEmit`. No ESLint/Prettier — strictness enforced by `tsc` + review.
- Entry: `gateway/src/server.ts` (startup + graceful shutdown on SIGTERM/SIGINT); app composition in `gateway/src/app.ts`

### Naming & Structure (layered)

```
gateway/src/
├── app.ts            # Fastify composition + all routes + error handler
├── server.ts         # bootstrap
├── schemas.ts        # JSON-Schema definitions (bootstrapSchema … metricsSchema)
├── config/config.ts  # loadConfig() from gateway/config/gateway.yaml (+ env overrides)
├── types/            # adapter.ts, capabilities.ts, context.ts, domain.ts, error.ts (leaf types)
├── core/             # cache/, circuit/, events/, health/, logging/, metrics/, registry/, selection/ (pure infra)
└── providers/        # mock/, youtube/, registerProviders.ts (adapters, leaf)
```

- Files: `PascalCase.ts` for classes (`CacheManager.ts`, `ProviderSelectionEngine.ts`, `MockProviderAdapter.ts`); `kebab-case.ts` for utils/config (`gateway.yaml`, `victory` naming in scripts only).
- Layers reference ADR numbers in headers and route comments: `// 1. GET /api/v1/bootstrap (ADR-020 & user spec)`.

### Error Handling (gateway)

**Hierarchy**: `ProviderError` base + typed subclasses in `gateway/src/types/error.ts`; each carries `code` (`ProviderErrorCode` union), `providerId`, `statusCode`, optional `retryAfterSeconds`.

| Class | code | HTTP |
|-------|------|------|
| `AuthenticationFailedError` | `AUTHENTICATION_FAILED` | 401 |
| `RateLimitedError` | `RATE_LIMITED` | 429 |
| `GeoBlockedError` | `GEO_BLOCKED` | 403 |
| `NotFoundError` | `NOT_FOUND` | 404 |
| `UnsupportedError` | `UNSUPPORTED` | 501 |
| `PlaybackError` | `PLAYBACK_ERROR` | 502 |
| `NetworkError` | `NETWORK_ERROR` | 503 |
| `TimeoutError` | `TIMEOUT_ERROR` | 504 |
| `InternalError` | `INTERNAL_ERROR` | 500 |

**Handler** (`gateway/src/app.ts`): `app.setErrorHandler(...)` distinguishes `ProviderError` (returns structured body with `code`, `message`, `providerId`, `retryAfterSeconds`, `traceId`) from schema-validation 4xx (mapped to `INVALID_REQUEST`) and everything else → `INTERNAL_ERROR`.

**Adapter error mapping**: `YouTubeProviderAdapter` wraps upstream failures — rate limits become `RateLimitedError`, failed format lookup becomes `NotFoundError`; `MockProviderAdapter` failure matrix covers all states (see below).

### Fastify Plugin & Route Style

- Single composed app via `buildApp(config?)` exported from `app.ts`; decorators (`config`, `registry`, `engine`, `cache`, `health`) + `declare module 'fastify'` augmentation for typed `app.decorate`
- Routes registered with JSON-Schema `{ schema: searchSchema }` — `default Response Configuration`: every response carries `x-trace-id`; trace middleware hooks `onRequest`/`onResponse`/`onSend` added once
- Provider discovery via `registerProviders(app, registry, config)` in `gateway/src/providers/registerProviders.ts` (config-driven enable/disable from `gateway/config/gateway.yaml`)
- DI of core: manual constructor injection on `app.decorate` (no IoC container)

### Event / Metrics Conventions

- `EventBus` (`gateway/src/core/events/EventBus.ts`): union-typed payloads (`REQUEST_RECEIVED`, `CACHE_CHECKED`, `PROVIDER_SELECTED`, `PROVIDER_FAILED`, `PROVIDER_FAILOVER`, `STREAM_RESOLVED`, `CIRCUIT_TRIPPED`); `logger` subscribes `onEvent('*', …)`
- `logger` (`gateway/src/core/logging/logger.ts`): pino with `LOG_LEVEL` env, ISO timestamps; structured context objects always first arg: `logger.info({ traceId, method, url }, 'incoming request')`
- Metrics: Prometheus counters/histograms in `gateway/src/core/metrics/metrics.ts` (`searchLatencyHistogram.observe({ cached }, seconds)`)

### Mock Provider Failure Matrix

`gateway/src/providers/mock/MockProviderAdapter.ts` implements `ProviderAdapter` with state machine:

```ts
export type MockProviderState = 'HEALTHY' | 'SLOW' | 'OFFLINE' | 'MALFORMED' | 'RATE_LIMITED' | 'AUTHENTICATION_FAILED' | 'GEO_BLOCKED' | 'INTERNAL_ERROR';
```

- `simulateFailure()` throws a canonical `ProviderError` per state; `MALFORMED` throws non-canonical `SyntaxError` to exercise parse-failure paths
- Seeded PRNG (`seed=42`) generates a deterministic dataset (5 artists, 100 tracks, 10 albums, 5 playlists)
- Knobs: `state` property; `shouldSimulateError`/`simulatedErrorCode` backwards-compat shims

---

## Cross-Cutting Naming & Style (both)

**Comments / Docs:**
- Android: KDoc links `[MusicProvider]`/`[Throwable]` in `app/src/main/java/com/clibeats/data/gateway/GatewayMusicProvider.kt`
- Gateway: `/** … */` block comments before functions; inline comments explain rationale (`// Backwards-compatible failure knobs`), not what code does
- ADR references inline: `// (ADR-016)`, `// (P4)`

**Imports (gateway TS):** grouped — external Fastify/DTO imports first (multiple `import { … } from 'fastify'` etc.), then internal relative; type-only imports use `import type { FastifyInstance }`.

**Empty/graceful behavior:** producers won't throw for cache misses (`null`) or empty results — the cache layer is fail-open (`RedisCacheBase`, covered by `tests/unit/redis-cache-resilience.test.ts`).

---

## Where to enforce when writing code

- Kotlin: run `./gradlew ktlintCheck detekt lintDebug`; new ViewModels → Hilt `@HiltViewModel`; new repo → interface + `Impl` + `@Binds` in the correct `di/*Module.kt`; new screen → `*UiState` sealed interface + `*Screen.kt` in `presentation/<feature>/`
- TypeScript: run `npm run check` + `npm test`; new provider → `gateway/src/providers/<name>/XProviderAdapter.ts` implementing `types/adapter.ts`, registered in `gateway/src/providers/registerProviders.ts`, enabled/disabled via `gateway/config/gateway.yaml`; new route → add to `gateway/src/app.ts` with a schema from `gateway/src/schemas.ts` and regenerate/validate with `npm run openapi:generate` / `openapi:validate`.

---

*Convention analysis: 2026-08-08*