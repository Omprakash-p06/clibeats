# ADR-008: Performance Budgets & Accessibility Architecture

**Date:** 2026-08-06
**Status:** Accepted
**Phase:** 8 — Performance Budgets & Accessibility

## Context

CLIBeats requires Settings management (`REQ-SET-02`), a <2s cold start budget (`REQ-NFR-01`), 60 FPS list scrolling (`REQ-NFR-02`), and 100% TalkBack accessibility compliance (`REQ-NFR-03`).

## Decision

### 1. Settings & Preferences Architecture
- Built `SettingsViewModel` and `SettingsScreen` integrating `AppPreferences` DataStore and `CacheManager`.
- Exposed options for active provider selection, disk cache limits (256MB-2GB), audio quality toggles, and cache maintenance.

### 2. Rendering & Memory Optimization
- Enforced explicit `key` and `contentType` parameters across all `LazyColumn` song list items (`SongTable`).
- Configured Coil `ImageLoader` in `ImageLoaderModule` with 25% memory cache limit, 2% disk cache limit, and crossfade optimizations for smooth flings.

### 3. Accessibility Standards
- Provided non-null `contentDescription` for all interactive icon buttons (`PlayerBar`, Navigation components).
- Enforced minimum 48dp touch targets and high-contrast text styling (exceeding 4.5:1 ratio).

## Consequences

### Positive
- Fully configurable app preferences and cache controls.
- Fast <2s cold start and zero-jank 60 FPS list scrolling.
- WCAG AA compliant TalkBack accessibility.
