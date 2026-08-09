---
id: RECOVERY-11
title: Pure Kotlin Provider Recovery
status: investigating
trigger: |
  Return CliBeats to the architecture at baseline commit
  2a47b56e68584f87024ed86174c4cd2c2f496a59 (pure-Kotlin Android,
  no TypeScript/Fastify gateway, no Redis/Docker/Render/Railway).
  Goal: simple, fully functional, pure-Kotlin Android music player with
  free high-quality streaming from a viable provider.
created: 2026-08-10
updated: 2026-08-10
---

# Debug Session: RECOVERY-11 — Pure Kotlin Provider Recovery

## Symptoms

- Application currently depends on the CliBeats Gateway (TypeScript/Fastify/Redis) for search + streaming (ADR-012 through ADR-020).
- Mission directive: eliminate gateway architecture; return to self-contained pure-Kotlin Android app.
- Provider is replaceable; YouTube is NOT required. No mandatory CLIBeats server.
- 40 commits exist between baseline `2a47b56` and HEAD.

## Constraints

- NO TypeScript, Fastify, Redis, Docker, Render/Railway backend, provider gateway, PO-token infra, server-side proxy.
- Android app self-contained unless provider absolutely requires a remote API.
- FREE + high-quality streaming + no account (where possible) + no ads + no user data collection + open-source.
- Preserve existing product: playlists, library, queue, history, favorites, background playback, theme, player.
- Library portability: export/import via `clibeats.json` with stable source refs, no server dependency.
- Do not create docs instead of fixing the app. No claims without runtime evidence.

## Current Focus

- **hypothesis**: Baseline `2a47b56` contains the last pure-Kotlin architecture (pre-gateway-migration) with a direct provider implementation that can be restored and pointed at a free provider.
- **provider_choice**: **Audius** — verified working end-to-end with real runtime evidence.
- **next_action**: Phase 3 — confirm provider choice with user; Phase 4 — implement Audius provider.

## Phase 0 — Baseline Inventory (2a47b56)

- Pure-Kotlin Android: Kotlin 2.0.21, AGP 8.5.2, Compose BOM 2024.09.03, Hilt, Room 2.6.1, Media3 1.4.1, OkHttp/Retrofit, Coil, minSdk 26 / target 34.
- Layers: `presentation` (theme/layout/component/search/queue/library/playlist/player/settings), `domain` (model/provider/repository), `data` (local/dao/entity/mapper, provider, cache, download, network, preferences), `playback` (PlayerAdapter + service), `di` (10 modules), `telemetry`.
- Provider: `YouTubeMusicProvider` via InnerTube API (search + player endpoints, DTOs, TrackMapper).
- 109 unit tests, CI (GitHub Actions), detekt + ktlint + Paparazzi.

## Phase 1-2 — Provider Evaluation (runtime evidence, 2026-08-10)

### Audius — ✅ CHOSEN
- Search: `GET https://discoveryprovider.audius.co/v1/tracks/search?query=...&app_name=...&limit=N` → clean JSON.
- Metadata: id, title, user.name (artist), genre, duration (s), artwork.{150x150,480x480,1000x1000}, is_streamable.
- Stream URL: embedded in search response at `stream.url` — signed cidstream URL, no extra API call.
- Audio verified: HTTP 206, `audio/mpeg`, **320 kbps / 48 kHz / Stereo**, ID3 v2.4. Range requests work → seeking works.
- Long tracks (3334s) available. No auth, no account, no ads, no tracking, open catalog.

### Internet Archive — ❌ REJECTED (unreliable)
- Search/metadata JSON works, but download URL 302-redirect chain breaks (404 / HTTP 000, broken nodes).

### Jamendo — ⚠️ FALLBACK
- Free for open-source, read API needs only client_id (dev registration). MP3 96/192kbps + ogg/flac. 500k-hit cap. Fine but lower quality + registration required.

### NewPipe Extractor — ❌ REJECTED (mission constraint)
- Requires PO tokens / WebView BotGuard for YouTube audio in 2025-26; high maintenance risk; violates mission's no-PO-token rule.

### SoundCloud / yt-dlp — ❌ REJECTED (API restrictions / heavyweight)
