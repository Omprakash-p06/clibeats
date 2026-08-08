# Coding Conventions

**Analysis Date:** 2026-08-09

## Naming Patterns

**Files:**
- Kotlin: `PascalCase.kt`, one primary class/object per file; `*Test.kt` for unit tests, `*ScreenshotTest.kt` for Paparazzi; DAOs as `*Dao.kt`, entities as `*Entity.kt`, Hilt modules as `*Module.kt`
- TypeScript: PascalCase for class/module files (`ProviderSelectionEngine.ts`, `CacheManager.ts`), camelCase for leaf modules (`config.ts`, `logger.ts`, `metrics.ts`); `*.test.ts` for Vitest suites
- Android resources: `snake_case.xml` (`network_security_config.xml`, `data_extraction_rules.xml`)

**Functions:**
- Kotlin/TS: `camelCase` (e.g., `executeWithFailover`, `getToken`, `forceRefresh`, `resolveCdnStreamUrl`)
- Async: no special prefix; `suspend` in Kotlin, `async` functions in TS
- Handlers: route handlers inline in `app.ts`; `healthCheck`, `onGetSession` style names for lifecycle methods

**Variables:**
- `camelCase` for locals; `UPPER_SNAKE_CASE` for constants (`OPERATION_TIMEOUT_MS`, `DEFAULT_REFRESH_BUFFER_SECONDS`, `MOCK_PROVIDER_STATES`)
- Private state with leading underscore for MutableStateFlow backing fields (`_playbackState`, `_queueFlow`) exposed via `asStateFlow()`

**Types:**
- Interfaces/types: `PascalCase`, no `I` prefix (`ProviderAdapter`, `GatewayConfig`, `Track`, `ProviderToken`, `StreamResult`)
- Enums/type unions: `PascalCase` type name, `UPPER_SNAKE_CASE` values (`MockProviderState`, `'HEALTHY' | 'OFFLINE' | ...`; `CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN'`)
- Kotlin sealed classes for results: `ProviderResult.Success/Error`
- DTOs: `*Dto`/`Gateway*` classes in `data/gateway/dto/`; mappers as extension functions (`toDomainTracks()`)

## Code Style

**Formatting:**
- Kotlin: ktlint (`org.jlleitschuh.gradle.ktlint` 12.1.1) enforced in CI (`./gradlew ktlintCheck`); 4-space indent; trailing commas in multiline calls (per ktlint defaults)
- TypeScript: `tsc --strict` + formatting consistent with Prettier-style defaults (2-space indent, semicolons required, single quotes, 100-col); no ESLint/Prettier config file present — enforcement is via type checking and review

**Linting:**
- Detekt 1.23.6 (`config/detekt/detekt.yml`): `maxIssues: 0` (fail on any issue), rules tuned — `TooManyFunctions` (15/15/10), `LongMethod` (60), `LargeClass` (300), `MagicNumber` (with ignore list -1/0/1/2/100/1000, hash-code fn, property declarations), `UnusedPrivateMember`
- `ForbiddenImport` (Detekt) enforces Clean Architecture: `com.clibeats.data.*` must NOT be imported by presentation layer. Legitimate intra-layer imports use targeted `@file:Suppress("ForbiddenImport")` with a comment explaining why (e.g., `CliBeatsDatabase.kt`, `PlaybackService.kt`, `PlayerAdapter.kt`)
- Android Lint: `abortOnError = true`, `checkDependencies = true`, warnings not errors
- Gateway: no linter config; `npm run check` (`tsc --noEmit`, strict mode) is the gate; `strict: true` in `tsconfig.json`

## Import Organization

**Kotlin:**
1. `java.*` / `android.*` (platform)
2. `androidx.*`
3. Third-party (kotlinx, dagger, retrofit, etc.)
4. `com.clibeats.*` internal, then relative — ktlint enforces ordering; alphabetical within groups, blank lines between groups

**TypeScript:**
1. Node built-ins (`stream`, `events`)
2. External packages (`fastify`, `ioredis`, `pino`, `youtubei.js`)
3. Relative imports (`./config/config`, `../core/...`) — alphabetical by path
- Type-only imports use `import type { ... }` (e.g., `import type { FastifyInstance } from 'fastify'`, `import type { ProviderAdapter } from '../../types/adapter'`)

**Path Aliases:**
- None — relative imports throughout both codebases

## Error Handling

**Patterns:**
- Gateway: throw typed `ProviderError` subclasses from adapters; global Fastify error handler in `app.ts` maps to canonical `{ error: { code, message, providerId, retryAfterSeconds, traceId } }` (ADR-016). Never leak raw stack traces to clients.
- Android: return `ProviderResult.Success/Error` from data layer via `runCatching { }`; map gateway errors to user-facing messages with `GatewayErrorMapper`. ViewModels surface errors in `*UiState` rather than throwing.
- Failover: `ProviderSelectionEngine.executeWithFailover` records failures on circuit breakers, rethrows last `ProviderError` or wraps in `InternalError`

**Error Types:**
- Canonical codes: `AUTHENTICATION_FAILED`, `RATE_LIMITED`, `GEO_BLOCKED`, `NOT_FOUND`, `UNSUPPORTED`, `PLAYBACK_ERROR`, `NETWORK_ERROR`, `TIMEOUT_ERROR`, `INTERNAL_ERROR` (+ `INVALID_REQUEST` for schema validation) in `gateway/src/types/error.ts` and `schemas.ts`
- Classification: unknown errors matched by regex (`rate limit|too many|quota` → RateLimited, `geo|region|country` → GeoBlocked, `network|fetch|socket|econn` → NetworkError) in `YouTubeProviderAdapter.errorCode()`
- Timeouts: `withTimeout` wrapper races operations against a `TimeoutError` after 30s (`.unref()` timers)
- Logging: log with traceId context before rethrowing; gateway logs `unhandled error` with stack server-side only

## Logging

**Framework:**
- Gateway: pino JSON (`gateway/src/core/logging/logger.ts`), level from `LOG_LEVEL` (default info), ISO timestamps, `service: clibeats-gateway`
- Android: `android.util.Log` with tag constants (`PlayerAdapterDiagnostics`, `CLIBeatsApp`); `StructuredLogger` abstraction in `core/logging/` implemented by `TimberTelemetryTracker`/`TimberCrashReporter` (sanitized, ADR-010)

**Patterns:**
- Structured object logging: `logger.info({ traceId, method, url, statusCode }, 'request completed')`
- Trace IDs: generate/accept `x-trace-id` in `onRequest` hook, log on request+response, echo back in header and errors
- EventBus-driven metrics/logs: events (`REQUEST_RECEIVED`, `CACHE_CHECKED`, `PROVIDER_FAILED`, `CIRCUIT_TRIPPED`) are logged automatically by a wildcard listener — do not manually log what the EventBus already covers
- Log at service boundaries and lifecycle transitions; avoid logging sensitive tokens/PII; release logcat sanitized (release notes mention logcat sanitization)

## Comments

**When to Comment:**
- Explain *why* and document non-obvious decisions: hardcoded IP defaults + NXDOMAIN fail-fast rationale (`app/build.gradle.kts`), PO-token binding/visitor-data coupling (`YouTubeProviderAdapter.getStreamingSession`), lazy ioredis-mock require for production `--omit=dev` (`app.ts`)
- Reference ADRs and recovery sessions in comments (`// ADR-013`, `// RECOVERY-10`)
- Avoid obvious comments; keep code self-documenting otherwise

**KDoc/TSDoc:**
- Public gateway classes/functions use JSDoc `/** ... */` with `@param`/`@returns` (e.g., `ProviderTokenService.getToken`, `resolveStream`); `@internal` marker used for diagnostics-only members (`tokenService`)
- Android public APIs and DTOs documented sparingly with KDoc; `@file:Suppress` always annotated with a justification comment

**TODO Comments:**
- Rare; deferred items tracked in `.planning/` docs (e.g., D-01/D-02 in STATE.md) rather than inline TODOs. Code search finds no inline TODO/FIXME/HACK markers as of this analysis.

## Function Design

**Size:**
- Detekt `LongMethod` threshold: 60 lines; `TooManyFunctions` 15/class — refactor beyond these
- Route handlers kept thin; logic delegated to engine/adapters/caches

**Parameters:**
- Prefer small param lists; option objects for config (e.g., `YouTubeProviderOptions`, `ProviderRegistrationOverride`)
- Destructure objects in Kotlin parameter lists (`constructor(private val player: ExoPlayer, ...)` style injection)

**Return Values:**
- Explicit returns; early guard clauses (`if (!query.trim()) return []`)
- Kotlin: `runCatching` + `ProviderResult`; `withContext(Dispatchers.IO)` for network

## Module Design

**Kotlin (Clean Architecture):**
- `presentation` → `domain` ← `data` dependency rule (domain depends on nothing); enforced by Detekt `ForbiddenImport`
- `data` implements `domain` interfaces (repository contracts in `domain/repository/`, impls in `data/repository/`)
- Hilt: `@Singleton`/`@InstallIn(SingletonComponent::class)` modules per concern (`DatabaseModule`, `NetworkModule`, `PlaybackModule`); constructor injection with `@Inject`

**TypeScript (gateway):**
- Separation: `types/` (pure contracts, no logic) → `core/` (framework-agnostic services) → `providers/` (adapters) → `app.ts` (composition root)
- Fastify instance decoration (`app.decorate('registry', ...)`) used as DI; no inversion-of-control container
- Singleton `globalEventBus` for cross-cutting concerns
- Cache segregation: one class per namespace extending `RedisCacheBase` (`SearchCache`, `AlbumCache`, ...)

## Testing Conventions

**General:**
- Arrange/Act/Assert structure in tests; `describe` (Vitest) / JUnit `@Test` method groups
- Unit tests hermetic: gateway tests run under `NODE_ENV=test` (ioredis-mock, no PO-token minting, no network)
- Gate: CI runs Android (ktlint, detekt, lint, build, unit tests) and gateway (tsc, vitest, openapi validate, docker build) quality gates; see TESTING.md

---

*Conventions analysis: 2026-08-09*
*Update when conventions change*
