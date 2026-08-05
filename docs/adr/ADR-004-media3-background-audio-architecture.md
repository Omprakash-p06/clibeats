# ADR-004: AndroidX Media3 & Background Audio Architecture

**Date:** 2026-08-05
**Status:** Accepted
**Phase:** 4 — Playback Engine & Background Media Service
**Requirements:** REQ-MUS-02, REQ-MUS-03, REQ-ENG-05

---

## Context

CLIBeats requires background audio playback, system media notification controls, and state binding to the Compose TUI player bar.

Legacy ExoPlayer (`com.google.android.exoplayer2`) is deprecated in favor of **AndroidX Media3**. Media3 standardizes playback engine management, media sessions, and system notifications into a unified library suite.

## Decision

1. **Service Architecture**: `PlaybackService` extends `MediaSessionService`. It hosts a singleton `ExoPlayer` instance and `MediaSession`.
2. **Audio Attributes & Headphone Disconnect**: Configured with `setAudioAttributes(C.USAGE_MEDIA)` and `setHandleAudioBecomingNoisy(true)` to automatically pause playback on headphone unplug.
3. **State Binding**: `PlayerAdapter` maps Media3 player listener events into a Kotlin `StateFlow<PlaybackState>`. `PlayerViewModel` consumes this state and exposes play/pause/skip actions to Compose `PlayerBar`.
4. **Permissions**: Android 14 (API 34) requires `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions.

## Consequences

### Positive
- Unified AndroidX Media3 implementation without legacy ExoPlayer dependencies.
- Background playback continues seamlessly when app is minimized.
- System media notification automatically managed by `MediaSessionService`.
- Reactive `StateFlow` state binding for Jetpack Compose UI.

### Negative
- Media3 requires careful lifecycle management to prevent memory leaks in the background service.
