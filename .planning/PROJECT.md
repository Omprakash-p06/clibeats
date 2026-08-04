# PROJECT: CLIBeats

## Vision
Build a free Android music client inspired by terminal interfaces (TUI) using a compact, keyboard-inspired UI while supporting multiple music providers through a modular abstraction layer.

## Core Values & Design Principles
- **Text-First & Dense**: Maximized information density with monospaced typography (JetBrains Mono) and flat surfaces.
- **Minimal Monochrome Aesthetic**: High contrast dark theme (`#0D0D0D` background, `#151515` surface, `#1DB954` accent) without decorative blurs, glassmorphism, or bounce animations.
- **Fast & Predictable Navigation**: Quick navigation, persistent player controls, compact song tables, and keyboard/touch efficiency.
- **Provider-Agnostic Architecture**: Decoupled `MusicProvider` interface supporting official and custom media sources.
- **Reliable Offline Playback**: Encrypted local storage, offline audio caching, queue persistence across restarts.

## Tech Stack & Architecture
- **Platform**: Android (Kotlin)
- **Architecture**: MVVM + Clean Architecture (`Presentation` -> `Domain` -> `Data`)
- **UI Framework**: Jetpack Compose with custom TUI Material theme
- **Audio Engine**: AndroidX Media3 / ExoPlayer with background playback service
- **Database**: Room Database (tracks, playlists, history, cache index)
- **Dependency Injection**: Hilt / Dagger
- **Storage**: EncryptedSharedPreferences / DataStore & Encrypted File Storage

## Visual System Tokens
- **Background**: `#0D0D0D`
- **Surface**: `#151515`
- **Accent**: `#1DB954`
- **Primary Text**: `#FFFFFF`
- **Secondary Text**: `#AAAAAA`
- **Typography**: JetBrains Mono (Titles: 18sp, Body: 14sp, Metadata: 12sp)
- **Spacing & Layout**: 8dp padding, 16dp margins, 48dp list row height, square album artwork

## Documentation Context
Original design specs available in `docs/`:
- `01_Design_Brief.docx`
- `02_UI_Design_Specification.docx`
- `03_SRS.docx`
- `04_SDD.docx`
- `05_FSD.docx`
- `06_User_Research_Report_Template.docx`
