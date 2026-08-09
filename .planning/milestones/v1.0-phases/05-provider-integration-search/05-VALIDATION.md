---
phase: 5
slug: provider-integration-search
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-06
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito-Kotlin + Coroutines-Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "com.clibeats.data.provider.*" --tests "com.clibeats.presentation.search.*"` |
| **Full suite command** | `./gradlew testDebugUnitTest` |
| **Estimated runtime** | ~22 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick run command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~22 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | REQ-SET-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-01-02 | 01 | 1 | REQ-SET-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-01-03 | 01 | 1 | REQ-MUS-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-01-04 | 01 | 1 | REQ-SET-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-02-01 | 02 | 1 | REQ-MUS-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-02-02 | 02 | 1 | REQ-MUS-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-02-03 | 02 | 1 | REQ-MUS-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-02-04 | 02 | 1 | REQ-MUS-01, REQ-MUS-04 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.provider.mapper.TrackMapperTest"` | ✅ | ✅ green |
| 05-03-01 | 03 | 2 | REQ-SET-01, REQ-MUS-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.provider.YouTubeMusicProviderTest"` | ✅ | ✅ green |
| 05-03-02 | 03 | 2 | REQ-SET-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-04-01 | 04 | 2 | REQ-MUS-01 | unit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-04-02 | 04 | 2 | REQ-MUS-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.search.SearchViewModelTest"` | ✅ | ✅ green |
| 05-04-03 | 04 | 2 | REQ-MUS-04 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.search.SearchScreenKtTest"` | ✅ | ✅ green |
| 05-04-04 | 04 | 2 | REQ-NAV-01 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 05-05-01 | 05 | 3 | REQ-MUS-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.provider.mapper.TrackMapperTest"` | ✅ | ✅ green |
| 05-05-02 | 05 | 3 | REQ-SET-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.provider.YouTubeMusicProviderTest"` | ✅ | ✅ green |
| 05-05-03 | 05 | 3 | REQ-MUS-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.search.SearchViewModelTest"` | ✅ | ✅ green |
| 05-05-04 | 05 | 3 | REQ-MUS-04 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.search.SearchScreenKtTest"` | ✅ | ✅ green |
| 05-05-05 | 05 | 3 | REQ-SET-01, REQ-MUS-01 | docs | N/A | ✅ | ✅ green |
| 05-05-06 | 05 | 3 | All Phase 5 REQs | full gate | `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` | ✅ | ✅ green |

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
| Total Automated Tests (Phase 5) | 24 |
| Total Project Test Suite | 84 |
| Quality Gates Status | All Green |
