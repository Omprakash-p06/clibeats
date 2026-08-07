# ADR-012: CliBeats Gateway Provider Architecture & Decoupled Extraction

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** Architecture Redesign & Provider Abstraction  

## Context

Empirical runtime diagnostics on Android hardware revealed that unauthenticated mobile requests to YouTube's `InnerTube` API `/player` endpoint are blocked with `LOGIN_REQUIRED: Sign in to confirm you're not a bot` (`ACCOUNT_EVENT_TRIGGER_VISITOR_SUSPICIOUS_REQUEST`) and return `streamingData = null`.

Extensive research into the 2026 YouTube extraction ecosystem (**NewPipeExtractor**, **LibreTube/Piped**, **yt-dlp**, and **YouTube.js**) confirms that:
- Direct unauthenticated mobile `Android -> InnerTube` extraction has become increasingly fragile due to platform-enforced BotGuard / DroidGuard Proof of Origin (PO) Tokens, session binding, and dynamic client requirements.
- Attempting to reverse-engineer BotGuard/DroidGuard JS evaluation or hardcode client contexts directly within the native Android application leads to frequent breakage whenever YouTube updates its internal API parameters.

## Decision

We decouple YouTube stream extraction from the CliBeats Android application and transition to a **Gateway Provider Architecture**.

```text
CliBeats Android Application (Jetpack Compose TUI)
         │
         │ (Stable, Provider-Agnostic REST API)
         ▼
CliBeats Gateway Service (Fastify / Node.js + YouTube.js)
 ┌───────┴───────────────────────────────────────────┐
 │ • YouTube.js & InnerTube Extraction Engine       │
 │ • PO Token & BotGuard JS Challenge Handling       │
 │ • VisitorData & Session Management                │
 │ • Audio Stream Resolution & Format Selection      │
 │ • Rate Limiting & Response Caching               │
 └───────────────────────────────────────────────────┘
         │
         ▼
     YouTube / Future Music Providers (Jellyfin, Navidrome, Spotify)
```

### 1. Technology Selection: Fastify (Node.js) + YouTube.js
- **Fastify (Node.js)** is selected for the gateway service to leverage the JavaScript/TypeScript ecosystem surrounding **YouTube.js** and Node.js-native BotGuard/player.js evaluation.
- Minimizes impedance by avoiding embedding JavaScript engines inside native Android runtimes.

### 2. Provider-Agnostic Android Client
- The Android application retains its clean `MusicProvider` domain interface (`domain/provider/MusicProvider.kt`).
- The Android app consumes a stable, unified REST API exposed by the CliBeats Gateway.

### 3. Isolation of Upstream Protocol Changes
- All YouTube-specific changes (PO Tokens, signature deciphering, visitorData, header updates) are isolated inside the gateway service.
- Upstream changes by YouTube are resolved via gateway backend updates without forcing Android app releases or user APK updates.

## Consequences

### Positive
- **Android Client Stability**: The mobile codebase remains provider-agnostic, clean, and robust against YouTube API updates.
- **Simplified Maintenance**: InnerTube and BotGuard extraction logic are centralized in the Node.js/Fastify ecosystem where extraction libraries (**YouTube.js**) are actively maintained by the community.
- **Future-Proof Multi-Provider Support**: Provides a clean foundation to add Jellyfin, Navidrome, Piped, or custom music adapters to the gateway without changing the mobile UI.

### Negative / Mitigations
- Requires deploying/hosting a lightweight Node.js gateway instance (mitigated by support for local self-hosting or simple cloud deployment).

## Referenced Files
- `domain/provider/MusicProvider.kt` — Domain provider contract
- `domain/provider/StreamResolver.kt` — Stream resolver interface
- `data/provider/YouTubeMusicProvider.kt` — Provider implementation
