# ADR-001: Clean Architecture Layering & Hilt Dependency Injection Strategy

## Status
Accepted

## Context & Problem Statement
CLIBeats requires a scalable, maintainable codebase supporting multiple music providers, local caching, and custom TUI UI components. We need an architectural foundation and dependency injection strategy that guarantees testability and prevents layer coupling.

## Decision Drivers
- **Strict Separation of Concerns**: Isolate business logic from UI and data sources.
- **Framework Independence**: Domain layer must be pure Kotlin (zero Android dependencies).
- **Automated Dependency Injection**: Compile-time safety and Jetpack Compose integration.
- **Testability**: Independent unit testing of UseCases, Repositories, and ViewModels.

## Considered Options
1. **MVVM + Clean Architecture (`Presentation` -> `Domain` -> `Data`) with Hilt 2.51+**
2. **Standard MVVM without Clean Architecture (ViewModels calling Repositories directly)**
3. **Koin for Dependency Injection**

## Decision Outcome
Chosen option: **MVVM + Clean Architecture with Hilt 2.51+**.

### Justification
- Clean Architecture separates concerns into three distinct layers:
  - `Presentation`: ViewModels and Compose UI components.
  - `Domain`: Pure Kotlin models, repository interfaces, and use cases.
  - `Data`: Repository implementations, Room DAOs, and MusicProvider network adapters.
- **Hilt 2.51+** provides compile-time injection verification, native Jetpack ViewModel support, and standard Android component scoping (`@SingletonComponent`, `@ActivityComponent`).

### Positive Consequences
- Domain layer can be tested with pure JUnit 4/5 without Robolectric or Android emulators.
- `MusicProvider` interface is isolated in `domain/`, allowing multiple provider implementations without changing UI code.
- Compile-time error detection for missing dependencies via Hilt KSP processor.

### Negative Consequences
- Slightly increased boilerplate (interfaces, data mapping DTOs to domain models).
- KSP annotation processing overhead during builds.
