---
phase: 8
slug: performance-budgets-accessibility
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-06
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito-Kotlin + Coroutines-Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.settings.*"` |
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
| 08-01-01 | 01 | 1 | REQ-SET-02 | unit | `./gradlew testDebugUnitTest --tests "com.clibeats.presentation.settings.SettingsViewModelTest"` | ✅ | ⬜ pending |
| 08-01-02 | 01 | 1 | REQ-SET-02 | integration | `./gradlew assembleDebug` | ✅ | ⬜ pending |
| 08-02-01 | 02 | 1 | REQ-NFR-01 | build | `./gradlew assembleDebug` | ✅ | ⬜ pending |
| 08-03-01 | 03 | 2 | REQ-NFR-02 | build | `./gradlew assembleDebug` | ✅ | ⬜ pending |
| 08-04-01 | 04 | 3 | REQ-NFR-03 | audit | `./gradlew assembleDebug` | ✅ | ⬜ pending |
| 08-04-02 | 04 | 3 | All Phase 8 REQs | full gate | `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` | ✅ | ⬜ pending |

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

**Approval:** pending
