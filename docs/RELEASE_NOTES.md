# CliBeats v1.0.0 — Production Release Notes

**Release Date:** 2026-08-08  
**Build Target:** Android 8.0+ (API 26+) / Fastify Provider Gateway Node v22+  
**Status:** READY FOR v1.0.0 RELEASE  

CliBeats v1.0.0 is a terminal-inspired, privacy-first audio streaming and native Android media application powered by a provider-agnostic Fastify Provider Gateway. Built with Jetpack Compose, Hilt, Room, Media3 ExoPlayer, and TypeScript.

## Core Features & Architectural Highlights

### 1. Decoupled Provider Gateway Architecture
- All YouTube extraction, InnerTube JS interaction, and PO token deciphering are isolated server-side inside the Fastify Provider Gateway (`gateway/`).
- Android client contains **zero** direct YouTube/InnerTube code, communicating exclusively via REST API contracts (`GatewayApi`).

### 2. Native Android Application
- **Terminal UI Design System**: JetBrains Mono typography, high-contrast monospace layout, dense song tables, persistent bottom player.
- **Background Playback Engine**: AndroidX Media3 `ExoPlayer` & `MediaSessionService` providing continuous background audio playback, system notification controls, and audio focus management.
- **Local Persistence & Security**: Room Database v1 schema (`CliBeatsDatabase`), `EncryptedSharedPreferences` for sensitive credentials backed by Android Keystore (`AES256_GCM`), and DataStore Preferences.
- **Resilience & Caching**: Cache lookup fallback, network disconnect/reconnect handling, trace ID propagation (`x-trace-id`).

### 3. Provider Gateway Service
- Fastify REST API (`/api/v1/search`, `/api/v1/stream`, `/api/v1/bootstrap`, `/health`, `/metrics`).
- Redis caching for search results and resolved direct-to-CDN stream URLs (`ioredis`).
- Circuit breaker resiliency and Prometheus metrics instrumentation.
- OpenAPI 3 spec validation (`openapi.json`).

---

## Production Quality Gates Passed

- Gateway TypeScript check (`npm run check`): **PASSED**
- Gateway Vitest suite (`npm test`): **PASSED** (12 test files, 72 tests clean)
- Gateway OpenAPI spec validation (`npm run openapi:validate`): **PASSED** (10 paths verified)
- Android Unit Test Suite (`.\gradlew.bat testDebugUnitTest`): **PASSED** (125 tests clean)
- Android Release Build (`.\gradlew.bat assembleRelease`): **PASSED** (R8 minification, ProGuard rules, resource shrinking clean)
