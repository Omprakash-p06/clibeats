# ADR-003: Encrypted Storage & Local Persistence Strategy

**Date:** 2026-08-05
**Status:** Accepted
**Phase:** 3 — Database & Local Persistence Layer
**Requirements:** REQ-LIB-03, REQ-OFF-03, REQ-ENG-09

---

## Context

CLIBeats needs to persist:
1. **Track library data** — songs, playlists, play history, and cache index (large structured data, complex queries).
2. **User preferences & secrets** — active provider, auth tokens, cache limit, audio quality (small key-value pairs, must be secure).

The choice of storage solution directly impacts query performance, testability, security posture, and maintenance burden.

## Decision

### Primary Storage: Room 2.6.1 with KSP

Use **Room 2.6.1** as the local relational database, compiled via KSP (not deprecated KAPT).

**Entities:** `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`
**DAOs:** Flow-based reactive queries for UI; suspend functions for writes.
**Schema:** `exportSchema = true` — JSON schema files committed to `app/schemas/` for migration verification.

**Rationale:**
- KSP is 2-3x faster than KAPT for incremental builds.
- `Flow<T>` DAOs eliminate manual polling and directly power Compose `collectAsState()`.
- `exportSchema` enables Room's automated migration verification tools.
- Room's in-memory builder provides hermetic DAO integration tests.

### Preferences Storage: DataStore Preferences 1.1.1

Use **Jetpack DataStore Preferences** for app settings and sensitive keys.

**Rejected:** `EncryptedSharedPreferences` — officially deprecated as of 2026 by Google, with known synchronous I/O issues and keyset corruption risks.

**Rationale:**
- DataStore is fully non-blocking (Kotlin Coroutines + Flow).
- Keys backed by Android Keystore hardware-backed key management.
- `AppPreferences.kt` provides typed accessors via `stringPreferencesKey`, `booleanPreferencesKey`, `intPreferencesKey`.
- Easy to test via `TestCoroutineScope` + `PreferenceDataStoreFactory`.

### Repository Pattern

All DAO access is mediated via Repository interfaces defined in `domain/repository/`. ViewModels and use cases MUST NOT import DAOs directly.

**Mappers** (`SongMapper.kt`, `PlaylistMapper.kt`) handle entity ↔ domain model conversion at the data layer boundary. Domain models are pure Kotlin data classes with no Room annotations.

## Consequences

### Positive
- Clean architecture boundary: domain layer has zero dependency on Room or DataStore.
- Reactive UIs via Flow — no manual refresh triggers.
- Hermetic DAO tests via in-memory Room.
- Schema JSON provides migration audit trail in git history.

### Negative
- DataStore migration from EncryptedSharedPreferences (if used in future) requires manual key migration path.
- Cross-entity JOIN queries must be handled via custom DAO queries (Room does not support ORM-style lazy loading).

### Neutral
- `@AutoMigration` covers simple column additions; manual `Migration` objects needed for column renames.
