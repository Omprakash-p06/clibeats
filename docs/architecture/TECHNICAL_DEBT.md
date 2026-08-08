# TECHNICAL_DEBT.md — Codebase Technical Debt & Risk Audit

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Codebase Audit & Execution Plan  
> **Date:** 2026-08-09

---

## Technical Debt Inventory

This document identifies architectural, security, testing, and operational technical debt accumulated during initial development, categorized by severity and prioritized for resolution in upcoming phases.

---

## 1. Debt Classification & Risk Audit

| Debt ID | Category | Description | Severity | Impact |
|---|---|---|---|---|
| **DEBT-01** | **Hosting & Deployment** | Gateway currently configured to run locally (`http://192.168.0.106:8080/`). Release builds require environment-based gateway base URL configuration. | **CRITICAL** | App unusable outside developer Wi-Fi LAN until deployed to public server (Railway). |
| **DEBT-02** | **Provider Coupling** | Gateway has only one production provider adapter (`YouTubeProviderAdapter`). No runtime dynamic fallback provider yet. | **HIGH** | If YouTube changes InnerTube schemas, zero fallback options exist until secondary provider is built. |
| **DEBT-03** | **Offline Download Manager** | Background downloads currently handle basic OkHttp byte streaming without native Android `DownloadManager` pause/resume or network constraint awareness. | **HIGH** | Large downloads can fail on spotty cellular networks or draw excess battery. |
| **DEBT-04** | **Testing Coverage Gaps** | Compose UI E2E test suite uses manual script execution rather than automated UIAutomator / Maestro CI test runs. | **MEDIUM** | UI regressions must be verified via manual execution script rather than automated CI step. |
| **DEBT-05** | **Lyrics Integration** | No native lyrics parser or LRCLIB integration built in `PlaybackViewModel` or `PlayerScreen`. | **MEDIUM** | Users cannot view synced/static lyrics during playback. |
| **DEBT-06** | **Audio Effects / EQ** | AndroidX Media3 ExoPlayer is connected directly to audio output without 10-band equalizer or ReplayGain normalization processing. | **LOW** | Audio volume varies across different YouTube stream sources. |
| **DEBT-07** | **ProGuard Obfuscation Rules** | Retrofit DTOs and kotlinx.serialization models require strict `@Keep` annotations to prevent R8 field stripping in production release builds. | **LOW** | Potential serialization crashes if R8 rules are missing for new DTO classes. |

---

## 2. Priority Execution Order for Debt Resolution

```
                  ┌─────────────────────────────────────┐
                  │ 1. Fix DEBT-01 (Railway Deployment) │  ← Priority 0 (Release Blocker)
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │ 2. Fix DEBT-02 (Secondary Provider) │  ← Priority 1 (High Viability)
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │ 3. Fix DEBT-03 (Robust Download Mgr)│  ← Priority 2 (Offline-First)
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │ 4. Fix DEBT-04 (Automated E2E in CI)│  ← Priority 3 (Quality Gate)
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │ 5. Fix DEBT-05, DEBT-06, DEBT-07   │  ← Priority 4 (Feature Polish)
                  └─────────────────────────────────────┘
```

### Execution Strategy:
1. **Immediate Action (Phase 3):** Deploy Gateway to Railway, update `app/build.gradle.kts` build flavors with public Railway domain, verify TLS.
2. **Short-Term (Phase 2):** Implement `PipedProviderAdapter` or `InvidiousProviderAdapter` as secondary failover provider in Gateway.
3. **Mid-Term (Phase 2):** Refactor `TrackDownloadManager` to use WorkManager / DownloadManager with network constraints (`UNMETERED`).
4. **Long-Term (Phase 4):** Add Maestro / UIAutomator E2E test runner to `.github/workflows/ci.yml`.
