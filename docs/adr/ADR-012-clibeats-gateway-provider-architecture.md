# ADR-012: Provider Gateway Architecture & Decoupled Extraction

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Architecture Redesign & Provider Abstraction  

## Context

Empirical runtime diagnostics on Android hardware revealed that unauthenticated mobile requests to YouTube's `InnerTube` API `/player` endpoint are blocked with `LOGIN_REQUIRED: Sign in to confirm you're not a bot` (`ACCOUNT_EVENT_TRIGGER_VISITOR_SUSPICIOUS_REQUEST`) and return `streamingData = null`.

Extensive research into the 2026 YouTube extraction ecosystem (**NewPipeExtractor**, **LibreTube/Piped**, **yt-dlp**, and **YouTube.js**) confirms that:
- Direct unauthenticated mobile `Android -> InnerTube` extraction has become increasingly fragile due to platform-enforced BotGuard / DroidGuard Proof of Origin (PO) Tokens, session binding, and dynamic client requirements.
- Attempting to reverse-engineer BotGuard/DroidGuard JS evaluation or hardcode client contexts directly within the native Android application leads to frequent breakage whenever YouTube updates its internal API parameters.

## Decision

We decouple upstream stream extraction from the CliBeats Android application and transition to a **Provider Gateway Architecture**. The gateway itself is provider-agnostic; YouTube is implemented as one adapter plugin alongside future music sources (Jellyfin, Navidrome, Spotify, Piped).

```text
CliBeats Android Application (Jetpack Compose TUI + Media3 ExoPlayer)
         │
         │ (Stable, Provider-Agnostic REST API)
         ▼
Provider Gateway Service (Fastify / Node.js)
 ┌───────┴───────────────────────────────────────────┐
 │ Core Gateway Services:                            │
 │ • Unified Provider Manager & Priority Router      │
 │ • Metadata & Playback Separation                  │
 │ • Redis Cache Layer (Metadata vs Playback URLs)   │
 │ • Circuit Breakers, Retries & Backoff             │
 │ • Prometheus Observability (/health, /metrics)    │
 └───────────────────────┬───────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
 YouTubeAdapter   JellyfinAdapter  NavidromeAdapter
 (YouTube.js,     (Subsonic REST)   (Subsonic API)
  PO Token,
  BotGuard)
```

### 1. Technology Selection: Fastify (Node.js)
- **Fastify (TypeScript/Node.js)** is selected for the gateway service to leverage the JavaScript/TypeScript ecosystem surrounding **YouTube.js**, official PO Token helper plugins, and Fastify's high-performance plugin lifecycle hooks.
- Minimizes impedance by keeping extraction logic in Node.js rather than embedding JavaScript runtimes inside Android native code.

### 2. Encapsulated Adapter Implementation
- YouTube.js, PO Token provider integration, and BotGuard helper orchestration are encapsulated entirely within the `YouTubeAdapter` plugin inside `providers/youtube/`.
- The core **Provider Gateway** remains provider-agnostic and relies strictly on internal TypeScript interfaces (`ProviderAdapter`).

### 3. Provider-Agnostic Android Client
- The Android application retains its clean `MusicProvider` domain interface (`domain/provider/MusicProvider.kt`).
- The Android app consumes a stable, unified REST API exposed by the Provider Gateway.

## Consequences

### Positive
- **Android Client Stability**: The mobile codebase remains provider-agnostic, clean, and robust against YouTube API updates.
- **Encapsulated Maintenance**: Upstream extraction changes affect only the specific adapter plugin (e.g., `YouTubeAdapter`), leaving core gateway routing and client interfaces untouched.
- **Multi-Provider Architecture**: Provides a clean foundation to route requests across YouTube, Piped, Jellyfin, Navidrome, or Spotify adapters.

### Negative / Mitigations
- Requires running a lightweight Node.js gateway service (mitigated by Docker containerization and self-hosting options).

## Referenced Files
- `domain/provider/MusicProvider.kt` — Domain provider contract
- `domain/provider/StreamResolver.kt` — Stream resolver interface
- `docs/adr/ADR-013-provider-plugin-architecture.md` — Provider Plugin Architecture ADR
