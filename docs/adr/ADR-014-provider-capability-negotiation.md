# ADR-014: Provider Capability & Feature Negotiation

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Architecture Redesign & Provider Abstraction  

## Context

Different music sources (**YouTube Music**, **Jellyfin**, **Navidrome**, **Spotify**, **Piped**) vary in supported features (e.g. YouTube supports playback and recommendations but not offline downloads; Jellyfin and Navidrome support downloads and custom libraries but limited recommendations; Spotify supports radio and lyrics but restricted web stream extraction).

Assuming every provider supports identical methods (`search`, `stream`, `album`, `playlist`, `lyrics`, `downloads`) forces adapters to return dummy fallbacks or fake unsupported operations.

---

## Decision

### 1. `ProviderCapabilities` Feature Matrix

Every `ProviderAdapter` MUST declare its explicit capability matrix via the `ProviderCapabilities` metadata contract:

```typescript
export interface ProviderCapabilities {
  search: boolean;
  playback: boolean;
  playlists: boolean;
  albums: boolean;
  artists: boolean;
  recommendations: boolean;
  radio: boolean;
  downloads: boolean;
  lyrics: boolean;
}
```

---

### 2. Contextual Adapter Invocations (`ProviderContext`)

Adapter calls MUST receive a contextual execution environment:

```typescript
export interface ProviderContext {
  country: string;
  language: string;
  authenticated: boolean;
  preferredAudioQuality: 'LOW' | 'MEDIUM' | 'HIGH' | 'LOSSLESS';
  device: 'mobile' | 'desktop';
  traceId: string;
}

export interface ProviderAdapter {
  readonly id: string;
  readonly name: string;
  readonly capabilities: ProviderCapabilities;

  search(query: string, context: ProviderContext): Promise<Track[]>;
  stream(trackId: string, context: ProviderContext): Promise<StreamResult>;
  album(albumId: string, context: ProviderContext): Promise<AlbumDetails>;
  artist(artistId: string, context: ProviderContext): Promise<ArtistDetails>;
  playlist(playlistId: string, context: ProviderContext): Promise<PlaylistDetails>;
  healthCheck(): Promise<AdapterHealth>;
}
```

---

### 3. Four-Layer Gateway Architecture

```text
       ┌──────────────────────────────────────────────┐
       │                  API Layer                   │
       │   (Fastify /api/v1 Routes, Input, Tracing)   │
       └──────────────────────┬───────────────────────┘
                              │
                              ▼
       ┌──────────────────────────────────────────────┐
       │               Gateway Services               │
       │  (Separated Caches, Auth, Tracing, Metrics)  │
       └──────────────────────┬───────────────────────┘
                              │
                              ▼
       ┌──────────────────────────────────────────────┐
       │           Provider Selection Engine          │
       │  (Adaptive Multi-Factor Health Scoring)      │
       └──────────────────────┬───────────────────────┘
                              │
                              ▼
       ┌──────────────────────────────────────────────┐
       │               Provider Registry              │
       │ (Auto-Discovered ProviderAdapter Plugins)    │
       └──────────────────────────────────────────────┘
```

---

### 4. Adaptive Provider Selection Engine & Health Scoring

Instead of binary flags or static priority, the **Provider Selection Engine** computes a dynamic adaptive score ($S$):

$$\text{Score} = \text{Health} + \text{Availability} + \text{Capability} + \text{LatencyScore} + \text{UserPreference} - \text{Penalty}$$

- Incoming client requests route to the highest-scoring available adapter.
- Failover across registered adapters is **transparent and invisible** to the client.

---

### 5. Direct-to-CDN Streaming & Expiration Refresh

- The gateway resolves short-lived signed stream URLs and headers, but **never proxies audio bytes**.
- The Android client streams directly from the provider's CDN.
- If a signed URL expires mid-playback or returns HTTP 403, the Android client automatically requests a URL refresh from `POST /api/v1/stream` before retrying playback.

---

### 6. Isolated Redis Cache Namespaces

Redis keys are strictly segmented into dedicated logical namespaces:
- `metadata:` (Track, album, artist, playlist metadata)
- `search:` (Search query response cache)
- `albums:` (Album tracklists and details)
- `artists:` (Artist profiles and discographies)
- `playlists:` (Playlist structures)
- `provider-health:` (Provider health scores and circuit breaker states)
- `session:` (Provider authentication sessions)
- `artwork:` (Image and artwork thumbnail cache)

---

### 7. Canonical Gateway Core Endpoints

The gateway exposes a minimalist initial API surface under `/api/v1/`:

- `GET  /api/v1/bootstrap` (Aggregated initialization context, capabilities, server version)
- `GET  /api/v1/search`
- `GET  /api/v1/album/{id}`
- `GET  /api/v1/artist/{id}`
- `GET  /api/v1/playlist/{id}`
- `POST /api/v1/stream` (Resolves direct stream URL and headers)
- `GET  /health` (Prometheus health check)
- `GET  /metrics` (Prometheus metrics exporter)
- `GET  /version` (Gateway version & active plugins)

---

## Consequences

### Positive
- **Heterogeneous Provider Support**: Adapters declare capabilities explicitly; client queries `/bootstrap` once during cold start and adapts UI dynamically.
- **Zero Gateway Bandwidth Bottleneck**: Direct-to-CDN streaming keeps gateway memory and CPU lightweight.
- **Transparent Failover**: Client receives clean responses regardless of backend provider retries or circuit breaker trips.

---

## Referenced Documents
- `docs/adr/ADR-012-clibeats-gateway-provider-architecture.md`
- `docs/adr/ADR-013-provider-plugin-architecture.md`
- `docs/adr/ADR-020-api-versioning-bootstrap.md`
