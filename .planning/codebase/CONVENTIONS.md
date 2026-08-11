# Coding Conventions

**Analysis Date:** 2026-08-12

## Naming Patterns

**Files:**
- Kotlin: `PascalCase.kt`, one primary class/object per file; `*Test.kt` for unit tests, `*ScreenshotTest.kt` for Paparazzi; DAOs as `*Dao.kt`, entities as `*Entity.kt`, Hilt modules as `*Module.kt`, DTO files as `*Dtos.kt`/`*Response.kt`/`*Request.kt`
- Android resources: `snake_case.xml` (`data_extraction_rules.xml`)

**Functions:**
- Kotlin: `camelCase` (e.g., `resolve`, `toggleShuffle`, `restoreQueue`, `withLocalFileUri`)
- Async: `suspend` functions; no special prefix
- Handlers: `onPlayPauseClick`, `onDestinationSelected` style names for UI callbacks; `Player.Listener` overrides use Media3 names (`onPlayerError`, `onMediaItemTransition`)

**Variables:**
- `camelCase` for locals; `UPPER_SNAKE_CASE` for constants (`POLL_INTERVAL_MS`, `DEFAULT_MAX_CACHE_BYTES`, `INNERTUBE_BASE_URL`)
- Private StateFlow backing fields with leading underscore (`_playbackState`, `_queueFlow`, `_downloads`) exposed via `asStateFlow()`

**Types:**
- Interfaces/types: `PascalCase`, no `I` prefix (`MusicProvider`, `ProviderRegistry`, `PlaybackState`, `Track`)
- Enums: `PascalCase` type, `UPPER_SNAKE_CASE` values (`RepeatMode.OFF/ONE/ALL`, `CliBeatsThemeMode.DARK/AMOLED`, `DownloadStatus.Downloading/Completed/Failed`)
- Sealed types for results: `ProviderResult.Success/Error/Loading`, `DownloadStatus`, `*UiState` (sealed interface with `Loading`/`Empty`/`Success`)
- DTOs: `*Dto`/`*Response`/`*Request` classes in `data/provider/dto/`; mappers as extension functions (`toDomainTrack()`, `toTrackList()`, `toEntity()`)

## Code Style

**Formatting:**
- Kotlin: ktlint (`org.jlleitschuh.gradle.ktlint` 12.1.1) enforced in CI (`./gradlew ktlintCheck`); 4-space indent; trailing commas in multiline calls
- `@file:Suppress` always annotated with a justification comment (e.g., `// ForbiddenImport: data-layer self-imports are legitimate...`)

**Linting:**
- Detekt 1.23.6 (`config/detekt/detekt.yml`): `maxIssues: 0` (fail on any issue); rules tuned — `TooManyFunctions` (15/15/10), `LongMethod` (60), `LargeClass` (300), `MagicNumber` (ignore -1/0/1/2/100/1000, hash-code fn, property declarations), `UnusedPrivateMember`
- `ForbiddenImport` enforces Clean Architecture: `com.clibeats.data.*` MUST NOT be imported by presentation layer; legitimate imports use targeted `@file:Suppress("ForbiddenImport")` with a comment (heavily used — 40+ files)
- `Indentation` and `ImportOrdering` detekt rules are disabled: Indentation misparses `ktlint_official @Inject constructor()` style (false positives), ImportOrdering conflicts with ktlint's ordering rule (ktlint is authoritative)
- Android Lint: `abortOnError = true`, `checkDependencies = true`, warnings not errors
- `@SuppressLint` used sparingly; note `PlaybackService.refreshNotification` suppresses `MissingPermission`/`NotificationPermission` (see CONCERNS)

## Import Organization

**Kotlin:**
1. `java.*` / `android.*` (platform)
2. `androidx.*`
3. Third-party (kotlinx, dagger, retrofit, okhttp, etc.)
4. `com.clibeats.*` internal — ktlint enforces ordering; alphabetical within groups, blank lines between groups

**Path Aliases:**
- None — relative imports throughout

## Error Handling

**Patterns:**
- Data layer returns `ProviderResult.Success/Error/Loading` via `runCatching { }` — exceptions never leak to UI
- Providers wrap network calls in `runCatching`/`try-catch` → `ProviderResult.Error(message, cause)`; `CancellationException` is rethrown, never swallowed (see `InternetArchiveMusicProvider.rankAndMap`)
- `PlaybackException` subclasses (`StreamResolutionFailed`) are thrown by `StreamResolver` and caught by `PlaybackRepositoryImpl` (which logs, see CONCERNS)
- ViewModels surface errors in `*UiState` rather than throwing
- `runCatching { }.onFailure { log }` is the standard idiom for background/repository work
- UI callbacks wired directly to ViewModel methods (`playerViewModel::onPlayPauseClick`)

## Logging

**Framework:**
- `DiagnosticLogger` (`app/src/main/java/com/clibeats/util/DiagnosticLogger.kt`) — single logcat tag `CliBeatsDiagnostic`, 8-char trace ids, `safeLog` with logcat→stdout fallback
- `StructuredLogger` abstraction in `core/logging/` implemented by `TimberTelemetryTracker`/`TimberCrashReporter` (logcat-only, ADR-010)

**Patterns:**
- Log every search/stream/player lifecycle transition: `SEARCH_REQUEST`, `STREAM_RESOLUTION_STARTED`, `PLAYER_REQUEST`/`PLAYER_RESPONSE` (client + status), `STREAM_URL_RESOLVED` (host/itag/mimeType/expiry), `MEDIA_PREPARE`/`MEDIA_READY`/`MEDIA_PLAYING`
- Log errors with a code + message: `logError(traceId, "STREAM_RESOLUTION_FAILED", message)`
- Never log token values (PO token/visitor data)
- Note: `logSearchRequest` logs the raw query at INFO in all builds (see CONCERNS)

## Comments

**When to Comment:**
- Explain *why* and document non-obvious decisions: Audius stream-endpoint rationale (`AudiusMusicProvider.kt`), PO-token/visitor-data coupling, MediaStore future work (`LocalMusicProvider.kt:17-19`), deprecated security-crypto with Tink migration plan (`StorageModule.kt`)
- Reference ADRs and plans in comments (`// ADR-003`, `// per Plan 03-03 spec`)
- KDoc on public classes/interfaces (`MusicProvider`, `ProviderId`, `MusicProviderRegistry`); `@param` docs on shared components (`TrackArtwork`)

**TODO Comments:**
- Rare; deferred items tracked in `.planning/` docs rather than inline TODOs

## Function Design

**Size:**
- Detekt `LongMethod` threshold: 60 lines; `TooManyFunctions` 15/class — refactor beyond these (or suppress with justification)
- ViewModels keep `UiState` + single event-handler methods; screens delegate to ViewModel methods

**Parameters:**
- Small param lists; constructor injection with `@Inject constructor` (Hilt)
- DI test-friendly secondary constructors for dispatcher injection (`PlaybackRepositoryImpl`)

**Return Values:**
- Explicit returns; early guard clauses (`if (tracks.isEmpty()) return`, `if (clientId.isBlank()) return ProviderResult.Error(...)`)
- Kotlin: `runCatching` + `ProviderResult`; `withContext(Dispatchers.Main)` for player ops after IO resolution

## Module Design

**Clean Architecture:**
- `presentation` → `domain` ← `data` dependency rule (domain depends on nothing); enforced by Detekt `ForbiddenImport`
- `data` implements `domain` interfaces (repository contracts in `domain/repository/`, impls in `data/repository/`)
- Hilt: `@Singleton`/`@InstallIn(SingletonComponent::class)` modules per concern (`DatabaseModule`, `NetworkModule`, `PlaybackModule`, `ProviderModule`, `StorageModule`); constructor injection with `@Inject`; `@Named` qualifiers for multi-binding (`JAMENDO_CLIENT_ID_QUALIFIER`, `INNERTUBE_OKHTTP_QUALIFIER`)
- One explicit exception to layering: `HistoryRepository` returns `HistoryEntity` (data type) — documented + suppressed

**Provider Pattern:**
- Every provider implements `MusicProvider` with a `providerId` (`"youtube_music"`, `"jamendo"`, `"audius"`, `"internet_archive"`, `"local"`); registered in `ProviderModule` in display-priority order
- Composite ids via `ProviderId.composite`/`rawSourceId`; mappers convert DTOs → domain

## Testing Conventions

**General:**
- Arrange/Act/Assert structure; JUnit `@Test` methods with backtick names describing behavior (`fun \`upsertTrack delegates to songDao\`()`)
- Hermetic unit tests: MockWebServer for HTTP, mocked DAOs/repositories, `MainDispatcherRule` (kotlinx-coroutines-test) for `Dispatchers.Main`
- DAO tests use `Room.inMemoryDatabaseBuilder` in `androidTest`
- Gate: CI runs ktlint, detekt, lint, assembleDebug, testDebugUnitTest; see TESTING.md

---

*Conventions analysis: 2026-08-12*
*Update when conventions change*
