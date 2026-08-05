---
phase: 03-database-local-persistence-layer
plan: "03-04"
subsystem: database
tags: [room, testing, mockito, datastore, adr, ktlint, detekt, android]

# Dependency graph
requires:
  - phase: 03-03
    provides: Room DAO interfaces + repository implementations + mappers + AppPreferences — the subjects of this plan's tests
  - phase: 03-02
    provides: CliBeatsDatabase, entities and TypeConverters exercised by the DAO integration tests
  - phase: 03-01
    provides: Room 2.6.1 deps, room-testing, coroutines-test 1.8.1, room.schemaLocation KSP wiring
provides:
  - 4 Room DAO integration tests (androidTest, in-memory Room, AndroidJUnit4) — SongDaoTest, PlaylistDaoTest, HistoryDaoTest, CacheIndexDaoTest
  - SongRepositoryImpl unit test with Mockito test doubles (mockito-kotlin 5.4.0, mockito-core 5.12.0)
  - ADR-003: Encrypted Storage & Local Persistence Strategy (docs/adr/)
  - Whole-project green quality gate: ktlintFormat/assembleDebug/testDebugUnitTest/ktlintCheck/detekt all exit 0, Room schema 1.json exported
  - androidx.test.ext:junit 1.2.1 added to androidTest deps (AndroidJUnit4 runner)
affects:
  - 04-playback-engine-background-media-service
  - 06-playlists-queue-management-library
  - 07-caching-downloads-security-layer
  - 09-comprehensive-testing-hardening-suite (coverage targets)

# Tech tracking
tech-stack:
  added:
    - org.mockito.kotlin:mockito-kotlin 5.4.0 (unit test doubles)
    - org.mockito:mockito-core 5.12.0
    - androidx.test.ext:junit 1.2.1 (androidTest AndroidJUnit4 runner + Android Test core)
  patterns:
    - Hermetic DAO integration tests via Room.inMemoryDatabaseBuilder + allowMainThreadQueries + Flow.first() (androidTest)
    - Runs-flow-based DAO verification in unit tests via mockito-kotlin whenever/thenReturn(flowOf(...))
    - Targeted @Suppress annotations with justification comments to reconcile detekt with the project ktlint_official style

key-files:
  created:
    - app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt
    - app/src/androidTest/java/com/clibeats/data/local/dao/PlaylistDaoTest.kt
    - app/src/androidTest/java/com/clibeats/data/local/dao/HistoryDaoTest.kt
    - app/src/androidTest/java/com/clibeats/data/local/dao/CacheIndexDaoTest.kt
    - app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt
    - docs/adr/ADR-003-encrypted-storage-local-persistence.md
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt (kstyle + @file:Suppress)
    - app/src/main/java/com/clibeats/data/local/CliBeatsTypeConverters.kt (kstyle)
    - app/src/main/java/com/clibeats/data/local/dao/{Song,Playlist,History,CacheIndex}Dao.kt (@file:Suppress)
    - app/src/main/java/com/clibeats/data/local/mapper/{Song,Playlist}Mapper.kt (@file:Suppress)
    - app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt (@Suppress)
    - app/src/main/java/com/clibeats/data/repository/{Song,Playlist,History}RepositoryImpl.kt (@file/@Suppress)
    - app/src/main/java/com/clibeats/di/DatabaseModule.kt (kstyle + @file:Suppress)
    - app/src/main/java/com/clibeats/domain/repository/HistoryRepository.kt (@file:Suppress)

key-decisions:
  - "Added androidx.test.ext:junit 1.2.1 to androidTest deps — the plan's DAO test templates use @RunWith(AndroidJUnit4::class) and ApplicationProvider, which are not on the androidTest classpath (not transitive from espresso-core)."
  - "Mockito test deps routed through the version catalog (libs.mockito.kotlin/libs.mockito.core) per the project's catalog-first pattern rather than inline coordinates in build.gradle.kts."
  - "Resolved detekt's 131 first-run issues with targeted @Suppress + justification comments (plan-prescribed): @file:Suppress for import-level violations, class-level @Suppress for style rules; documented a config-level recommendation (D-02) for future waves."
  - "Fixed deferred D-01 ktlint violations in 3 wave-2 files (CliBeatsDatabase/TypeConverters/DatabaseModule) via ktlintFormat — formatting only."

requirements-completed: [REQ-LIB-03, REQ-OFF-03, REQ-ENG-09]

# Coverage metadata — one entry per shipped deliverable
coverage:
  - id: D1
    description: "4 Room DAO integration tests (androidTest) using in-memory Room, AndroidJUnit4, runTest and Flow.first() — Song/Playlist/History/CacheIndex"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "`.\gradlew.bat compileDebugAndroidTestKotlin` — success (exit 0); all 4 files compiled with androidx.test.ext:junit"
        status: pass
      - kind: other
        ref: "acceptance greps on the 4 test files (@RunWith(AndroidJUnit4::class), clearBefore_removesOldEntries, getTotalCacheSizeBytes_sumsCorrectly, addAndGetSongsForPlaylist)"
        status: pass
    human_judgment: true
    rationale: "Instrumented connectedDebugAndroidTest suite not executed — requires an emulator/device unavailable in this environment. Files compile and follow plan templates; runtime pass must be confirmed on CI/a device."
  - id: D2
    description: "SongRepositoryImpl unit test with Mockito test doubles (getAllTracksAsFlow_mapsToDomain, upsertTrack_callsDaoUpsert)"
    requirement: REQ-LIB-03
    verification:
      - kind: unit
        ref: "app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt#getAllTracksAsFlow_mapsToDomain"
        status: pass
      - kind: unit
        ref: "app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt#upsertTrack_callsDaoUpsert"
        status: pass
    human_judgment: false
  - id: D3
    description: "ADR-003 Encrypted Storage & Local Persistence Strategy (Room 2.6.1 + KSP, DataStore Preferences 1.1.1, Repository pattern)"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "acceptance greps (H1 title, ## Decision, DataStore Preferences, Room 2.6.1, ## Consequences)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whole-project green quality gate — ktlintFormat, assembleDebug, testDebugUnitTest, ktlintCheck, detekt all BUILD SUCCESSFUL; Room schema 1.json exported"
    requirement: REQ-ENG-09
    verification:
      - kind: other
        ref: "`.\gradlew.bat ktlintFormat` exit 0"
        status: pass
      - kind: other
        ref: "`.\gradlew.bat assembleDebug` exit 0"
        status: pass
      - kind: other
        ref: "`.\gradlew.bat testDebugUnitTest` exit 0"
        status: pass
      - kind: other
        ref: "`.\gradlew.bat ktlintCheck` exit 0"
        status: pass
      - kind: other
        ref: "`.\gradlew.bat detekt` exit 0"
        status: pass
      - kind: other
        ref: "app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json exists (formatVersion 1)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Deferred D-01 ktlint violations in wave-2 files (CliBeatsDatabase, CliBeatsTypeConverters, DatabaseModule) resolved via ktlintFormat"
    requirement: REQ-ENG-09
    verification:
      - kind: other
        ref: "`.\gradlew.bat ktlintCheck` exit 0 on whole project (previously flagged these 3 files); WINDOWS.md entry #1 marked fixed"
        status: pass
    human_judgment: false

# Metrics
duration: 20min
completed: 2026-08-05
status: complete
---

# Phase 03 Plan 04: DAO Integration Tests, Repository Unit Tests & ADR-003 Summary

**In-memory-Room DAO integration tests for all 4 DAOs, a Mockito unit test for SongRepositoryImpl, ADR-003 (Room 2.6.1 + DataStore Preferences), a new androidx.test.ext:junit androidTest dependency, and a now-fully-green whole-project quality gate (assembleDebug, unit tests, ktlint, detekt all exit 0) with the Room schema JSON exported.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-08-05T08:30:59Z
- **Completed:** 2026-08-05T08:50:37Z
- **Tasks:** 4
- **Files created:** 6 (5 test + 1 ADR)
- **Files modified:** 22 (2 gradle + 20 source)

## Accomplishments
- **DAO integration tests (androidTest):** Created `SongDaoTest`, `PlaylistDaoTest`, `HistoryDaoTest`, `CacheIndexDaoTest` — each builds a hermetic in-memory Room database (`Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries`), drives Flow reads via `runTest` + `Flow.first()` and suspend writes, and covers upsert idempotency, search, deletion, history ordering/clearing, cache-size aggregation, and playlist cross-ref membership. `compileDebugAndroidTestKotlin` confirms all four compile against the real schema (verified twice — before and after the ktlint_official reflow).
- **Repository unit test:** Created `SongRepositoryImplTest` with Mockito test doubles (`mockito-kotlin` + `mockito-core` added to the version catalog and `testImplementation`) — `getAllTracksAsFlow_mapsToDomain` verifies entity→domain mapping, `upsertTrack_callsDaoUpsert` verifies the Track→SongEntity round-trip and DAO delegation. **2 tests, 0 failures.**
- **androidx.test.ext:junit added:** The plan's templates require `@RunWith(AndroidJUnit4::class)` (androidx.test.ext:junit) and `ApplicationProvider` (androidx.test:core); neither was on the androidTest classpath, so the runner dependency 1.2.1 was added via the version catalog.
- **ADR-003:** Wrote the encrypted-storage & local-persistence strategy exactly as specified — Room 2.6.1 (KSP) for relational data with exported JSON schemas, DataStore Preferences 1.1.1 for settings/secrets (EncryptedSharedPreferences rejected as deprecated), Repository pattern mandate, and positive/negative/neutral consequences.
- **Full quality gate is green:** `ktlintFormat`, `assembleDebug`, `testDebugUnitTest`, `ktlintCheck`, `detekt` all exit 0 with `BUILD SUCCESSFUL`. `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` exists (formatVersion 1). This is the first time detekt has passed on the complete Phase 3 data layer.
- **Deferred D-01 resolved:** The 3 wave-2 ktlint violations (CliBeatsDatabase, CliBeatsTypeConverters, DatabaseModule) logged in deferred-items.md were fixed by `ktlintFormat` (formatting only) as part of the quality gate; WINDOWS.md ledger entry #1 marked fixed.

## Task Commits

Each task was committed atomically:

1. **Task 1: DAO Integration Tests** - `b7d9d49` (test)
2. **Task 2: Repository Unit Test (SongRepositoryImpl)** - `960087e` (test)
3. **Task 3: Write ADR-003** - `78e7da1` (docs)
4. **Task 4: Quality Gate — Full Build & Static Analysis** - `6e2d0e4` (style)

## Files Created/Modified

**Created:**
- `app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt` - Track upsert/get-by-id/flow/delete/search DAO tests
- `app/src/androidTest/java/com/clibeats/data/local/dao/PlaylistDaoTest.kt` - Playlist cross-ref membership add/remove tests
- `app/src/androidTest/java/com/clibeats/data/local/dao/HistoryDaoTest.kt` - Recent-history ordering + clear-before/clear-all tests
- `app/src/androidTest/java/com/clibeats/data/local/dao/CacheIndexDaoTest.kt` - Cache entry upsert/delete + total-size sum test
- `app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt` - Mockito unit test for the song repository
- `docs/adr/ADR-003-encrypted-storage-local-persistence.md` - Storage/persistence ADR

**Modified:**
- `gradle/libs.versions.toml` - Added `androidxTestExtJunit`, `mockitoKotlin`, `mockitoCore` versions + 3 library refs
- `app/build.gradle.kts` - Added `androidx.test.ext:junit` (androidTest), `mockito-kotlin`/`mockito-core` (testImplementation)
- 13 source files - `@file:Suppress`/`@Suppress` annotations with justification comments for detekt
- 3 wave-2 files - ktlint formatting fixes (D-01)

## Decisions Made
- **Added `androidx.test.ext:junit 1.2.1` to androidTest deps.** The plan's templates import `AndroidJUnit4` (androidx.test.ext:junit) and `ApplicationProvider` (androidx.test:core) — not present (or not transitive) on the androidTest classpath. Without it the DAO tests would fail to compile on any device/CI run. This is a Rule 2 auto-add within Task 1's commit.
- **Route Mockito through the version catalog** (`libs.mockito.kotlin`/`libs.mockito.core`), honouring the project's established catalog-first dependency pattern (03-01), rather than inline coordinates as the plan's build.gradle.kts snippet showed.
- **Detekt reconciliation via targeted `@Suppress` with justification** (plan-prescribed). First-ever detekt run on the data layer exposed 131 pre-existing Phase-3 issues: 94 `Indentation` from detekt 1.23.6 misparsing the ktlint_official `@inject` constructor style (false positives), 31 `ForbiddenImport` from the over-broad Phase-0 `com.clibeats.data.*` pattern flagging legitimate data self-imports, and 6 line-length/wrapping hits from detekt's 120-char default vs ktlint's tolerance. `@file:Suppress` for import-level violations; class-level `@Suppress` for style rules. Left a config-level recommendation (disable duplicate `Indentation`, refine `ForbiddenImport` to an explicit-import list) in deferred-items.md D-02 rather than changing Phase-0 config in this plan.
- Kept `HistoryRepository`'s `HistoryEntity` return type (plan-specified) with a `@file:Suppress` noting its deliberate data-layer boundary exposure.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added `androidx.test.ext:junit` to the androidTest classpath**
- **Found during:** Task 1 (DAO integration tests)
- **Issue:** All four DAO test templates use `@RunWith(AndroidJUnit4::class)` (from `androidx.test.ext:junit`) and `ApplicationProvider` (from `androidx.test:core`) — neither on the androidTest classpath (espresso-core does not transitively bring `androidx.test.ext:junit`). Without it the instrumented tests could not compile on any device run.
- **Fix:** Added `androidxTestExtJunit = "1.2.1"` version + `androidx-test-ext-junit` library to `gradle/libs.versions.toml`, and `androidTestImplementation(libs.androidx.test.ext.junit)` to `app/build.gradle.kts`.
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Verification:** `.\gradlew.bat compileDebugAndroidTestKotlin` exit 0.
- **Committed in:** `b7d9d49` (Task 1 commit)

**2. [Rule 3 - Blocking] Detekt gate surfaced 131 pre-existing issues in the Phase 3 data layer (first detekt run)**
- **Found during:** Task 4 (quality gate)
- **Issue:** `.\gradlew.bat detekt` exited 1 with 131 weighted issues in files created by Plans 03-02/03-03 — 94 `Indentation` (detekt 1.23.6 misparses the ktlint_official `@inject`-on-separate-line constructor style this project mandates; false positives), 31 `ForbiddenImport` (Phase-0 pattern `com.clibeats.data.*` over-broad — flags every data-layer self-import), 4 `MaxLineLength` + 2 `MaximumLineLength` + 2 `Wrapping` (detekt 120-char default vs ktlint_official's relaxed tolerance). detekt had never run on the data layer before this gate.
- **Fix:** Per plan Task 4 prescription, added targeted `@Suppress` annotations with justification comments — `@file:Suppress("ForbiddenImport", "MaxLineLength")` at file top for import-level violations, class-level `@Suppress("Indentation", ...)` for style rules on the 4 impl/prefs files. (The mappers also received file-level annotations.)
- **Files modified:** 13 files across `data/local`, `data/repository`, `data/preferences`, `di`, `domain/repository` + the unit test.
- **Verification:** `.\gradlew.bat detekt` exit 0 (BUILD SUCCESSFUL); `ktlintCheck` still exit 0 after annotations.
- **Committed in:** `6e2d0e4` (Task 4 commit)

**3. [Rule 3 - Blocking] Fixed deferred D-01 ktlint violations in wave-2 files**
- **Found during:** Task 4 (quality gate)
- **Issue:** Whole-project `ktlintCheck` would fail on the 3 pre-existing wave-2 files (`CliBeatsDatabase.kt`, `CliBeatsTypeConverters.kt`, `DatabaseModule.kt`) logged to deferred-items.md D-01 by Plan 03-03.
- **Fix:** Ran `.\gradlew.bat ktlintFormat` (blank lines between DAO accessors, removed class-body leading blank lines, joined expression body, added EOF newline) — formatting only; no semantic change.
- **Files modified:** the 3 wave-2 files
- **Verification:** `ktlintCheck` exit 0 on the whole project; WINDOWS.md entry #1 marked fixed.
- **Committed in:** `6e2d0e4` (Task 4 commit)

**4. [Rule 1 - Bug] Reflowed new test files to ktlint_official, then re-verified androidTest compile**
- **Found during:** Task 4 (`ktlintFormat` after file creation)
- **Issue:** The plan's test templates needed ktlint_official reflow (wrapped assignments, expanded constructor args, multiline function bodies) — same as Plan 03-03 observed.
- **Fix:** `ktlintFormat` normalized all 5 test files; re-ran `compileDebugAndroidTestKotlin` to confirm the reformatted instrumented tests still compile.
- **Files modified:** 4 androidTest DAO tests + `SongRepositoryImplTest.kt`
- **Verification:** `compileDebugAndroidTestKotlin` exit 0; `assembleDebug`/`testDebugUnitTest` exit 0.
- **Committed in:** `b7d9d49` (Task 1 — pre-format) and `6e2d0e4` (Task 4 — final format)

---

**Total deviations:** 4 auto-fixed (1 Rule 2 missing-critical, 2 Rule 3 blocking, 1 Rule 1 bug/formatting).
**Impact on plan:** All fixes were correctness/verification requirements for the gate to pass; no scope creep. Test and ADR deliverables match the plan exactly; dependency and static-analysis tailoring was required to reach the plan's own success criteria.

## Issues Encountered
- **detekt first-run failure** on the Phase 3 data layer (131 issues). Root causes are tooling-level (detekt 1.23.6 `@inject` constructor style conflict + over-broad Phase-0 `ForbiddenImport` regex + 120-char default vs ktlint tolerance), not code defects. Resolved via targeted suppressions; a config-refinement recommendation (D-02) was logged for a future wave.
- **Initial `@file:Suppress` placement error:** placing a file-level annotation after the `package` declaration is invalid Kotlin (ktlint failed to parse). Corrected by moving `@file:Suppress` before the `package` line in the mapper files.
- **Instrumented DAO tests not executed:** `connectedDebugAndroidTest` requires an emulator/device unavailable in this environment. Test files compile and follow the plan templates; they must be run on CI/device (tracked as WINDOWS.md unrun-verify #2). Unit tests, build and static analysis all pass locally.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. No placeholder values, empty bodies, or mock data in deliverables.

## Next Phase Readiness
- Room persistence layer for Phase 3 is complete and quality-gated: schemas, DAOs, repositories, mappers, AppPreferences, integration tests (androidTest), unit tests, and ADR-003 all in place with a green `assembleDebug`/`testDebugUnitTest`/`ktlintCheck`/`detekt`.
- Phase 4 (Playback Engine & Background Media Service) can inject the repository layer into Media3 and ViewModels with test coverage already in place.
- **Open item:** run `connectedDebugAndroidTest` on CI/emulator to execute the 4 DAO integration tests (WINDOWS.md #2).
- **Recommendation (deferred, D-02):** refine `config/detekt/detekt.yml` — disable the duplicate `Indentation` rule and narrow `ForbiddenImport` to an explicit import list so future data-layer files don't each need `@Suppress`.
- Mockito (mockito-kotlin/mockito-core) is now available in `testImplementation` for Phase 4+ ViewModel/UseCase tests.

## Self-Check: PASSED

- `app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt` — FOUND
- `app/src/androidTest/java/com/clibeats/data/local/dao/PlaylistDaoTest.kt` — FOUND
- `app/src/androidTest/java/com/clibeats/data/local/dao/HistoryDaoTest.kt` — FOUND
- `app/src/androidTest/java/com/clibeats/data/local/dao/CacheIndexDaoTest.kt` — FOUND
- `app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt` — FOUND
- `docs/adr/ADR-003-encrypted-storage-local-persistence.md` — FOUND
- `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` — FOUND (formatVersion 1)
- Commit `b7d9d49` (Task 1) — FOUND
- Commit `960087e` (Task 2) — FOUND
- Commit `78e7da1` (Task 3) — FOUND
- Commit `6e2d0e4` (Task 4) — FOUND
- `assembleDebug` — BUILD SUCCESSFUL (exit 0)
- `testDebugUnitTest` — BUILD SUCCESSFUL (exit 0; SongRepositoryImplTest 2/2 pass)
- `ktlintCheck` — BUILD SUCCESSFUL (exit 0)
- `detekt` — BUILD SUCCESSFUL (exit 0)

---
*Phase: 03-database-local-persistence-layer*
*Completed: 2026-08-05*