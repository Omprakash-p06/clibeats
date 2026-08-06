# ADR-005: YouTube Music Provider via InnerTube API

**Date:** 2026-08-05
**Status:** Accepted
**Phase:** 5 — Provider Integration & Search

## Context

CLIBeats requires a concrete `MusicProvider` implementation to satisfy `REQ-MUS-01` (provider-agnostic track search) and `REQ-SET-01` (multi-provider plugin architecture). YouTube Music was chosen as the default provider due to its comprehensive music catalog and free access (no API key required for basic searches).

## Decision

### 1. InnerTube API (unofficial YouTube Music internal API)
- **Base URL:** `https://music.youtube.com/youtubei/v1/`
- **Authentication:** Unauthenticated browser client (`clientName: "WEB_REMIX"`, `clientVersion: "1.20240101.01.00"`)
- **No API key or quota limits** for unauthenticated song searches
- **Reference spec:** `sigma67/ytmusicapi` Python library used as implementation guide for request/response shapes

### 2. HTTP Client: OkHttp 4.12.0 + Retrofit 2.11.0
- `OkHttp` for interceptor-based header injection and logging
- `Retrofit` for declarative interface-based HTTP calls
- `kotlinx.serialization-json 1.7.1` for JSON deserialization with `ignoreUnknownKeys = true` (InnerTube responses contain hundreds of undocumented fields)
- `retrofit2-kotlinx-serialization-converter 1.0.0` as bridge

### 3. Search Response Parsing Strategy
The InnerTube search response JSON is deeply nested and frequently changes shape. Rather than annotating the full tree as data classes, we use `JsonElement` for the `contents` tree and navigate it imperatively in `TrackMapper` with null-safe extensions. This trades compile-time safety for resilience to API shape changes.

### 4. Stream URL Strategy (Phase 5 only)
In Phase 5, stream URLs are fetched fresh via `/player` on each play. URLs expire in ~6 hours. Persistent caching of stream URLs with `n` parameter deobfuscation is deferred to Phase 7.

### 5. Image Loading: Coil 2.7.0
`coil-compose` is used for asynchronous artwork loading in `SearchScreen` and `SongTableRow`.

## Consequences

### Positive
- Zero cost, zero API key management for Phase 5 search functionality
- `MusicProvider` interface is fulfilled — adding Spotify/SoundCloud providers in future phases requires only a new `@Binds` in `ProviderModule`
- `kotlinx.serialization` is consistent with project's Kotlin-first toolchain

### Negative / Risks
- InnerTube is an unofficial API — YouTube may change response shapes or rate-limit without notice
- Stream URL parsing may break if YouTube changes their player response format
- No OAuth token support in Phase 5 — personalized results (library, playlists) not available until Phase 6

### Mitigations
- `ignoreUnknownKeys = true` on `Json` instance
- Defensive null-safe navigation in `TrackMapper` — partial responses return partial data, not crashes
- `ProviderResult.Error` path cleanly surfaces failures to the UI
- Full provider is replaceable via `ProviderModule` with zero changes to domain or presentation layers

## Referenced Files
- `domain/provider/MusicProvider.kt` — interface contract
- `data/provider/YouTubeMusicProvider.kt` — InnerTube implementation
- `data/provider/api/InnerTubeApi.kt` — Retrofit API interface
- `data/provider/mapper/TrackMapper.kt` — JSON → Track parsing
- `di/NetworkModule.kt` — OkHttp/Retrofit Hilt wiring
- `di/ProviderModule.kt` — MusicProvider binding
