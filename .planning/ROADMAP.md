# ROADMAP: CLIBeats Engineering Roadmap

## Milestone 1: Engineering Foundation & Core TUI Client

### Phase 0: Engineering Foundation & CI/CD Pipeline

**Goal**: Establish quality gates, static analysis, automated linting, GitHub Actions CI pipeline, ADR framework, and branch protection before writing production feature code.

- Requirements: `REQ-ENG-01`, `REQ-ENG-02`, `REQ-ENG-03`, `REQ-ENG-04`, `REQ-ENG-05`, `REQ-ENG-06`, `REQ-ENG-07`
- Deliverables:
  - Static analysis configuration (**Detekt** with custom rule set + **ktlint** formatting integration).
  - Android Lint rules and architecture boundary validation rules (`Presentation` -> `Domain` -> `Data`).
  - Automated CI Pipeline via **GitHub Actions** (`.github/workflows/ci.yml`) enforcing checkout, compile, detekt, ktlint, android lint, unit tests, coverage, security audit, and APK build.
  - Architecture Decision Records framework (`docs/adr/ADR-000-template.md` & initial ADRs).
  - Definition of Done (DoD) verification script / checklist.

### Phase 1: Architecture Core & Provider API Abstraction

**Goal**: Establish clean architecture project structure, dependency injection, and core domain models with pure Kotlin contracts.

- Requirements: `REQ-NFR-04`, `REQ-NFR-05`, `REQ-SET-01`, `REQ-ENG-04`, `REQ-ENG-05`
- Deliverables:
  - Base Kotlin Android application setup with Clean Architecture packages (`presentation`, `domain`, `data`).
  - Hilt / Dagger DI configuration.
  - `MusicProvider` interface definition (`search()`, `getTrack()`, `stream()`, `playlists()`, `queue()`).
  - Domain models (`Track`, `Album`, `Artist`, `Playlist`, `PlaybackState`, `ProviderResult`).
  - ADR-001: Clean Architecture Layering & Hilt DI strategy.
  - ADR-002: MusicProvider Abstraction Layer.

### Phase 2: TUI Design System & Navigation Layout

**Goal**: Implement the monochrome TUI design system, JetBrains Mono typography, top app bar, navigation drawer, and persistent bottom player.

- Requirements: `REQ-NAV-01`, `REQ-NAV-02`, `REQ-NAV-03`, `REQ-UI-01`, `REQ-UI-02`, `REQ-UI-03`, `REQ-UI-04`, `REQ-NFR-03`
- Deliverables:
  - Compose theme with `#0D0D0D` background, `#151515` surface, `#1DB954` accent, `#FFFFFF` text.
  - Custom JetBrains Mono typography hierarchy (Titles 18sp, Body 14sp, Metadata 12sp).
  - Main Layout shell: Top App Bar, Collapsible Nav Rail/Drawer, Main Content Container, Persistent Player Bar.
  - TUI Song Table row component (48dp height, square artwork, dense layout).
  - Compose UI visual component tests.

### Phase 3: Database & Local Persistence Layer

**Goal**: Setup Room database schemas, DAOs, and encrypted storage for tracks, playlists, history, and user settings.

- Requirements: `REQ-LIB-03`, `REQ-OFF-03`, `REQ-ENG-09`
- Progress: Plans 03-01 (Dependency Setup), 03-02 (Room Entities & Database), 03-03 (DAOs, Repositories, Mappers & AppPreferences) and 03-04 (DAO Integration Tests, Repository Unit Tests & ADR-003) COMPLETE 2026-08-05. Phase 3 COMPLETE (4/4 plans).
- Deliverables:
  - Room Database setup (`SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`).
  - DAOs for track library, playlists, search history, and cache index.
  - EncryptedSharedPreferences / DataStore wrapper for app settings and secrets.
  - Room integration tests and migration test suite.
  - ADR-003: Encrypted Storage & Local Persistence Strategy.

### Phase 4: Playback Engine & Background Media Service

**Goal**: Build AndroidX Media3 / ExoPlayer integration with foreground playback service, notification controls, and state management.

- Requirements: `REQ-MUS-02`, `REQ-MUS-03`
- Deliverables:
  - `PlaybackService` supporting background audio play/pause/seek/skip.
  - System media notification and media session connector.
  - Playback State Machine exposed via Kotlin Flows/StateFlow.
  - Connection between UI Persistent Player bar and Media3 controller.
  - Playback integration test harness.
  - ADR-004: AndroidX Media3 & Background Audio Architecture.

### Phase 5: Provider Integration & Search

**Goal**: Implement default `MusicProvider` adapter (referencing `sigma67/ytmusicapi` for YouTube Music InnerTube API schemas), debounced search UI, and track metadata display.

- Requirements: `REQ-MUS-01`, `REQ-MUS-04`, `REQ-NAV-01`, `REQ-SET-01`
- Deliverables:
  - Default `MusicProvider` adapter implementation (`YouTubeMusicProvider` using InnerTube API specification based on `sigma67/ytmusicapi`).
  - Search ViewModel and debounced search input flow.
  - Search results display in dense TUI song table layout.
  - Track detail view and metadata formatting.
  - Provider repository unit and integration tests.

### Phase 6: Playlists, Queue Management & Library

**Goal**: Enable complete queue management, library browsing, and playlist CRUD operations.

- Requirements: `REQ-MUS-03`, `REQ-LIB-01`, `REQ-LIB-02`
- Deliverables:
  - Interactive Queue panel (reorder tracks, remove, clear, persist queue across restarts).
  - Library Screen (browse saved tracks, artists, albums).
  - Playlist Screen (create, edit, delete playlists, add/remove tracks).
  - ViewModels unit test suite + Compose UI interactive tests.

### Phase 7: Caching, Downloads & Security Layer

**Goal**: Implement offline audio caching engine, track download manager, and security hardening.

- Requirements: `REQ-OFF-01`, `REQ-OFF-02`, `REQ-OFF-03`, `REQ-ENG-09`
- Deliverables:
  - `CacheManager` using LRU strategy and Room index for offline playback.
  - Download Manager for background track downloads (where allowed).
  - Auto-fallback to offline cache when network connection is lost.
  - Security audit: Secret scanning, dependency vulnerability scan, secure logging.

### Phase 8: Performance Budgets & Accessibility

**Goal**: Enforce performance budgets (cold start <2s, 60 FPS scrolling, memory caps) and achieve 100% Material accessibility compliance.

- Requirements: `REQ-SET-02`, `REQ-NFR-01`, `REQ-NFR-02`, `REQ-NFR-03`, `REQ-ENG-08`
- Deliverables:
  - Settings Screen (theme selection, provider management, cache limits, audio quality).
  - Cold start optimization (<2 seconds verified via Macrobenchmark).
  - 60 FPS scrolling optimization for song lists and tables.
  - Screen reader content descriptions, contrast verification, 100% accessibility audit.

### Phase 9: Comprehensive Testing & Hardening Suite

**Goal**: Achieve >=85% unit/integration code coverage, end-to-end Compose UI tests, regression test suite, and static analysis zero-issue state.

- Requirements: `REQ-ENG-01`, `REQ-ENG-06`, `REQ-ENG-07`
- Deliverables:
  - Unit tests for 100% of ViewModels, UseCases, Repositories, Mappers.
  - Integration tests covering Provider -> Repository -> Database -> Playback.
  - Compose UI automated E2E user flows (Search -> Select -> Play -> Queue -> Restart).
  - Regression test suite for bug prevention.
  - Final Detekt (0 critical) & Android Lint (0 error) audit.

### Phase 10: Beta Validation & Telemetry

**Goal**: Integrate structured logging, crash telemetry, analytics abstractions, and conduct beta testing validation.

- Requirements: `REQ-ENG-10`
- Deliverables:
  - Telemetry & structured logging framework.
  - Crash reporting abstraction layer (privacy-first, no PII).
  - Beta testing distribution build pipeline.
  - User research & SUS score validation.

### Phase 11: Production Release & Distribution

**Goal**: Production APK/AAB build, release signing, license compliance audit, and release documentation.

- Requirements: `REQ-ENG-06`
- Deliverables:
  - Production release build configuration with R8/ProGuard rules.
  - Release signing and APK artifact verification.
  - Open source license audit and dependency attribution.
  - Release notes & User Manual.
