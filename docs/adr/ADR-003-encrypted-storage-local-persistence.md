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

### Preferences Storage: Hybrid DataStore + EncryptedSharedPreferences

Use a **hybrid model** split by data sensitivity:

1. **Non-sensitive settings** (`active_provider_id`, `cache_max_mb`, `high_quality_streaming`) — **Jetpack DataStore Preferences 1.1.1**. DataStore is fully non-blocking (Kotlin Coroutines + Flow) and provides typed accessors via `stringPreferencesKey`, `booleanPreferencesKey`, `intPreferencesKey`, and is easy to test via `TestCoroutineScope` + `PreferenceDataStoreFactory`.

2. **Sensitive credentials** (`auth_token`) — **`EncryptedSharedPreferences` backed by `MasterKey`** (`androidx.security.crypto`, version `1.1.0-alpha06`). The MasterKey is generated with `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM)`, which creates an AES-256-GCM key wrapped by an **Android Keystore** asymmetric key — the key material never leaves the Keystore. Values are encrypted with AES256_GCM and keys with AES256_SIV before being written to `clibeats_secure_prefs.xml`, so only ciphertext is persisted.

**Rejected:** storing the auth token in plaintext DataStore Preferences. DataStore Preferences writes an unencrypted protobuf (`clibeats_prefs.preferences_pb`) and has **no Keystore involvement whatsoever** — an earlier draft of this ADR falsely claimed DataStore was "Keys backed by Android Keystore hardware-backed key management"; that claim is incorrect and is corrected here. `EncryptedSharedPreferences` was deprecated by Google in 2026 due to known synchronous I/O and keyset-corruption issues; the replacement is Tink's `KeystoreAesGcm`/`DeterministicAesGcm` (security-crypto 1.1.0-alpha+). This ADR pins `1.1.0-alpha06` because the legacy API remains functional and the Tink-based API was not stable at decision time; migration to the Tink API is planned as part of Phase 7 security work (REQ-OFF-03 / REQ-ENG-09 re-listed).

### Cloud Backup Exclusions

`android:allowBackup="true"` remains set, but `android:dataExtractionRules="@xml/data_extraction_rules"` excludes both `clibeats_secure_prefs.xml` (encrypted token) and `clibeats_prefs.preferences_pb` (DataStore file) from cloud backup and device transfer, so credentials never leave the device via backup/restore.

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
- `EncryptedSharedPreferences` performs synchronous I/O on the main thread at first access; acceptable for a single small credential file but worth revisiting if token count grows.
- EncryptedSharedPreferences keyset corruption can clear stored credentials (no key rotation strategy yet); the Tink-based replacement (security-crypto 1.1.0-alpha+) mitigates this and is the planned Phase 7 migration path.
- Cross-entity JOIN queries must be handled via custom DAO queries (Room does not support ORM-style lazy loading).

### Neutral
- `@AutoMigration` covers simple column additions; manual `Migration` objects needed for column renames.
