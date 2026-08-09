# Phase 1 Context: Architecture Core & Provider API Abstraction

## Phase Goal
Establish clean architecture project structure, dependency injection, and core domain models.

## Decisions (Locked)
- **D-01 — DI Framework**: Hilt 2.51+ (over Dagger bare or Koin) — standard Android recommendation, integrates cleanly with Compose.
- **D-02 — Kotlin DSL Build**: All Gradle files use `.kts` (Kotlin DSL) with a version catalog (`libs.versions.toml`) — consistent with modern Android tooling, enables IDE autocomplete.
- **D-03 — ProviderResult type**: Sealed class (not `kotlin.Result`) — required to expose `Loading` state for StateFlow emissions.
- **D-04 — Domain Purity**: `domain/` package has zero `android.*` imports — enforces Clean Architecture layering, makes domain independently testable on JVM.
- **D-05 — MusicProvider as interface**: Interface (not abstract class) — allows Hilt multi-bindings when multiple providers are added in Phase 5+.

## Plans
| Plan | Wave | Description |
|------|------|-------------|
| 01-01 | 1 | Android project scaffold + version catalog + Hilt wiring |
| 01-02 | 2 | Domain models + MusicProvider interface + ProviderResult |

## Requirements Addressed
- REQ-NFR-04: MVVM + Clean Architecture (presentation → domain → data)
- REQ-NFR-05: Atomic git commit as soon as any debug session or phase is completed and verified
- REQ-SET-01: MusicProvider abstraction interface (foundation for multi-provider)
