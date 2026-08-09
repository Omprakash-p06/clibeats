---
phase: 03-database-local-persistence-layer
plan: "03-05"
subsystem: database
tags: [room, datastore, encryptedsharedpreferences, masterkey, keystore, foreignkey, security-crypto, android]

# Dependency graph
requires:
  - phase: 03-database-local-persistence-layer
    provides: Room entities + DAOs + repositories + AppPreferences (DataStore) from plans 03-01..03-04
provides:
  - Keystore-backed EncryptedSharedPreferences (MasterKey, AES256_GCM) for the auth token
  - Cloud backup exclusions (data_extraction_rules.xml) for secure and preference stores
  - Mapper upserts that preserve localPath/cachedAt/createdAt state
  - Room @ForeignKey CASCADE + indices on playlist_song_cross_ref (orphan prevention)
  - LIKE wildcard escaping in song search
  - Corrected ADR-003 documenting the hybrid storage model
affects:
  - 04-playback-engine-background-media-service
  - 07-caching-downloads-security-layer

# Tech tracking
tech-stack:
  added:
    - androidx.security:security-crypto 1.1.0-alpha06 (EncryptedSharedPreferences + MasterKey)
  patterns:
    - Hybrid storage: EncryptedSharedPreferences for secrets, DataStore Preferences for non-sensitive settings
    - Room @ForeignKey CASCADE with explicit indices for referential integrity
    - LIKE query escaping via ESCAPE clause + escapeForLike() helper

key-files:
  created:
    - app/src/main/res/xml/data_extraction_rules.xml
    - app/src/test/java/com/clibeats/data/preferences/AppPreferencesTest.kt
    - app/src/test/java/com/clibeats/data/local/mapper/MapperTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt
    - app/src/main/java/com/clibeats/di/StorageModule.kt
    - app/src/main/java/com/clibeats/data/local/entity/PlaylistSongCrossRef.kt
    - app/src/main/java/com/clibeats/data/local/dao/SongDao.kt
    - app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt
    - app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt
    - app/src/main/java/com/clibeats/data/repository/PlaylistRepositoryImpl.kt
    - app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt
    - app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json
    - docs/adr/ADR-003-encrypted-storage-local-persistence.md

key-decisions:
  - "EncryptedSharedPreferences (MasterKey AES256_GCM via Android Keystore) chosen for the AUTH_TOKEN credential; DataStore Preferences retained for non-sensitive UI settings."
  - "Room @ForeignKey CASCADE on playlist_song_cross_ref prevents orphaned cross-ref rows when playlists/songs are deleted."
  - "LIKE searches escape % _ \\ via an ESCAPE clause and a shared escapeForLike() helper to prevent wildcard search broadening."
  - "ADR-003 corrected to accurately describe the hybrid model after the previous version falsely claimed Keystore backing for DataStore."

patterns-established:
  - "Pattern 1: Sensitive credentials and non-sensitive settings are stored in separate backends with different security postures (EncryptedSharedPreferences vs DataStore)."
  - "Pattern 2: Mapper extension functions accept optional existing-state parameters so REPLACE upserts preserve persisted fields."

requirements-completed: [REQ-LIB-03, REQ-OFF-03, REQ-ENG-09]

# Coverage metadata — one entry per shipped deliverable
coverage:
  - id: G1
    description: "Keystore-backed EncryptedSharedPreferences for AUTH_TOKEN + backup exclusions + corrected ADR-003"
    requirement: REQ-OFF-03
    verification:
      - kind: unit
        ref: "app/src/test/java/com/clibeats/data/preferences/AppPreferencesTest.kt (6 tests pass)"
        status: pass
      - kind: static
        ref: "data_extraction_rules.xml excludes clibeats_secure_prefs.xml; ADR-003 documents MasterKey AES256_GCM"
        status: pass
    human_judgment: false
  - id: G2
    description: "Mapper upserts preserve localPath/cachedAt/createdAt; repository impls read existing entity state"
    requirement: REQ-LIB-03
    verification:
      - kind: unit
        ref: "app/src/test/java/com/clibeats/data/local/mapper/MapperTest.kt (5 tests pass)"
        status: pass
    human_judgment: false
  - id: G3
    description: "Foreign key CASCADE + indices on playlist_song_cross_ref; schema 1.json regenerated"
    requirement: REQ-LIB-03
    verification:
      - kind: other
        ref: "PlaylistSongCrossRef.kt has onDelete = ForeignKey.CASCADE for PlaylistEntity and SongEntity; app/schemas/.../1.json contains foreign keys"
        status: pass
    human_judgment: false
  - id: G4
    description: "LIKE search escaping for % _ \\ via ESCAPE clause"
    requirement: REQ-ENG-09
    verification:
      - kind: other
        ref: "SongDao.searchAsFlow uses ESCAPE '\\'; SongRepositoryImpl applies escapeForLike()"
        status: pass
    human_judgment: false
  - id: G5
    description: "Full quality gate green"
    requirement: REQ-ENG-09
    verification:
      - kind: other
        ref: "assembleDebug, testDebugUnitTest (40 tests), ktlintCheck, detekt all exit 0"
        status: pass
    human_judgment: false

# Metrics
duration: 20min
completed: 2026-08-05
status: complete
---

# Phase 03 Plan 05: Gap Closure — Encrypted Storage, Mapper Integrity, Foreign Keys & Query Safety

**Closed all 4 verified gaps from 03-VERIFICATION.md / 03-REVIEW.md: Keystore-backed encrypted credential storage with cloud-backup exclusions, mapper state-preserving upserts, foreign-key cascade referential integrity, and LIKE wildcard escaping — with a green full quality gate (40 unit tests, 0 failures).**

## Performance

- **Duration:** ~20 min
- **Tasks:** 5
- **Files created:** 3
- **Files modified:** 13

## Accomplishments

- **Encrypted storage (REQ-OFF-03, CR-01):** Added `security-crypto` 1.1.0-alpha06. `AppPreferences` now stores `AUTH_TOKEN` in `EncryptedSharedPreferences` backed by a `MasterKey` (`AES256_GCM` via Android Keystore); DataStore Preferences retained for non-sensitive settings (active provider, cache limit, quality). `data_extraction_rules.xml` excludes both `clibeats_secure_prefs.xml` and `clibeats_prefs.preferences_pb` from cloud backup and device transfer; `AndroidManifest.xml` wired `android:dataExtractionRules`. ADR-003 corrected to accurately document the hybrid model (replacing the false Keystore claim for DataStore).
- **Mapper integrity (WR-05/WR-02):** `SongMapper.toEntity` now accepts `existingLocalPath`/`existingCachedAt`; `SongRepositoryImpl` and `PlaylistRepositoryImpl` upserts fetch the existing entity first and preserve `localPath`, `cachedAt`, and `createdAt`. `MapperTest.kt` (5 tests) locks in the preservation behavior.
- **Foreign keys (WR-04):** `PlaylistSongCrossRef` gained `@ForeignKey` constraints on both `PlaylistEntity` and `SongEntity` with `onDelete = ForeignKey.CASCADE` plus indices on `playlist_id` and `song_id`. Exported schema `1.json` regenerated with the foreign keys.
- **Query safety (WR-01):** `SongDao.searchAsFlow` now uses `ESCAPE '\'`; `SongRepositoryImpl.searchTracksAsFlow` escapes `%`, `_`, and `\` via the `escapeForLike()` helper before querying.
- **Tests:** `AppPreferencesTest.kt` (6 tests) and `MapperTest.kt` (5 tests) added; whole suite `testDebugUnitTest` = 40 tests, 0 failures.

## Task Commits

Each task was committed atomically:

1. **Task 1: Keystore-backed encrypted credential storage & backup exclusions** - `400f151`
2. **Task 2: Preserve local cache fields and timestamps in mapper upserts** - `57d2ea8`
3. **Task 3: Foreign key cascade on playlist_song_cross_ref** - `05291ab`
4. **Task 4: Escape LIKE wildcards in song search** - `e17d639`
5. **Task 5: AppPreferences encrypted storage unit tests + quality gate** - `7c9f61c` (gate re-verified after execution)

## Files Created/Modified

- `gradle/libs.versions.toml`, `app/build.gradle.kts` — security-crypto dependency
- `app/src/main/AndroidManifest.xml` — `android:dataExtractionRules`
- `app/src/main/res/xml/data_extraction_rules.xml` — cloud/device-transfer exclusions
- `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt` — hybrid DataStore + EncryptedSharedPreferences
- `app/src/main/java/com/clibeats/di/StorageModule.kt` — MasterKey + EncryptedSharedPreferences provider
- `app/src/main/java/com/clibeats/data/local/entity/PlaylistSongCrossRef.kt` — FK CASCADE + indices
- `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt` — ESCAPE clause
- `app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt`, `PlaylistMapper.kt` — state-preserving signatures
- `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt`, `PlaylistRepositoryImpl.kt` — preserve existing state, escape queries
- `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json` — regenerated with foreign keys/indices
- `docs/adr/ADR-003-encrypted-storage-local-persistence.md` — corrected security documentation
- Tests: `AppPreferencesTest.kt`, `MapperTest.kt`

## Decisions Made

- Followed plan 03-05 exactly. security-crypto 1.1.0-alpha06 retained despite the known deprecation of the `EncryptedSharedPreferences`/`MasterKey` APIs in newer releases — the pinned version keeps the documented approach functional.

## Deviations from Plan

- Minor: Task 2 also added an `escapeForLike()` helper in `SongDao.kt` and the repository used it (as the plan's Task 4 described) — no semantic deviation.
- Task 5's quality-gate run and SUMMARY/tracking commit were completed by the orchestrator after the executor's dispatch returned early; all gates re-verified on the final committed state.

## Issues Encountered

- None blocking. `connectedDebugAndroidTest` (instrumented DAO tests) still not executed in this environment — no emulator (WINDOWS.md #2).

## Next Phase Readiness

- REQ-OFF-03 (encrypted storage) now satisfied at the data layer; ADR-003 accurate.
- Referential integrity and search safety gaps closed; mapper state preserved under REPLACE.
- Phase 3 gap closure complete pending re-verification.

## Self-Check: PASSED

- `data_extraction_rules.xml` — FOUND (Task 1)
- `AppPreferencesTest.kt` — FOUND, 6/6 pass (Task 5)
- `MapperTest.kt` — FOUND, 5/5 pass (Task 2)
- `PlaylistSongCrossRef.kt` FK CASCADE — FOUND in source and schema 1.json (Task 3)
- `SongDao.kt` ESCAPE clause + `escapeForLike()` — FOUND (Task 4)
- Commits `400f151`, `57d2ea8`, `05291ab`, `e17d639`, `7c9f61c` — FOUND in git log
- `assembleDebug`, `testDebugUnitTest` (40/0), `ktlintCheck`, `detekt` — all `BUILD SUCCESSFUL` (exit 0)

---
*Phase: 03-database-local-persistence-layer*
*Completed: 2026-08-05*
