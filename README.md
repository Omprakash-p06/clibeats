# CLIBeats 🎵

> A free, production-grade Android music client inspired by terminal interfaces (TUI), featuring a text-dense monospaced aesthetic and provider-agnostic architecture.

[![CI Pipeline](https://github.com/Omprakash-p06/clibeats/actions/workflows/ci.yml/badge.svg)](https://github.com/Omprakash-p06/clibeats/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-SDK%2034-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📌 Overview

**CLIBeats** reimagines mobile music streaming with terminal-inspired design principles: high contrast, monospaced typography, flat zero-radius surfaces, and zero decorative animations or blurs. Under the hood, CLIBeats is built with strict Clean Architecture boundaries and a modular multi-provider backend.

---

## 🎨 Visual Aesthetics & Design System

- **Monochrome Dark Theme**: Background (`#0D0D0D`), Surface (`#151515`), Spotify Green Accent (`#1DB954`), Primary Text (`#FFFFFF`).
- **Typography**: Bundled **JetBrains Mono** font hierarchy across all UI components.
- **Flat Geometry**: 0dp corner radius globally — no rounded bubble cards or glassmorphism.
- **Dense Components**: 48dp list rows with 32x32dp square album artwork and persistent 64dp player bar.

---

## 🏗 Architecture & Tech Stack

CLIBeats adheres to **MVVM + Clean Architecture** (`Presentation` ➔ `Domain` ➔ `Data`):

```
┌────────────────────────────────────────────────────────┐
│               Presentation Layer (UI)                  │
│    Jetpack Compose · Material3 Adaptive · MainLayout   │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                 Domain Layer (Core)                    │
│   MusicProvider Contract · Track / Playlist Models     │
└──────────────────────────▲─────────────────────────────┘
                           │
┌──────────────────────────┴─────────────────────────────┐
│                 Data Layer (Storage)                   │
│   Room Database · DataStore · EncryptedSharedPreferences│
└────────────────────────────────────────────────────────┘
```

- **Platform**: Android (Min SDK 26, Target SDK 34)
- **UI Framework**: Jetpack Compose (Material3 + Navigation Suite)
- **Audio Engine**: AndroidX Media3 / ExoPlayer with background foreground service
- **Database**: Room 2.6 with KSP (Kotlin Symbol Processing) & JSON Schema Export
- **Security & Persistence**: `EncryptedSharedPreferences` (AES256_GCM via Android Keystore `MasterKey`) + DataStore Preferences
- **Dependency Injection**: Hilt / Dagger 2.51
- **Testing & Screenshot Baselines**: JUnit 4, Mockito, Paparazzi screenshot regression suite
- **Static Analysis & Linting**: Detekt, ktlint, Android Lint

---

## ⚙️ Building & Running Locally

### Prerequisites
- JDK 17
- Android SDK 34 (Android 14)
- Gradle 8.5+

### Build Commands

```powershell
# Clone repository
git clone https://github.com/Omprakash-p06/clibeats.git
cd clibeats

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Run Paparazzi screenshot verification
.\gradlew.bat verifyPaparazziDebug

# Run static analysis & formatting checks
.\gradlew.bat ktlintCheck
.\gradlew.bat detekt

# Compile Debug APK
.\gradlew.bat assembleDebug
```

---

## 📋 Quality Gates & Definition of Done (DoD)

Every commit and pull request must pass 10 automated quality criteria before merge:

1. `✓ Builds` — Clean Gradle compile with 0 errors.
2. `✓ Lint` — Android Lint passes with 0 error-level issues.
3. `✓ Static Analysis` — Detekt passes with 0 critical issues.
4. `✓ Formatting` — ktlint formatting passes cleanly.
5. `✓ Unit Tests` — 100% pass rate.
6. `✓ Screenshot Tests` — Paparazzi snapshot baselines verified.
7. `✓ Accessibility` — Screen reader labels and 48dp touch targets verified.
8. `✓ Performance` — Cold start <2s budget.
9. `✓ ADR` — Architecture Decision Records updated in `docs/adr/`.
10. `✓ CI` — GitHub Actions workflow green (`.github/workflows/ci.yml`).

---

## 📜 Architecture Decision Records (ADRs)

- [ADR-000: Architecture Decision Record Template](docs/adr/ADR-000-template.md)
- [ADR-001: Clean Architecture Layering & Hilt DI Strategy](docs/adr/ADR-001-clean-architecture-hilt.md)
- [ADR-002: MusicProvider Abstraction Layer](docs/adr/ADR-002-music-provider-abstraction.md)
- [ADR-003: Encrypted Storage & Local Persistence Strategy](docs/adr/ADR-003-encrypted-storage-local-persistence.md)

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
