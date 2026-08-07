---
phase: 11
slug: production-release-distribution
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-07
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito-Kotlin + Coroutines-Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew assembleRelease testDebugUnitTest ktlintCheck detekt` |
| **Estimated runtime** | ~40 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick run command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~40 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | REQ-ENG-06 | release-build | `./gradlew assembleRelease` | ✅ | ⬜ pending |
| 11-02-01 | 02 | 1 | REQ-ENG-06 | license-audit | `./gradlew testDebugUnitTest --tests "com.clibeats.license.*"` | ✅ | ⬜ pending |
| 11-03-01 | 03 | 2 | REQ-ENG-06 | full gate | `./gradlew assembleRelease testDebugUnitTest ktlintCheck detekt` | ✅ | ⬜ pending |

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
- [x] Feedback latency < 40s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
