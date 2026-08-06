---
phase: 7
slug: caching-downloads-security-layer
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-06
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito-Kotlin + Coroutines-Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "com.clibeats.data.cache.*" --tests "com.clibeats.data.download.*"` |
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
| 07-01-01 | 01 | 1 | REQ-OFF-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.cache.CacheManagerTest"` | ✅ | ✅ green |
| 07-01-02 | 01 | 1 | REQ-OFF-01 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 07-02-01 | 02 | 1 | REQ-OFF-02 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.download.TrackDownloadManagerTest"` | ✅ | ✅ green |
| 07-02-02 | 02 | 1 | REQ-OFF-02 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 07-03-01 | 03 | 2 | REQ-OFF-03 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.network.NetworkMonitorTest"` | ✅ | ✅ green |
| 07-03-02 | 03 | 2 | REQ-OFF-03 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 07-04-01 | 04 | 3 | REQ-ENG-09 | audit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 07-04-02 | 04 | 3 | All Phase 7 REQs | full gate | `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` | ✅ | ✅ green |

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
| Total Automated Tests (Phase 7) | 3 |
| Total Project Test Suite | 96 |
| Quality Gates Status | All Green |
