---
phase: 4
slug: playback-engine-background-media-service
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-08-05"
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito + kotlinx-coroutines-test |
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
| 04-01-01 | 01 | 1 | REQ-MUS-02 | compile | `./gradlew assembleDebug` | ⬜ pending |
| 04-02-01 | 02 | 2 | REQ-MUS-02 | compile | `./gradlew assembleDebug` | ⬜ pending |
| 04-03-01 | 03 | 3 | REQ-MUS-03 | unit | `./gradlew testDebugUnitTest` | ⬜ pending |
| 04-04-01 | 04 | 4 | REQ-MUS-02 | unit | `./gradlew testDebugUnitTest` | ⬜ pending |
| 04-04-02 | 04 | 4 | REQ-ENG-05 | static | `./gradlew ktlintCheck detekt` | ⬜ pending |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
