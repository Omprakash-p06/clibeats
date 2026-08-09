# PROJECT: CLIBeats

## What This Is
CLIBeats is a free, production-grade Android music client inspired by terminal interfaces (TUI). v1.0 ships a complete client: a compact, text-dense layout with monospaced JetBrains Mono typography, high-contrast dark theme, persistent playback controls, encrypted local persistence, offline caching, and a provider-agnostic architecture backed by the CliBeats Gateway for search and streaming.

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
Project requirements are tracked in detail in `.planning/REQUIREMENTS.md` (fresh for the next milestone). Key functional requirement pillars:
- `REQ-ARCH`: Modular Architecture & Provider Abstraction
- `REQ-UI`: TUI Design System & Compact Components
- `REQ-NAV`: Predictable Layout Shell & Navigation
- `REQ-DATA`: Local Room Persistence & Storage
- `REQ-AUDIO`: ExoPlayer Playback Engine & Background Service
- `REQ-ENG`: Testing, CI/CD Pipeline & Static Analysis Gates

**Validated in v1.0 (2026-08-10):** All 12 engineering phases verified `passed` with 44/44 plans executed. Highlights: `REQ-DATA` (Room persistence — 5 entities, 4 DAOs, repository pattern), `REQ-AUDIO` (Media3 PlaybackService + PlayerAdapter), encrypted storage (Keystore-backed `EncryptedSharedPreferences` AES256_GCM + backup exclusions), provider abstraction evolved to the CliBeats Gateway architecture (ADR-012–ADR-020), offline cache/download layer, telemetry with PII redaction, and production release (R8, licenses, 109 unit tests green). Full requirement outcomes archived in `.planning/milestones/v1.0-REQUIREMENTS.md`.

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

## Current State (v1.0 shipped 2026-08-10)

- ✅ **Milestone v1.0 shipped**: 12/12 phases verified `passed`, 44/44 plans, 109 unit tests green, production `assembleRelease` clean.
- **Provider architecture**: CliBeats Gateway (Fastify + Redis, provider plugin architecture ADR-012–ADR-014, auth/session ADR-015, canonical models/errors/events ADR-016–018, config ADR-019, API versioning ADR-020) serves search/streaming; Android client consumes it via `GatewayMusicProvider`/`GatewayApi`.
- **Release artifacts**: `docs/RELEASE_NOTES.md`, `docs/USER_GUIDE.md`, `docs/LICENSES.md`, ADR-011, production signing + R8.
- **Known deferred**: 4 open debug sessions, 4 pending phase-02 UAT scenarios, detekt config refinement (D-02) — see `.planning/STATE.md` Deferred Items.

## Next Milestone Goals

- Validate v1.0 in the field: beta feedback, crash telemetry review, SUS scoring.
- Polish release pipeline: real keystore signing (non-debug fallback), Play/APK distribution, automated release evidence.
- Refine `config/detekt/detekt.yml` (duplicate Indentation rule, over-broad ForbiddenImport) to remove per-file suppressions.
- Close out remaining UAT scenarios for the TUI design system (phase 02).

## Milestone 1 Progress (archived in `.planning/milestones/v1.0-ROADMAP.md`)
- `[x]` **Phase 0**: Engineering Foundation & CI/CD Pipeline (Completed 2026-08-04)
- `[x]` **Phase 1**: Architecture Core & Provider API Abstraction (Completed 2026-08-05)
- `[x]` **Phase 2**: TUI Design System & Navigation Layout (Completed 2026-08-05)
- `[x]` **Phase 3**: Database & Local Persistence Layer (Completed 2026-08-05) — Room schemas, DAOs, repositories, DataStore + Keystore-backed EncryptedSharedPreferences (MasterKey AES256_GCM), DAO/repo tests, ADR-003. Gap closure (03-05) verified; 7/7 must-haves.
- `[x]` **Phase 4**: Playback Engine & Background Media Service (Completed 2026-08-05) — AndroidX Media3 (1.4.1), PlaybackService (MediaSessionService), PlayerAdapter (ExoPlayer), PlaybackRepository, PlayerViewModel, MainLayout PlayerBar state binding, ADR-004. 7/7 must-haves verified.
- `[x]` **Phase 5**: Provider Integration & Search (Completed 2026-08-06) — YouTubeMusicProvider (InnerTube v1 API), NetworkModule (OkHttp/Retrofit/kotlinx.serialization), SearchViewModel (debounced flow), SearchScreen (dense TUI song table), Coil artwork loading, ADR-005, 84/84 tests passing.
- `[x]` **Phase 6**: Playlists, Queue Management & Library (Completed 2026-08-06) — Queue reorder/clear/remove, QueueEntity/QueueDao Room persistence, QueueViewModel & QueueScreen, LibraryViewModel & LibraryScreen (Tracks/Artists/Albums tabs), PlaylistViewModel & PlaylistScreen (CRUD + Create Dialog + Detail View), ADR-006, 93/93 tests passing.
- `[x]` **Phase 7**: Caching, Downloads & Security Layer (Completed 2026-08-06) — CacheManager (LRU 500MB capacity + CacheIndexDao Room sync), TrackDownloadManager (OkHttp byte stream writer + DownloadStatus flow), NetworkMonitor (ConnectivityManager + offline fallback resolution in PlayerAdapter), ProGuard rules + ADR-007, 96/96 tests passing.
- `[x]` **Phase 8**: Performance Budgets & Accessibility (Completed 2026-08-06) — SettingsUiState, SettingsViewModel, SettingsScreen (active provider selection, disk cache limits 256MB-2GB, streaming quality switch, maintenance buttons), CLIBeatsApp cold start audit (<2s budget), ImageLoaderModule Coil memory tuning (25% memory budget), TalkBack accessibility audit (PlayerBar contentDescriptions, 48dp touch targets, contrast audit) + ADR-008, 100/100 tests passing.
- `[x]` **Phase 9**: Comprehensive Testing & Hardening Suite (Completed 2026-08-06) — SongRepositoryImplTest, PlaylistRepositoryImplTest, InnerTubeHeaderInterceptorTest, PlayerBarTest, SongTableRowTest, PlaybackIntegrationTest (106 unit tests passing / 0 failures), CI workflow file (.github/workflows/ci.yml) synchronized with setup-java@v5 + ADR-009, 106/106 tests passing.
- `[x]` **Phase 10**: Beta Validation & Telemetry (Completed 2026-08-07) — AnalyticsEvent, TelemetryTracker, CrashReporter, TimberTelemetryTracker, TimberCrashReporter (with automated PII & bearer token redaction), TelemetryModule Hilt bindings, TimberCrashReporterTest & TimberTelemetryTrackerTest + ADR-010, 108/108 tests passing.
- `[x]` **Phase 11**: Production Release & Distribution (Completed 2026-08-07) — Production release build type and signing configuration in app/build.gradle.kts, R8 ProGuard rules in proguard-rules.pro, open source license audit inventory (docs/LICENSES.md), LicenseComplianceTest (109/109 unit tests passing), RELEASE_NOTES.md, USER_GUIDE.md, ADR-011, 109/109 tests passing.

## Documentation Context
- Original specs: `docs/01_Design_Brief.docx` through `docs/06_User_Research_Report_Template.docx`
- Architecture Decision Records: `docs/adr/` (ADR-001 through ADR-020)

---
*Last updated: 2026-08-10 after v1.0 milestone*
