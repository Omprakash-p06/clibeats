# MASTER_ROADMAP.md — CliBeats Long-Term Product Roadmap

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Planning Only — No Code Changes  
> **Owner:** Omprakash Panda  
> **Date:** 2026-08-09

---

## Vision Statement

CliBeats is a **free, open-source, privacy-first Android music client**.  
It does **not** collect analytics, display advertisements, or require an account.  
The user owns all data. Every dependency is replaceable.  
The application is designed to work for **years** with minimal maintenance.

---

## Phase 1 — Stable MVP

**Timeline Estimate:** Complete (Milestone 1 done)

### Objectives
- Working Android music client with ExoPlayer audio playback
- YouTube provider via gateway (ANDROID_VR client, Range-safe relay)
- Local Room persistence (library, playlists, history, queue)
- TUI design system (JetBrains Mono, #0D0D0D dark, #1DB954 accent)
- CI/CD via GitHub Actions
- Release APK signed and distributable

### Deliverables
- ✅ Phases 0–11 (Milestone 1) complete
- ✅ Release APK: `v1.0.0` — SHA256: `F833982492D36DE29A37319D1A9700B7C1FA5AE008C87A05D6B0F6BAEF1DE1BA`
- ✅ Gateway: Node.js / Fastify / youtubei.js ANDROID_VR relay
- ✅ 109 unit tests passing

### Risks
| Risk | Severity | Mitigation |
|---|---|---|
| YouTube InnerTube API breakage | HIGH | youtubei.js maintained, ANDROID_VR stable, gateway abstraction decouples Android from API |
| Gateway not hosted publicly | HIGH | Phase 3 Railway deployment |
| Single provider dependency | MEDIUM | Phase 2 adds secondary provider |

### Success Criteria
- App installs and plays audio on physical Android device
- Search, playback, queue, library work end-to-end
- Gateway proxy resolves Range-safe HTTP 206 on all tracks

---

## Phase 2 — Feature Complete

**Timeline Estimate:** 6–10 weeks after Phase 1

### Objectives
- Second music provider integration (Piped or Invidious)
- Lyrics display (static/synchronized via LRCLIB or local)
- Crossfade and gapless playback
- Full offline-first capability (download manager, cache TTL, eviction)
- Portable library export/import (`.clibeats` archive format)
- Equalizer (10-band, Android AudioEffect API)
- ReplayGain normalization
- Android Auto support
- Wear OS companion (basic playback controls)

### Deliverables
- `ProviderPlugin` interface in gateway (dynamic provider loading)
- `PipedProvider` or `InvidiousProvider` adapter
- `LyricsProvider` abstraction + LRCLIB integration
- `CrossfadeEngine` wrapper around ExoPlayer's `AudioTrack`
- `PortableLibraryExporter` / `PortableLibraryImporter`
- `EqualizerViewModel` + `EqualizerScreen`
- Android Auto MediaBrowserServiceCompat bridge
- `WearOSCompanionService` (Media3 WearOS module)

### Risks
| Risk | Severity | Mitigation |
|---|---|---|
| Piped/Invidious instances go offline | MEDIUM | Instance-based config, user can specify custom instance URL |
| LRCLIB rate limiting | LOW | Local cache of fetched lyrics |
| Android Auto certification requirements | MEDIUM | Use official Media3 CarAppService API |

### Success Criteria
- Two providers selectable in Settings
- Lyrics visible in player screen
- Library export produces valid `.clibeats` archive importable on fresh install
- Android Auto shows Now Playing and basic controls

---

## Phase 3 — Production

**Timeline Estimate:** 4–6 weeks after Phase 2

### Objectives
- Gateway hosted on Render.com (public HTTPS endpoint)
- HTTPS, TLS, domain name configured
- Gateway autoscaling, health monitoring, Prometheus/Grafana
- Production error handling and circuit breakers
- Play Store / F-Droid / GitHub Releases distribution
- GDPR compliance review
- Security audit (OWASP Mobile Top 10)

### Deliverables
- `render.yaml` + `Dockerfile` for gateway
- GitHub Actions CI/CD → Render deploy pipeline
- Grafana dashboard: provider health, stream latency, error rates
- `.github/workflows/deploy-gateway.yml`
- F-Droid compatible build (reproducible, no proprietary SDKs)
- `docs/SECURITY.md` and `docs/PRIVACY_POLICY.md`
- ADR-012: Hosting strategy

### Risks
| Risk | Severity | Mitigation |
|---|---|---|
| Render instance spin-down on free plan | MEDIUM | Use keep-alive pings or upgraded instance plan |
| Play Store review rejection | MEDIUM | F-Droid remains primary distribution; GitHub Releases fallback |
| HTTPS cert renewal failure | LOW | Render manages certs via Let's Encrypt automatically |

### Success Criteria
- Gateway accessible at `https://clibeats-gateway.onrender.com` (or similar)
- Health endpoint returns `HEALTHY` from outside LAN
- App build uses public gateway URL, no hardcoded local IP
- F-Droid submission PR open

---

## Phase 4 — Community Edition

**Timeline Estimate:** 8–12 weeks after Phase 3

### Objectives
- Plugin system: users can add custom providers via URL
- Theme engine: user-defined color palettes
- Shared playlists (local P2P via QR code, no cloud)
- Last.fm scrobbling (opt-in)
- Discord Rich Presence (opt-in)
- Desktop client (Linux/macOS via Compose Multiplatform or TUI via Kotlin/JVM)
- MPRIS D-Bus integration for Linux desktop
- Translations / i18n

### Deliverables
- `PluginManifest.json` spec + gateway plugin loader
- `ThemeEngine` (user-editable token overrides persisted in DataStore)
- QR-based playlist share (JSON encoded, scanned on recipient device)
- `LastFmScrobbler` opt-in module (no data sent unless toggled)
- `DiscordRpcBridge` opt-in module
- Compose Multiplatform desktop target (experimental)
- Linux MPRIS adapter (`org.mpris.MediaPlayer2`)

### Risks
| Risk | Severity | Mitigation |
|---|---|---|
| Plugin security (malicious provider URLs) | HIGH | Sandboxed HTTP-only plugin API, no native code allowed |
| Compose Multiplatform desktop maturity | MEDIUM | Treat as experimental; Android remains primary target |
| Last.fm API changes | LOW | Auth token stored locally, graceful degradation |

### Success Criteria
- Community can submit provider plugins via PR
- User can change accent color and persist theme
- QR playlist share works between two phones on different networks

---

## Phase 5 — Future Expansion

**Timeline Estimate:** Long-term / community driven

### Objectives
- Self-hosted gateway support (user runs own Node.js instance)
- Jellyfin / Navidrome / Funkwhale self-hosted provider
- Internet Archive / Jamendo free music provider
- Smart playlists (rule-based auto-generation from library)
- Radio stations (Radio Browser integration)
- AI recommendations (local ML, on-device — no cloud)
- Casting support (Chromecast via Cast SDK, opt-in)
- Remote playback (companion app controls from another device)

### Deliverables
- Self-hosting documentation (`docs/SELF_HOSTING.md`)
- `JellyfinProvider` + `NavidromeProvider` adapters
- `InternetArchiveProvider` + `JamendoProvider`
- `SmartPlaylistEngine` (filter by BPM, genre, year, play count)
- `RadioBrowserProvider`
- `CastManager` (Chromecast integration, opt-in)

### Risks
| Risk | Severity | Mitigation |
|---|---|---|
| Jellyfin API incompatibility across versions | LOW | Version-pinned adapter, user specifies Jellyfin URL |
| Chromecast SDK adds proprietary dependency | MEDIUM | Feature-flagged, excluded from F-Droid build flavor |

### Success Criteria
- User can run full stack locally with zero external dependencies
- Smart playlists auto-populate from local library
