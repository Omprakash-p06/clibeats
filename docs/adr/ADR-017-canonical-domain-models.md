# ADR-017: Canonical Domain Models & DTO Translation

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Provider Gateway Architecture  

## Context

Upstream music sources represent tracks, albums, artists, playlists, and streams in incompatible JSON formats (e.g. YouTube uses `musicResponsiveListItemRenderer` / `videoDetails`; Spotify uses `TrackObject`; Jellyfin uses `BaseItemDto`).

Exposing raw provider DTOs to the mobile app requires writing custom UI mappers for every provider and breaks client-side persistence models.

---

## Decision

### 1. Gateway Canonical Domain Schemas

The gateway defines a unified TypeScript domain schema that all adapters MUST map into:

```typescript
export interface Track {
  id: string;
  providerId: string;
  title: string;
  artist: string;
  album?: string;
  durationSeconds: number;
  artworkUrl?: string;
  explicit?: boolean;
}

export interface Album {
  id: string;
  providerId: string;
  title: string;
  artist: string;
  artworkUrl?: string;
  trackCount: number;
  releaseYear?: number;
  tracks: Track[];
}

export interface Artist {
  id: string;
  providerId: string;
  name: string;
  avatarUrl?: string;
  bio?: string;
}

export interface Playlist {
  id: string;
  providerId: string;
  title: string;
  description?: string;
  artworkUrl?: string;
  trackCount: number;
  tracks: Track[];
}

export interface StreamResult {
  trackId: string;
  streamUrl: string;
  mimeType: string;
  bitrateKbps?: number;
  expiresAtEpochSeconds: number;
  headers?: Record<string, string>;
}
```

---

### 2. Adapter Translation Boundary

- Adapters implement bi-directional mappers (`UpstreamDTO -> CanonicalModel`).
- The gateway REST API returns ONLY canonical models.
- The Android client's `domain/model/Track.kt` maps 1:1 with the gateway's canonical `Track` model.

---

## Consequences

### Positive
- **Domain Stability**: The Android client and TUI presentation components deal strictly with clean domain models.
- **Provider Interchangeability**: Adding a new provider adapter requires only mapping its DTOs into `Track`, `Album`, `Artist`, and `Playlist`.

---

## Referenced Documents
- `docs/adr/ADR-013-provider-plugin-architecture.md`
- `domain/model/Track.kt`
