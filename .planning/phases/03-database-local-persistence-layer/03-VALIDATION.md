---
phase: 3
slug: database-local-persistence-layer
status: validated
nyquist_compliant: true
wave_0_complete: true
created: "2026-08-05"
updated: "2026-08-05"
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Room Testing + kotlinx-coroutines-test + Mockito (mockito-kotlin 5.4.0) |
| **Config file** | `app/build.gradle.kts` (androidTest and test source sets) |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew testDebugUnitTest connectedDebugAndroidTest ktlintCheck detekt assembleDebug` |
| **Estimated runtime** | ~30-60 seconds (unit), ~2-4 min (instrumented) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest ktlintCheck`
- **After every plan wave:** Run `./gradlew testDebugUnitTest ktlintCheck detekt`
- **Before `/gsd:verify-work`:** Full suite green (`assembleDebug` + all tests)
- **Max feedback latency:** 60 seconds (unit test cycle)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 03-01-01 | 01 | 1 | REQ-ENG-09 | compile | `./gradlew assembleDebug` | ✅ green |
| 03-02-01 | 02 | 2 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` | ✅ green |
| 03-02-02 | 02 | 2 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` | ✅ green |
| 03-03-01 | 03 | 3 | REQ-OFF-03 | unit | `./gradlew testDebugUnitTest` | ✅ green |
| 03-03-02 | 03 | 3 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` | ✅ green |
| 03-04-01 | 04 | 4 | REQ-ENG-09 | instrumented | `./gradlew connectedDebugAndroidTest` | ⚠️ compiled, unexecuted |
| 03-04-02 | 04 | 4 | REQ-ENG-09 | static | `./gradlew ktlintCheck detekt` | ✅ green |
| 03-05-01 | 05 | 5 | REQ-OFF-03 | unit | `./gradlew testDebugUnitTest` (`AppPreferencesTest`, 6) | ✅ green |
| 03-05-02 | 05 | 5 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` (`MapperTest`, 5) | ✅ green |
| 03-05-03 | 05 | 5 | REQ-LIB-03 | other | schema `1.json` FK/indices grep | ✅ green |
| 03-05-04 | 05 | 5 | REQ-ENG-09 | other | `SongDao` ESCAPE + `escapeForLike()` grep | ✅ green |
| 03-05-05 | 05 | 5 | REQ-ENG-09 | unit+static | full gate (`assembleDebug`, `testDebugUnitTest` 40/0, `ktlintCheck`, `detekt`) | ✅ green |

*Status: ✅ green · ⚠️ compiled-unexecuted (env-limited) · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `app/src/androidTest/java/com/clibeats/data/local/dao/` — DAO integration tests for REQ-LIB-03, REQ-OFF-03 (4 files, 14 @Test)
- [x] Room testing (`room-testing`) added to `app/build.gradle.kts`
- [x] `kotlinx-coroutines-test` in test + androidTest dependencies
- [x] `androidx.test.ext:junit` 1.2.1 (AndroidJUnit4 runner) added to androidTest deps

*Wave 0 embedded across Plans 03-01/03-04.*

---

## Manual-Only / Environment-Deferred Verifications

| Behavior | Requirement | Why | How to Close |
|----------|-------------|-----|--------------|
| Instrumented DAO integration tests pass at runtime (4 files, 14 tests) | REQ-LIB-03 / REQ-ENG-09 | Requires emulator/device (none in this environment) | Run `./gradlew connectedDebugAndroidTest` on CI/device. Recorded as `behavior_unverified` in 03-VERIFICATION.md and WINDOWS.md #2. |
| DataStore encryption key wraps via Android Keystore hardware | REQ-ENG-09 | Requires real device hardware keystore | On physical device: clear app data, relaunch, verify prefs survive restart without corruption |
| DAO query performance at 1000+ tracks | REQ-NFR-02 | Performance requires device profiling | Open Library screen, observe no jank during scroll with 1000 tracks in DB |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ✔ 2026-08-05

---

## Validation Audit 2026-08-05

| Requirement | Automated tests (present) | Result |
|-------------|---------------------------|--------|
| REQ-LIB-03 | `MapperTest` (5), `SongRepositoryImplTest` (2), DAO androidTests (14, compiled) | COVERED |
| REQ-OFF-03 | `AppPreferencesTest` (6) | COVERED |
| REQ-ENG-09 | full gates + androidTest compile | COVERED |
| **Gaps found** | 0 MISSING tests to write | — |
| **Escalated** | 1 environment-deferred (instrumented run) | Manual-Only |