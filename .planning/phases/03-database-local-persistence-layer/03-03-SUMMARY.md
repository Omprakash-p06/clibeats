---
phase: 03-database-local-persistence-layer
plan: "03-03"
subsystem: database
tags: [room, daos, datastore, repositories, hilt, kotlin, android]

# Dependency graph
requires:
  - phase: 03-02
    provides: Room entities (songs/playlists/playlist_song_cross_ref/history/cache_index), CliBeatsDatabase, CliBeatsTypeConverters, Hilt DatabaseModule
  - phase: 01-architecture-core-provider-api
    provides: Domain models Track & Playlist, Clean architecture package structure, Hilt DI configuration
provides:
  - 4 Room DAO interfaces (SongDao, PlaylistDao, HistoryDao, CacheIndexDao) with Flow-based queries and suspend writes — resolves CliBeatsDatabase's forward references
  - Entity↔domain mappers (SongMapper, PlaylistMapper)
  - 3 domain repository interfaces (SongRepository, PlaylistRepository, HistoryRepository)
  - 3 Hilt-bound data-layer implementations (SongRepositoryImpl, PlaylistRepositoryImpl, HistoryRepositoryImpl)
  - DataStore-backed AppPreferences (provider id, cache limit, quality toggle, auth token) + StorageModule DataStore provider
  - First clean `assembleDebug` build for the whole phase; Room schema 1.json exported to app/schemas
affects:
  - 03-04 (DAO integration tests, migration test suite against committed schema, ADR-003)
  - 04-playback-engine-background-media-service
  - 05-provider-integration-search
  - 06-playlists-queue-management-library
  - 07-caching-downloads-security-layer

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Flow-backed read queries (getAllAsFlow/searchAsFlow) with suspend one-shot writes (upsert/delete) in Room DAOs
    - Domain-layer repository interfaces consumed by data-layer @Singleton @Inject implementations delegating to DAOs
    - Extension-function mappers (toDomain/toEntity) keeping entities and domain models decoupled
    - DataStore<Preferences> injected via Hilt module with private top-level `by preferencesDataStore` delegate

key-files:
  created:
    - app/src/main/java/com/clibeats/data/local/dao/SongDao.kt
    - app/src/main/java/com/clibeats/data/local/dao/PlaylistDao.kt
    - app/src/main/java/com/clibeats/data/local/dao/HistoryDao.kt
    - app/src/main/java/com/clibeats/data/local/dao/CacheIndexDao.kt
    - app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt
    - app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt
    - app/src/main/java/com/clibeats/domain/repository/SongRepository.kt
    - app/src/main/java/com/clibeats/domain/repository/PlaylistRepository.kt
    - app/src/main/java/com/clibeats/domain/repository/HistoryRepository.kt
    - app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt
    - app/src/main/java/com/clibeats/data/repository/PlaylistRepositoryImpl.kt
    - app/src/main/java/com/clibeats/data/repository/HistoryRepositoryImpl.kt
    - app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt
    - app/src/main/java/com/clibeats/di/StorageModule.kt
    - app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json
  modified: []

key-decisions:
  - "DAOs expose Flow<List<...>> for reads and suspend functions for writes — reactive UI-ready queries with explicit transactional boundaries."
  - "HistoryRepository deliberately returns HistoryEntity (data-layer type) exactly as the plan specifies — the domain history model is intentionally thin for now."
  - "Room schema 1.json committed to app/schemas/ so Plan 03-04's migration test suite has a stable baseline."
  - "AppPreferences stores AUTH_TOKEN in plaintext DataStore per plan spec — encryption trade-off deferred to ADR-003 in Plan 03-04."

patterns-established:
  - "Pattern 1: Data-layer repository implementations are @Singleton @Inject classes in com.clibeats.data.repository that delegate to DAOs and map via data.local.mapper extension functions."
  - "Pattern 2: Preferences accessors are Flow-based (`Flow<String?>`, `Flow<Int>`, `Flow<Boolean>`) with defaults resolved at read time (cache 512 MB, high-quality streaming true)."

requirements-completed: [REQ-LIB-03, REQ-OFF-03]

# Coverage metadata — one entry per shipped deliverable
coverage:
  - id: D1
    description: "4 Room DAO interfaces (SongDao, PlaylistDao, HistoryDao, CacheIndexDao) with Flow reads, suspend writes and search/membership/cache-size queries"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "grep acceptance checks (@Dao x4, getAllAsFlow, getSongsForPlaylistAsFlow, ORDER BY played_at DESC, getTotalCacheSizeBytes)"
        status: pass
      - kind: other
        ref: "`.\gradlew.bat assembleDebug` — Room KSP compiled all DAO queries without errors"
        status: pass
    human_judgment: false
  - id: D2
    description: "Entity↔domain mappers (SongMapper toDomain/toEntity, PlaylistMapper toDomain/toEntity with createdAt/updatedAt defaults)"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "grep acceptance checks (all 4 mapper funs present; field names match Track/Playlist)"
        status: pass
    human_judgment: false
  - id: D3
    description: "3 domain repository interfaces (SongRepository, PlaylistRepository, HistoryRepository)"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "grep acceptance checks (getAllTracksAsFlow, getSongsForPlaylistAsFlow, recordPlay)"
        status: pass
    human_judgment: false
  - id: D4
    description: "3 Hilt @Singleton data-layer implementations delegating to DAOs with mapper conversion"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "grep acceptance checks (@Inject constructors x3, recordPlay inserts HistoryEntity)"
        status: pass
      - kind: other
        ref: "`.\gradlew.bat assembleDebug` — Hilt aggregate deps compiled, DI graph valid"
        status: pass
    human_judgment: false
  - id: D5
    description: "DataStore-backed AppPreferences wrapper (activeProviderId, cacheMaxMb, highQualityStreaming, authToken + setters) and StorageModule providing DataStore<Preferences>"
    requirement: REQ-OFF-03
    verification:
      - kind: other
        ref: "grep acceptance checks (val activeProviderId: Flow<String?>, suspend fun setAuthToken, provideDataStore(@ApplicationContext ...))"
        status: pass
    human_judgment: false
  - id: D6
    description: "Whole-phase clean build gate — CliBeatsDatabase DAO forward references resolve"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "`.\gradlew.bat assembleDebug` exits 0 (BUILD SUCCESSFUL) on final committed state"
        status: pass
    human_judgment: false

# Metrics
duration: 6min
completed: 2026-08-05
status: complete
---

# Phase 03 Plan 03: DAOs, Repositories, Mappers & AppPreferences Summary

**Four Room DAO interfaces, entity↔domain mappers, three repository interfaces with Hilt-bound data-layer implementations, and a DataStore-backed AppPreferences wrapper — the phase's first clean assembleDebug build passes and the Room schema baseline is exported for the 03-04 migration test suite.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-05T13:49:31Z
- **Completed:** 2026-08-05T13:54:55Z
- **Tasks:** 5
- **Files created:** 15 (14 source + 1 generated schema)

## Accomplishments
- Created 4 DAO interfaces — `SongDao` (upsert/upsertAll, search flow), `PlaylistDao` (CRUD + cross-ref membership + ordered join query), `HistoryDao` (recent-history flow with `LIMIT :limit`, clear-before/clear-all), `CacheIndexDao` (eviction queries + `SUM(file_size_bytes)` total). Room KSP compiled every query with zero errors — the forward references in `CliBeatsDatabase` now resolve, unblocking the phase's build.
- Added `SongMapper` and `PlaylistMapper` extension functions mapping `SongEntity ↔ Track` and `PlaylistEntity ↔ Playlist`, defaulting persistence-only fields (`localPath`/`cachedAt`; `createdAt`/`updatedAt` at insert time).
- Added domain-layer repository interfaces (`SongRepository`, `PlaylistRepository`, `HistoryRepository`) with Flow-based reads and suspend writes; `HistoryRepository` returns `HistoryEntity` exactly per plan spec.
- Added `@Singleton @Inject` data-layer implementations in `data/repository/` delegating to DAOs and mapping through the mappers; `HistoryRepositoryImpl.recordPlay` stamps `System.currentTimeMillis()` and inserts a `HistoryEntity`.
- Added DataStore-backed `AppPreferences` (active provider, cache limit defaulting to 512 MB, high-quality streaming defaulting true, auth token with clear) and `StorageModule` providing `DataStore<Preferences>` (`clibeats_prefs`) through the Hilt singleton graph.
- **First clean build of the phase:** `.\gradlew.bat assembleDebug` exits 0. Room KSP emitted `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` (schema baseline committed for 03-04 migration tests).
- All 14 new source files are ktlint-clean after formatting.

## Task Commits

Each task was committed atomically:

1. **Task 1: Create DAO Interfaces** - `c104aa6` (feat)
2. **Task 2: Create Entity ↔ Domain Mappers** - `82ffea6` (feat)
3. **Task 3: Create Repository Interfaces (Domain Layer)** - `2f95d70` (feat)
4. **Task 4: Create Repository Implementations (Data Layer)** - `ef17826` (feat)
5. **Task 5: Create AppPreferences + StorageModule** - `67edb9d` (feat)
6. **Formatting pass (ktlint)** - `6d6cb06` (style)
7. **Schema export commit** - `fd44bd3` (chore)

## Files Created

- `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt` - Track upsert/get-all/search/delete DAO
- `app/src/main/java/com/clibeats/data/local/dao/PlaylistDao.kt` - Playlist CRUD + cross-ref membership DAO
- `app/src/main/java/com/clibeats/data/local/dao/HistoryDao.kt` - Recent history flow + clears DAO
- `app/src/main/java/com/clibeats/data/local/dao/CacheIndexDao.kt` - Cache eviction + size aggregate DAO
- `app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt` - `SongEntity ↔ Track` extension mappers
- `app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt` - `PlaylistEntity ↔ Playlist` extension mappers
- `app/src/main/java/com/clibeats/domain/repository/SongRepository.kt` - Domain interface for track library
- `app/src/main/java/com/clibeats/domain/repository/PlaylistRepository.kt` - Domain interface for playlists
- `app/src/main/java/com/clibeats/domain/repository/HistoryRepository.kt` - Domain interface for play history (HistoryEntity per plan)
- `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt` - DAO-delegating Hilt singleton
- `app/src/main/java/com/clibeats/data/repository/PlaylistRepositoryImpl.kt` - DAO-delegating Hilt singleton
- `app/src/main/java/com/clibeats/data/repository/HistoryRepositoryImpl.kt` - DAO-delegating Hilt singleton
- `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt` - DataStore preferences wrapper
- `app/src/main/java/com/clibeats/di/StorageModule.kt` - Hilt `DataStore<Preferences>` provider
- `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` - Room schema baseline (generated, committed)

## Decisions Made
- Followed the plan exactly for all interfaces, mapper signatures, repository methods and preferences keys — including the intentional `HistoryEntity` return type in `HistoryRepository` and plaintext DataStore `auth_token` (both plan-specified; encryption trade-off tracked for ADR-003 in Plan 03-04).
- Committed the Room schema JSON — it is the migration-test baseline 03-02's summary explicitly anticipated and Plan 03-04 depends on.
- Kept `ktlintFormat`'s reflow of the plan's code samples (wrapped parameter lists, blank lines between declarations, trailing commas) — `ktlint_official` style requires them; formatting only, zero semantic change.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed unused `androidx.room.Transaction` import from PlaylistDao**
- **Found during:** Task 1 (DAO interface creation)
- **Issue:** The plan's `PlaylistDao.kt` template imports `androidx.room.Transaction` but no method uses `@Transaction` — ktlint's `no-unused-imports` rule would fail the check.
- **Fix:** Dropped the unused import line.
- **Files modified:** `app/src/main/java/com/clibeats/data/local/dao/PlaylistDao.kt`
- **Verification:** ktlintCheck clean on this file; assembleDebug passes.
- **Committed in:** `c104aa6` (Task 1 commit)

**2. [Rule 1 - Bug] Reflowed 11 new files to match ktlint_official style**
- **Found during:** Post-build `ktlintCheck`
- **Issue:** The plan's code samples (function signatures with multiple params on one line, `= Type(` expression bodies starting inline, declarations without separating blank lines, missing trailing commas) violate the project's ktlint 12.1.1 `ktlint_official` style.
- **Fix:** Ran `.\gradlew.bat ktlintFormat` on the new files — pure formatting (wrapped params + trailing commas, blank lines between declarations, same-line expression bodies). No semantics changed; re-ran `assembleDebug` (exit 0) to confirm.
- **Files modified:** all 11 multi-line files from Tasks 1-5 (PlaylistDao, both mappers, all 3 repository interfaces, all 3 impls, AppPreferences, StorageModule)
- **Verification:** `ktlintCheck` no longer reports any violation in plan-03-03 files.
- **Committed in:** `6d6cb06` (style commit)

**3. [Auto-added artifact] Committed Room schema export `app/schemas/.../1.json`**
- **Found during:** Post-build check
- **Issue:** The first clean `assembleDebug` produced the Room KSP schema export (wired in 03-01 via `room.schemaLocation`, enabled in 03-02 via `exportSchema = true`); leaving it untracked would strand the 03-04 migration-test baseline.
- **Fix:** Committed `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` (271 lines, expected content).
- **Committed in:** `fd44bd3` (chore commit)

**Pre-existing out-of-scope discovery (not fixed, logged):** `ktlintCheck` also flags 3 wave-2 files committed by Plan 03-02 (`CliBeatsDatabase.kt` lines 30-32 blank lines, `CliBeatsTypeConverters.kt` lines 6/11, `DatabaseModule.kt` EOF newline + blank line). These predate this plan and are outside its task scope — the formatter's changes to them were reverted and the issue logged to `deferred-items.md` (D-01) for a future wave (recommend 03-04).

---

**Total deviations:** 3 auto-fixed (2 Rule 1, 1 expected build artifact) + 1 logged out-of-scope
**Impact on plan:** All fixes are formatting/import corrections and a required artifact handoff. No scope creep; plan behavior implemented exactly as specified.

## Issues Encountered
- First `ktlintCheck` run failed across 14 new files plus 3 pre-existing wave-2 files. Resolved by running `ktlintFormat`, committing only the plan-03-03 formatting, and reverting/logging the wave-2 files per the executor scope boundary. `assembleDebug` and `ktlintCheck` (for plan-03-03 files) are green.
- PowerShell rendered Gradle's "uses or overrides a deprecated API" javac note as an stderr record — cosmetic only, build exit code 0 (same behavior noted in 03-01).

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. `AppPreferences` default values (cache 512 MB, high-quality streaming true) are intentional runtime defaults, not placeholders.

## Next Phase Readiness
- Plan 03-04 (DAO integration tests, migration test suite, ADR-003) can build against the committed schema baseline `app/schemas/.../1.json` and the now-resolved DAO interfaces.
- Repository layer is ready for injection into ViewModels in Phase 4+ (playback, provider integration, playlists, caching).
- **Open item for 03-04:** fix the 3 pre-existing ktlint violations in `CliBeatsDatabase.kt`, `CliBeatsTypeConverters.kt`, `DatabaseModule.kt` (see `deferred-items.md` D-01) — recommended while 03-04 is already touching these files for tests/ADR.
- **Open item for 03-04:** ADR-003 should evaluate encrypted storage for the `auth_token` preference (currently plaintext DataStore per plan spec).

## Self-Check: PASSED

- `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt` — FOUND
- `app/src/main/java/com/clibeats/data/local/dao/PlaylistDao.kt` — FOUND
- `app/src/main/java/com/clibeats/data/local/dao/HistoryDao.kt` — FOUND
- `app/src/main/java/com/clibeats/data/local/dao/CacheIndexDao.kt` — FOUND
- `app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt` — FOUND
- `app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt` — FOUND
- `app/src/main/java/com/clibeats/domain/repository/SongRepository.kt` — FOUND
- `app/src/main/java/com/clibeats/domain/repository/PlaylistRepository.kt` — FOUND
- `app/src/main/java/com/clibeats/domain/repository/HistoryRepository.kt` — FOUND
- `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt` — FOUND
- `app/src/main/java/com/clibeats/data/repository/PlaylistRepositoryImpl.kt` — FOUND
- `app/src/main/java/com/clibeats/data/repository/HistoryRepositoryImpl.kt` — FOUND
- `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt` — FOUND
- `app/src/main/java/com/clibeats/di/StorageModule.kt` — FOUND
- `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` — FOUND
- Commit `c104aa6` (Task 1) — FOUND in git log
- Commit `82ffea6` (Task 2) — FOUND in git log
- Commit `2f95d70` (Task 3) — FOUND in git log
- Commit `ef17826` (Task 4) — FOUND in git log
- Commit `67edb9d` (Task 5) — FOUND in git log
- Commit `6d6cb06` (style) — FOUND in git log
- Commit `fd44bd3` (chore) — FOUND in git log
- Commit `1cb030c` (docs) — FOUND in git log
- `assembleDebug` — BUILD SUCCESSFUL (exit 0)

---
*Phase: 03-database-local-persistence-layer*
*Completed: 2026-08-05*
