# Codebase Concerns

**Analysis Date:** 2026-08-05

## Scope Note

This codebase is at the very start of its roadmap: **Phases 0 (Engineering Foundation) and 1 (Architecture Core & Provider API) are complete**; Phases 2–11 (UI, storage, playback, providers, caching, performance, testing, telemetry, release) are planned but not started. There are only 9 production Kotlin files and 1 test file. As a result, most concerns below are **structural and forward-looking** (foundations that will compound once the planned UI/data/playback layers land), plus a few concrete defects already present today.

## Tech Debt

### CI Pipeline is dead — triggers reference branches that do not exist

- Issue: `.github/workflows/ci.yml` triggers on `push`/`pull_request` to `main` and `develop`, but the repository's only branch is `master` (verified via `git branch`). There is no configured git remote.
- Files: `.github/workflows/ci.yml`
- Impact: **The entire quality-gate pipeline (ktlint, detekt, Android lint, `assembleDebug`, `testDebugUnitTest`) currently never executes.** Definition of Done requirement "CI Passed" (see `.planning/PROJECT.md`) is silently unverifiable, and regressions can merge undetected.
- Fix approach: Align the workflow branches with reality — change `branches:` to `[ master ]`, or rename the default branch to `main`. Add a remote and push. Reconsider keeping the `develop` branch trigger until that branch is actually used.

### Stale / inconsistent planning state

- Issue: `.planning/STATE.md` reports `**Current Phase**: Phase 0 - Engineering Foundation & CI/CD Pipeline`, `**Phase Status**: Pending Planning`, and `**Next Action**: Run /gsd-plan-phase 0` — yet the same file's Phase Matrix marks Phases 0 and 1 as `COMPLETED`, and phase execution plans/summaries exist under `.planning/phases/00-engineering-foundation/` and `.planning/phases/01-architecture-core-provider-api/`.
- Files: `.planning/STATE.md`
- Impact: Automation that reads STATE.md (planning/execution commands) will target the wrong next phase. Confuses any agent or human inspecting project status.
- Fix approach: Update `Current Phase`, `Phase Status`, and `Next Action` to reflect Phase 2 being next. Reconcile this file on every phase transition.

### Empty scaffolding masquerading as a feature-complete surface

- Issue: The app compiles and ships one blank screen. `MainActivity.kt` has an empty `setContent { }` body, `AppModule.kt` is an empty `@Module @InstallIn(SingletonComponent::class) object`, and `presentation/`, `data/`, and `domain/` directories are placeholder `.gitkeep` markers. No theme is applied, so the launcher renders a plain `Theme.Material.NoActionBar` window with nothing in it.
- Files: `app/src/main/java/com/clibeats/MainActivity.kt`, `app/src/main/java/com/clibeats/di/AppModule.kt`, `app/src/main/java/com/clibeats/presentation/.gitkeep`, `app/src/main/java/com/clibeats/data/.gitkeep`, `app/src/main/java/com/clibeats/domain/.gitkeep`, `app/src/main/AndroidManifest.xml`
- Impact: Nothing user-visible works yet. This is expected per the roadmap (UI wires in Phase 2), but the `.gitkeep` markers underlines the discrepancy: `domain/` now contains real files (`Track.kt`, `MusicProvider.kt`, etc.) yet still carries a redundant `.gitkeep`.
- Fix approach: Remove `app/src/main/java/com/clibeats/domain/.gitkeep` (domain is populated). Leave `presentation/`/`data/` markers until those phases land. Do not treat the current app as releasable.

### Detekt `ForbiddenImport` rule is a global footgun for future test/integration code

- Issue: `config/detekt/detekt.yml` blocks `com.clibeats.data.*` imports globally, with a reason string scoped to "Presentation layer". Detekt's default source set run includes **test sources**, so an integration test (Phase 5/9) that legitimately constructs or exercises a `com.clibeats.data.*` repository implementation will trip this rule and fail the `maxIssues: 0` gate.
- Files: `config/detekt/detekt.yml`, `app/build.gradle.kts`
- Impact: Once repository/data-layer code exists, writing integration tests against it becomes obstructed; teams may paper over it by adding the import to an ignore list, weakening the architecture guard.
- Fix approach: Make the rule intentionally scoped (e.g. target only `app/src/main` via a detekt `excludes` filter on test source sets, or split config per source set) so tests can import data-layer internals while production `presentation` code still cannot.

### "Domain must be pure Kotlin" (D-04) is stated but not enforced

- Issue: `docs/adr/ADR-001` and decision D-04 in `.planning/phases/01-architecture-core-provider-api/CONTEXT.md` mandate that `domain/` has zero `android.*` imports. Today that holds by inspection, but **no tool enforces it** — the detekt config has no rule blocking `android.*` in the domain package.
- Files: `config/detekt/detekt.yml`, `docs/adr/ADR-001-architecture-and-di-strategy.md`, `.planning/phases/01-architecture-core-provider-api/CONTEXT.md`
- Impact: A silent regression (accidental `android.` import in a domain file) would not fail CI despite the ADR stating it as a hard decision. The purity guarantee becomes convention-only.
- Fix approach: Add a detekt `ForbiddenImport` (or Android Lint rule) that rejects `android.*` imports within `com.clibeats.domain` files.

### `compileSdk = 35` used with AGP 8.5.2 (out of supported matrix), relying on a suppress flag

- Issue: `app/build.gradle.kts` sets `compileSdk = 35`, but AGP 8.5.2's officially supported maximum compile SDK is 34. The resulting "unsupported compile SDK" warning is silenced via `android.suppressUnsupportedCompileSdk=35` in `gradle.properties`.
- Files: `app/build.gradle.kts`, `gradle.properties`
- Impact: Building against an SDK newer than the toolchain supports can produce missing/incorrect resource merges or AndroidX behavior the AGP was never tested against. Silence of the warning hides the real recommendation.
- Fix approach: Upgrade AGP to 8.6+ (which officially supports compileSdk 35) or drop `compileSdk` to 34 and remove the suppress flag. Prefer the AGP upgrade before Phase 2 work.

### Release build is not a production artifact

- Issue: `app/build.gradle.kts` sets `isMinifyEnabled = false` for `release`, `proguard-rules.pro` contains only a comment, and `versionCode = 1` / `versionName = 0.1.0`. There are no signing configs, no R8 rules, no version bumping for the release track.
- Files: `app/build.gradle.kts`, `app/proguard-rules.pro`
- Impact: Any `release` build today is effectively a debug build with no shrinking/obfuscation and placeholder identifiers. Cannot be distributed safely (trivially reverse-engineered; no R8 stripping).
- Fix approach: Deferred to Phase 11 by the roadmap, but plan for it — minimum: document that `release` is not distributable until `isMinifyEnabled = true`, real ProGuard rules, and signing are configured.

## Known Bugs

### CI never executes on the only existing branch

- Symptoms: No pipeline runs on `master` pushes or PRs; `main`/`develop` branches referenced by `.github/workflows/ci.yml` do not exist.
- Files: `.github/workflows/ci.yml`
- Trigger: Any push or PR to `master`.
- Workaround: None local — would require manually editing to `[ master ]`. This is the highest-impact defect in the repo because it voids the single automated quality gate the project explicitly prioritizes.

### Android 15 (targetSdk 35) edge-to-edge not prepared for

- Symptoms: No Compose theme, `enableEdgeToEdge`, or insets handling exists; `MainActivity` uses `Theme.Material.NoActionBar`. For apps targeting SDK 35, edge-to-edge is **enforced** on Android 15 — content will be drawn under system bars once UI is added in Phase 2.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/clibeats/MainActivity.kt`
- Trigger: Phase 2 introduces the first Compose screen on an Android 15 device.
- Workaround: Handle system-bar insets and `enableEdgeToEdge()` as part of the Phase 2 theme work.
- Fix approach: Add edge-to-edge handling in the Phase 2 TUI shell before styling any navigation/player bars.

## Security Considerations

### Android Auto Backup is enabled with no extraction rules

- Risk: `android:allowBackup="true"` is set in `app/src/main/AndroidManifest.xml` with **no** `dataExtractionRules` (API 31+) or `fullBackupContent` attribute. When the roadmap's "Encrypted local storage / EncryptedSharedPreferences / DataStore" and offline caches arrive, those files will be included in Android Auto Backup. Backups are transported to Google Drive daily with limited transport-encryption guarantees and can be restored onto other devices, undermining the stated "encrypted local storage" vision (`.planning/PROJECT.md`).
- Files: `app/src/main/AndroidManifest.xml`
- Current mitigation: No credential/secret storage exists yet, so nothing sensitive is currently exposed. This is a forward risk that becomes real at Phase 3/7.
- Recommendations: Before Phase 3 stores any app secrets, either set `android:allowBackup="false"` or add explicit `dataExtractionRules`/`fullBackupContent` that exclude credential and cache-index files. Decide this in an ADR.

### No network security policy — relies on implicit defaults

- Risk: No `android:networkSecurityConfig` and no `usesCleartextTraffic` attribute. The app declares `INTERNET` permission. For `targetSdk 35` cleartext (HTTP) traffic is blocked by default (good), but the moment a third-party music provider returns an `http://` stream or API URL (Phase 5), the app will silently fail to connect unless a config explicitly allows a scoped exception.
- Files: `app/src/main/AndroidManifest.xml`
- Current mitigation: Default cleartext blocking on targetSdk 35; only `https://` used in fixtures (`app/src/test/java/com/clibeats/domain/model/TrackTest.kt`).
- Recommendations: Add a `network_security_config.xml` that keeps cleartext blocked globally and only allows specific trusted hosts if any provider legally requires it. Keep HTTPS-only as the default.

### No secrets/dependency scanning in CI

- Risk: No secret-scanning step and no dependency vulnerability scan in `.github/workflows/ci.yml` (Phase 7 plans "secret scanning, dependency vulnerability scan"). Provider API keys are anticipated in Phase 5; without scanning, a committed key can slip into git history.
- Files: `.github/workflows/ci.yml`, `.gitignore`
- Current mitigation: `.gitignore` excludes `local.properties` and `.env`-class files; no secrets are present in tracked source today (searched — none found).
- Recommendations: Add `gitleaks`-style secret scanning and `./gradlew dependencyCheckAnalyze` (or equivalent) as CI stages before Phase 5 provider keys are introduced.

### No dependency verification / lockfile

- Risk: `settings.gradle.kts` resolves from `google()`/`mavenCentral()`, and no `gradle/verification-metadata.xml` or Gradle dependency locking is enabled. The supply chain of the pinned catalog (`gradle/libs.versions.toml`) is unverified.
- Files: `gradle/libs.versions.toml`, `settings.gradle.kts`
- Current mitigation: All versions are pinned in the catalog (no floating `+`), reducing drift risk.
- Recommendations: Enable Gradle dependency verification once the dependency set stabilizes, and revisit the catalog's translation hash when versions change.

### Release APK is unminified / non-obfuscated

- Risk: Because `isMinifyEnabled = false`, the release APK preserves class/method names and unused code, trivializing reverse engineering of the `MusicProvider` abstraction and any provider-agnostic logic.
- Files: `app/build.gradle.kts`
- Recommendations: Enable R8 for the production distribution (planned for Phase 11) and keep Hilt's generated components in `proguard-rules.pro`.

## Performance Bottlenecks

### No baseline exists to enforce the DoD performance budget

- Problem: The app is blank, so there is nothing to measure; nonetheless no Macrobenchmark/JankStats wiring or performance test harness is present. The DoD explicitly demands "Cold start <2s, 60 FPS list scrolling" (`.planning/PROJECT.md`), but nothing can verify it until a benchmark module is added (planned Phase 8).
- Files: `app/build.gradle.kts`, `.github/workflows/ci.yml`
- Cause: Phase 8 (Performance Budgets) is not started; no `:benchmark` module exists.
- Improvement path: Add a Macrobenchmark module and a baseline `.json` profile as part of Phase 8, before the song-table components it targets.

### Gradle build memory and CI time are tight for the growing toolchain

- Problem: `org.gradle.jvmargs=-Xmx2048m` (`gradle.properties`) is modest for Compose + KSP + Hilt + detekt end-to-end, and CI runs five separate Gradle invocations serially (`--continue`) under a 20-minute `timeout-minutes` budget with cold dependency cache on first runs.
- Files: `gradle.properties`, `.github/workflows/ci.yml`
- Cause: Single-job workflow; toolchain growth (Room, Media3, coroutines-test, UI tests) in Phases 3–9 will increase build cost.
- Improvement path: Raise JVM heap to 4g, add Gradle build caching, and consider splitting CI into parallel jobs (lint/static on one, build/tests on another) as the project grows.

## Fragile Areas

### Strict, all-or-nothing quality gates from day one

- Component: `config/detekt/detekt.yml` with `build.maxIssues: 0` + `excludeCorrectable: false` (any issue — including formatting — fails), combined with Android Lint `abortOnError = true`. `warningsAsErrors = false` is set for lint warnings, but detekt's `maxIssues: 0` has no warning/error distinction.
- Files: `config/detekt/detekt.yml`, `app/build.gradle.kts`
- Why fragile: Both ktlint (`ktlintCheck`) **and** detekt-formatting enforce overlapping ktlint-style rules. Any cross-tool disagreement, or a one-off formatting slip, hard-fails CI with little diagnostic nuance.
- Safe modification: Adjust rules by category; keep `maxIssues` but rely on `excludeCorrectable` or an allow-list only after confirming rule applicability. Both tools share the ktlint engine, so conflict risk is currently low.
- Test coverage: No automated CI to catch gate misconfiguration (bug #1 makes actual gate runs moot today).

### The `MusicProvider` interface contracts every provider and its tests

- Component: `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt` — `search(query, limit = 20)`, `getTrack`, `stream`, `playlists()`, `queue()` are the entire provider surface. Since no implementation exists yet, this interface is volatile.
- Files: `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`
- Why fragile: Changing `MusicProvider` (e.g. adding pagination to search, adding `queue` size param, changing `stream` to return a streamable object rather than `String`) ripples into every future provider adapter (Phase 5) and all tests written against it.
- Safe modification: Treat the current signature as provisional; lock it via an ADR and add integration tests before permitting callers. Prefer additive changes (default params) over removals.
- Test coverage: `MusicProvider` has **zero** tests today (only `TrackTest.kt` exercises models).

### Single `:app` module with convention-based layering

- Component: The whole app builds in one Gradle module (`:app`); Clean Architecture boundaries (`presentation` → `domain` → `data`) exist only as directory conventions enforced by detekt `ForbiddenImport` (which is incomplete — see Tech Debt).
- Files: `settings.gradle.kts`, `config/detekt/detekt.yml`
- Why fragile: Nothing at the module/compilation level prevents a stray `data` import inside a `presentation` file, or an `android.*` import inside `domain`. Isolation is a lint rule away from being bypassed.
- Safe modification: Rely on the enforced rules but keep them scoped correctly; consider module splitting (`:domain`, `:data`) only if/when the compile-time guarantee becomes necessary.
- Test coverage: No architectural test runs in CI currently.

## Scaling Limits

### Search/queue have no pagination or bound

- Current capacity: `search(query, limit: Int = 20)` caps results; `queue()` returns an **unbounded** `List<Track>`; `playlists()` returns an unbounded `List<Playlist>`.
- Files: `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`, `app/src/main/java/com/clibeats/domain/model/Playlist.kt`, `app/src/main/java/com/clibeats/domain/model/Track.kt`
- Limit: Large real-world providers return cursor/stream-based result sets; an unbounded `queue()` on a big library loads everything into memory at once and blocks.
- Scaling path: Introduce cursor/pagination tokens or `Flow<List<Track>>` for `queue()`/`playlists()`, and an offset/cursor param for `search` — before Phase 5 adapters are written, since the interface is still provisional.

### Directory-convention boundaries do not scale to compile-time guarantees

- Current capacity: One `:app` module.
- Limit: With only lint-level enforcement, a growing team can accidentally couple layers; there is no Gradle-level isolation.
- Scaling path: Extract `:domain` as a pure-Kotlin module first (cheapest — it has zero Android deps today), then `:data`, leaving presentation in `:app`. This formalizes D-04 into a compile-time guarantee.

## Dependencies at Risk

### AGP 8.5.2 does not officially support compileSdk 35

- Risk: `app/build.gradle.kts` (`compileSdk = 35`) exceeds AGP 8.5.2's tested max (34); the warning is suppressed via `android.suppressUnsupportedCompileSdk=35`.
- Impact: Potential silent resource/behavior mismatches on API 35 builds.
- Migration plan: Upgrade AGP to ≥8.6 (supports compileSdk 35) and remove the suppress flag; keep `kotlin = 2.0.21` and `ksp = 2.0.21-1.0.27` aligned.

### Test tooling gaps become blocking at Phase 5/9

- Risk: No Mockito/MockK, no `kotlinx-coroutines-test`, no JUnit 5, no `compose-ui-test-junit4`, and `espresso-core` is declared but has **no `androidTest` sources** (`app/src/androidTest/` does not exist).
- Files: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Impact: Writing tests for the `suspend` `MusicProvider` boundary and Compose UI (planned Phase 5/9) requires adding these libraries; without them the DoD ">=85% coverage" and "Compose UI tests" gates are unachievable.
- Migration plan: Add MockK + `kotlinx-coroutines-test` for provider/use-case tests and `compose-ui-test-junit4` + `androidx.test.ext:junit` before Phase 5.

### AndroidX dependency versions are aging (for the analysis date)

- Risk: Compose BOM `2024.09.03`, `lifecycle-runtime-ktx 2.8.6`, `activity-compose 1.9.2`, `core-ktx 1.13.1` are roughly two years old at the analysis date.
- Files: `gradle/libs.versions.toml`
- Impact: Missing newer APIs/bugfixes; `warningsAsErrors = false` in lint means deprecated-API warnings won't be surfaced.
- Migration plan: Bump the Compose BOM and associated androidx versions in a dedicated chore before starting Phase 2 UI work (aligning with the AGP 8.6+ upgrade).

## Missing Critical Features

### No app functionality built yet

- Problem: The shipped app is a blank screen. No UI, navigation, playback, storage, providers, or settings exist.
- Blocks: Everything in the product vision is blocked until Phase 2 lands the TUI shell.
- Files: `app/src/main/java/com/clibeats/MainActivity.kt`

### No logging, crash reporting, or telemetry

- Problem: No logging framework, no `Timber`, no Crashlytics/Sentry-style reporting; `CONVENTIONS.md` notes Android `Log` is the current implicit default.
- Blocks: Debugging provider failures (Phase 5) and obtaining release feedback (Phase 10) will be hard without structured logging/crash capture.
- Files: `gradle/libs.versions.toml`

### No persistence, playback, or provider infrastructure

- Problem: `AppModule.kt` is empty (no DI bindings); no Room, no Media3/ExoPlayer, no repository/data-layer code, no provider implementations exist.
- Blocks: Offline caching, background playback, and multi-provider search (Phases 3–7).
- Files: `app/src/main/java/com/clibeats/di/AppModule.kt`

## Test Coverage Gaps

### Only model-layer tests exist; core contracts untested

- What's not tested: `MusicProvider` (all five `suspend` functions and their `ProviderResult` semantics including the `Error`/`Loading` paths), `ProviderResult` itself, the Hilt graph, `MainActivity`/`CLIBeatsApp`, and `Album/Artist/Playlist` models individually.
- Files: `app/src/test/java/com/clibeats/domain/model/TrackTest.kt`, `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`, `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`
- Risk: `ProviderResult.Error` handling and `MusicProvider` streaming/queue behavior are the highest-value untested surfaces and the first thing Phase 5 will depend on.
- Priority: **High**

### No instrumentation/Compose UI tests

- What's not tested: Any UI flow (none exists yet), and there is no `androidTest` source set despite `espresso-core` being declared.
- Files: `app/build.gradle.kts`, `gradle/libs.versions.toml`
- Risk: Compose UI features in Phase 2 will ship without the automated screen-flow coverage the DoD requires.
- Priority: **High** (at Phase 2)

### No coverage metric enforced

- What's not tested: No JaCoCo setup; CI does not track or gate coverage, though the DoD demands ">=85% coverage target".
- Files: `app/build.gradle.kts`, `.github/workflows/ci.yml`
- Risk: Coverage cannot be measured or enforced until tooling is added.
- Priority: **Medium**

### Error-path and async tests absent

- What's not tested: No test exercises a `ProviderResult.Error(message, cause)` result, and there is no `kotlinx-coroutines-test` infrastructure for the `suspend` provider methods.
- Files: `app/src/test/java/com/clibeats/domain/model/TrackTest.kt`
- Risk: The error-handling contract (sealed `ProviderResult`) is unverified and likely to be misused by the first repository/use-case writers.
- Priority: **Medium**

---

*Concerns audit: 2026-08-05*