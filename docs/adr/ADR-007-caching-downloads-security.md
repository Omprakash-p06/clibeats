# ADR-007: Caching, Downloads & Security Architecture

**Date:** 2026-08-06
**Status:** Accepted
**Phase:** 7 — Caching, Downloads & Security Layer

## Context

CLIBeats requires an offline audio caching engine (`REQ-OFF-01`), background track downloads (`REQ-OFF-02`), automatic network fallback (`REQ-OFF-03`), and security hardening (`REQ-ENG-09`).

## Decision

### 1. LRU Cache & Room Index
- Implemented `CacheManager` managing audio files in `context.cacheDir/audio_cache/`.
- Synced metadata (`fileSizeBytes`, `cachedAt`) with Room database `CacheIndexDao`.
- Enforced 500 MB default max capacity with automatic LRU evictions.

### 2. Track Downloads & Media Resolution
- Built `TrackDownloadManager` downloading audio streams via OkHttp to disk cache.
- Updated `PlayerAdapter` to check `CacheManager.getCachedFile(track.id)` prior to playing remote stream URLs.

### 3. Security Hardening
- Added ProGuard rules preserving Room entities, DTO serialization, and domain models.
- Verified network interceptors sanitize log outputs to protect sensitive tokens.

## Consequences

### Positive
- Zero-buffering offline playback for cached and downloaded tracks.
- Seamless fallback when network drops.
- Production-grade security & obfuscation rules.
