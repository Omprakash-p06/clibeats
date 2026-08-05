---
phase: 03-database-local-persistence-layer
reviewed: 2026-08-05T00:00:00Z
depth: standard
files_reviewed: 32
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/androidTest/java/com/clibeats/data/local/dao/CacheIndexDaoTest.kt
  - app/src/androidTest/java/com/clibeats/data/local/dao/HistoryDaoTest.kt
  - app/src/androidTest/java/com/clibeats/data/local/dao/PlaylistDaoTest.kt
  - app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt
  - app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt
  - app/src/main/java/com/clibeats/data/local/CliBeatsTypeConverters.kt
  - app/src/main/java/com/clibeats/data/local/dao/CacheIndexDao.kt
  - app/src/main/java/com/clibeats/data/local/dao/HistoryDao.kt
  - app/src/main/java/com/clibeats/data/local/dao/PlaylistDao.kt
  - app/src/main/java/com/clibeats/data/local/dao/SongDao.kt
  - app/src/main/java/com/clibeats/data/local/entity/CacheIndexEntity.kt
  - app/src/main/java/com/clibeats/data/local/entity/HistoryEntity.kt
  - app/src/main/java/com/clibeats/data/local/entity/PlaylistEntity.kt
  - app/src/main/java/com/clibeats/data/local/entity/PlaylistSongCrossRef.kt
  - app/src/main/java/com/clibeats/data/local/entity/SongEntity.kt
  - app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt
  - app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt
  - app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt
  - app/src/main/java/com/clibeats/data/repository/HistoryRepositoryImpl.kt
  - app/src/main/java/com/clibeats/data/repository/PlaylistRepositoryImpl.kt
  - app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt
  - app/src/main/java/com/clibeats/di/DatabaseModule.kt
  - app/src/main/java/com/clibeats/di/StorageModule.kt
  - app/src/main/java/com/clibeats/domain/repository/HistoryRepository.kt
  - app/src/main/java/com/clibeats/domain/repository/PlaylistRepository.kt
  - app/src/main/java/com/clibeats/domain/repository/SongRepository.kt
  - app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt
  - docs/adr/ADR-003-encrypted-storage-local-persistence.md
  - gradle/libs.versions.toml
findings:
  critical: 1
  warning: 6
  info: 3
  total: 10
status: issues_found
---

# Phase 3: Code Review Report

**Reviewed:** 2026-08-05
**Depth:** standard
**Files Reviewed:** 32
**Status:** issues_found

## Summary

Reviewed the Phase 3 Database & Local Persistence Layer: Room schema (5 entities, 4 DAOs, `CliBeatsDatabase`, type converters), Hilt DI wiring (`DatabaseModule`, `StorageModule`), repository layer (3 domain interfaces + 3 implementations), DataStore `AppPreferences`, 4 DAO instrumented tests, 1 Mockito unit test, ADR-003, and Gradle dependency wiring.

Overall the Room schema, DAO queries, and DI wiring are solid: entities match the committed schema JSON, DAO Flow/suspend split is correct, in-memory DAO tests genuinely verify query behavior with `.first()` collection, and Hilt modules are scoped correctly. The exported schema (`app/schemas/.../1.json`) matches the entity definitions byte-for-byte.

The significant problems are concentrated in two areas: (1) **secret storage** — the auth token is persisted in plaintext DataStore Preferences while ADR-003 claims "Android Keystore hardware-backed" encryption that does not exist, and `android:allowBackup="true"` ships the token to cloud backups; (2) **entity↔domain mappers are lossy** — `Track.toEntity()` drops `localPath`/`cachedAt` and `Playlist.toEntity()` resets `createdAt` on every upsert, both of which silently destroy persisted state under `OnConflictStrategy.REPLACE`.

## Critical Issues

### CR-01: Auth token stored in plaintext DataStore; ADR-003 falsely claims Keystore-backed encryption; token exposed via cloud backup

**File:** `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt:26,44-47,61-67` and `docs/adr/ADR-003-encrypted-storage-local-persistence.md:36,42` and `app/src/main/AndroidManifest.xml:8`

**Issue:** `AUTH_TOKEN` is stored with a plain `stringPreferencesKey` in DataStore Preferences. DataStore Preferences writes an unencrypted protobuf file (`clibeats_prefs.preferences_pb`) in app-private storage — it performs **no encryption and has no Android Keystore involvement whatsoever**. Three compounding problems:

1. ADR-003 (line 42) states "Keys backed by Android Keystore hardware-backed key management" as a rationale for choosing DataStore for secrets. This claim is factually false and gives the security decision a false sense of guarantee. Line 36 also mischaracterizes the EncryptedSharedPreferences deprecation ("officially deprecated as of 2026 by Google" — it was deprecated because of keyset-corruption bugs; the replacement is Tink's `DeterministicAesGcm`/`KeystoreAesGcm` from security-crypto 1.1.0-alpha+ or manual Keystore wrapping, not plaintext DataStore).
2. The manifest has `android:allowBackup="true"` with no `dataExtractionRules`/`fullBackupContent` exclusion — the plaintext token file is included in Android Auto Backup to Google Drive (and `adb backup` on older APIs), exposing the streaming credential to the cloud in the clear.
3. `AppPreferences` exposes the token as a public `Flow<String?>` (`authToken`) with no isolation — any component with DataStore access can read it; the app sandbox is the only boundary.

**Fix:**
- Correct the ADR: DataStore Preferences is NOT encrypted and NOT Keystore-backed; document the real threat model.
- Encrypt the token before storing (e.g., `androidx.security.crypto.MasterKey` + Keystore-wrapped encryption, or Tink `KeystoreAesGcm`/`DeterministicAesGcm`), storing only ciphertext in DataStore.
- Add `android:dataExtractionRules="@xml/data_extraction_rules"` excluding `clibeats_prefs.preferences_pb` from cloud backup (or set `android:allowBackup="false"`).

```xml
<!-- res/xml/data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="file" path="datastore/clibeats_prefs.preferences_pb" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="file" path="datastore/clibeats_prefs.preferences_pb" />
    </device-transfer>
</data-extraction-rules>
```

## Warnings

### WR-01: Unescaped `%` / `_` wildcards in search query return wrong results

**File:** `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt:27`

**Issue:** `searchAsFlow` interpolates the raw user query into `LIKE '%' || :query || '%'`. In SQL, `%` and `_` are wildcards: a search for "100%" matches every title, and "a_b" matches any single character in between. There is no `ESCAPE` clause, so user input can silently broaden results. (Parameter binding prevents SQL injection, but not wildcard injection.)

**Fix:**
```sql
SELECT * FROM songs
WHERE title LIKE '%' || :escaped || '%' ESCAPE '\'
   OR artist LIKE '%' || :escaped || '%' ESCAPE '\'
```
with `escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")` computed before invoking the DAO.

### WR-02: `Playlist.toEntity()` resets `createdAt` on every upsert, destroying the original creation timestamp

**File:** `app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt:20-23` and `app/src/main/java/com/clibeats/data/repository/PlaylistRepositoryImpl.kt:31`

**Issue:** `toEntity()` defaults both `createdAt` and `updatedAt` to `System.currentTimeMillis()`. `PlaylistDao.upsert` uses `OnConflictStrategy.REPLACE` (delete + reinsert), so every metadata refresh overwrites `created_at` with "now" — the original creation date is permanently lost after the first update. The mapper cannot distinguish "new playlist" from "existing playlist".

**Fix:** Preserve the existing row's `createdAt` when present:
```kotlin
override suspend fun upsertPlaylist(playlist: Playlist) {
    val existing = playlistDao.getById(playlist.id)
    playlistDao.upsert(
        playlist.toEntity(
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        ),
    )
}
```

### WR-03: Domain-layer `HistoryRepository` leaks the Room entity across the clean-architecture boundary

**File:** `app/src/main/java/com/clibeats/domain/repository/HistoryRepository.kt:6,10`

**Issue:** The domain interface returns `Flow<List<HistoryEntity>>` — a `@Entity`-annotated Room data-layer class. ADR-003 (lines 48-50) mandates "All DAO access is mediated via Repository interfaces" with domain models "pure Kotlin data classes with no Room annotations" and claims "domain layer has zero dependency on Room or DataStore". This repository violates that stated contract, forcing any consumer (use case / ViewModel) to depend on the data layer and making the Room annotation leak into UI logic. The `@file:Suppress("ForbiddenImport")` masks the problem rather than fixing it.

**Fix:** Introduce a domain `PlayHistory` model (mirroring `HistoryEntity` fields) and map in `HistoryRepositoryImpl`; or, if the entity is intentionally reused, document the exception in ADR-003 instead of suppressing the rule.

### WR-04: Deleting a playlist or song leaves orphaned `playlist_song_cross_ref` rows

**File:** `app/src/main/java/com/clibeats/data/local/dao/PlaylistDao.kt:26-27` and `app/src/main/java/com/clibeats/data/local/entity/PlaylistSongCrossRef.kt:6-13`

**Issue:** `PlaylistDao.deleteById` deletes only the playlist row; the `playlist_song_cross_ref` rows (and `songs` rows) for that playlist remain. There are no `@ForeignKey` definitions anywhere, so Room cannot cascade. Orphaned rows accumulate forever and corrupt the `position` ordering semantics if a song id is later reused. Same applies to song deletion (no DAO path removes its cross-refs).

**Fix:** Add foreign keys with cascade, e.g.:
```kotlin
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlist_id", "song_id"],
    foreignKeys = [
        ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlist_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE),
    ],
)
```
(This requires a schema version bump to 2 with a migration, or add it now while still on version 1.)

### WR-05: `Track.toEntity()` silently drops `localPath`/`cachedAt` — caching columns can never be populated via the repository and REPLACE would wipe them

**File:** `app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt:21-31` and `app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt:32-34`

**Issue:** `SongEntity` has `localPath` and `cachedAt` columns, but `Track.toEntity()` never sets them (defaults to null) and `Track` has no corresponding fields. Nothing in the codebase writes these columns — they are permanently null. Worse, because `SongDao.upsert`/`upsertAll` use `REPLACE` (delete + reinsert), any future code path that populates `local_path`/`cached_at` (e.g., an offline-cache feature) will have those values silently erased on the next track refresh through the repository. The unit test `SongRepositoryImplTest.upsertTrack_callsDaoUpsert` (line 58) asserts the DAO is called with `testEntity("s1")` — an entity whose `localPath`/`cachedAt` are also null — so the test passes and actively locks in the buggy mapping.

**Fix:** Either carry the fields through the domain model (`Track` gains `localPath`/`cachedAt` and `toEntity()` maps them), or remove the dead columns from `SongEntity` and rely solely on `CacheIndexEntity` (the table that actually stores cache metadata). Add a round-trip unit test asserting `entity.toDomain().toEntity() == entity`.

### WR-06: AppPreferences flows will crash collectors on corrupt/unreadable DataStore file

**File:** `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt:29-47`

**Issue:** `dataStore.data` throws `IOException` on read failure (e.g., corrupted prefs file after a crash or partial write). None of the five public Flows handle this, so any collector (ViewModel/Compose) crashes instead of falling back to defaults. DataStore's own documentation requires consumers to catch `IOException`.

**Fix:**
```kotlin
val cacheMaxMb: Flow<Int> =
    dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.CACHE_MAX_MB] ?: 512 }
```
(apply the same pattern to all five flows, or wrap once in a private `safeData` helper).

## Info

### IN-01: `CliBeatsTypeConverters` are dead code and the round-trip is lossy for commas

**File:** `app/src/main/java/com/clibeats/data/local/CliBeatsTypeConverters.kt:5-11`

**Issue:** No entity field is `List<String>` — the converters are registered at DB level (`CliBeatsDatabase.kt:30`) but never invoked. If they are ever used, `joinToString(",")` + `split(",")` corrupts any string containing a comma (e.g., `listOf("a,b")` round-trips to `["a", "b"]`). Remove them, or if a list type is needed, use a delimiter that cannot appear in values (e.g., JSON via kotlinx.serialization).

### IN-02: Unused DAO methods not reachable from the repository layer

**File:** `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt:33-34` and `app/src/main/java/com/clibeats/data/local/dao/CacheIndexDao.kt:27-28`

**Issue:** `SongDao.deleteAll` and `CacheIndexDao.deleteBefore` are never called by any repository. `HistoryDao.clearBefore` is exposed but `CacheIndexDao.deleteBefore` (its cache-eviction counterpart) is not. Either expose them through the repository interfaces or remove them until needed.

### IN-03: `SongRepositoryImplTest` does not exercise mapper round-trip or error paths

**File:** `app/src/test/java/com/clibeats/data/repository/SongRepositoryImplTest.kt:41-59`

**Issue:** The two tests cover happy-path mapping and DAO delegation only. No test covers `upsertTracks`, `deleteTrack`, `getTrackById` null-handling, or — most importantly — field preservation through the `Track → SongEntity → Track` round-trip (which would have caught WR-05). `verify(songDao).upsert(testEntity("s1"))` passes only because both the production code and the test drop the same fields.

---

_Reviewed: 2026-08-05_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
