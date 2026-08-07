---
title: CLIBeats Testing Patterns
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

# Testing Patterns

**Analysis Date:** 2026-08-07

## Test Framework

**Runner:** JUnit 4 (`junit:junit:4.13.2`, `gradle/libs.versions.toml`).

**Assertions:** Two styles in use:
- Google Truth (`com.google.common.truth.Truth.assertThat`) — majority of unit tests (`SearchViewModelTest.kt`, `YouTubeMusicProviderTest.kt`, `CacheManagerTest.kt`). **Not declared in the version catalog** — it resolves transitively via `androidx.room:room-testing`.
- JUnit4 `assertEquals` / `assertNull` / `assertNotNull` — DAO tests, mapper tests, theme token tests (`app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt`, `app/src/test/java/com/clibeats/data/local/mapper/MapperTest.kt`, `app/src/test/java/com/clibeats/theme/CliBeatsColorsTest.kt`).

**Mocking:** Mockito Kotlin `5.4.0` + Mockito Core `5.12.0` (`mock`, `whenever`, `verify`, `argumentCaptor`, `any`, `eq`, `never`).

**Async:** `kotlinx-coroutines-test:1.8.1` (`runTest`, `StandardTestDispatcher`, `UnconfinedTestDispatcher`, `Dispatchers.setMain/resetMain`, `advanceTimeBy`, `advanceUntilIdle`).

**Screenshot baselines:** Paparazzi `1.3.4` (`app.cash.paparazzi`) — runs as JVM unit tests, no emulator needed.

**Network:** `okhttp3:mockwebserver` for HTTP interceptor tests.

**Test deps wiring:** `app/build.gradle.kts:111-123` — unit deps under `testImplementation`, instrumented under `androidTestImplementation`, `debugImplementation(libs.compose.ui.test.manifest)`.

## Run Commands

```bash
./gradlew testDebugUnitTest          # All JVM unit tests (incl. Paparazzi screenshots + detekt runtime checks)
./gradlew ktlintCheck                # Code style gate
./gradlew detekt                     # Static analysis gate
./gradlew lintDebug                  # Android Lint gate
./gradlew connectedDebugAndroidTest  # Instrumented DAO tests (needs device/emulator — NOT in CI)
```

CI (`app/../.github/workflows/ci.yml`, job `quality-and-test` on `ubuntu-latest`, JDK 17 temurin, 20-min timeout) runs, in order: `ktlintCheck --continue` → `detekt --continue` → `lintDebug --continue` → `assembleDebug` → `testDebugUnitTest`, then uploads `app/build/reports/{tests,detekt,lint-results-debug.html}`. `scripts/check-quality-gates.sh` mirrors the same four gates locally. **No coverage tool (jacoco/kover) configured — no coverage threshold enforced.**

## Test File Organization

**Mirror the production package** under `app/src/test/java/com/clibeats/...`:

```
app/src/test/java/com/clibeats/
├── presentation/  # ViewModel tests, component tests, screen tests (SearchViewModelTest.kt, PlayerBarTest.kt)
├── data/          # repository, provider, cache, download, preferences, network, local/mapper tests
├── playback/      # PlayerAdapterTest.kt, PlayerAdapterQueueTest.kt
├── telemetry/     # TimberTelemetryTrackerTest.kt, TimberCrashReporterTest.kt
├── theme/         # CliBeatsColorsTest.kt, CliBeatsTypographyTest.kt, *ScreenshotTest.kt
├── domain/model/  # TrackTest.kt
├── integration/   # PlaybackIntegrationTest.kt (stub)
└── license/       # LicenseComplianceTest.kt
```

Instrumented (device) Room DAO tests live in `app/src/androidTest/java/com/clibeats/data/local/dao/` (`SongDaoTest.kt`, `PlaylistDaoTest.kt`, `HistoryDaoTest.kt`, `CacheIndexDaoTest.kt`). Paparazzi baselines are committed at `app/src/test/snapshots/images/*.png` (+ `snapshots/videos/` dir); Room schemas exported to `app/schemas/` via `ksp.arg("room.schemaLocation", ...)`.

## Test Structure

**Class pattern** — `@Before setUp()` / `@After tearDown()`, `lateinit` subjects:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var musicProvider: MusicProvider
    private lateinit var viewModel: SearchViewModel

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        musicProvider = mock()
        viewModel = SearchViewModel(musicProvider)
    }
    @After fun tearDown() { Dispatchers.resetMain() }
}
```
(`app/src/test/java/com/clibeats/presentation/search/SearchViewModelTest.kt:24-40`)

**Naming:** backtick sentence-style for behavior tests (``fun `search returns Success state on successful provider call`()``); snake_case for Paparazzi screenshots (`playerBar_idleState`) and theme token tests (`background_is_0C0C0C`); camelCase for pure mapper/utility tests (`upsertAndGetById` in DAO tests).

**ViewModel flow test pattern** — collect the StateFlow in `backgroundScope` with `UnconfinedTestDispatcher`, mutate, then `runCurrent()` / `advanceTimeBy`:

```kotlin
@Test
fun `search returns Success state on successful provider call`() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.searchResults.collect() }
    whenever(musicProvider.search(any(), any())).thenReturn(ProviderResult.Success(listOf(fakeTrack)))
    viewModel.onQueryChange("Wonderwall")
    advanceTimeBy(400L)
    testDispatcher.scheduler.runCurrent()
    assertThat(viewModel.searchResults.value).isInstanceOf(SearchUiState.Success::class.java)
}
```
(`SearchViewModelTest.kt:56-84`). `runTest(testDispatcher)` + `advanceUntilIdle()` used in `SettingsViewModelTest.kt:51-57`.

## Mocking

**Framework:** Mockito Kotlin — `mock()` for interfaces/classes, `whenever(...).thenReturn(...)` stubbing, `verify` for delegation.

**Delegation verification:**
```kotlin
viewModel.onClear()
verify(playbackRepository).clearQueue()
```
(`app/src/test/java/com/clibeats/presentation/queue/QueueViewModelTest.kt:82-85`)

**Argument captors + never():**
```kotlin
val mediaItems = argumentCaptor<List<MediaItem>>()
adapter.setQueue(tracks)
verify(exoPlayer).setMediaItems(mediaItems.capture(), eq(0), eq(0L))
verify(exoPlayer, never()).seekToNextMediaItem()
```
(`app/src/test/java/com/clibeats/playback/PlayerAdapterQueueTest.kt:50-59,101-107`)

**Flow stubbing:** `whenever(dao.getAllAsFlow()).thenReturn(flowOf(listOf(entity)))` (`SongRepositoryImplTest.kt:42`) or a live `MutableStateFlow` for reactive ViewModels (`LibraryViewModelTest.kt:29,36`).

**What to mock:** DAOs, repositories, `MusicProvider`, `AppPreferences`, `ExoPlayer`, `CacheManager`, `OkHttpClient` — any heavy/instrumented collaborator. **What NOT to mock:** domain models (constructed directly as fixtures), mappers (tested for real), Room in-memory DB (real via `androidx.room:room-testing`).

## Fixtures and Factories

No shared fixture files — each test class defines private factory functions with defaulted params:

```kotlin
private fun testSong(id: String = "s1") =
    SongEntity(id = id, title = "Song $id", artist = "Artist", album = "Album",
        durationMs = 180_000L, artworkUrl = null, streamUrl = null, providerId = "local")
```
(`SongDaoTest.kt:38-48`; also `track()` in `PlayerAdapterQueueTest.kt:37-47`, `testTrack()` in `MapperTest.kt:15-25`, inline `Track(...)` in `SearchViewModelTest.kt:61-71`).

## Coverage

**Requirements:** None enforced — no Jacoco/Kover configured, CI runs `testDebugUnitTest` without coverage flags.

## Test Types

**Unit tests (JVM):** ViewModel behavior, repository delegation, provider mapping, mappers, player adapter, telemetry, preferences, cache/download managers, network interceptor. 

**Screenshot tests (Paparazzi, JVM):**
```kotlin
class PlayerBarScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun playerBar_idleState() {
        paparazzi.snapshot {
            CliBeatsTheme { PlayerBar(trackTitle = "Not playing", artist = "", isPlaying = false, progress = 0f) }
        }
    }
}
```
(`app/src/test/java/com/clibeats/theme/PlayerBarScreenshotTest.kt`; same for `SongTableRowScreenshotTest.kt`, `CliBeatsThemeScreenshotTest.kt`). Baselines committed under `app/src/test/snapshots/images/` (7 PNGs present); regeneration via `./gradlew recordPaparazziDebug`, verify with `verifyPaparazziDebug`.

**Instrumented DAO tests (androidTest):** `@RunWith(AndroidJUnit4::class)`, `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`, `@Before` build / `@After` close, `runTest` for suspend DAO calls (`SongDaoTest.kt`).

**Integration:** `PlaybackIntegrationTest.kt` is a stub — mocks `PlayerAdapter` and asserts on a `Track` (no real Media3 wiring yet).

**Structural component tests:** `PlayerBarTest.kt` / `SongTableRowTest.kt` assert only that the generated Compose class exists via `Class.forName("com.clibeats.presentation.component.PlayerBarKt")` — smoke tests, not behavior tests.

**Compliance test:** `LicenseComplianceTest.kt` asserts `docs/LICENSES.md` exists and mentions Apache-2.0, AndroidX, Media3, OkHttp.

## Common Patterns

**Async/flow testing:** always `runTest`; `StandardTestDispatcher` injected via `Dispatchers.setMain`; deterministic time via `advanceTimeBy(400L)` (debounce), `scheduler.runCurrent()`, or `advanceUntilIdle()`. Never `Thread.sleep` or real delays.

**Error-path testing:** stub `whenever(...).thenThrow(RuntimeException("network error"))` and assert `ProviderResult.Error` with message containment (`YouTubeMusicProviderTest.kt:39-47`).

**Real-storage testing:** `PreferenceDataStoreFactory.create(scope = CoroutineScope(UnconfinedTestDispatcher() + Job()))` over a temp file, deleted in `@After` (`AppPreferencesTest.kt:51-54`); `MockWebServer` start/`server.shutdown()` in `@Before`/`@After` (`InnerTubeHeaderInterceptorTest.kt:18-31`).

**Local-only facts to respect when adding tests:**
- Truth is only transitively available — declare `libs.google.truth` explicitly before relying on it in new tests.
- `@file:Suppress("ForbiddenImport")` is required on data-layer tests that import sibling `com.clibeats.data.*` packages (`SongRepositoryImplTest.kt:1`, `MapperTest.kt:2`).
- Instrumented tests are NOT part of the CI gate; put critical DAO behavior in JVM-testable code paths or accept emulator-only coverage.

---

*Testing analysis: 2026-08-07*
