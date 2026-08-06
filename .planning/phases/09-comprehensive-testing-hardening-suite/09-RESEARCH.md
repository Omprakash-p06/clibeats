# Phase 9: Comprehensive Testing & Hardening Suite — Technical Research

## Objective
Research test coverage expansion, Compose UI component tests, integration test flows, and CI static analysis hardening for Phase 9.

## 1. Requirements Mapping
- **`REQ-ENG-01`**: Unit test suite expansion across Repositories, Interceptors, Mappers.
- **`REQ-ENG-06`**: CI pipeline synchronization, zero Android Lint errors, zero Detekt issues, zero ktlint errors.
- **`REQ-ENG-07`**: Compose UI component tests (`PlayerBarTest`, `SongTableRowTest`) and E2E integration test flows.

## 2. Technical Architecture

### Data & Domain Layer Unit Tests (`REQ-ENG-01`)
- `SongRepositoryImplTest`: verify track fetching, cache check, database persistence.
- `PlaylistRepositoryImplTest`: verify playlist creation, deletion, track addition/removal.
- `InnerTubeHeaderInterceptorTest`: verify client headers (`x-youtube-client-name`, `x-youtube-client-version`) injected properly without leaking tokens in logs.

### Compose UI Component Tests (`REQ-ENG-07`)
- Using `androidx.compose.ui.test.junit4.createComposeRule()`.
- Test `PlayerBar`: verify title/artist rendering, toggle Play/Pause action, progress bar.
- Test `SongTableRow`: verify click triggers callback, active track indicator highlights correctly.

### Static Analysis & CI Pipeline (`REQ-ENG-06`)
- Ensure `.github/workflows/ci.yml` runs full quality gate suite (`assembleDebug`, `testDebugUnitTest`, `ktlintCheck`, `detekt`).
- Ensure 0 Android Lint errors and 0 Detekt issues.

## 3. Quality Gate Targets
- 0 compile errors (`assembleDebug`).
- 0 Android Lint errors.
- 0 Detekt critical issues.
- 0 ktlint formatting errors.
- 100% passing unit test suite in `testDebugUnitTest`.
