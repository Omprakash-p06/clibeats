# Testing Patterns

**Analysis Date:** 2026-08-08

Two test regimes exist in this repo:

- **Android app** (`app/`) — Gradle/JUnit 4 unit tests + instrumented Room tests + Paparazzi golden tests (109 passing as of Phase 11)
- **Gateway** (`gateway/`) — Vitest 3 with unit/integration/contract/property/architecture suites + autocannon load test (72 tests, 70%+ coverage enforced)

---

## Android Testing (`app/`)

### Test Framework

**Runner:** JUnit 4 (`junit:4.13.2`, version catalog `gradle/libs.versions.toml`)
**Assertions:** Google Truth (`com.google.common.truth.Truth.assertThat`) in most tests; `org.junit.Assert.assertEquals/assertNull` in Room/DAO and mapper tests (`app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt`, `app/src/test/java/com/clibeats/data/local/mapper/MapperTest.kt`)
**Coroutines:** `kotlinx-coroutines-test` 1.8.1 — `runTest`, `StandardTestDispatcher`, `UnconfinedTestDispatcher`, `Dispatchers.setMain/resetMain`, `advanceTimeBy`
**Mocks:** Mockito (`org.mockito:mockito-core` 5.12.0) + `mockito-kotlin` 5.4.0 (`mock()`, `whenever`, `verify`, `any`)
**Other test deps in `app/build.gradle.kts`:** `room-testing`, `okhttp-mockwebserver`, `media3-test-utils`, `compose-ui-test-junit4`
**Instrumented:** `androidx.test.ext:junit` + Espresso for DAO androidTests

**Run commands:**
```bash
./gradlew testDebugUnitTest          # All JVM unit tests
./gradlew connectedAndroidTest       # Instrumented Room DAO tests (needs emulator/device)
./gradlew testReleaseUnitTest        # Release variant unit tests
./gradlew ktlintCheck detekt         # Static analysis (quality gate)
./gradlew assembleDebug              # Compile gate
```

### Test File Organization

**Location:** mirror of `src/main` under `app/src/test/java/` for unit; `app/src/androidTest/java/` for instrumented.

```
app/src/test/java/com/clibeats/
├── data/
│   ├── cache/CacheManagerTest.kt
│   ├── download/TrackDownloadManagerTest.kt
│   ├── gateway/GatewayMusicProviderTest.kt
│   ├── gateway/mapper/{GatewayErrorMapperTest,GatewayMapperTest}.kt
│   ├── local/mapper/MapperTest.kt
│   ├── network/NetworkMonitorTest.kt
│   ├── preferences/AppPreferencesTest.kt
│   └── repository/{SongRepositoryImplTest,PlaylistRepositoryImplTest}.kt
├── domain/{model/TrackTest.kt, playback/QueueManagerTest.kt}
├── presentation/
│   ├── component/{PlayerBarTest,SongTableRowTest}.kt
│   ├── library/LibraryViewModelTest.kt
│   ├── player/PlayerViewModelTest.kt
│   ├── playlist/PlaylistViewModelTest.kt
│   ├── queue/QueueViewModelTest.kt
│   ├── search/{SearchScreenKtTest,SearchViewModelTest}.kt
│   └── settings/SettingsViewModelTest.kt
├── theme/{CliBeatsColorsTest,CliBeatsTypographyTest,CliBeatsThemeScreenshotTest,PlayerBarScreenshotTest,SongTableRowScreenshotTest}.kt
├── playback/{PlayerAdapterTest,PlayerAdapterQueueTest}.kt
├── integration/PlaybackIntegrationTest.kt
├── telemetry/{TimberCrashReporterTest,TimberTelemetryTrackerTest}.kt
└── license/LicenseComplianceTest.kt
app/src/androidTest/java/com/clibeats/data/local/dao/
├── SongDaoTest.kt  HistoryDaoTest.kt  PlaylistDaoTest.kt  CacheIndexDaoTest.kt
```

**Naming:** `{ClassUnderTest}Test.kt` (e.g. `SearchViewModelTest.kt`); screenshot tests named `*ScreenshotTest.kt`; tests for a `SearchScreen.kt` file targeting top-level functions use `SearchScreenKtTest` convention (`app/src/test/java/com/clibeats/presentation/search/SearchScreenKtTest.kt`).

### Suite Structure

**ViewModel tests** (`app/src/test/java/com/clibeats/presentation/library/LibraryViewModelTest.kt`, `SearchViewModelTest.kt`, `SettingsViewModelTest.kt`):

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(testDispatcher); musicProvider = mock(); viewModel = SearchViewModel(musicProvider) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `search returns Success state on successful provider call`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.searchResults.collect() }
            whenever(musicProvider.search(any(), any())).thenReturn(ProviderResult.Success(listOf(fakeTrack)))
            viewModel.onQueryChange("Wonderwall")
            advanceTimeBy(400L)              // advance past 300ms debounce
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.searchResults.value).isInstanceOf(SearchUiState.Success::class.java)
        }
}
```

Key conventions:
- `@Before`/`@After` for `Dispatchers.setMain`/`resetMain` (JUnit 4 lifecycle, no `@Rule` for coroutines)
- Flow collection via `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))` so `stateIn`/`debounce` flows emit under virtual time
- `whenever(...).thenReturn(...)` stubbing, then `advanceTimeBy` + `scheduler.runCurrent()` (debounce = 300ms)

**Repository DAO tests use mocked DAOs with `flowOf(...)`:**

```kotlin
whenever(songDao.getAllAsFlow()).thenReturn(flowOf(listOf(entity)))
val tracks = repository.getAllTracksAsFlow().first()
```
(`app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt`)

### Room DAO Tests (androidTest)

In-memory DB per test: `Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CliBeatsDatabase::class.java).allowMainThreadQueries().build()` — see `app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt`. Pattern: `@RunWith(AndroidJUnit4::class)`, `@Before setup()` builds fresh DB, `@After tearDown() { db.close() }`, each test wrapped in `runTest`. A private `testSong()` factory centralizes fixture data. 4 DAOs covered: SongDao, PlaylistDao, HistoryDao, CacheIndexDao.

### Screenshot Tests (Paparazzi)

Paparazzi 1.3.4, `DeviceConfig.PIXEL_5`, snapshots recorded to `app/src/test/snapshots/`:

```kotlin
class PlayerBarScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun playerBar_idleState() {
        paparazzi.snapshot { CliBeatsTheme { PlayerBar(trackTitle = "Not playing", artist = "", isPlaying = false, progress = 0f) } }
    }
}
```
(`app/src/test/java/com/clibeats/theme/PlayerBarScreenshotTest.kt`; also `SongTableRowScreenshotTest.kt`, `CliBeatsThemeScreenshotTest.kt`)

Golden images committed under `app/src/test/snapshots/images/` (e.g. `com.clibeats.theme_PlayerBarScreenshotTest_playerBar_idleState.png`); updated via `RECORD_PAPARAZZI`/gradle `recordPaparazziDebug`.

### Network / Provider Tests (MockWebServer)

`app/src/test/java/com/clibeats/data/gateway/GatewayMusicProviderTest.kt`: starts `MockWebServer`, builds a Retrofit client against `server.url("/")`, and enqueues canned JSON bodies via `MockResponse().setBody(...)`. Covers happy path, empty result, RATE_LIMITED 429 mapping, and blank stream URL.

### Async & Time Testing Patterns

- `runTest` everywhere; `StandardTestDispatcher` when asserting intermediate states; `advanceTimeBy` to cross debounce windows
- `PlayerAdapterTest.kt` and `PlayerAdapterQueueTest.kt` mock `ExoPlayer` (`whenever(exoPlayer.currentPosition).thenReturn(0L)`) — Media3 test utils not needed for simple delegations
- `AppPreferencesTest.kt` builds a real DataStore via `PreferenceDataStoreFactory.create(scope = CoroutineScope(UnconfinedTestDispatcher() + Job())) { File.createTempFile(...) }`, deletes on teardown, and mocks `SharedPreferences`/`Editor` for the secure-prefs half (`whenever(editor.putString(...)).thenReturn(editor)` chain)

### Naming / Style Inside Tests

- Backticked sentences: `fun \`clearQuery resets query to empty string\`()`
- Assertions: Truth (`assertThat(x).isEqualTo`, `.isInstanceOf`, `.hasSize`) in ViewModel/provider/repo tests; JUnit `assertEquals` in DAO/mapper tests
- Suppressions: tests that cross layer packages carry `@file:Suppress("ForbiddenImport")` (e.g. `GatewayMusicProviderTest.kt`)
- **Test counts per phase (from `app/tests` and phase summaries)** — currently **109 passing**:
  - Phase 0–2: theme token + Paparazzi baseline (`CliBeatsColorsTest` 10, `CliBeatsTypographyTest` 7)
  - Phase 3: DAO in-memory (androidTest) + repo mappers, 40 unit tests
  - Phase 4/5: PlayerAdapter, PlaybackIntegration, provider/search (`GatewayMusicProviderTest`, `GatewayMapperTest`, `SearchViewModelTest`), `TrackModelTest`
  - Phase 6: Queue/Library/Playlist ViewModel tests (9 new → 93)
  - Phase 7: Cache, Download, NetworkMonitor (3 → 96)
  - Phase 8: SettingsViewModel (4 → 100)
  - Phase 9: repository + component + PlaybackIntegration (106)
  - Phase 10: telemetry (108), Phase 11: LicenseCompliance (109)

---

## Gateway Testing (`gateway/`)

### Framework

**Runner:** Vitest (`vitest` 3.0.4) with `@vitest/coverage-v8`
**Config:** `gateway/vitest.config.ts` — `environment: 'node'`, includes `tests/**/*.test.ts`
**Run:**
```bash
npm run check            # tsc --noEmit
npm test                 # vitest run (all suites)
npm run test:watch
npm run test:coverage    # with coverage gates (70%)
npm run test:load        # autocannon load test (ts-node tests/load/load-test.ts)
npm run openapi:generate / openapi:validate
```

### Coverage (enforced)

`gateway/vitest.config.ts`:

```ts
thresholds: { statements: 70, branches: 70, functions: 70, lines: 70 }
```

Recent report (`gateway/coverage/coverage-summary.json`): lines/statements 80.3%, functions 78.2%, branches 76.2% — above gate. Weak files: `src/config/config.ts` (33%), `src/types/domain.ts` (0, type-only), `src/providers/youtube/YouTubeProviderAdapter.ts` (60%) — adapter tests exist (`tests/unit/youtube-adapter.test.ts`) but many `branch` paths unmeasured.

### Suite Layout (6 directories)

```
gateway/tests/
├── unit/          # core.test.ts, mock-provider.test.ts, redis-cache-resilience.test.ts, redis-health.test.ts, youtube-adapter.test.ts
├── integration/   # api.test.ts, failover.test.ts, health.test.ts, metrics.test.ts
├── contract/      # openapi.test.ts
├── property/      # search-property.test.ts (fast-check)
├── architecture/  # layers.test.ts (static analysis)
└── load/          # load-test.ts (autocannon)
```

### Unit Tests

**Mock provider failure matrix** (`tests/unit/mock-provider.test.ts`) — iterates every `MockProviderState`:

```ts
it('OFFLINE throws NetworkError (no fall-through to PlaybackError)', async () => {
  const mock = new MockProviderAdapter('mock', 42, 100);
  mock.state = 'OFFLINE';
  await expect(mock.search('cyber', ctx)).rejects.toThrow(NetworkError);
  expect(await mock.healthCheck()).toMatchObject({ status: 'UNHEALTHY', score: 0 });
});
```

- `core.test.ts`: registry/selection/circuit-breaker logic with a registered `MockProviderAdapter`
- `redis-cache-resilience.test.ts` — uses `ioredis-mock` (`new RedisMock()`), validates key namespacing (`clibeats:search:hello`), fail-open behavior (Redis read throws → `get` returns `null`; write throws → resolves), TTL on sessions
- `redis-health.test.ts` — `mock` redis via `{ ping: vi.fn() }`, fake timers (`vi.useFakeTimers`/`advanceTimersByTimeAsync`) for the timeout path
- `youtube-adapter.test.ts` — mocks `youtubei.js` module (`vi.mock('youtubei.js', () => ({ ... Innertube: { create: mockCreate } }))`), tests search mapping, stream format selection, `NotFoundError` when no audio format, rate-limit wrap, `healthCheck` UP/DOWN, and MUSIC vs IOS client selection

### Integration Tests (fastify.inject)

`tests/integration/**` — boot full app via `buildApp()` (which swaps in `ioredis-mock` when `NODE_ENV === 'test'`), `await app.ready()`, drive requests with `app.inject({ method, url, payload, headers })`:

- `api.test.ts` — endpoint coverage incl. trace-ID header propagation (`x-trace-id` echo)
- `failover.test.ts` — sets `primaryMock.shouldSimulateError`, verifies secondary provider serves the request; circuit-breaker opens after 3 failures (`cb.getState() === 'OPEN'`)
- `health.test.ts` — injects a fake failing Redis into `buildApp({}, failingRedis)`, asserts `redis: 'DOWN'` and gateway `DEGRADED`, plus the "health does not lie" invariant
- `metrics.test.ts` — asserts Prometheus metric names present in `/metrics` payload and that cache hit/miss counters move

Test env: `buildApp()` uses `process.env.NODE_ENV === 'test'` → `RedisMock` (in `src/app.ts`).

### Contract Tests (OpenAPI)

`tests/contract/openapi.test.ts` — `app.swagger({ yaml: false })` returns live spec:
- openapi `3.x`, deterministic serialization (generation is idempotent)
- exact path list contract (`/api/v1/{bootstrap,search,album/{id},artist/{id},playlist/{id},stream,providers}`, `/health`, `/metrics`, `/version`)
- every operation documents `tags`, `description`, `responses` incl. 200/default, and 400-validation via `fastify.inject` (missing `trackId` → 400)

### Property Tests (fast-check)

`tests/property/search-property.test.ts`:

```ts
fc.assert(fc.asyncProperty(fc.fullUnicodeString({ maxLength: 100 }), async (query) => {
  const res = await app.inject({ method: 'GET', url: `/api/v1/search?q=${encodeURIComponent(query)}` });
  expect(res.statusCode).toBe(200);
  // schema invariant: tracks[] all have id/providerId/title/artist/durationSeconds as strings/number
}), { numRuns: 100 });
```

### Architecture Tests (static layering)

`tests/architecture/layers.test.ts` — walk `src/`, parse `from '...'` imports, assert:
- `core/` never imports `providers/`
- `providers/**` (except register) never import `core|config|app`
- `types/` is a leaf; `config/config.ts` depends only on `external` packages
- `registerProviders.ts` composes `core` + adapter (dependency-injection wiring is real)

### Load Test (autocannon)

`tests/load/load-test.ts` — sets `NODE_ENV=test`, `buildApp()`, listens on ephemeral port, runs `autocannon({ connections: 100, duration: 10, pipelining: 1 })` against `/api/v1/search?q=cyber`, logs requests/sec + p99, exits non-zero if any non-2xx. Invoked by CI via `npm run test:load`.

### Mocking (gateway)

- Module mock: `vi.mock('youtubei.js', ...)` with `mockCreate`/`mockSearch`/`mockGetBasicInfo` hoisted (`tests/unit/youtube-adapter.test.ts`)
- Redis mock: `ioredis-mock` in `tests/unit/redis-cache-resilience.test.ts` and implicit in `buildApp` (NODE_ENV test)
- Inline fake objects for `ProviderAdapter`/Redis (`{ ping: vi.fn() }`, `{ get/set/ping: async () => { throw ... } }`) in health/cache resilience tests
- `MockProviderAdapter` is itself the canonical fake provider, reused across unit/integration/failover suites

### Fixtures / Factories

- Shared typed `const ctx: ProviderContext = { country: 'US', language: 'en', ... }` at top of each consumer-file (`mock-provider.test.ts`, `youtube-adapter.test.ts`)
- Mock provider datasets deterministic via seeded PRNG (`seed=42`) in `MockProviderAdapter`

**Fixtures are not centralized** — each gateway test file builds its own `ProviderContext` and fake payloads inline.

### Test Types Summary

| Type | Location | Tool |
|------|----------|------|
| Unit (JVM) | `app/src/test/` | JUnit4, Mockito, kotlinx-coroutines-test, Paparazzi |
| Unit (node) | `gateway/tests/unit/` | vitest, `vi.mock`, ioredis-mock |
| Instrumented Room | `app/src/androidTest/` | AndroidJUnit4, Room in-memory |
| Integration | `gateway/tests/integration/` | vitest + `app.inject` |
| Contract | `gateway/tests/contract/` | vitest + `app.swagger` |
| Property | `gateway/tests/property/` | fast-check |
| Architecture | `gateway/tests/architecture/` | vitest + fs walk |
| Load | `gateway/tests/load/` | autocannon |

### CI Hooks

`.github/workflows/ci.yml`:
- `quality-and-test` (Android): JDK 17 → `ktlintCheck` → `detekt` → `lintDebug` → `assembleDebug` → `testDebugUnitTest` → upload `app/build/reports/`
- `gateway-quality-and-test`: Node 20 → `npm ci` → `npm run check` → `npm test` → `npm run test:coverage` (fails if <70%) → `openapi:validate` → `test:load` (fails on non-2xx) → `docker build`

---

*Testing analysis: 2026-08-08*