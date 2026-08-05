---
phase: 3
slug: database-local-persistence-layer
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-08-05"
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Room Testing + kotlinx-coroutines-test |
| **Config file** | `app/build.gradle.kts` (androidTest and test source sets) |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew testDebugUnitTest connectedDebugAndroidTest ktlintCheck detekt assembleDebug` |
| **Estimated runtime** | ~30-60 seconds (unit), ~2-4 min (instrumented) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest ktlintCheck`
- **After every plan wave:** Run `./gradlew testDebugUnitTest ktlintCheck detekt`
- **Before `/gsd:verify-work`:** Full suite must be green (`assembleDebug` + all tests)
- **Max feedback latency:** 60 seconds (unit test cycle)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 03-01-01 | 01 | 1 | REQ-ENG-09 | compile | `./gradlew assembleDebug` | ⬜ pending |
| 03-02-01 | 02 | 2 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` | ⬜ pending |
| 03-02-02 | 02 | 2 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` | ⬜ pending |
| 03-03-01 | 03 | 3 | REQ-OFF-03 | unit | `./gradlew testDebugUnitTest` | ⬜ pending |
| 03-03-02 | 03 | 3 | REQ-LIB-03 | unit | `./gradlew testDebugUnitTest` | ⬜ pending |
| 03-04-01 | 04 | 4 | REQ-ENG-09 | instrumented | `./gradlew connectedDebugAndroidTest` | ⬜ pending |
| 03-04-02 | 04 | 4 | REQ-ENG-09 | static | `./gradlew ktlintCheck detekt` | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/androidTest/java/com/clibeats/data/` — DAO test stubs for REQ-LIB-03, REQ-OFF-03
- [ ] Room testing library dependency added to `app/build.gradle.kts`
- [ ] `kotlinx-coroutines-test` added to test dependencies

*Wave 0 is embedded in Plan 03-01 (dependency additions).*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| DataStore encryption key in Android Keystore | REQ-ENG-09 | Requires real device hardware keystore | On physical device: clear app data, relaunch, verify prefs survive restart without corruption |
| DAO query performance at 1000+ tracks | REQ-NFR-02 | Performance requires device profiling | Open Library screen, observe no jank during scroll with 1000 tracks in DB |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
