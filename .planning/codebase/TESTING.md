# Testing Patterns

**Analysis Date:** 2026-08-09

## Test Framework

**Runner:**
- Gateway: Vitest 3.0.4 (`gateway/vitest.config.ts`, node environment, include `tests/**/*.test.ts`)
- Android: JUnit 4.13.2 via Gradle (`testDebugUnitTest`), plus instrumented tests (`connectedDebugAndroidTest`, Room `androidTest` suites)
- Paparazzi 1.3.4 for Compose screenshot tests (run as JVM tests)

**Assertion Library:**
- Gateway: Vitest built-in `expect` (toBe, toEqual, toThrow, toMatchObject)
- Android: JUnit 4 assertions + Mockito `verify`/`when` (mockito-kotlin 5.4.0, mockito-core 5.12.0)
- Property testing: `fast-check` 3.23.2 in `gateway/tests/property/search-property.test.ts`

**Run Commands:**
```bash
# Gateway
npm test                          # Run all Vitest suites (from gateway/)
npm run test:watch                # Watch mode
npm run test:coverage             # Coverage report (v8, 70% thresholds)
npm run check                     # tsc --noEmit type check
npm run test:load                 # autocannon load test
npm run openapi:validate          # Contract test: OpenAPI matches route schemas

# Android
./gradlew testDebugUnitTest       # JVM unit + Paparazzi screenshot tests
./gradlew connectedDebugAndroidTest  # Instrumented Room DAO tests (device/emulator)
./gradlew lintDebug / ktlintCheck / detekt   # Static analysis gates
```

## Test File Organization

**Location:**
- Gateway: dedicated `gateway/tests/` tree mirroring concerns: `unit/`, `integration/`, `contract/`, `property/`, `load/`, `architecture/`
- Android: JVM tests under `app/src/test/java/com/clibeats/` mirroring main package layout (e.g., `presentation/player/PlayerViewModelTest.kt`); instrumented tests under `app/src/androidTest/java/com/clibeats/`

**Naming:**
- Gateway: `module.test.ts` (e.g., `youtube-adapter.test.ts`, `provider-token-service.test.ts`)
- Android: `ClassUnderTestTest.kt` (e.g., `QueueManagerTest.kt`, `GatewayMapperTest.kt`); Paparazzi as `*ScreenshotTest.kt` (e.g., `CliBeatsThemeScreenshotTest.kt`, `PlayerBarScreenshotTest.kt`)

**Structure:**
```
gateway/tests/
  unit/            # adapter, engine, circuit breaker, token service, redis resilience
  integration/     # full app: api, failover, health, metrics (buildApp + ioredis-mock)
  contract/        # openapi.test.ts — generated spec matches route schemas
  property/        # search-property.test.ts — fast-check invariants
  load/            # load-test.ts — autocannon
  architecture/    # layers.test.ts — import-boundary enforcement
app/src/test/java/com/clibeats/
  data/            # cache, download, gateway, local, network, preferences, repository
  domain/          # model, playback (QueueManager)
  presentation/    # viewmodels, components, screens
  playback/        # PlayerAdapter tests
  integration/     # PlaybackIntegrationTest
  theme/           # colors/typography + screenshot tests
  di/, telemetry/, license/
app/src/androidTest/java/com/clibeats/data/local/dao/  # Room DAO instrumented tests
```

## Test Structure

**Suite Organization (Vitest):**
```typescript
describe('YouTubeProviderAdapter', () => {
  describe('search', () => {
    it('returns parsed tracks from raw sections', async () => {
      // arrange
      // act
      // assert
    });
  });
});
```

**Suite Organization (Android):**
```kotlin
class PlayerViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    @Test
    fun `playTrack loads and plays the selected track`() {
        // arrange
        // act
        // assert
    }
}
```

**Patterns:**
- Setup: `beforeEach`/`@Before` for shared state; Android uses `MainDispatcherRule` (kotlinx-coroutines-test) to swap `Dispatchers.Main` with `StandardTestDispatcher`
- Teardown: restore mocks (`afterEach`), release players/`TestScope` where relevant
- Gateway integration tests build the full app via `buildApp()` (from `gateway/src/app.ts`) with ioredis-mock and `NODE_ENV=test` so no real network/Redis is touched
- Hermeticity is a hard rule: gateway provider tests never mint PO tokens (disabled under `NODE_ENV=test` in `registerProviders.ts`)

## Mocking

**Framework:**
- Gateway: Vitest `vi.fn()`/`vi.mocked()`; `ioredis-mock` 8.9.0 for Redis (lazy-required in `app.ts` when `NODE_ENV === 'test'`)
- Android: Mockito (mockito-kotlin) for ViewModels/repositories; MockWebServer (OkHttp) for HTTP (`GatewayMusicProviderTest`, `NetworkModuleTest`); Room `inMemoryDatabaseBuilder` for DAO tests

**Patterns:**
```typescript
// Gateway — stub provider adapter in failover tests
const mockAdapter = { search: vi.fn(), stream: vi.fn(), ... } as ProviderAdapter;
engine.executeWithFailover('search', ctx, (a) => a.search('x', ctx));
```
```kotlin
// Android — mock repository
val repo = mock<SongRepository>()
whenever(repo.search("query")).thenReturn(flowOf(listOf(track)))
```

**What to Mock:**
- External boundaries: YouTube/youtubei.js, Redis, HTTP (MockWebServer), ExoPlayer state, DAOs
- PO-token minting is injected (`ProviderTokenService` takes a `mint` function) so unit tests pass a stub mint

**What NOT to Mock:**
- Pure mappers/parsers where practical — `GatewayMapperTest`, `MapperTest`, `GatewayErrorMapperTest` test real logic; the YouTube adapter parses real-shaped fixtures (RECOVERY-06 evidence format) rather than mock responses where possible

## Fixtures and Factories

**Gateway:**
- `MockProviderAdapter` doubles as a deterministic fixture provider (seeded PRNG dataset: 100 tracks, 10 albums, 5 artists, 5 playlists) used by property tests and failover tests
- Mock provider failure states (`HEALTHY`, `SLOW`, `OFFLINE`, `MALFORMED`, `RATE_LIMITED`, `AUTHENTICATION_FAILED`, `GEO_BLOCKED`, `INTERNAL_ERROR`) drive error-path tests (`tests/unit/mock-provider.test.ts`)

**Android:**
- DAO instrumented tests build in-memory Room DBs and insert entities directly (`CacheIndexDaoTest`, `HistoryDaoTest`, `PlaylistDaoTest`, `SongDaoTest`)
- Screenshot tests render Compose components via Paparazzi with fixed theme fixtures

## Coverage & Quality Gates

**Coverage:**
- Gateway: `@vitest/coverage-v8`, thresholds 70% statements/branches/functions/lines (`vitest.config.ts`); excludes `server.ts` and declaration files
- Android: coverage not thresholded in CI, but a comprehensive unit suite exists — 125 tests passing at v1.0.0 (RELEASE_NOTES)

**CI Quality Gates (`.github/workflows/ci.yml`):**
- Android job: `ktlintCheck` → `detekt` → `lintDebug` → `assembleDebug` → `testDebugUnitTest` (artifacts uploaded)
- Gateway job: `npm ci` → `tsc --noEmit` → `vitest run` → `openapi:validate` → Docker build
- Concurrency group cancels stale runs per ref

**Known Gaps:**
- No automated instrumented UI/E2E run in CI — `connectedDebugAndroidTest` (Room DAO suites) and Maestro UI flows are manual (`WINDOWS.md`, TECHNIQUE_DEBT DEBT-04); emulator validation done during RECOVERY-06 with Maestro scripts + screenshots in `.planning/debug/`
- Android UI test deps exist (`compose-ui-test-junit4`, espresso, androidx.test.ext:junit) and `PlaybackIntegrationTest`/`SearchScreenKtTest` exist, but full instrumentation CI is not wired

## Test Data & Secrets

- No real YouTube API keys used in tests; gateway tests run against the mock provider or real youtubei.js only in manual/load contexts (load test uses mock to avoid rate limits)
- Test env avoids touching EncryptedSharedPreferences; `AppPreferencesTest` uses fake preference scopes (DataStore test instances)
- PO-token minting never runs in unit tests (network + jsdom side effects); tested via injected stub mint in `provider-token-service.test.ts`

---

*Testing analysis: 2026-08-09*
*Update when testing practices change*
