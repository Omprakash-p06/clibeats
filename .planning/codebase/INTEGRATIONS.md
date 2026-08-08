# Codebase Integrations: CliBeats

## 1. Gateway Client Integration (Android App <-> Gateway)
- **Base URL**: `BuildConfig.GATEWAY_BASE_URL` (Configurable, default `http://localhost:3000` or production Gateway endpoint).
- **Interface**: [GatewayApi.kt](file:///c:/Users/OM%20Prakash/Documents/clibeats/app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt)
- **Endpoints**:
  - `GET /api/v1/search`: Accepts `query` and `filterSongs=true` parameters. Returns JSON array of tracks mapped via [GatewayMapper.kt](file:///c:/Users/OM%20Prakash/Documents/clibeats/app/src/main/java/com/clibeats/data/gateway/mapper/GatewayMapper.kt).
  - `POST /api/v1/stream`: Accepts JSON body `{ "trackId": "string" }`. Returns resolved stream metadata (`streamUrl`, `format`, `expiresAt`).
  - `GET /health`: Health check status endpoint.

---

## 2. Server-Side Gateway Integrations (Node.js Gateway)
- **YouTube.js (`youtubei.js`)**: Server-side InnerTube integration for resolving track metadata and direct GoogleVideo audio stream URLs without exposing InnerTube details to the Android client.
- **Redis Cache (`ioredis`)**: Caching layer for resolved stream URLs and search results to reduce upstream provider load and lower latency.
- **Prometheus Metrics (`prom-client`)**: Exposes `/metrics` endpoint for operational telemetry, response latency monitoring, and provider failure tracking.
- **OpenAPI Schema Generator**: Generates and validates OpenAPI specifications via `scripts/generate-openapi.ts` and `scripts/validate-openapi.ts`.

---

## 3. Decoupling & Isolation Guarantees
- The Android app contains **zero direct YouTube or InnerTube dependencies**.
- All signature deciphering, PO Tokens, visitor data, and stream extractions are isolated server-side inside the gateway's `YouTubeProviderAdapter`.