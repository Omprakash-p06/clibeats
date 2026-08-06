---
phase: 9
slug: comprehensive-testing-hardening-suite
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-06
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito-Kotlin + Coroutines-Test + Compose-Ui-Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` |
| **Estimated runtime** | ~35 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick run command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~35 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | REQ-ENG-01 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.data.repository.*"` | ✅ | ✅ green |
| 09-01-02 | 01 | 1 | REQ-ENG-01 | integration | `./gradlew assembleDebug` | ✅ | ✅ green |
| 09-02-01 | 02 | 1 | REQ-ENG-07 | ui-test | `./gradlew testDebugUnitTest` | ✅ | ✅ green |
| 09-03-01 | 03 | 2 | REQ-ENG-07 | integration | `./gradlew testDebugUnitTest` | ✅ | ✅ green |
| 09-04-01 | 04 | 3 | REQ-ENG-06 | ci-audit | `./gradlew assembleDebug` | ✅ | ✅ green |
| 09-04-02 | 04 | 3 | All Phase 9 REQs | full gate | `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` | ✅ | ✅ green |

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
- [x] Feedback latency < 35s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-08-06

---

## Validation Audit 2026-08-06

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
| Total Automated Tests (Phase 9) | 6 |
| Total Project Test Suite | 106 |
| Quality Gates Status | All Green |
