# ADR-006: Playlists, Queue Management & Library Architecture

**Date:** 2026-08-06
**Status:** Accepted
**Phase:** 6 — Playlists, Queue Management & Library

## Context

CLIBeats requires complete queue management (`REQ-MUS-03`), local library browsing (`REQ-LIB-01`), and playlist CRUD operations (`REQ-LIB-02`). All state must adhere to Clean Architecture (`Presentation` -> `Domain` -> `Data`) and maintain high-contrast TUI visual system rules.

## Decision

### 1. Queue Management & ExoPlayer Binding
- Extended `PlayerAdapter` with `moveTrack`, `removeFromQueue`, `clearQueue`, and `queueFlow`.
- Delegated queue changes directly to underlying `ExoPlayer` media items (`moveMediaItem`, `removeMediaItem`, `clearMediaItems`).
- Persisted active queue state in Room database (`QueueEntity`, `QueueDao`) for restore across app restarts.

### 2. Library Browsing
- Created `LibraryViewModel` that transforms `SongRepository.getAllTracksAsFlow()` into reactive `Tracks`, `Artists`, and `Albums` groupings.
- Structured `LibraryScreen` with Jetpack Compose `TabRow` and dense `SongTableRow` items.

### 3. Playlist CRUD Operations
- Connected `PlaylistRepository` to `PlaylistViewModel` supporting `createPlaylist`, `deletePlaylist`, `selectPlaylist`, `removeSongFromPlaylist`, and `playPlaylist`.
- Built `PlaylistScreen` (list & create dialog) and `PlaylistDetailScreen` (song list & play controls).

## Consequences

### Positive
- Fully interactive queue and playlist management without external dependencies.
- Reactive `StateFlow` updates across all screens.
- Strict Clean Architecture boundaries preserved.

### Negative / Risks
- Large queues (>1000 items) require lazy list optimizations.

## Referenced Files
- `domain/repository/PlaylistRepository.kt`
- `domain/repository/PlaybackRepository.kt`
- `data/local/dao/QueueDao.kt`
- `presentation/queue/QueueViewModel.kt`
- `presentation/library/LibraryViewModel.kt`
- `presentation/playlist/PlaylistViewModel.kt`
