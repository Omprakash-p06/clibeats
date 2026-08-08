# PROVIDER_STRATEGY.md — CliBeats Provider Ecosystem Design

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Planning Only — No Code Changes  
> **Date:** 2026-08-09

---

## Design Principle

The gateway must **never** be tightly coupled to any single provider.  
Every provider is a **plugin**: a TypeScript class that implements the `MusicProvider` interface.  
Providers are loaded by priority; if one fails, the next is tried.

---

## Provider Interface (Gateway Contract)

```typescript
interface MusicProvider {
  id: string;
  name: string;
  priority: number;
  capabilities: ProviderCapability[];

  search(query: string, ctx: ProviderContext): Promise<Track[]>;
  getTrack(id: string, ctx: ProviderContext): Promise<Track>;
  stream(id: string, ctx: ProviderContext): Promise<StreamResult>;
  healthCheck(): Promise<HealthResult>;
}
```

---

## Provider Evaluation Matrix

### youtubei.js (Current Primary)

| Attribute | Assessment |
|---|---|
| **License** | MIT |
| **Maintenance** | Very active (LuanRT, 4k+ stars, updated weekly) |
| **Reliability** | HIGH — ANDROID_VR client bypasses web API rate limits |
| **API Stability** | MEDIUM — InnerTube is unofficial; youtubei.js abstracts breakages |
| **Search Quality** | EXCELLENT — YouTube Music catalog, 100M+ tracks |
| **Playback Quality** | EXCELLENT — WebM/Opus 139kbps (itag=251), Range-safe HTTP 206 |
| **Metadata Quality** | EXCELLENT — Title, artist, album, artwork, duration |
| **Artwork Quality** | EXCELLENT — High-res YouTube thumbnails |
| **Playlist Support** | YES — YouTube playlists, albums, mixes |
| **Lyrics Availability** | NO (separate integration required) |
| **Rate Limits** | Soft — ANDROID_VR client has looser limits than WEB |
| **Auth Requirements** | None for search/stream; optional cookies for private playlists |
| **Android Integration** | Via gateway HTTP relay — no direct Android dependency |
| **Gateway Integration** | ✅ Current implementation, working in production |
| **Long-Term Viability** | HIGH — YouTube is not disappearing; community will maintain client |

---

### yt-dlp

| Attribute | Assessment |
|---|---|
| **License** | Unlicense (public domain) |
| **Maintenance** | Very active (yt-dlp org, ~80k stars, updated weekly) |
| **Reliability** | HIGH — Battle-tested across 1000+ sites |
| **API Stability** | HIGH — Stable CLI interface; JSON output is consistent |
| **Search Quality** | EXCELLENT — YouTube, SoundCloud, Bandcamp, etc. |
| **Playback Quality** | EXCELLENT — Best format selection logic in ecosystem |
| **Metadata Quality** | EXCELLENT — Comprehensive metadata extraction |
| **Artwork Quality** | EXCELLENT |
| **Playlist Support** | EXCELLENT |
| **Lyrics Availability** | YES (via `--write-subs` for some platforms) |
| **Rate Limits** | Manageable with cookies/throttling |
| **Auth Requirements** | Optional cookies for age-restricted/private content |
| **Android Integration** | Via gateway process spawn or yt-dlp-nodejs wrapper |
| **Gateway Integration** | MEDIUM — Spawning subprocess adds latency (~500ms cold) |
| **Long-Term Viability** | VERY HIGH — Widest platform coverage, excellent community |
| **Notes** | Best for fallback and multi-site support; not ideal for real-time search latency |

---

### Piped

| Attribute | Assessment |
|---|---|
| **License** | AGPL-3.0 |
| **Maintenance** | Active (TeamPiped, ~8k stars) |
| **Reliability** | MEDIUM — Dependent on instance health |
| **API Stability** | MEDIUM — REST API, documented, reasonably stable |
| **Search Quality** | GOOD — YouTube proxy, returns same results |
| **Playback Quality** | GOOD — Pixi-generated stream URLs |
| **Metadata Quality** | GOOD |
| **Artwork Quality** | GOOD |
| **Playlist Support** | YES |
| **Lyrics Availability** | NO |
| **Rate Limits** | Instance-dependent; public instances may throttle |
| **Auth Requirements** | None (public instances) |
| **Android Integration** | Via gateway HTTP — simple REST calls |
| **Gateway Integration** | EASY — Standard REST API |
| **Long-Term Viability** | MEDIUM — Instance-based; if all public instances close, becomes self-hosted only |

---

### Invidious

| Attribute | Assessment |
|---|---|
| **License** | AGPL-3.0 |
| **Maintenance** | Active (inv-sig-helper project active) |
| **Reliability** | MEDIUM — Instance-dependent |
| **API Stability** | HIGH — REST API is well-documented and stable |
| **Search Quality** | GOOD — YouTube proxy |
| **Playback Quality** | GOOD |
| **Metadata Quality** | GOOD |
| **Artwork Quality** | GOOD |
| **Playlist Support** | YES |
| **Lyrics Availability** | NO |
| **Rate Limits** | Instance-dependent |
| **Auth Requirements** | None for public content |
| **Android Integration** | Via gateway REST |
| **Gateway Integration** | EASY |
| **Long-Term Viability** | MEDIUM — Same instance-dependency risk as Piped |

---

### Jellyfin

| Attribute | Assessment |
|---|---|
| **License** | GPL-2.0 |
| **Maintenance** | Very active (~35k stars) |
| **Reliability** | VERY HIGH — Self-hosted, user controls instance |
| **API Stability** | HIGH — Stable REST API, versioned |
| **Search Quality** | EXCELLENT within user's library |
| **Playback Quality** | EXCELLENT — Direct file serving, lossless support |
| **Metadata Quality** | EXCELLENT — User manages metadata |
| **Artwork Quality** | EXCELLENT |
| **Playlist Support** | YES |
| **Lyrics Availability** | YES (via plugin) |
| **Rate Limits** | None (self-hosted) |
| **Auth Requirements** | YES — API token (user sets in CliBeats settings) |
| **Android Integration** | Via gateway Jellyfin REST adapter |
| **Gateway Integration** | MEDIUM — Auth token management required |
| **Long-Term Viability** | VERY HIGH — Self-hosted, no external dependency |

---

### Navidrome

| Attribute | Assessment |
|---|---|
| **License** | GPL-3.0 |
| **Maintenance** | Active (~12k stars) |
| **Reliability** | VERY HIGH — Self-hosted |
| **API Stability** | HIGH — OpenSubsonic API (backward compatible with Subsonic) |
| **Search Quality** | EXCELLENT within user's library |
| **Playback Quality** | EXCELLENT |
| **Metadata Quality** | EXCELLENT |
| **Artwork Quality** | EXCELLENT |
| **Playlist Support** | YES |
| **Lyrics Availability** | YES |
| **Rate Limits** | None |
| **Auth Requirements** | YES — Subsonic credentials |
| **Gateway Integration** | EASY — OpenSubsonic REST API is well-known |
| **Long-Term Viability** | VERY HIGH |

---

### Funkwhale

| Attribute | Assessment |
|---|---|
| **License** | AGPL-3.0 |
| **Maintenance** | Moderate activity |
| **Reliability** | MEDIUM — Instance-dependent |
| **Search Quality** | GOOD |
| **Playback Quality** | GOOD |
| **Playlist Support** | YES (ActivityPub-based federation) |
| **Lyrics Availability** | NO |
| **Auth Requirements** | Optional OAuth2 |
| **Long-Term Viability** | MEDIUM |

---

### Internet Archive

| Attribute | Assessment |
|---|---|
| **License** | Creative Commons (content varies) |
| **Maintenance** | N/A — Stable institution |
| **Reliability** | HIGH — archive.org is permanent |
| **Search Quality** | GOOD for free/public domain music |
| **Playback Quality** | VARIABLE — depends on upload quality |
| **Auth Requirements** | None for public content |
| **Long-Term Viability** | VERY HIGH — Institutional |
| **Notes** | Best for free/public domain music discovery |

---

### Jamendo

| Attribute | Assessment |
|---|---|
| **License** | Creative Commons music catalog |
| **Maintenance** | Active commercial API |
| **Reliability** | HIGH |
| **Search Quality** | GOOD for CC-licensed music |
| **Playback Quality** | GOOD |
| **Auth Requirements** | API key (free tier available) |
| **Long-Term Viability** | MEDIUM — Commercial entity |

---

### Radio Browser

| Attribute | Assessment |
|---|---|
| **License** | MIT API, CC content |
| **Maintenance** | Community maintained |
| **Reliability** | HIGH — Distributed API |
| **Search Quality** | EXCELLENT for radio stations (100k+ stations) |
| **Playback Quality** | Depends on station |
| **Auth Requirements** | None |
| **Long-Term Viability** | HIGH |

---

## Provider Recommendations

### Primary Provider
**`youtubei.js` (ANDROID_VR client via gateway relay)**

*Justification:* Widest catalog (100M+ tracks), free, no auth required, WebM/Opus quality (139kbps), Range-safe relay solves the HTTP 403 long-duration problem. Already proven in production. youtubei.js community actively maintains against InnerTube changes.

### Secondary Provider
**`Piped` (configurable instance URL)**

*Justification:* REST API is simple, no extra dependencies in gateway, provides a secondary YouTube-compatible stream source when youtubei.js is rate-limited. User can point to their own self-hosted Piped instance for full privacy.

### Fallback Provider
**`yt-dlp` (subprocess via gateway)**

*Justification:* Covers 1000+ platforms beyond YouTube. When both primary and secondary fail, yt-dlp's broad platform support ensures continuity. Latency (~500ms cold start) is acceptable for fallback scenarios.

### Self-Hosted Provider
**`Navidrome` (OpenSubsonic API)**

*Justification:* OpenSubsonic is the most widely implemented self-hosted music API. Navidrome is the leading implementation. Subsonic-compatible clients are ubiquitous. The API is stable and versioned. Zero external dependency.

### Future Providers (Phase 5)
- **Jellyfin** — for users with existing Jellyfin media servers
- **Internet Archive** — free, public domain music library
- **Jamendo** — Creative Commons music catalog
- **Radio Browser** — 100k+ radio stations
- **Funkwhale** — federated community music

---

## Gateway Plugin Architecture

```
gateway/
  src/
    providers/
      youtube/          ← Current (youtubei.js ANDROID_VR)
      piped/            ← Phase 2
      yt-dlp/           ← Phase 2 (fallback)
      navidrome/        ← Phase 4 (self-hosted)
      jellyfin/         ← Phase 5
      internet-archive/ ← Phase 5
      jamendo/          ← Phase 5
      radio-browser/    ← Phase 5
    core/
      registry/         ← ProviderRegistry (current)
      selection/        ← ProviderSelectionEngine (current)
      plugins/          ← Phase 4: dynamic plugin loader
```

Each provider is loaded from the registry at startup. Priority determines selection order. Health checks determine circuit breaker state.
