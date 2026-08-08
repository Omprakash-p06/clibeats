# PRIVACY_MODEL.md — CliBeats Privacy & Data Protection Model

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Architecture Specification — No Code Changes  
> **Date:** 2026-08-09

---

## Core Privacy Guarantee

CliBeats adheres to a zero-surveillance, local-first engineering model:
1. **Zero Advertisements** — No ad networks, no sponsored content SDKs.
2. **Zero Telemetry by Default** — No analytics, no user tracking, no event funnel collection.
3. **Zero Device Fingerprinting** — No Android Advertising ID (AAID), hardware serial, or MAC address reads.
4. **Zero Cloud Account** — No login screen, no email collection, no remote user profile.
5. **Local Data Sovereignty** — All library data, history, and configuration remain exclusively on the user's physical device.

---

## 1. Data Collection & Network Exposure Matrix

| Data Type | Stored Locally? | Transmitted Remotely? | Destination |
|---|---|---|---|
| Playlists & Liked Songs | Encrypted Room DB | **NEVER** | None |
| Playback History | Encrypted Room DB | **NEVER** | None |
| User Preferences | DataStore | **NEVER** | None |
| Search Queries | Search Cache (TTL 24h) | Only when searching | Gateway Proxy / Provider |
| Stream Requests | Stream Cache (TTL 15m) | Only when playing track | Gateway Proxy / Provider |
| Crash Logs | Local Timber file log | **NEVER** (unless user manually exports) | None |

---

## 2. Gateway Proxy Privacy Protections

To protect the user's mobile device IP address and identity when querying third-party providers (e.g. YouTube):
1. **User-Agent Normalization:** The gateway overwrites user-agent headers to a standardized, generic string. Mobile device hardware details are stripped.
2. **IP Obfuscation:** Provider requests originate from the Gateway server IP, masking the Android client's physical location from YouTube servers.
3. **No Request Persistence:** The gateway does not log full client IPs or search queries to permanent disk storage. Pino logs use structured, anonymized trace IDs.

---

## 3. Opt-In Anonymous Diagnostics (Optional Debug Mode)

If a user explicitly enables **"Verbose Diagnostics"** in Settings:
- Logs are written locally to `app_diagnostics.log`.
- PII (Personally Identifiable Information), bearer tokens, authorization headers, and IP addresses are **automatically redacted** via `TimberCrashReporter` sanitization rules before writing.
- Diagnostics are **never automatically uploaded**. The user must explicitly export and upload the log file manually.

---

## 4. Legal Compliance & GDPR Assessment

| GDPR Principle | CliBeats Architecture Compliance |
|---|---|
| **Lawfulness, Fairness, Transparency** | Fully compliant. No personal data processed or sold. Transparent open-source code. |
| **Purpose Limitation** | Network traffic is strictly limited to media streaming and metadata retrieval requested directly by user. |
| **Data Minimization** | Zero user data requested beyond what is required to display music titles and play audio. |
| **Storage Limitation** | Local caches enforce automatic LRU eviction (configurable 256MB–2GB limits). |
| **Integrity & Confidentiality** | Sensitive key-value configuration encrypted with Android Keystore MasterKey (AES256_GCM). |
| **Right of Access / Erasure** | User can clear database, purge caches, or delete app with 100% data destruction guaranteed. |

---

## 5. F-Droid & Open-Source Compliance

- **No Proprietary Dependencies:** The release build flavor uses zero Google Play Services (no Firebase, no AdMob, no Google Analytics).
- **Reproducible Build:** Source code can be built end-to-end using standard Gradle tools (`./gradlew assembleRelease`).
- **F-Droid Anti-Features Audit:** CliBeats triggers zero F-Droid Anti-Features flags (`DisabledAlgorithm`, `Tracking`, `NonFreeNet`).
