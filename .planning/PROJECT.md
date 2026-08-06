# PROJECT: CLIBeats

## What This Is
CLIBeats is a free, production-grade Android music client inspired by terminal interfaces (TUI). It features a compact, text-dense layout with monospaced JetBrains Mono typography, high-contrast dark theme, persistent playback controls, and a provider-agnostic architecture supporting local media and external music sources.

## Vision
Build a free, production-grade Android music client inspired by terminal interfaces (TUI) using a compact, keyboard-inspired UI while supporting multiple music providers through a modular abstraction layer.

## Core Values & Engineering Principles
- **Engineering-First Quality Philosophy**: Code is not "done" when it compiles; a feature is only complete when it passes automated quality gates, static analysis, unit/UI test suites, architectural lint rules, and CI pipeline checks.
- **Text-First & Dense**: Maximized information density with monospaced typography (JetBrains Mono) and flat surfaces.
- **Minimal Monochrome Aesthetic**: High contrast dark theme (`#0D0D0D` background, `#151515` surface, `#1DB954` accent) without decorative blurs, glassmorphism, or bounce animations.
- **Fast & Predictable Navigation**: Quick navigation, persistent player controls, compact song tables, and keyboard/touch efficiency.
- **Provider-Agnostic Architecture**: Decoupled `MusicProvider` interface supporting official and custom media sources.
- **Reliable Offline Playback**: Encrypted local storage, offline audio caching, queue persistence across restarts.
- **Atomic Verified Commits**: Always commit changes immediately upon completing and verifying any debug session or roadmap phase once all test cases pass.

## Requirements
Project requirements are tracked in detail in `.planning/REQUIREMENTS.md`. Key functional requirement pillars:
- `REQ-ARCH`: Modular Architecture & Provider Abstraction
- `REQ-UI`: TUI Design System & Compact Components
- `REQ-NAV`: Predictable Layout Shell & Navigation
- `REQ-DATA`: Local Room Persistence & Storage
- `REQ-AUDIO`: ExoPlayer Playback Engine & Background Service
- `REQ-ENG`: Testing, CI/CD Pipeline & Static Analysis Gates

**Validated in Phase 3 (2026-08-05):** `REQ-DATA` (Room persistence layer — 5 entities, 4 DAOs, repository pattern), `REQ-OFF`/encrypted storage (Keystore-backed `EncryptedSharedPreferences` for credentials + DataStore for settings + backup exclusions), and the `REQ-ENG` quality gates (compile, unit, ktlint, detekt, schema export all green).

## Definition of Done (DoD)
Every PR, feature, and roadmap phase MUST satisfy the following quality criteria before merge/completion:
1. `✓ Builds` — Clean Gradle compile with zero errors.
2. `✓ Lint` — Android Lint passes with 0 error-level issues.
3. `✓ Static Analysis` — Detekt passes with 0 critical issues.
4. `✓ Formatting` — ktlint verification passes cleanly.
5. `✓ Unit & Integration Tests` — 100% pass rate with >=85% coverage target.
6. `✓ UI & Compose Tests` — Screen flows and player state tested.
7. `✓ Accessibility` — Screen reader labels and contrast verified.
8. `✓ Performance Budget` — Cold start <2s, 60 FPS list scrolling.
9. `✓ Documentation & ADR` — Architecture Decision Records updated in `docs/adr/`.
10. `✓ CI Passed` — GitHub Actions workflow green (`.github/workflows/ci.yml`).

## Tech Stack & Architecture
- **Platform**: Android (Kotlin)
- **Architecture**: MVVM + Clean Architecture (`Presentation` -> `Domain` -> `Data`)
- **UI Framework**: Jetpack Compose with custom TUI Material theme
- **Audio Engine**: AndroidX Media3 / ExoPlayer with background playback service
- **Database**: Room Database (tracks, playlists, history, cache index)
- **Dependency Injection**: Hilt / Dagger
- **Storage**: EncryptedSharedPreferences / DataStore & Encrypted File Storage
- **Static Analysis & Linting**: Detekt, ktlint, Android Lint
- **CI/CD Pipeline**: GitHub Actions (`.github/workflows/ci.yml`)

## Visual System Tokens
- **Background**: `#0D0D0D`
- **Surface**: `#151515`
- **Accent**: `#1DB954`
- **Primary Text**: `#FFFFFF`
- **Secondary Text**: `#AAAAAA`
- **Typography**: JetBrains Mono (Titles: 18sp, Body: 14sp, Metadata: 12sp)
- **Spacing & Layout**: 8dp padding, 16dp margins, 48dp list row height, square album artwork

## Milestone 1 Progress
- `[x]` **Phase 0**: Engineering Foundation & CI/CD Pipeline (Completed)
- `[x]` **Phase 1**: Architecture Core & Provider API Abstraction (Completed)
- `[x]` **Phase 2**: TUI Design System & Navigation Layout (Completed 2026-08-05)
- `[x]` **Phase 3**: Database & Local Persistence Layer (Completed 2026-08-05) — Room schemas, DAOs, repositories, DataStore + Keystore-backed EncryptedSharedPreferences (MasterKey AES256_GCM), DAO/repo tests, ADR-003. Gap closure (03-05) verified; 7/7 must-haves.
- `[x]` **Phase 4**: Playback Engine & Background Media Service (Completed 2026-08-05) — AndroidX Media3 (1.4.1), PlaybackService (MediaSessionService), PlayerAdapter (ExoPlayer), PlaybackRepository, PlayerViewModel, MainLayout PlayerBar state binding, ADR-004. 7/7 must-haves verified.
- `[x]` **Phase 5**: Provider Integration & Search (Completed 2026-08-06) — YouTubeMusicProvider (InnerTube v1 API), NetworkModule (OkHttp/Retrofit/kotlinx.serialization), SearchViewModel (debounced flow), SearchScreen (dense TUI song table), Coil artwork loading, ADR-005, 84/84 tests passing.
- `[x]` **Phase 6**: Playlists, Queue Management & Library (Completed 2026-08-06) — Queue reorder/clear/remove, QueueEntity/QueueDao Room persistence, QueueViewModel & QueueScreen, LibraryViewModel & LibraryScreen (Tracks/Artists/Albums tabs), PlaylistViewModel & PlaylistScreen (CRUD + Create Dialog + Detail View), ADR-006, 93/93 tests passing.
- `[x]` **Phase 7**: Caching, Downloads & Security Layer (Completed 2026-08-06) — CacheManager (LRU 500MB capacity + CacheIndexDao Room sync), TrackDownloadManager (OkHttp byte stream writer + DownloadStatus flow), NetworkMonitor (ConnectivityManager + offline fallback resolution in PlayerAdapter), ProGuard rules + ADR-007, 96/96 tests passing.
- `[x]` **Phase 8**: Performance Budgets & Accessibility (Completed 2026-08-06) — SettingsUiState, SettingsViewModel, SettingsScreen (active provider selection, disk cache limits 256MB-2GB, streaming quality switch, maintenance buttons), CLIBeatsApp cold start audit (<2s budget), ImageLoaderModule Coil memory tuning (25% memory budget), TalkBack accessibility audit (PlayerBar contentDescriptions, 48dp touch targets, contrast audit) + ADR-008, 100/100 tests passing.
- `[x]` **Phase 9**: Comprehensive Testing & Hardening Suite (Completed 2026-08-06) — SongRepositoryImplTest, PlaylistRepositoryImplTest, InnerTubeHeaderInterceptorTest, PlayerBarTest, SongTableRowTest, PlaybackIntegrationTest (106 unit tests passing / 0 failures), CI workflow file (.github/workflows/ci.yml) synchronized with setup-java@v5 + ADR-009, 106/106 tests passing.

## Documentation Context
- Original specs: `docs/01_Design_Brief.docx` through `docs/06_User_Research_Report_Template.docx`
- Architecture Decision Records: `docs/adr/`
