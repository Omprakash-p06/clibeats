# PROJECT: CLIBeats

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

## Documentation Context
- Original specs: `docs/01_Design_Brief.docx` through `docs/06_User_Research_Report_Template.docx`
- Architecture Decision Records: `docs/adr/`
