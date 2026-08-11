# Testing Patterns

**Analysis Date:** 2026-08-12

## Test Framework

**Runner:**
- JUnit 4.13.2 via Gradle (`testDebugUnitTest`) — JVM unit tests
- Instrumented tests (`connectedDebugAndroidTest`) — Room DAO suites in `androidTest` (manual; not in CI)
- Paparazzi 1.3.4 — Compose screenshot tests (run as JVM tests, `verifyPaparazziDebug`)

**Assertion Library:**
- JUnit 4 assertions + Truth-style `assertThat` used across tests (com.google.common.truth transitive via test deps)
- Mockito `verify`/`when` via mockito-kotlin 5.4.0 (mockito-core 5.12.0)
- MockWebServer (OkHttp) for HTTP mocking; `Room.inMemoryDatabaseBuilder` for DAO tests; Media3 test-utils for ExoPlayer test doubles

**Run Commands:**
```bash
./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # Instrumented DAO tests (device/emulator)
./gradlew verifyPaparazziDebug         # Paparazzi screenshot tests
./gradlew ktlintCheck / detekt / lintDebug   # Static analysis gates
./gradlew assembleDebug / assembleRelease
```

## Test File Organization

**Location:**
- JVM tests under `app/src/test/java/com/clibeats/` mirroring main package layout; instrumented DAO tests under `app/src/androidTest/java/com/clibeats/data/local/dao/`
- 47 test source files total (43 JVM + 4 instrumented) covering: providers (Audius, Internet Archive, Jamendo, Local, YouTube + InnerTube interceptor + mappers), repositories (Song, Playlist, History, Playback, Library), playback (PlayerAdapter, PlayerAdapterQueue, integration), presentation ViewModels (Home, Search, Library, Player, Playlist, Queue, Settings), components (PlayerBar, SongTableRow, SearchScreen), theme (colors/typography/screenshots), data (cache, download, prefs, network, playlist codec, local mappers), telemetry, license, domain model

**Naming:**
- `ClassUnderTestTest.kt` (e.g., `QueueManagerTest.kt`, `GatewayMapperTest.kt` legacy naming remains in a few); Paparazzi as `*ScreenshotTest.kt` (`CliBeatsThemeScreenshotTest.kt`, `PlayerBarScreenshotTest.kt`, `SongTableRowScreenshotTest.kt`)
- Repository tests: `*RepositoryImplTest.kt`; provider tests: `*MusicProviderTest.kt`; DAO tests: `*DaoTest.kt`

## Test Structure

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
- Setup: `@Before` for shared mocks; `MainDispatcherRule` (kotlinx-coroutines-test) swaps `Dispatchers.Main` with a test dispatcher
- Teardown: `@After` restores state where relevant
- Repository tests mock DAOs and verify delegation (`whenever(songDao.upsert(any())).thenReturn(...)`, `verify(...)`)
- DAO instrumented tests build in-memory Room DBs (`Room.inMemoryDatabaseBuilder`) and insert entities directly
- Screenshot tests render Compose components via Paparazzi with fixed theme fixtures
- Hermeticity is a hard rule: no real network in unit tests (MockWebServer serves provider responses)

## Mocking

**Framework:**
- Mockito (mockito-kotlin) for DAOs/repositories/players; MockWebServer for HTTP (`AudiusMusicProviderTest`, `InternetArchiveMusicProviderTest`, `JamendoMusicProviderTest` use MockWebServer); `Room.inMemoryDatabaseBuilder` for DAO tests; Media3 `TestExoPlayerBuilder`/test utils for player tests

**Patterns:**
```kotlin
val repo = mock<SongRepository>()
whenever(repo.search("query")).thenReturn(flowOf(listOf(track)))
```

**What to Mock:**
- External boundaries: provider HTTP APIs (MockWebServer), DAOs, ExoPlayer, repositories
- PO-token generation and NewPipe extraction are not exercised in unit tests (YouTube provider tests cover search mapping and error paths with mocked `InnerTubeApi`)

**What NOT to Mock:**
- Pure mappers/parsers where practical — `TrackMapperTest`, `AudiusMapperTest`, `InternetArchiveMapperTest`, `MapperTest`, `CliBeatsFileCodecTest` test real logic
- Cache/preferences logic tested against real implementations with fakes (`AppPreferencesTest` uses DataStore test instances; `CacheManagerTest` uses temp dirs + in-memory DAO)

## Fixtures and Factories

- Provider tests serve canned JSON fixtures via MockWebServer (e.g., `InternetArchiveMusicProviderTest` asserts `artworkUrl` from `https://archive.org/services/img/...`)
- `PlaybackRepositoryImplTest` builds `QueueEntity`/`SongEntity` fixtures to verify queue persistence + restore
- Theme tests assert color/typography contracts (`CliBeatsColorsTest`, `CliBeatsTypographyTest`)

## Coverage & Quality Gates

**Coverage:**
- No CI coverage threshold for Android (unit suite exists but coverage % is not enforced)
- 47 test files; CI runs `testDebugUnitTest` only — the 4 instrumented DAO suites never execute in CI

**CI Quality Gates (`.github/workflows/ci.yml`):**
- Single job: `ktlintCheck` → `detekt` → `lintDebug` → `assembleDebug` → `testDebugUnitTest` (reports uploaded)
- Concurrency group cancels stale runs per ref
- `assembleRelease` is NOT in CI; no dependency vulnerability scan (no Dependabot/OWASP)

**Known Gaps:**
- No automated instrumented/UI/E2E in CI — `connectedDebugAndroidTest` (Room DAO suites) and Maestro UI flows are manual; emulator validation was done during recovery sessions with evidence in `.planning/debug/`
- Screenshot tests (`verifyPaparazziDebug`) not wired into CI
- Playback failure/retry paths, YouTube fallback chain ordering, `StreamUrlDeobfuscator` cipher handling, `PoTokenGenerator` WebView path, and `PlaybackService` notification lifecycle lack coverage (see CONCERNS → Test Coverage Gaps)

## Test Data & Secrets

- No real API keys in tests; Jamendo tests inject a fake client id via the `@Named` qualifier (not `BuildConfig`)
- Test env avoids EncryptedSharedPreferences; `AppPreferencesTest` uses DataStore test instances
- No PO-token minting or NewPipe network extraction in unit tests

---

*Testing analysis: 2026-08-12*
*Update when testing practices change*
