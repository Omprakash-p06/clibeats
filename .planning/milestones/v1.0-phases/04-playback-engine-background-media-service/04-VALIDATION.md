---
phase: 4
slug: playback-engine-background-media-service
status: validated
nyquist_compliant: true
wave_0_complete: true
created: "2026-08-05"
updated: "2026-08-05"
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito (mockito-kotlin 5.4.0) + kotlinx-coroutines-test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew testDebugUnitTest ktlintCheck detekt assembleDebug` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest ktlintCheck`
- **After every plan wave:** Run `./gradlew testDebugUnitTest ktlintCheck detekt`
- **Before `/gsd:verify-work`:** Full suite must be green (`assembleDebug` + all tests)
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 04-01-01 | 01 | 1 | REQ-MUS-02 | compile | `./gradlew assembleDebug` | ✅ green |
| 04-02-01 | 02 | 2 | REQ-MUS-02 | compile | `./gradlew assembleDebug` | ✅ green |
| 04-03-01 | 03 | 3 | REQ-MUS-03 | unit | `./gradlew testDebugUnitTest` (`PlayerAdapterQueueTest`, 12) | ✅ green |
| 04-04-01 | 04 | 4 | REQ-MUS-02 | unit | `./gradlew testDebugUnitTest` (`PlayerAdapterTest` 4 + `PlayerViewModelTest` 4) | ✅ green |
| 04-04-02 | 04 | 4 | REQ-ENG-05 | static | `./gradlew ktlintCheck detekt` | ✅ green |

---

## Requirement-to-Test Map

| Requirement | Tests | Status |
|-------------|-------|--------|
| REQ-MUS-02 | `PlayerAdapterTest` (play/pause/seek/initial), `PlayerViewModelTest` (play-pause toggle, skips) | COVERED |
| REQ-MUS-03 | `PlayerAdapterQueueTest` — setQueue (order, startIndex, currentTrack), playTrack, skipToNext/Previous (has/not-has), setRepeatMode, toggleShuffle (12 tests) | COVERED |
| REQ-ENG-05 | ADR-004 in `docs/adr/` (docs artifact) + static gates | COVERED |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ✔ 2026-08-05

---

## Validation Audit 2026-08-05

| Metric | Count |
|--------|-------|
| Gaps found | 1 (REQ-MUS-03 queue management — `setQueue` etc. untested) |
| Resolved | 1 (`PlayerAdapterQueueTest.kt`, 12 tests; suite 48 → 60 tests, 0 failures) |
| Escalated | 0 |