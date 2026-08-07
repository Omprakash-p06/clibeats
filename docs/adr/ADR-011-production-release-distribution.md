# ADR-011: Production Release & Distribution Architecture

**Date:** 2026-08-07  
**Status:** Accepted  
**Phase:** 11 — Production Release & Distribution  

## Context
CLIBeats requires a production release build configuration, R8/ProGuard obfuscation rules, license compliance audit, and comprehensive release documentation (`REQ-ENG-06`).

## Decision
### 1. Release Packaging & ProGuard
- Production build targets `release` variant with ProGuard rules retaining DTO models, Room DAOs, and Hilt components.
- Release signing configured with fallback debug keystore for automated release builds.

### 2. License Attribution & Documentation
- All third-party open source dependencies (AndroidX, Media3, Hilt, OkHttp, Retrofit, Coil, Room) documented in `docs/LICENSES.md`.
- `RELEASE_NOTES.md` and `USER_GUIDE.md` published to `docs/`.

## Consequences
### Positive
- Production release builds compile cleanly via `./gradlew assembleRelease`.
- 100% open source license compliance verified.
