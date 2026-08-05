# Testing Patterns

**Analysis Date:** 2026-08-05

## Test Framework

**Runner:**
- **JUnit 4** (`junit:junit:4.13.2` — version pinned in `gradle/libs.versions.toml`, declared as `testImplementation` in `app/build.gradle.kts`).
- No JUnit 5, no Kotlin test framework (`kotlin.test`) present.

**Assertion Library:**
- `org.junit.Assert` static methods (`assertEquals`, `assertFalse`, `assertNull`) imported explicitly per call site.

**Support libraries (not yet used but declared):**
- `androidx.test.espresso:espresso-core:3.6.1` declared as `androidTestImplementation` in `app/build.gradle.kts` — ready for instrumentation tests, though **no `androidTest` sources exist yet** (`app/src/androidTest/` is empty).

**Run Commands:**
```bash
./gradlew testDebugUnitTest        # Unit tests
./gradlew ktlintCheck              # Code style gate
./gradlew detekt                   # Static analysis gate
./gradlew lintDebug                # Android lint gate
./gradlew test                    # All test variants
```
- A convenience wrapper `scripts/check-quality-gates.sh` runs all four quality gates (ktlint, detekt, lint, unit tests) sequentially with `set -euo pipefail`.

## Test File Organization

**Location:**
- Co-located by package under `app/src/test/java/` mirroring the main source package. Examples:
  - `app/src/test/java/com/clibeats/domain/model/TrackTest.kt` tests `app/src/main/java/com/clibeats/domain/model/Track.kt` (and `PlaybackState.kt`).

**Naming:**
- `<ClassBeingTested>Test.kt` — e.g. `TrackTest.kt`.

**Structure:**
```
app/src/
├── main/java/com/clibeats/...      # production code
└── test/java/com/clibeats/...      # unit tests (mirror packages)
```

## Test Structure

**Suite Organization:** A plain JUnit4 test class (no setup/teardown boilerplate). No `@Before`/`@After`, no `Mock` initialization, no Robolectric.

```kotlin
package com.clibeats.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackTest {
    @Test
    fun track_construction_succeeds_with_valid_fields() {
        val track = Track(...)
        assertEquals("track_1", track.id)
    }
}
```

**Name style (project convention):** Test method names use descriptive snake_case describing the behavior being verified:
- `track_construction_succeeds_with_valid_fields`
- `track_equality_is_structural`
- `track_copy_updates_single_field`
- `playbackState_defaults_to_not_playing`

**Patterns:**
- **Arrange/Act/Assert**: construct input, invoke, assert with `assertEquals`/`assertFalse`/`assertNull`.
- No teardown needed — tests are stateless (pure data-class assertions).
- No test doubles or mocks used in the existing test file.

## Mocking

**Framework:** **None.** No Mockito, MockK, or similar library is declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`.

**Patterns:** Not established — the only existing test targets immutable data classes and requires no mocking.

**Guidance for new code:**
- Add MockK (or Mockito) and `kotlinx-coroutines-test` before writing repository/use-case tests against `MusicProvider` or Hilt-managed components.
- Abstract provider results behind `ProviderResult` (`app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`) so provider boundaries are mockable without Android framework dependencies (per ADR-001, domain is pure Kotlin and testable with plain JUnit).

**What to Mock:**
- Provider/Repository interfaces (`MusicProvider` in `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`) when testing use cases/ViewModels.

**What NOT to Mock:**
- Immutable domain models (`Track`, `Album`, `Artist`, `Playlist`) — construct real instances directly.

## Fixtures and Factories

**Test Data:**
- Inline construction with named arguments at the call site (see `TrackTest.kt`). No factory/helper classes, no external fixture files yet.

```kotlin
val track =
    Track(
        id = "track_1",
        title = "Midnight Sun",
        artist = "Vaporwave",
        album = "Neon Lights",
        durationMs = 210000L,
        artworkUrl = "https://example.com/art.jpg",
        streamUrl = "https://example.com/audio.mp3",
        providerId = "local",
    )
```

**Location:**
- Fixtures are colocated within the test files that use them. Consider extracting shared builders (`data class` factories) once multiple test suites share model construction.

## Coverage

**Requirements:** **None enforced.** No JaCoCo (or similar) is configured in `app/build.gradle.kts`, and no coverage threshold exists in CI.

**View Coverage:**
```bash
./gradlew testDebugUnitTest
```
- Test reports are generated to `app/build/reports/tests/`.
- CI uploads `app/build/reports/tests/`, `app/build/reports/detekt/`, and `app/build/reports/lint-results-debug.html` as artifacts (`.github/workflows/ci.yml`). No HTML coverage report is produced.

## Test Types

**Unit Tests:**
- JUnit4 tests targeting pure domain models. Currently limited to one class (`TrackTest.kt` covering `Track` and `PlaybackState`).
- CI runs `./gradlew testDebugUnitTest` (`.github/workflows/ci.yml`).
- ADR-001 (`docs/adr/ADR-001-architecture-and-di-strategy.md`) states the design goal: domain layer testable with pure JUnit 4/5 without Robolectric or Android emulators.

**Integration Tests:**
- None present. Expected for repository/data-layer code connecting to Room and music providers (not yet implemented).

**E2E/Instrumentation Tests:**
- `espresso-core` is declared as `androidTestImplementation` but **no `app/src/androidTest/` sources exist**. The CI workflow does not run connected/interconnected tests or Compose UI tests. No `compose-ui-test-junit4` dependency is in the catalog.
- **Note from scope:** Compose UI tests are anticipated (CI files and build config reference Compose), but no UI-test harness/artifact is currently configured in `gradle/libs.versions.toml` or `app/build.gradle.kts`.

## Common Patterns

**Async Testing:** Not yet established. `MusicProvider` exposes `suspend` functions; when testing them, introduce `kotlinx-coroutines-test` (`runTest`, StandardTestDispatcher) and inject dispatchers. No existing async test exists.

**Error Testing:** Not yet established. When a `ProviderResult.Error(message, cause)` path is added, assert on the sealed type — e.g. `assertTrue(result is ProviderResult.Error)` and verify `result.message`. No error-path test currently exists.

**Testing Gaps (drives future phase planning):**
- Only model-layer behavior is covered; use cases, ViewModels, repositories, and providers are untested (no code exists yet).
- No instrumentation/Compose UI tests despite `espresso-core` being wired.
- No code coverage metric in place.

---

*Testing analysis: 2026-08-05*