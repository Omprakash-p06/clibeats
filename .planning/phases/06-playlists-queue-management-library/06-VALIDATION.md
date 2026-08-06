---
phase: 6
slug: playlists-queue-management-library
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-06
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito-Kotlin + Coroutines-Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.*" --tests "com.clibeats.playback.*"` |
| **Full suite command** | `./gradlew testDebugUnitTest` |
| **Estimated runtime** | ~25 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick run command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~25 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | REQ-MUS-03 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.playback.PlayerAdapterQueueTest"` | ✅ | ✅ green |
| 06-01-02 | 01 | 1 | REQ-MUS-03 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.queue.QueueViewModelTest"` | ✅ | ✅ green |
| 06-01-03 | 01 | 1 | REQ-MUS-03 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 06-02-01 | 02 | 1 | REQ-LIB-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.library.LibraryViewModelTest"` | ✅ | ✅ green |
| 06-02-02 | 02 | 1 | REQ-LIB-01 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 06-03-01 | 03 | 2 | REQ-LIB-02 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.playlist.PlaylistViewModelTest"` | ✅ | ✅ green |
| 06-03-02 | 03 | 2 | REQ-LIB-02 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 06-04-01 | 04 | 3 | REQ-MUS-03, REQ-LIB-01, REQ-LIB-02 | unit | `./gradlew testDebugUnitTest` | ✅ | ✅ green |
| 06-04-02 | 04 | 3 | REQ-MUS-03, REQ-LIB-01, REQ-LIB-02 | full gate | `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` | ✅ | ✅ green |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements.

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 25s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-08-06

---

## Validation Audit 2026-08-06

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
| Total Automated Tests (Phase 6) | 9 |
| Total Project Test Suite | 93 |
| Quality Gates Status | All Green |
