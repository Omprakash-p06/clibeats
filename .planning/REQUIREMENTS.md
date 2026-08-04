# REQUIREMENTS: CLIBeats

## Functional Requirements

### Core & Navigation
- **REQ-NAV-01**: Top App Bar with quick search access, app status, and view switcher.
- **REQ-NAV-02**: Collapsible navigation rail / drawer for fast section switching (Home, Search, Library, Playlists, Queue, Settings).
- **REQ-NAV-03**: Persistent bottom player bar present across all main screens.

### Music & Search
- **REQ-MUS-01**: Provider-agnostic track search with metadata (title, artist, album, duration, artwork).
- **REQ-MUS-02**: Track playback controls (Play, Pause, Skip Next, Skip Previous, Seek, Repeat, Shuffle).
- **REQ-MUS-03**: Playback queue management (View queue, reorder, remove track, clear queue, persist queue across app restarts).
- **REQ-MUS-04**: Track metadata display with dense song table layout.

### Library & Playlists
- **REQ-LIB-01**: Local library management (browse tracks, artists, albums).
- **REQ-LIB-02**: Playlist management (create, rename, delete playlist, add/remove tracks).
- **REQ-LIB-03**: Playback history tracking.

### Caching & Downloads
- **REQ-OFF-01**: Offline track audio caching for fast playback and low data usage.
- **REQ-OFF-02**: Track downloading for offline playback (where permitted by provider).
- **REQ-OFF-03**: Encrypted local storage for credentials, cache indexes, and preferences.

### Settings & Multi-Provider
- **REQ-SET-01**: Multi-provider plugin architecture (`MusicProvider` abstraction interface).
- **REQ-SET-02**: Settings panel for theme customizations, provider configuration, cache limits, and audio preferences.

## Non-Functional Requirements
- **REQ-NFR-01**: Cold start time under 2 seconds.
- **REQ-NFR-02**: 60 FPS smooth scrolling in song lists and tables.
- **REQ-NFR-03**: Material accessibility compliance (screen reader labels, high contrast text).
- **REQ-NFR-04**: Architecture following MVVM + Clean Architecture principles (`Presentation` -> `Domain` -> `Data`).
- **REQ-NFR-05**: Atomic git commit as soon as any debug session or phase is completed, verified, and test cases pass.

## UI & Design Specifications
- **REQ-UI-01**: Dark monochrome theme (`#0D0D0D` bg, `#151515` surface, `#1DB954` accent, `#FFFFFF` text).
- **REQ-UI-02**: JetBrains Mono typography across all UI elements.
- **REQ-UI-03**: Minimal transitions (100–150ms slide/fade), zero blur/glassmorphism/bounce effects.
- **REQ-UI-04**: Compact 48dp list rows with square album artwork.
