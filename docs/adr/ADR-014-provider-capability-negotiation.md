# ADR-014: Provider Capability & Feature Negotiation

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Architecture Redesign & Provider Abstraction  

## Context

Different music sources (**YouTube Music**, **Jellyfin**, **Navidrome**, **Spotify**, **Piped**) vary in supported features. For instance, YouTube supports playback and recommendations but not offline downloads; Jellyfin and Navidrome support downloads and custom libraries but limited recommendations; Spotify supports radio and lyrics but restricted web stream extraction.

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

The gateway exposes `GET /providers/capabilities` so the Android UI queries capability matrices once during startup and dynamically enables/disables UI features (e.g. download buttons, radio tabs) without faking unsupported operations.

---

### 2. Contextual Adapter Invocations (`ProviderContext`)

Adapter calls MUST receive a contextual execution environment to avoid leaking Android-specific assumptions into provider implementations:

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
       │   (Fastify Routes, Input Validation, Trace)  │
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
       │  (Health Scoring 0-100, Latency, Capability) │
       └──────────────────────┬───────────────────────┘
                              │
                              ▼
       ┌──────────────────────────────────────────────┐
       │               Provider Registry              │
       │ (Auto-Discovered ProviderAdapter Plugins)    │
       └──────────────────────────────────────────────┘
```

1. **API Layer**: Fastify HTTP handlers, input validation, request `Trace ID` injection.
2. **Gateway Services**: Distributed caching (separate namespaces for Search, Metadata, Artwork, Provider Health), request tracing.
3. **Provider Selection Engine**: Dynamically routes requests based on health scores ($0 - 100$), latency, capability support, and content availability. Transparent to the client — provider failover is invisible to the Android UI.
4. **Provider Registry & Adapters**: Auto-discovered provider plugins (`YouTubeAdapter`, `PipedAdapter`, `JellyfinAdapter`, `NavidromeAdapter`).

---

### 4. Direct-to-CDN Streaming Strategy

The gateway **NEVER** proxies audio data bytes through Node.js.
- **Gateway Responsibility**: Resolves signed stream URLs, format tokens, and headers.
- **Client Responsibility**: The Android application (`ExoPlayer`) streams directly from the provider's CDN using the returned signed URL and request headers.
- Prevents the gateway from becoming a network bandwidth bottleneck.

---

### 5. Health Scoring ($0 - 100$) & Failover Routing

Instead of binary `Healthy/Unhealthy` flags, the **Provider Selection Engine** computes a continuous health score ($S \in [0, 100]$):
$$S = \text{BaseScore} - (\text{FailureRate} \times 50) - (\text{P95LatencyMs} / 100)$$

- Score $> 80$: **Primary** routing choice.
- Score $30 - 80$: **Degraded** (secondary fallback).
- Score $< 30$: **Circuit Open** (bypassed until health probe recovers).

---

## Consequences

### Positive
- **Heterogeneous Provider Support**: Adapters declare exact capabilities; UI adapts dynamically without runtime crashes or fake fallbacks.
- **Transparent Failover**: Client receives clean responses without exposing backend retry/failover attempts.
- **Zero Gateway Bandwidth Bottlenecks**: Direct-to-CDN streaming ensures gateway resources are reserved for lightweight metadata resolution.
- **End-to-End Tracing**: `Trace ID` correlation across gateway layers and loggers.

### Negative / Mitigations
- Requires explicit capability checks in gateway services before delegating to adapters (enforced by the `Provider Selection Engine`).

---

## Referenced Documents
- `docs/adr/ADR-012-clibeats-gateway-provider-architecture.md`
- `docs/adr/ADR-013-provider-plugin-architecture.md`
- `domain/provider/MusicProvider.kt`
