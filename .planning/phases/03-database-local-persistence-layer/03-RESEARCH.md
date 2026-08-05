---
phase: 3
name: Database & Local Persistence Layer
researched: "2026-08-05"
requirements: [REQ-LIB-03, REQ-OFF-03, REQ-ENG-09]
---

# Phase 3 Research: Database & Local Persistence Layer

## Phase Goal
Setup Room database schemas, DAOs, and encrypted storage for tracks, playlists, history, and user settings.

---

## 1. Technical Approach

### Room Database (Primary Storage)

**Library Versions (2026 current):**
- `room-runtime:2.6.1`, `room-ktx:2.6.1` — Coroutine-native Room
- `room-testing:2.6.1` — In-memory database for tests
- Already using KSP — add `ksp("androidx.room:room-compiler:2.6.1")`

**Key decisions:**
- Use `suspend` functions for one-shot write operations (insert, update, delete)
- Use `Flow<T>` for reactive read queries (UI auto-updates)
- `exportSchema = true` to generate JSON schema snapshots for migration verification
- `@AutoMigration` for simple column additions; manual `Migration` objects for complex schema changes
- `@TypeConverters` registered at `@Database` level to be globally available

**Schema Design (from domain models in Phase 1):**

| Entity | Table | Key Fields |
|--------|-------|------------|
| `SongEntity` | `songs` | `id TEXT PK`, `title`, `artist`, `album`, `durationMs`, `artworkUrl`, `streamUrl`, `providerId`, `localPath?`, `cachedAt?` |
| `PlaylistEntity` | `playlists` | `id TEXT PK`, `name`, `description?`, `artworkUrl?`, `trackCount`, `isOwned`, `providerId`, `createdAt`, `updatedAt` |
| `PlaylistSongCrossRef` | `playlist_song_cross_ref` | `playlistId TEXT FK`, `songId TEXT FK`, `position INT` — composite PK |
| `HistoryEntity` | `history` | `id INTEGER PK AUTOINCREMENT`, `songId TEXT FK`, `playedAt LONG`, `providerId` |
| `CacheIndexEntity` | `cache_index` | `songId TEXT PK`, `localPath TEXT`, `fileSizeBytes LONG`, `cachedAt LONG`, `expiresAt LONG?` |

**DAO interface per entity:**
- `SongDao`: `upsert`, `getById`, `getAllAsFlow`, `deleteById`, `searchByTitle`
- `PlaylistDao`: `upsert`, `getById`, `getAllAsFlow`, `deleteById`, `getSongsForPlaylist`
- `HistoryDao`: `insert`, `getRecentAsFlow`, `clearBefore`, `getAll`
- `CacheIndexDao`: `upsert`, `getById`, `deleteById`, `getAll`, `deleteBefore`

**Database class:**
```kotlin
@Database(
  entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class,
              HistoryEntity::class, CacheIndexEntity::class],
  version = 1,
  exportSchema = true,
)
@TypeConverters(CliBeatsTypeConverters::class)
abstract class CliBeatsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun cacheIndexDao(): CacheIndexDao
}
```

**Hilt module:** Provide as `@Singleton` via `DatabaseModule.kt` in `app/src/main/java/com/clibeats/di/`.

---

### Encrypted Settings Storage (DataStore + Tink)

**`EncryptedSharedPreferences` is DEPRECATED** as of 2026. The modern approach:
- `androidx.datastore:datastore-preferences:1.3.0` — non-blocking Kotlin Coroutines/Flow based storage
- `androidx.datastore:datastore-tink:1.3.0` — official Tink encryption for DataStore
- Backed by Android Keystore for hardware-backed key management

**What to store in DataStore:**
- Auth tokens / OAuth credentials
- User theme preferences
- Audio quality preference
- Cache size limit setting
- Last active provider

**Implementation:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton
    fun providePrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("clibeats_prefs") }
        )
}
```

Wrapper: `AppPreferences.kt` — typed accessors for each key using `stringPreferencesKey`, `booleanPreferencesKey`, etc., exposing `Flow<T>` properties and `suspend` edit functions.

---

### Repository Pattern

Each entity gets a repository interface in `domain/repository/` and an implementation in `data/repository/`:

- `SongRepository` (interface) / `SongRepositoryImpl` (implementation)
- `PlaylistRepository` / `PlaylistRepositoryImpl`
- `HistoryRepository` / `HistoryRepositoryImpl`
- `AppPreferencesRepository` / `AppPreferencesRepositoryImpl`

Repository methods return domain model types (not entities). Mappers (`SongMapper.kt` etc.) convert between entity ↔ domain model.

---

### ADR-003: Encrypted Storage & Local Persistence Strategy

Decisions to document:
- Room 2.6 + KSP (not KAPT) for compile-time correctness
- DataStore + Tink instead of deprecated EncryptedSharedPreferences
- `exportSchema = true` + schema JSON committed to git for migration audit trail
- Repository pattern as boundary between domain and data layers
- All DAOs expose `Flow<>` for reactive queries; `suspend` for writes
- Test strategy: Room in-memory database builder for hermetic DAO tests

---

## 2. Dependency Additions Required

In `gradle/libs.versions.toml`:
```toml
[versions]
room = "2.6.1"
datastore = "1.1.1"
coroutinesTest = "1.8.1"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
```

In `app/build.gradle.kts`:
```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
implementation(libs.datastore.preferences)
testImplementation(libs.room.testing)
testImplementation(libs.coroutines.test)
```

---

## 3. Testing Strategy (REQ-ENG-07)

**DAO tests:** `androidTest/` with `Room.inMemoryDatabaseBuilder`, JUnit4 + `runTest`.
- Insert/update/delete + verify via `Flow.first()` collection
- Query boundary cases (empty DB, missing IDs)
- Concurrent inserts
- History ordering (`ORDER BY playedAt DESC`)
- Cross-ref integrity (playlist ↔ song)

**Migration tests:** `MigrationTest.kt` using `MigrationTestHelper` with JSON schema exports.

**Repository unit tests:** Fake/stub DAOs injected via Hilt test modules. Test repository mapping logic independently of Room.

**DataStore tests:** Use `TestCoroutineScope` + in-memory `PreferenceDataStoreFactory`.

---

## 4. Execution Waves

| Wave | Plan | Content |
|------|------|---------|
| 1 | 03-01 | Dependencies, libs.versions.toml, app/build.gradle.kts, Room plugin registration, DataStore dependency |
| 2 | 03-02 | Room entities (5), TypeConverters, CliBeatsDatabase class, DatabaseModule Hilt binding |
| 3 | 03-03 | DAOs (4), Repository interfaces + implementations, Mappers, AppPreferences, StorageModule |
| 4 | 03-04 | DAO integration tests, Repository unit tests, ADR-003, ktlint/detekt/compile gate |

---

## Validation Architecture

> Standard Room + DataStore validation approach for Android.

**Dimension 4 (State Management):**
- Verify `Flow`-based queries update correctly on insert/delete
- Verify in-memory DB isolation across tests

**Dimension 6 (Error Handling):**
- `ProviderResult.Error` wrapping for repository operations
- `IOException` caught in DataStore writes

**Dimension 8 (Test Coverage):**
- DAO tests: all CRUD paths
- Repository tests: mapping correctness
- Migration tests: v1 schema matches exported JSON
