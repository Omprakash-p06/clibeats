# ADR-013: Provider Plugin Architecture & Modular Gateway Design

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Architecture Redesign & Provider Abstraction  

## Context

Following **ADR-012** (Provider Gateway Architecture), the **Provider Gateway** requires a modular, resilient design to orchestrate multiple music sources (**YouTube**, **Piped**, **Jellyfin**, **Navidrome**, **Spotify**) without tight coupling.

To prevent the gateway from turning into a monolithic YouTube scraper and to ensure high availability during upstream rate-limiting or API enforcement changes, we establish a formal **Provider Plugin Architecture**.

---

## Decision

### 1. Internal `ProviderAdapter` Interface Contract

All music source integrations MUST implement the internal `ProviderAdapter` TypeScript interface. The gateway core interacts with providers exclusively through this interface:

```typescript
export interface ProviderAdapter {
  readonly id: string;
  readonly name: string;
  readonly priority: number;

  search(query: string, filterSongs?: boolean): Promise<Track[]>;
  stream(trackId: string): Promise<StreamResult>;
  album(albumId: string): Promise<AlbumDetails>;
  artist(artistId: string): Promise<ArtistDetails>;
  playlist(playlistId: string): Promise<PlaylistDetails>;
  healthCheck(): Promise<AdapterHealth>;
}
```

---

### 2. Modular Gateway Directory Structure

```text
gateway/
├── docker-compose.yml
├── package.json
└── src/
    ├── api/                  # Fastify REST endpoints & HTTP handlers
    ├── core/                 # Gateway core infrastructure
    │   ├── cache/            # Redis caching layer (Metadata vs Playback)
    │   ├── circuit/          # Circuit breaker, retries & backoff logic
    │   ├── metrics/          # Prometheus metrics exporter
    │   └── router/           # ProviderManager & priority failover router
    └── providers/            # Plugin adapters
        ├── youtube/          # YouTubeAdapter (YouTube.js, PO Token helper)
        ├── piped/            # PipedAdapter (Piped REST API fallback)
        ├── jellyfin/         # JellyfinAdapter
        └── navidrome/        # NavidromeAdapter
```

---

### 3. Strict Separation of Metadata vs. Playback

- **Metadata Operations** (`/search`, `/album`, `/artist`, `/playlist`):
  - Aggressively cached in **Redis** with a 24-hour TTL.
  - Safe for caching across all clients.
- **Playback Operations** (`/stream/:id`):
  - Bypasses long-term metadata caching.
  - Stream URLs and signed tokens are short-lived, session-bound, and video-bound; cached only briefly (e.g. 5–15 minutes) or resolved fresh per play request.

---

### 4. Circuit Breakers, Retries & Fallback Priority

1. **Priority-Based Provider Routing**:
   The `ProviderManager` queries registered adapters in order of priority:
   $$\text{Primary (YouTubeAdapter)} \longrightarrow \text{Fallback 1 (PipedAdapter)} \longrightarrow \text{Fallback 2 (JellyfinAdapter)}$$
2. **Circuit Breaker Mechanics**:
   - If an adapter encounters 3 consecutive upstream failures (HTTP 429, 502, or `LOGIN_REQUIRED`), its circuit breaker trips to `OPEN`.
   - Incoming requests automatically skip `OPEN` adapters and route to the next priority provider.
   - Exponential backoff attempts half-open probes after a 60-second cooldown.

---

### 5. Observability & Health Standards

The gateway exposes standard Prometheus-compatible endpoints:
- **`GET /health`**: Aggregate health status across all registered provider adapters.
- **`GET /metrics`**: Prometheus metrics (request counts, latency histograms, cache hit ratios, circuit breaker states).
- **`GET /providers`**: Active provider status, priorities, and health metrics.
- **`GET /version`**: Gateway and adapter plugin version information.

---

## Consequences

### Positive
- **Complete Decoupling**: Adding or updating a provider plugin (e.g., adding Jellyfin or upgrading YouTube.js) requires zero changes to the core gateway or Android application.
- **High Availability**: Circuit breakers and multi-provider fallbacks ensure song streaming and search remain functional even if YouTube rate-limits a specific IP or client context.
- **Production Observability**: Full metric visibility via Prometheus and Grafana dashboards.

### Negative / Mitigations
- Requires Redis dependency for metadata caching (mitigated by docker-compose template for local/server setup).

---

## Referenced Documents
- `docs/adr/ADR-012-clibeats-gateway-provider-architecture.md`
- `domain/provider/MusicProvider.kt`
