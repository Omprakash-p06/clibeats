# EXECUTION_ORDER.md — Long-Term Development Sequence & Dependency Graph

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Specification Only — No Code Changes  
> **Date:** 2026-08-09

---

## Strategic Dependency Graph

```
Milestone 1 (Stable MVP v1.0.0) — [COMPLETED]
   │
   ▼
Phase 3 (Railway Cloud Hosting)
   ├── Deployment of Gateway to Railway
   ├── HTTPS & Domain Setup
   └── Android Release Build with Public Gateway URL
   │
   ▼
Phase 2 (Feature Complete)
   ├── Secondary Provider (Piped / Invidious)
   ├── Portable Library Export (.clibeats)
   ├── Synced Lyrics (LRCLIB)
   └── Equalizer & Audio Effects
   │
   ▼
Phase 4 (Community Edition)
   ├── Provider Plugin System
   ├── Theme Customization Engine
   ├── QR-Code Local Playlist Sharing
   └── Desktop & Linux MPRIS Client
   │
   ▼
Phase 5 (Future Expansion)
   ├── Self-Hosted Providers (Navidrome / Jellyfin)
   ├── Radio Browser & Internet Archive
   └── Smart Playlists & On-Device ML Recommendations
```

---

## Sequential Execution Plan

### Milestone 2: Cloud Infrastructure & Public Release (Phase 3)
1. **Task 2.1:** Create `gateway/Dockerfile` and `railway.toml`.
2. **Task 2.2:** Deploy Gateway to Railway PaaS with Redis plugin.
3. **Task 2.3:** Rebuild Android Release APK pointing to `https://[railway-domain]/`.
4. **Task 2.4:** Conduct E2E validation gate on public Railway domain.
5. **Task 2.5:** Publish `v1.1.0` release artifacts to GitHub Releases and submit to F-Droid.

### Milestone 3: Multi-Provider & Portability (Phase 2)
1. **Task 3.1:** Build `PipedProviderAdapter` in Gateway with automatic failover.
2. **Task 3.2:** Implement `.clibeats` Portable Library Exporter & Importer in Android app.
3. **Task 3.3:** Add Lyrics service (`LRCLIB` integration) to Player UI.
4. **Task 3.4:** Add 10-band Equalizer and ReplayGain normalization in ExoPlayer engine.
5. **Task 3.5:** Add Android Auto `MediaBrowserServiceCompat` integration.

### Milestone 4: Extensibility & Desktop (Phase 4)
1. **Task 4.1:** Gateway Provider Plugin architecture (load dynamic TypeScript adapters).
2. **Task 4.2:** Custom TUI Theme Token editor in Settings.
3. **Task 4.3:** Local P2P QR Code playlist sharing.
4. **Task 4.4:** Compose Multiplatform Desktop target build setup.

### Milestone 5: Self-Hosted & Local Ecosystem (Phase 5)
1. **Task 5.1:** Add OpenSubsonic (`NavidromeProviderAdapter`) support.
2. **Task 5.2:** Add Jellyfin API support (`JellyfinProviderAdapter`).
3. **Task 5.3:** Add Radio Browser (100k+ radio stations) support.
4. **Task 5.4:** Implement Smart Playlist rules engine in Room database.
