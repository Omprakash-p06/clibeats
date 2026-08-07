---
title: Codebase Concerns — CLIBeats
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

# Codebase Concerns

**Analysis Date:** 2026-08-07

Android music client (Jetpack Compose / Kotlin, MVVM + Clean Architecture). Concerns below are ordered roughly by expected impact on planning/execution.

## Uncommitted / Partial Work (Working Tree)

A TUI restyle wave is mid-flight and **not committed** against `f4a1654`. `git status` shows 8 modified files plus an untracked component:

- Modified: `app/src/main/java/com/clibeats/presentation/component/PlayerBar.kt`, `app/src/main/java/com/clibeats/presentation/component/SongTableRow.kt`, `app/src/main/java/com/clibeats/presentation/home/HomeScreen.kt`, `app/src/main/java/com/clibeats/presentation/library/LibraryScreen.kt`, `app/src/main/java/com/clibeats/presentation/search/SearchScreen.kt`, `app/src/main/java/com/clibeats/presentation/settings/SettingsScreen.kt`, `app/src/main/java/com/clibeats/presentation/theme/CliBeatsColors.kt`, `app/src/test/java/com/clibeats/theme/CliBeatsColorsTest.kt`
- Untracked: `app/src/main/java/com/clibeats/presentation/component/TuiBlock.kt` (new "spotify-tui" border-box composable) and a stray `.kotlin/` directory at repo root.
- **Hazard:** the modified screens (`HomeScreen.kt`, `LibraryScreen.kt`, `SearchScreen.kt`) import `TuiBlock` and `SongTableHeader` from the components package. If the modified screens are committed **without** the untracked `TuiBlock.kt`, the build breaks (unresolved reference). Commit the restyle as one atomic change, or drop the uncommitted diffs if the TUI restyle is not desired.
- `.kotlin/` is untracked build/IDE session state and is **not** covered by `.gitignore`; add it to `.gitignore`.

## Tech Debt

### [Dead settings: authToken, activeProviderId, highQualityStreaming]

Settings are persisted and surfaced in the Settings UI but never consumed by the provider/playback layer.

- `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt` exposes `authToken` (lines 53-66), `activeProviderId` (line 38), `highQualityStreaming` (line 48).
- `app/src/main/java/com/clibeats/presentation/settings/SettingsViewModel.kt` combines and forwards these to UI; the values are **only** read by `SettingsScreen.kt`.
- `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt` and `app/src/main/java/com/clibeats/data/provider/api/InnerTubeApi.kt` never read `authToken`, `highQualityStreaming`, or `activeProviderId`.
- `app/src/main/java/com/clibeats/di/ProviderModule.kt` binds exactly one provider (`YouTubeMusicProvider`); the "active provider" selector cannot switch anything.
- **`authToken` has no setter caller in production code** — `AppPreferences.setAuthToken()` is never invoked, only `clearAuthToken()`. So "CLEAR SESSION CREDENTIALS" in `SettingsScreen.kt:154` can never appear (token stays null). The whole auth surface is dead UI.
- Fix approach: either wire the token into `InnerTubeApi` requests via a Cookie/`SAPISID` header in `InnerTubeHeaderInterceptor.kt`, honor `highQualityStreaming` by choosing different `PlayerRequest` fields, and select providers via a registry; or remove the unused settings. If removed, delete the SecureKeys plumbing and slim `AppPreferences`.

### Stubbed provider methods

`app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`:
- `getTrack()` returns `ProviderResult.Error("Not implemented in Phase 5")` (line 43).
- `playlists()` and `queue()` return `ProviderResult.Success(emptyList())` (lines 63, 67) — MusicProvider contract methods are unimplemented; only `search()` and `stream()` work.

### Hardcoded fake system-status strings in UI

`HomeScreen.kt` (lines 86-92) and `MoreScreen.kt` (lines 103-110) render static text such as `"CACHE CAP : 500 MB (LRU Active)"`, `"LATENCY : 38ms | CODEC: AAC-LC"`, `"PROVIDER : YouTube Music [ ONLINE ]"`, and `"SYSTEM STATUS : CONNECTED & OPERATIONAL"`. These are not bound to any real state. If a provider offline error occurs, the UI will still claim "ONLINE / CONNECTED & OPERATIONAL", which contradicts the `SearchUiState.Error` rendering in `HomeScreen.kt:177`.

### Cache size mismatch: 500 MB vs 512 MB

- `app/src/main/java/com/clibeats/data/cache/CacheManager.kt:83` uses `DEFAULT_MAX_CACHE_BYTES = 524_288_000L` (500 MB).
- `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt:45` defaults `cacheMaxMb` to `512`.
- `SettingsViewModel.setCacheMaxMb()` (`SettingsViewModel.kt:52-58`) is the **only** place that writes `cacheManager.maxCacheSizeBytes`, so a user-chosen limit is **not reapplied on app restart** — CacheManager always reboots at 500 MB. Wire the stored `cacheMaxMb` flow into `CacheManager` initialization, and reconcile the two defaults (500 vs 512).

### `SearchViewModel` repurposed as Home's "trending" source

`HomeScreen.kt:52` injects `SearchViewModel`; the Home "Trending Tracks" list is really the search flow (empty query → `YouTubeMusicProvider.search("Trending Hits")`, `SearchViewModel.kt:41`). `HomeScreen.kt:157` silently caps with `state.tracks.take(15)`. There is no dedicated `HomeViewModel`. This couples Home's identity to the search pipeline and fires a network call on every Home entry (see Performance).

### Deferred detekt/ktlint config debt

`deferred-items.md` (`.planning/phases/03-database-local-persistence-layer/deferred-items.md`, item D-02): `detekt 1.23.6 misparses ktlint_official indent config and its over-broad `ForbiddenImport` pattern (`com.clibeats.data.*`) blocks legitimate data-layer self-imports. Workaround today is a proliferation of `@file:Suppress("ForbiddenImport")` / `@Suppress("Indentation")` on nearly every data-layer file (e.g., `app/src/main/java/com/clibeats/di/PlaybackModule.kt:1`, `TrackUtils`/`TrackDownloadManager.kt:1`, `CacheManager.kt:1`). Fix the config instead of suppressing: replace `ForbiddenImport` with an explicit-import deny-list and disable the duplicate `Indentation` rule (ktlint is the formatting authority).

## Known Bugs

### `PlaybackService.onDestroy()` releases the singleton ExoPlayer

`app/src/main/java/com/clibeats/playback/service/PlaybackService.kt:27-31` calls `player.release()` in `onDestroy()`. But `ExoPlayer` is injected as a `@Singleton` in `app/src/main/java/com/clibeats/di/PlaybackModule.kt:27-34`. After the service is destroyed, the app-wide player reference is permanently released and later `PlaybackRepository`/`PlayerAdapter` calls throw `IllegalStateException`. The service should not release a singleton-scoped player, and `onTaskRemoved` should be handled so a swipe-away doesn't kill background playback.

### Missing media notification / `POST_NOTIFICATIONS`

`app/src/main/AndroidManifest.xml` declares `PlaybackService` with `foregroundServiceType="mediaPlayback"` but the app does not declare `POST_NOTIFICATIONS` and `PlaybackService.kt` sets no `MediaNotification` provider. On API 33+ the media-controls notification will not be shown without `POST_NOTIFICATIONS`. This contradicts the release notes ("notification controls").

### No-op TopAppBar menu button

`app/src/main/java/com/clibeats/presentation/layout/MainLayout.kt:164` — `IconButton(onClick = {})` for the hamburger does nothing.

### `.mp3` extension for non-MP3 streams

`app/src/main/java/com/clibeats/data/cache/CacheManager.kt:40` writes every stream to `$songId.mp3`; YouTube Music audio streams are typically AAC/.m4a. Mislabeled containers may mislead future media-type detection/playback.

### `STATE.md` is stale

`.planning/STATE.md` reports `Current Phase: Phase 3`, `status: unknown`, and `Next Action: Execute Phase 4`, while commit history shows phases 09-11 summaries (`docs/plans`/`docs/RELEASE_NOTES.md`, `git log` `2a47b56 docs(summary)...`) are already present. Reconcile state before planning the next phase.

## Security Considerations

### Deprecated alpha security library

`app/src/main/java/com/clibeats/di/StorageModule.kt:19-22` pins `androidx.security:security-crypto:1.1.0-alpha06` and uses the deprecated `EncryptedSharedPreferences`/`MasterKey` APIs (explicit `@Suppress("DEPRECATION")`). An alpha security lib in a release build is risk; ADR-003 tracks migration to Tink `KeystoreAesGcm`. Plan this migration.

### AUTH_TOKEN stored but never transmitted — keep it that way when wiring auth

`AUTH_TOKEN` is encrypted at rest (Keystore MasterKey AES256_GCM) and excluded from cloud backup via `app/src/main/res/xml/data_extraction_rules.xml`. Good. When auth is eventually wired in, inject the token via `InnerTubeHeaderInterceptor` request headers — never via query string, and keep `HttpLoggingInterceptor` (DEBUG-only, `NetworkModule.kt:43-48`) at `Level.BODY` only in debug binaries.

### `PlaybackService` exported=true

`AndroidManifest.xml` exports the media session service (required for other apps to bind via `androidx.media3.session.MediaSessionService`, but note that any app can issue player controls). Acceptable for a media app; do not add exported intent handlers beyond the MediaSessionService action.

### InnerTube scraping terms/legal risk

The provider reverse-engineers `music.youtube.com/youtubei/v1` with a spoofed `User-Agent` (`InnerTubeHeaderInterceptor.kt:15`). This is a terms-of-service and API-fragility risk (YouTube changes break the mapper). `docs/adr/ADR-005` documents the decision; keep the mapper versioned and isolated so a parser break does not take down playback.

### Downloads have no size cap

`app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt:39-56` streams the full HTTP body into cache with no content-length guard; `CacheManager.evictLruIfNeeded()` trims only **after** the full download lands. A very large stream can temporarily exceed the cache budget (and fill storage) before eviction. Guard against `Content-Length` exceeding the remaining budget, abort mid-download, and delete the partial file on abort.

## Performance Bottlenecks

### `runBlocking` disk I/O in UI path

`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:190` calls `runBlocking { cacheManager.getCachedFile(id) }` **synchronously on the calling thread** (which is the main thread via `PlayerViewModel`/Compose). Each `toMediaItem()` performs a blocking DB query + file stat on the main thread. Move preliminary caching resolution to a dispatchable (suspend/`withContext(IO)`) path, ideally caching the resolved media item.

### Home entry fires a trending network call

Every Home composition subscribes to `SearchViewModel` (`SharingStarted.WhileSubscribed(5_000L)`), which issues an InnerTube search (`SEO Trending Hits`) on each visit. Together with `runBlocking` cache checks, entering Home is the slowest screen. Add a proper `HomeViewModel` that debounces/parallels, and cache the trending result.

### Cache-eviction is a full pass

`CacheManager.evictLruIfNeeded()` (`CacheManager.kt:59-73`) reloads the entire `CacheIndex` via DB flow, sorts, then deletes file+row in a loop per save. Fine for small caches; will degrade at the 500 MB / 2 GB limit (many entries). Consider a `DELETE ... WHERE` cap query or a small-un-indexed HW-eviction cursor.

## Fragile Areas

- **TUI restyle mid-commit** (see Uncommitted Work) — build-breaking if screens and `TuiBlock.kt` land separately.
- **`PlayerAdapter` / `PlaybackService` / singleton `ExoPlayer`** lifecycle coupling — any service teardown corrupts playback globally. Add integration coverage around start/stop.
- **Cross-package data leakage:** `LibraryScreen.kt`/`HomeScreen.kt` import `com.clibeats.presentation.search.formatDuration` (`SearchScreen.kt`) and `SearchUiState` — presentation modules are not cleanly separated; a refactor in Search ripples into Home/Library.
- **`@Suppress` proliferation** in `presentation` files: many screens start with `@file:Suppress("FunctionNaming","LongMethod",...)` (e.g., `HomeScreen.kt:1`, `LibraryScreen.kt:1`, `SearchScreen.kt`, `PlaylistScreen.kt:1`). Fine for now, but it masks genuine complexity; keep an eye on the 300-line `PlaylistScreen.kt:304`.
- **Screenshot-only UI coverage**: UI is validated with Paparazzi (`app/src/test/...`) and the DAO layer with `app/src/androidTest` Room tests — but `connectedDebugAndroidTest` is recorded as an open item in `STATE.md` / `WINDOWS.md` unchecked; screens have no Compose UI instrumented tests.

## Dependencies at Risk

- `androidx.security:security-crypto` at an alpha version (`gradle/libs.versions.toml`) — deprecated API, planned migration.
- `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0` is unmaintained/archived; Retrofit 2.11 now ships built-in `kotlinx-serialization` converter (`NetworkModule.kt:10`). Drop the Jake Wharton dep and use the built-in.
- `Compose BOM 2024.09.03`, `AGP 8.5.2`, `Kotlin 2.0.21`, `Media3 1.4.1` — current but aging; an upgrade pass (Kotlin/Compose/Catalogs) is worthwhile before the production milestone.
- ktlint `12.1.1` and detekt `1.23.6` conflict on line-length/indent (see D-02).

## Test Coverage Gaps

- **Auth / token plumbing**: no test forwards the token into requests (because it is not forwarded — test the behavior once decided).
- **Cache-startup alignment**: race between `AppPreferences.cacheMaxMb` and `CacheManager` default (only `SettingsViewModel` covers the mutate path in `CacheManagerTest`).
- **PlaybackService lifecycle**: `PlayerAdapterTest` covers queue/repeat; there is no service-driven ExoPlayer release test (would catch the singleton-`release()` bug).
- **Home trending**: no `HomeScreen`/`SearchViewModel`-driven test for the `take(15)` cap or the trending-vs-empty transition.
- Instrumented/Compose UI tests are limited to Paparazzi stills; the app's critical paths (navigation, player, auth) are exercised only by screenshots.

---

*Concerns audit: 2026-08-07*