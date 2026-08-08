# RISK_REGISTER.md — CliBeats Comprehensive Risk Register

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Risk Assessment — No Code Changes  
> **Date:** 2026-08-09

---

## Risk Evaluation Matrix

| Risk ID | Category | Risk Description | Probability | Impact | Mitigation Strategy | Contingency Plan |
|---|---|---|---|---|---|---|
| **RISK-01** | **API / External** | YouTube changes InnerTube API schemas or blocks `ANDROID_VR` client headers. | High | High | Decouple Android app behind Gateway proxy. Update `youtubei.js` dependency on Gateway without touching Android app. | Fall back automatically to Piped / Invidious or `yt-dlp` fallback adapters. |
| **RISK-02** | **Infrastructure** | Render.com free tier resource caps reached or IP range throttled by YouTube. | Medium | Medium | Stateless Gateway design allows instant deployment to alternative host (Fly.io, Railway, self-hosted VPS). | Allow users to enter custom Gateway URL in App Settings. |
| **RISK-03** | **Android OS** | Android 15/16 background service restrictions kill ExoPlayer `MediaSessionService`. | Medium | High | Use official AndroidX Media3 `MediaSessionService` with proper foreground notification types (`type="mediaPlayback"`). | Implement WorkManager watchdog service for queue preservation. |
| **RISK-04** | **Legal / Distribution** | Play Store policy rejection or DMCA takedown request against repository. | Low | High | CliBeats contains zero copyright media files and zero proprietary API keys. F-Droid is primary distribution channel. | Maintain direct GitHub Releases APK distribution and F-Droid package. |
| **RISK-05** | **Performance** | Memory exhaustion or OOM crashes on low-end devices during large playlist rendering. | Low | Medium | Room pagination (`Paging3`), Coil image loader memory caps (25% max memory budget), Compose lazy lists. | Force low-memory TUI mode (disable image thumbnails). |
| **RISK-06** | **Security / Privacy** | Third-party dependencies introduce tracking telemetry or vulnerability. | Low | High | CI pipeline enforces vulnerability scanning (`npm audit`, `gradle dependencyCheck`, Detekt rules). | Zero external analytics SDKs allowed in `build.gradle.kts`. |
| **RISK-07** | **Data Loss** | Android OS purges application data or Room database corruption during update. | Low | Medium | Keystore-backed backup exclusions, local atomic SQLite transactions, portable archive export. | Automatic daily local DB backup snapshot to app private folder. |
