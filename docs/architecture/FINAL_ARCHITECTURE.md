# FINAL_ARCHITECTURE.md — CliBeats Master System Architecture

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Final Architectural Specification — No Code Changes  
> **Date:** 2026-08-09

---

## 1. System Overview & Architectural Vision

CliBeats is a free, open-source, privacy-first, local-first Android music client with a Terminal User Interface (TUI) design language.  
The system architecture decouples media playback on Android from upstream music source complexity using a lightweight **Provider Gateway** middleware.

---

## 2. High-Level System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID MOBILE CLIENT                           │
│                                                                        │
│  ┌───────────────────────┐             ┌────────────────────────────┐  │
│  │   Jetpack Compose     │             │   AndroidX Media3 /        │  │
│  │   TUI Presentation    │◄───────────►│   ExoPlayer Engine         │  │
│  └───────────┬───────────┘             └─────────────┬──────────────┘  │
│              │                                       │                 │
│              ▼                                       ▼                 │
│  ┌───────────────────────┐             ┌────────────────────────────┐  │
│  │   Room Persistence    │             │   GatewayMusicProvider     │  │
│  │   (Library, Playlists)│             │   (Retrofit Client)        │  │
│  └───────────────────────┘             └─────────────┬──────────────┘  │
└──────────────────────────────────────────────────────┼─────────────────┘
                                                       │ HTTPS (API / Stream Proxy)
                                                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     RAILWAY PROVIDER GATEWAY                           │
│                                                                        │
│  ┌───────────────────────┐             ┌────────────────────────────┐  │
│  │  Fastify REST Engine  │────────────►│  Redis Cache Engine        │  │
│  │  (Port 8080)          │             │  (Stream URLs, Artwork)    │  │
│  └───────────┬───────────┘             └────────────────────────────┘  │
│              │                                                         │
│              ▼                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │             ProviderSelectionEngine & Failover Router            │  │
│  └──────┬──────────────────────┬──────────────────────┬─────────────┘  │
│         │                      │                      │                │
│         ▼                      ▼                      ▼                │
│  ┌──────────────┐      ┌──────────────┐       ┌──────────────┐         │
│  │ YouTube      │      │ Piped        │       │ OpenSubsonic │         │
│  │ Adapter      │      │ Adapter      │       │ Adapter      │         │
│  └──────┬───────┘      └──────┬───────┘       └──────┬───────┘         │
└─────────┼─────────────────────┼──────────────────────┼─────────────────┘
          │                     │                      │
          ▼                     ▼                      ▼
   YouTube InnerTube       Piped REST API        Navidrome / Local
```

---

## 3. Key Architectural Pillars

### A. Android Client Architecture (`MVVM + Clean Architecture`)
- **Presentation Layer:** Jetpack Compose screens (`SearchScreen`, `PlayerScreen`, `LibraryScreen`, `SettingsScreen`) styled with monospaced JetBrains Mono typography, dark surfaces (`#0D0D0D`), and crisp contrast.
- **Domain Layer:** Pure Kotlin use cases, entity models (`Track`, `Playlist`, `Queue`), and repository interfaces.
- **Data Layer:**
  - `Room Database`: Encrypted local persistence for songs, playlists, queue, history, and cache index.
  - `GatewayMusicProvider`: Implements `MusicProvider` via Retrofit REST calls to the gateway.
  - `CacheManager`: LRU disk cache with automatic fallback to offline audio files when network connectivity is lost.

### B. Gateway Architecture (`Node.js / Fastify / TypeScript`)
- **Stateless Design:** All session state resides in Redis or on the client device.
- **InnerTube Abstraction:** Uses `youtubei.js` with `ClientType.ANDROID_VR` to resolve WebM/Opus audio streams without 403 authorization drops.
- **Range-Safe Stream Proxy:** `/api/v1/stream/proxy/:trackId` relays partial HTTP 206 chunk requests to ExoPlayer seamlessly.

### C. Storage & Portability Architecture
- **Local-First Data Sovereignty:** Library state is 100% stored in Room SQLite DB on device.
- **`.clibeats` Archive Export:** ZIP-compressed JSON schema containing playlists, history, liked tracks, and settings with SHA-256 checksum validation.

### D. Hosting & Operations Architecture (Railway)
- **Deployment:** Containerized Node.js application running on Railway PaaS.
- **Observability:** Pino structured JSON logging, Prometheus metrics exporter (`/metrics`), machine-readable health probes (`/health`).

---

## 4. Compliance & Quality Verification Summary

All 10 required architectural planning deliverables have been generated in `docs/architecture/`:
1. [`MASTER_ROADMAP.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/MASTER_ROADMAP.md) — 5-Phase Product Vision
2. [`PROVIDER_STRATEGY.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/PROVIDER_STRATEGY.md) — Provider Matrix & Adapter Specification
3. [`HOSTING_PLAN_RAILWAY.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/HOSTING_PLAN_RAILWAY.md) — Railway Deployment Checklist & Docker Spec
4. [`PORTABLE_LIBRARY_SPEC.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/PORTABLE_LIBRARY_SPEC.md) — `.clibeats` Export/Import Schema
5. [`PRIVACY_MODEL.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/PRIVACY_MODEL.md) — Zero-Surveillance & GDPR Compliance
6. [`PRODUCTION_OPERATIONS.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/PRODUCTION_OPERATIONS.md) — Logging, Metrics & Circuit Breakers
7. [`TECHNICAL_DEBT.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/TECHNICAL_DEBT.md) — Risk Audit & Resolution Order
8. [`EXECUTION_ORDER.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/EXECUTION_ORDER.md) — Sequential Phase Roadmap
9. [`RISK_REGISTER.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/RISK_REGISTER.md) — Risk Evaluation Matrix
10. [`FINAL_ARCHITECTURE.md`](file:///c:/Users/OM%20Prakash/Documents/clibeats/docs/architecture/FINAL_ARCHITECTURE.md) — Master Architecture Document
