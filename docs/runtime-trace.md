# CliBeats Runtime Structured Logging & Trace Audit

This document defines the runtime structured logging specifications and trace correlation model across both the Android application and the Fastify Provider Gateway.

---

## 1. Trace ID Correlation Model

Every request originating from the user interface generates or attaches a unique **Trace ID** (`trace-XXXXXXXX`), which is propagated across all layers:

```text
Compose UI (SearchViewModel)
  │ (traceId)
  ▼
PlaybackRepositoryImpl / GatewayMusicProvider
  │ (HTTP Header: x-trace-id)
  ▼
Fastify Provider Gateway (app.ts)
  │ (Context: req.traceId)
  ▼
YouTubeProviderAdapter / youtubei.js
```

---

## 2. Event Types & Standard Schema

All log events emit single-line JSON with the following fields:

```json
{
  "timestamp": 1786188100000,
  "traceId": "trace-a1b2c3d4",
  "event": "SearchRequest",
  "query": "Wonderwall",
  "durationMs": 420
}
```

### Event Taxonomies

| Event Name | Source | Description |
| :--- | :--- | :--- |
| `SearchRequest` | Android `SearchViewModel` | User submitted search query. |
| `SearchResponse` | Android `GatewayMusicProvider` | Search results received from Gateway with track count and latency. |
| `TrackSelected` | Android `PlaybackRepositoryImpl` | User selected a track from the results list. |
| `StreamRequest` | Android `PlaybackRepositoryImpl` | App initiated direct-to-CDN stream resolution request. |
| `StreamResolved` | Android `GatewayMusicProvider` | Direct CDN URL resolved from Gateway with latency. |
| `PlayerPreparing` | Android `PlayerAdapter` | ExoPlayer setting media item and loading media buffer. |
| `PlayerReady` | Android `PlayerAdapter` | ExoPlayer state changed to `STATE_READY`. |
| `PlayerPlaying` | Android `PlayerAdapter` | ExoPlayer active playback started. |
| `PlayerError` | Android `PlayerAdapter` | Playback failure event with error message and stage. |
| `NetworkError` | Android `GatewayApi` | Retrofit or HTTP transport layer error. |
| `GatewayError` | Android `GatewayErrorMapper` | Gateway returned non-200 error code. |
| `CACHE_HIT` / `CACHE_MISS` | Gateway `CacheManager` | Redis search/stream cache lookup result. |
| `PROVIDER_SELECTED` | Gateway `SelectionEngine` | Selected active provider for route execution. |
| `PROVIDER_FAILED` | Gateway `CircuitBreaker` | Provider execution failed and triggered circuit trip. |
