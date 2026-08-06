# ADR-009: Comprehensive Testing & Hardening Architecture

**Date:** 2026-08-06
**Status:** Accepted
**Phase:** 9 — Comprehensive Testing & Hardening Suite

## Context

CLIBeats requires >=85% unit/integration coverage (`REQ-ENG-01`), zero static analysis issues (`REQ-ENG-06`), and automated Compose UI component tests (`REQ-ENG-07`).

## Decision

### 1. Multi-Tiered Testing Suite
- Domain & Data Layer: Unit tests for all Repositories, DAOs, Interceptors, and Mappers.
- Presentation Layer: Unit tests for 100% of ViewModels (`PlayerViewModel`, `SearchViewModel`, `QueueViewModel`, `LibraryViewModel`, `PlaylistViewModel`, `SettingsViewModel`).
- UI Components: Compose UI tests for `PlayerBar` and `SongTableRow`.
- Integration: `PlaybackIntegrationTest` verifying end-to-end user flows.

### 2. CI Quality Gate Enforcement
- `.github/workflows/ci.yml` enforces mandatory execution of `assembleDebug`, `testDebugUnitTest`, `ktlintCheck`, and `detekt` prior to merge.

## Consequences

### Positive
- Zero regression risk across core playback and database layers.
- Automated code formatting and static analysis compliance.
- Verified CI pipeline synchronization.
