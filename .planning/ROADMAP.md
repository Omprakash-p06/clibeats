# ROADMAP: CLIBeats

## Milestone 1: Core TUI Music Client Foundation

### Phase 1: Architecture Core & Provider API Abstraction
**Goal**: Establish clean architecture project structure, dependency injection, and core domain models.
- Requirements: `REQ-NFR-04`, `REQ-NFR-05`, `REQ-SET-01`
- Deliverables:
  - Base Kotlin Android application setup with Clean Architecture packages (`presentation`, `domain`, `data`).
  - Hilt / Dagger DI configuration.
  - `MusicProvider` interface definition (`search()`, `getTrack()`, `stream()`, `playlists()`, `queue()`).
  - Domain models (`Track`, `Album`, `Artist`, `Playlist`, `PlaybackState`).

### Phase 2: TUI Design System & Navigation Layout
**Goal**: Implement the monochrome TUI design system, JetBrains Mono typography, top app bar, navigation drawer, and persistent bottom player.
- Requirements: `REQ-NAV-01`, `REQ-NAV-02`, `REQ-NAV-03`, `REQ-UI-01`, `REQ-UI-02`, `REQ-UI-03`, `REQ-UI-04`
- Deliverables:
  - Compose theme with `#0D0D0D` background, `#151515` surface, `#1DB954` accent, `#FFFFFF` text.
  - Custom JetBrains Mono typography hierarchy (Titles 18sp, Body 14sp, Metadata 12sp).
  - Main Layout shell: Top App Bar, Collapsible Nav Rail/Drawer, Main Content Container, Persistent Player Bar.
  - TUI Song Table row component (48dp height, square artwork, dense layout).

### Phase 3: Database & Local Persistence Layer
**Goal**: Setup Room database schemas and encrypted storage for tracks, playlists, history, and user settings.
- Requirements: `REQ-LIB-03`, `REQ-OFF-03`
- Deliverables:
  - Room Database setup (`SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`).
  - DAOs for track library, playlists, search history, and cache index.
  - EncryptedSharedPreferences / DataStore wrapper for app settings and secrets.

### Phase 4: Playback Engine & Background Media Service
**Goal**: Build AndroidX Media3 / ExoPlayer integration with foreground playback service, notification controls, and state management.
- Requirements: `REQ-MUS-02`, `REQ-MUS-03`
- Deliverables:
  - `PlaybackService` supporting background audio play/pause/seek/skip.
  - System media notification and media session connector.
  - Playback State Machine exposed via Kotlin Flows/StateFlow.
  - Connection between UI Persistent Player bar and Media3 controller.

### Phase 5: Provider Integration & Search
**Goal**: Implement initial `MusicProvider` adapter and track search UI.
- Requirements: `REQ-MUS-01`, `REQ-MUS-04`, `REQ-NAV-01`
- Deliverables:
  - Default `MusicProvider` adapter implementation.
  - Search View model and debounced search input.
  - Search results display in dense TUI song table layout.
  - Track detail view and metadata formatting.

### Phase 6: Playlists, Queue Management & Library
**Goal**: Enable complete queue management, library browsing, and playlist CRUD operations.
- Requirements: `REQ-MUS-03`, `REQ-LIB-01`, `REQ-LIB-02`
- Deliverables:
  - Interactive Queue panel (reorder tracks, remove, clear, persist queue across restarts).
  - Library Screen (browse saved tracks, artists, albums).
  - Playlist Screen (create, edit, delete playlists, add/remove tracks).

### Phase 7: Caching, Downloads & Security Layer
**Goal**: Implement offline audio caching engine, track download manager, and storage encryption.
- Requirements: `REQ-OFF-01`, `REQ-OFF-02`, `REQ-OFF-03`
- Deliverables:
  - `CacheManager` using LRU strategy and Room index for offline playback.
  - Download Manager for background track downloads (where allowed).
  - Auto-fallback to offline cache when network connection is lost.

### Phase 8: Settings, Performance Tuning & Accessibility
**Goal**: Add settings screen, achieve cold start <2s, 60 FPS scrolling, and complete Material accessibility compliance.
- Requirements: `REQ-SET-02`, `REQ-NFR-01`, `REQ-NFR-02`, `REQ-NFR-03`
- Deliverables:
  - Settings Screen (theme selection, provider management, cache limits, audio quality).
  - Cold start optimization (<2 seconds).
  - 60 FPS scrolling optimization for song lists and tables.
  - Screen reader content descriptions, contrast verification, accessibility audit.
