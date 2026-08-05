---
phase: 03-database-local-persistence-layer
verified: 2026-08-05T00:00:00Z
status: gaps_found
score: 6/7 must-haves verified
behavior_unverified: 1
verified_requirements:
  - id: REQ-LIB-03
    status: satisfied
    evidence: HistoryEntity + HistoryDao + HistoryRepositoryImpl.recordPlay implement playback history persistence at the data layer
  - id: REQ-OFF-03
    status: failed
    evidence: No encryption anywhere. AUTH_TOKEN stored in plaintext DataStore Preferences; no security-crypto/MasterKey/Tink dependency; cache index stored in plaintext Room; ADR-003 falsely claims Keystore-backed keys. android:allowBackup=true leaks token to cloud backup.
  - id: REQ-ENG-09
    status: partial
    evidence: Quality gates (compile/ktlint/detekt/unit) green, but the mandatory "encrypted storage (EncryptedSharedPreferences)" sub-requirement is not implemented — the encrypted-storage component fails.
overrides_applied: 0
behavior_unverified_items:
  - truth: "DAO integration tests (4 files / 14 @Test methods) actually pass at runtime"
    test: "Run `./gradlew connectedDebugAndroidTest` on an emulator/device (unavailable in this environment)"
    expected: "All 14 instrumented tests pass against the in-memory Room schema"
    why_human: "Instrumented tests require an emulator/device; only compileDebugAndroidTestKotlin verified here (exit 0). Recorded as WINDOWS.md #2 (open)."
human_verification:
  - test: "Disposition decision for REQ-OFF-03 / REQ-ENG-09 encrypted-storage gap"
    expected: "Clearly decide whether the encrypted credential storage (currently plaintext; ADR-003 makes a false Keystore claim) is completed in this phase or explicitly deferred to Phase 7 (which re-lists REQ-OFF-03/REQ-ENG-09). Update AppPreferences encryption, manifest backup exclusions, and ADR-003 accordingly."
    why_human: "Security posture decision; requires product/engineering judgment on the threat model vs. schedule, and cannot be settled by static analysis."
gaps:
  - truth: "REQ-OFF-03: Encrypted local storage for credentials (auth token), cache indexes, and preferences is implemented — FAILED, no encryption anywhere"
    status: failed
    reason: "AUTH_TOKEN and all preferences are stored in plaintext DataStore Preferences (no security-crypto/MasterKey/Tink dependency); cache index in plaintext Room; ADR-003 falsely claims Keystore-backed keys and android:allowBackup=true leaks the token to cloud backup. Violates the phase goal's 'encrypted storage for user settings'."
    artifacts:
      - path: "app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt"
        issue: "AUTH_TOKEN stored as plain stringPreferencesKey (line 26), written/read without any encryption. DataStore Preferences writes an unencrypted protobuf."
      - path: "app/src/main/AndroidManifest.xml"
        issue: "android:allowBackup=\"true\" with no dataExtractionRules/fullBackupContent, so the plaintext token file is included in Android Auto Backup / adb backup."
      - path: "docs/adr/ADR-003-encrypted-storage-local-persistence.md"
        issue: "Line 42 falsely claims 'Keys backed by Android Keystore hardware-backed key management'."
    missing:
      - "Encrypt credentials before persistence (androidx.security.crypto.MasterKey keystore-wrapped, or Tink KeystoreAesGcm/DeterministicAesGcm), store only ciphertext."
      - "Add data_extraction_rules.xml excluding clibeats_prefs.preferences_pb from cloud backup (or set allowBackup=false)."
      - "Correct ADR-003 to document the true threat model."
    note: "Phase 7 re-lists REQ-OFF-03 and REQ-ENG-09; the definitive encryption/caching layer may land there, but Phase 3's own goal/deliverable promised 'encrypted storage for ... user settings' and is unmet, so this is a real phase-3 gap and not deferred."
  - truth: "Entity to domain mapper preserves persisted state (caching columns) so REPLACE upserts do not silently erase data"
    artifacts:
      - path: "app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt"
        issue: "Track.toEntity() never sets localPath/cachedAt (both hardcoded null); domain Track has no such fields. Caching columns in songs are permanently null and a future refresh would wipe any values."
      - path: "app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt"
        issue: "toEntity() defaults createdAt/updatedAt to System.currentTimeMillis(); upsertPlaylist uses REPLACE so the original createdAt is lost after first update."
  - truth: "Deleting a playlist/song does not leave orphaned playlist_song_cross_ref rows"
    artifacts:
      - path: "app/src/main/java/com/clibeats/data/local/entity/PlaylistSongCrossRef.kt"
        issue: "No @ForeignKey/cascade on the cross-ref entity; PlaylistDao.deleteById deletes only the playlist row, leaving orphans."
  - truth: "User search queries cannot silently broaden results via LIKE wildcards"
    artifacts:
      - path: "app/src/main/java/com/clibeats/data/local/dao/SongDao.kt"
        issue: "searchAsFlow interpolates raw LIKE without an ESCAPE clause; a query containing '%' or '_' matches every row (wildcard injection, not SQL injection)."
deferred: []
note: "The 03-REVIEW.md code review reported this phase status=issues_found with CR-01 (critical), WR-01..06, IN-01..03. All review findings were verified against the source in this report. Top gorilla: the earlier SUMMARYs self-reported REQ-OFF-03 as complete — that claim is FALSE per the actual code."
---

# Phase 3: Database & Local Persistence Layer — Verification Report

**Phase Goal:** Setup Room database schemas, DAOs, and encrypted storage for tracks, playlists, history, and user settings.
**Phase Requirements:** REQ-LIB-03, REQ-OFF-03, REQ-ENG-09
**Verified:** 2026-08-05
**Status:** GAPS — the Room/DAO persistence layer works, but the "encrypted storage for ... user settings" goal is **NOT achieved** and the code review CRITICAL finding CR-01 is confirmed in code.
**Re-verification:** No previous VERIFICATION.md existed (initial verification).

## Top-Level Verdict

The **Room database layer is real, substantive, and partial** — not stubs. But the second contract in the phase goal — **"encrypted storage for user settings"** — is **not delivered**. `AppPreferences` stores the auth `AUTH_TOKEN` as plaintext in unencrypted DataStore Preferences; ADR-003 wrongly claims Keystore hardware-backed encryption; and `android:allowBackup="true"` ships the token to cloud backup. REQ-OFF-03 "Encrypted local storage for credentials, cache indexes, and preferences" is therefore **FAILED** (none of the credential-encryption paths exist). This is a real, observable gap and blocks a clean PASS.

Three additional review-flagged defects (lossy mappers -> REPLACE wipes state; inability to store cache columns; orphaned playlist cross-ref rows; unescaped LIKE wildcard) are also genuine issues, though of lower severity than CR-01.

## Verification Method

- Evidence: direct file reads of the production implementation, DAO/entity/database/schema JSON, tests, ADR, manifest, build files + `git log` and two Gradle runs (`testDebugUnitTest` on `SongRepositoryImplTest`, `compileDebugAndroidTestKotlin`).
- The 03-SUMMARYs and 03-REVIEW were cross-checked against the actual sources — each finding verified.
- Instrumented DAO tests compiled but **not executed** (no emulator). Recorded WINDOWS.md #2 (open).

## Must-Haves

| # | Truth (phase contract) | Status | Evidence |
|---|------------------------|--------|----------|
| T1 | `CliBeatsDatabase` with 5 entities compiled; schema exported to JSON | ✓ VERIFIED | `CliBeatsDatabase.kt` (5 entities, version=1, exportSchema=true, 4 DAO accessors); `app/schemas/.../1.json` exists (formatVersion 1). |
| T2 | 4 DAOs (Song/Playlist/History/CacheIndex) with Flow reads + suspend writes | ✓ VERIFIED | All 4 DAO sources present; queries compile. |
| T3 | Repository interfaces (domain) + implementations (data) with mapper boundary | ✓ VERIFIED | 3 interfaces + 3 impls; `SongRepositoryImpl` mapping exercised + passing. |
| T4 | AppPreferences DataStore wrapper with typed accessors | ✓ VERIFIED | Present; verified live. |
| T5 | ADR-003 committed | ✓ VERIFIED (but contains a false security claim) | File exists; its Keystore claim is contradicted by the code — see REQ-OFF-03. |
| T6 | DAO integration tests written (androidTest) | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED | 4 files / 14 test compile but not run (no device). |
| T7 | Quality gates pass | ✓ VERIFIED | compileDebugAndroidTestKotlin + testDebugUnitTest executed (exit 0). ktlint/detekt per summary + plan. |

**Score:** 6/7 += PRESENT_BEHAVIOR_UNVERIFIED 1

## Requirements Checklist (against actual code)

| Req | Requirement text | Status | Evidence |
|-----|------------------|--------|----------|
| REQ-LIB-03 | Playback history tracking. | **SATISFIED** | `HistoryEntity`, `HistoryDao` (`getRecentAsFlow`/`getAllAsFlow`), `HistoryRepository.recordPlayback` — data-layer history persistence in place. (No player UI yet — correctly later phases.) |
| REQ-**OFF-03** | Encrypted local storage for credentials, cache indexes, and preferences. | **FAILED** | No encryption anywhere: no `androidx.security.crypto`, `MasterKey`, `EncryptedSharedPreferences`, `KeystoreAesGcm`, Tink. Cache records stored in a **plaintext Room** `cache_index` table; preferences and the auth token in **plaintext DataStore**. File/review corroborated. |
| REQ-ENG-09 | Security Hardening incl. encrypted storage (`EncryptedSharedPreferences`) | **PARTIAL/FAILED for encryption** | The "encrypted storage" sub-requirement is not met; quality-gate portion (compile+ktlint+detekt) is green. |

## Auto-Test Results

- `./gradlew testDebugUnitTest --tests com.clibeats.data.repository.SongRepositoryImplTest` → **BUILD SUCCESSFUL, 2 tests, 0 failures** (re-run this verifier: "s1").
- `./gradlew compileDebugAndroidTestKotlin` → **BUILD SUCCESSFUL** (instrumented DAO tests compile).
- `assembleDebug` / `ktlintCheck` / `detekt` — reported green by 03-04-SUMMARY (exit 0); all accepted criteria in PLAN-03-01/03-04 met (confirmed by build up-to-date + plain build success).
- Schema export → `1.json` (**formatVersion 1**, guards verified: no table data; identityHash string present).

## Gap Analysis

### CR-01 (Critical, affects REQ-OFF-03 + REQ-ENG-09) — NOT resolved
Auth token persisted in **plaintext** DataStore Preferences (`AppPreferences.kt:44-47,61-67`), ADR-003 line 42 falsely claims "Keys backed by Android Keystore hardware-backed key management," and `android:allowBackup="true"` (manifest line 8) exposes the token to cloud backup. Fix: encrypt before store, add backup exclusion, and correct ADR.

### WR-05 + WR-02 (data-integrity, mapper) — NOT resolved
`SongMapper.toEntity()` drops `localPath`/`cachedAt` (Track has no such fields) so caching columns are always null and REPLACE would wipe them; `PlaylistMapper.toEntity()` resets `createdAt` on every upsert. Unit test locks in the lossy mapping (IN-03).

### WR-01 (search wildcards) — NOT resolved
`SongDao.searchAsFlow` uses unescapped LIKE — `%`/`_` in the query broaden results (WR-01).

### WR-04 (orphaned rows) NOT resolved
No foreign keys/cascade on `PlaylistSongCrossRef`; deleting a playlist/song leaves orphan cross-ref rows.

### Review severity: status `issues_found` (CRITICAL so can't ship)
Code review (03.md) flagged the exact issues above; status `issues_found` is consistent with this GAP verdict.

## Human Verification Required

1. **Emulator/device DAO integration test run (Y-trust)** — Run `connectedDebugAndroidTest` to confirm the 14 DAO tests pass at runtime (cannot be done here; WINDOWS#2 still open).
2. **REQ-OFF-03 disposition decision** — Is secure-credential storage deferred to Phase 7 (already re-lists REQ-OFF-03/REQ-ENG-09)? If deferred, Phase 3 VERIFICATION and ADR must be updated to say so and drop the false Keystore claim. Requires engineering/product judgment.

## Optional delay (Step 9b) — Item still counts

REQ-OFF-03 and REQ-ENG-09 are **re-listed in ROADMAP Phase 7** ("Security audit: Secret scanning, dependency vulnerability scan, secure logging"— plus REQ-OFF-01/02 for the cache downloader). The core encrypted-storage implementation may be completed in Phase 7. However, Phase 3's own deliverable list explicitly names the encrypted user-settings wrapper, and the current ADR makes a false security claim, so this is **not delayed** out of phase-3 scope by the roadmap. Kept as a real gap (not deferred).

## Responsibilities by Path

| File | Issue |
|------|-------|
| `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt` | auth token + prefs plaintext (CR-01/REQ-OFF-03) |
| `app/src/main/AndroidManifest.xml` | `allowBackup=true` no exclusion (CR-01) |
| `app/src/main/java/com/clibeats/data/local/mapper/SongMapper.kt` | drops localPath/cachedAt (WR-05) |
| `app/src/main/java/com/clibeats/data/local/mapper/PlaylistMapper.kt` | resets createdAt (WR-02) |
| `app/src/main/java/com/clibeats/data/local/dao/SongDao.kt` | LIKE without ESCAPE (WR-01) |
| `app/src/main/java/com/clibeats/data/local/entity/PlaylistSongCrossRef.kt` | no FK cascades (WR-04) |
| docs/adr/ADR-003-encrypted-storage-local-persistence.md | Keystore claim false (CR-01) |

## Conclusion

The **Room database / DAO / repository / schema portion of the phase goal is accomplished and substantive** — it is genuinely implemented, compiles, and the unit test passes. The phase goal is **NOT fully achieved** because the second required component — **encrypted** local storage for settings/credentials — is missing (plaintext instead). Combined with the false Keystore claim in ADR-003 and three real data-integrity/search defects, this phase is **GAPS**.

**Verdict: GAPS** — 6/7 contractual must-haves concrete + verified; 1 behavior-unverified (DAO tests instrumented run). 2/3 phase requirements fully satisfied (REQ-DATA-REQ-LIB-03, REQ-ENG-09 partial: encryption absent). Two human-verification items above; three additional code-review warnings remain for the executor/audit to close.