# Codebase Concerns

**Analysis Date:** 2026-08-12

## Tech Debt

**Stream URL staleness — no expiry-aware re-resolution:**
- Issue: `StreamResolver.resolve()` (`app/src/main/java/com/clibeats/playback/StreamResolver.kt`) returns a track untouched whenever `track.streamUrl` is non-blank. Persisted URLs (`SongEntity.streamUrl`) and in-memory queue tracks therefore never expire or refresh. YouTube googlevideo stream URLs expire (~6 h, `expire` param).
- Files: `app/src/main/java/com/clibeats/playback/StreamResolver.kt`, `app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt`, `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`
- Impact: Playing a YouTube track from the library/history/queue after its URL expired fails with HTTP 403; the only response is a log line in `PlayerAdapter.onPlayerError` (`PlayerAdapter.kt:106`). No user-facing error, no retry with a freshly resolved URL.
- Fix approach: StreamResolver should check URL expiry (or accept an optional `forceRefresh` flag), and `PlayerAdapter.onPlayerError` should re-resolve the current track once before failing.

**Queue tracks beyond index 0 never get stream URLs resolved:**
- Issue: `PlaybackRepositoryImpl.setQueue` (`app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt`) resolves only the start track and copies the rest of the list verbatim. `StreamResolver` skips any track with a non-blank URL — and search-returned YouTube tracks already carry a `streamUrl` from `TrackMapper`.
- Impact: Auto-advance to the second item in a freshly searched YouTube queue typically fails silently. Only the first track reliably plays.
- Fix approach: Resolve the whole queue (bounded parallelism) in `setQueue`, or add a per-item on-error resolve in `PlayerAdapter`.

**Playback failures are swallowed — silent no-op UX:**
- Issue: `PlaybackRepositoryImpl` launches work on an internal `repositoryScope` and catches all failures inside `runCatching { ... }.onFailure { log }`. `PlayerViewModel.playTrack` is fire-and-forget. `PlaybackException` subclasses (`app/src/main/java/com/clibeats/domain/model/PlaybackException.kt`) are built but never surfaced to any UI state.
- Files: `app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt`, `app/src/main/java/com/clibeats/presentation/player/PlayerViewModel.kt`
- Impact: Tapping a track on a dead link (403 or provider outage) appears to do nothing.
- Fix approach: Add an error field to `PlaybackState` (or an event flow) and show a snackbar in `PlayerBar`/`HomeScreen`.

**History recording is unwired (dead repository API):**
- Issue: `HistoryRepositoryImpl.recordPlay` (`app/src/main/java/com/clibeats/data/repository/HistoryRepositoryImpl.kt:31`) and the `HistoryDao` queries exist, but `recordPlay` has **zero production callers** (only its unit test). No playback path records history.
- Impact: The "recently played" queries (`getRecentlyPlayedTracks`, `LibraryRepository`/library UI depend on persisted songs) are non-functional — the `history` table stays empty.
- Fix approach: Call `historyRepository.recordPlay(songId, providerId)` from `PlaybackRepositoryImpl` on track play / `PlayerAdapter` on track transition.

**Library has no user-facing population path (partially mitigated):**
- Issue: `SongRepository.upsertTrack/upsertTracks` is only invoked by `PlaylistExchangeManager.import()` (`app/src/main/java/com/clibeats/data/playlist/PlaylistExchangeManager.kt:90`). Search, home, and playback never save tracks to the library. `LocalMusicProvider` reads from the same `SongRepository`; its MediaStore scanning is explicitly future work (`LocalMusicProvider.kt:17-19`).
- Mitigation: `LibraryRepository` now reads liked songs + saved albums/artists, and `PlaybackRepositoryImpl.persistQueueItems` writes queue songs via `songDao.upsertAll` — so some tracks enter the `songs` table via playback.
- Impact: The Library tab still has no reliable user-facing "add to library" path.
- Fix approach: Add "add to library" from search/player UI, or implement MediaStore scan (requires `READ_MEDIA_AUDIO`).

**"Download" feature is actually cache-only:**
- Issue: `TrackDownloadManager.downloadTrack` (`app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt`) writes into `context.cacheDir/audio_cache` via `CacheManager`, which is subject to LRU eviction (500 MB) and OS cache clearing. There is no progress reporting (`DownloadStatus.Downloading(0)` never updates), no cancellation, no duplicate-download dedupe, and failures can embed the full stream URL in the error message.
- Impact: Users believe they have offline downloads; files vanish on eviction or `clearAllCache`; no restart/resume for large files.
- Fix approach: Move downloads to a durable app-private directory, add progress streaming, dedupe by songId, keep the cache index separate from permanent downloads.

**Detekt clean-architecture rule defeated by `ForbiddenImport` suppressions:**
- Issue: The detekt rule `ForbiddenImport` (`config/detekt/detekt.yml`) is suppressed via `@file:Suppress("ForbiddenImport")` in 40+ files, including playback/data-crossing files and presentation ViewModels that import `com.clibeats.data.*` (e.g. `HomeViewModel.kt`, `SearchViewModel.kt`, `SettingsViewModel.kt`, `LibraryScreen.kt`).
- Impact: The architecture boundary is cosmetically enforced; refactors could silently deepen data-layer coupling in the presentation layer.
- Fix approach: Tighten the rule to specific offending imports, or move `AppPreferences`/`CacheManager` access behind domain interfaces.

**`@file:Suppress` masks complexity in the most fragile modules:**
- Issue: The heaviest suppressions sit on the highest-risk files: `YouTubeMusicProvider.kt` (6 rules incl. `LongMethod`, `CyclomaticComplexMethod`), `NewPipeExtractorResolver.kt` (6 rules incl. `SwallowedException`), `StreamUrlDeobfuscator.kt`, `PlaybackRepositoryImpl.kt` (`LongMethod`, `TooManyFunctions`), `MainLayout.kt` (ktlint suppressions).
- Impact: These files are undetectable by static analysis; regressions must be caught by tests, which mostly don't exist for these paths (see Test Coverage Gaps).

**Dead/placeholder cipher signature logic:**
- Issue: `StreamUrlDeobfuscator.deobfuscateSignature` (`app/src/main/java/com/clibeats/data/provider/youtube/StreamUrlDeobfuscator.kt:103`) implements the YouTube signature decipher as simply `signature.reversed()`. Real ciphered URLs require the player JS decipher function.
- Impact: Any URL arriving through the `cipher`/`signatureCipher` path produces an invalid signed URL; the fallback chain stage silently yields a 403.
- Fix approach: Remove the misleading placeholder, or integrate NewPipe's decipher logic; document that only non-ciphered formats are supported.

**Repeatedly suppressed notification permission (Android 13+ gap):**
- Issue: `PlaybackService.refreshNotification` annotates `@SuppressLint("MissingPermission", "NotificationPermission")` (`app/src/main/java/com/clibeats/playback/service/PlaybackService.kt:108`), yet the app never declares `POST_NOTIFICATIONS` in `app/src/main/AndroidManifest.xml` and never requests runtime permission.
- Impact: On Android 13+ (targetSdk 34), the media notification and its controls are hidden from the notification drawer (FGS banner may still show); users lose playback controls and ongoing-status. The lint suppression hides this instead of fixing it.
- Fix approach: Declare `POST_NOTIFICATIONS`, request it on first entry to playback UI, and drop the suppression.

**Release build is not production-ready:**
- Issue: `app/build.gradle.kts` release type has `isMinifyEnabled = false` and `signingConfig = signingConfigs.getByName("debug")` — release APKs are signed with the debug keystore. `proguard-rules.pro` is therefore dormant. CI (`ci.yml`) only runs `assembleDebug`, so the release path is never validated. `versionName = "0.1.0"` despite a `v1.0` release tag.
- Impact: No obfuscation, debug-signed artifacts cannot be distributed on Play, version reporting is misleading.
- Fix approach: Enable R8 (ship keep rules for NewPipeExtractor/kotlinx.serialization), add a release signing config, build `assembleRelease` in CI, align `versionName` (ADR-011 topic).

**EncryptedSharedPreferences on a deprecated alpha:**
- Issue: `StorageModule` uses `androidx.security:security-crypto:1.1.0-alpha06` — deprecated API with known synchronous I/O and keyset-corruption issues; ADR-003 tracks migration to Tink `KeystoreAesGcm`, not yet done. First `securePrefs` access happens synchronously during `AppPreferences` init.
- Impact: On OEMs with buggy Keystore implementations, `create` can throw and crash the app; restore-to-new-device leaves unreadable ciphertext (see Security — backup gap).
- Fix approach: Execute the Tink migration; wrap `EncryptedSharedPreferences.create` with a fallback to plain `SharedPreferences`.

**Cache default mismatch:**
- Issue: `AppPreferences.cacheMaxMb` defaults to 512 MB (`app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt`) while `CacheManager.DEFAULT_MAX_CACHE_BYTES` is 500 MB (`app/src/main/java/com/clibeats/data/cache/CacheManager.kt:83`). The setting is only applied after the user changes it in Settings.
- Impact: Settings screen reports "512 MB" while the actual cap is 500 MB until toggled.

## Known Bugs

**Expired stream URLs break library/queue playback (no recovery path):**
- Symptoms: YouTube track played from Library/Queue/History plays once, then 403s on every later attempt; queue auto-advance stops after track 1.
- Files: `app/src/main/java/com/clibeats/playback/StreamResolver.kt`, `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`
- Trigger: Wait >6 h after first play of a YouTube track (persisted `streamUrl` in `SongEntity`), then play again.
- Workaround: None in-app; clearing app data or re-importing playlists regenerates URLs.

**Playlist import ID collisions silently overwrite playlists:**
- Symptoms: Importing two different playlists whose names share a `String.hashCode()` produces the same `imported_<hash>` playlist id.
- Files: `app/src/main/java/com/clibeats/data/playlist/PlaylistExchangeManager.kt:76`
- Trigger: Import a clibeats.json containing two colliding playlist names.
- Workaround: Rename before import; the overwrite is permanent (songs rows remain but the playlist mapping is replaced by upsert).

**Imported `clibeats.json` stream URLs are trusted without validation:**
- Symptoms: `sourceUrl` from the file is stored verbatim and ExoPlayer is pointed at it (`PlaylistExchangeManager.import` → `PlayerAdapter.toMediaItem`). A crafted file can make the player fetch arbitrary URLs.
- Files: `app/src/main/java/com/clibeats/data/playlist/PlaylistExchangeManager.kt:86`
- Trigger: Attacker with write access to `Android/data/com.clibeats/files/clibeats_exchange/clibeats.json` (via adb or rooted device).
- Workaround: None.
- Fix approach: Validate `sourceUrl` scheme (`https`/`file` only) on import; ignore URLs for remote providers to force re-resolution.

**Audius signed stream URLs are replaced wholesale:**
- Symptoms: `AudiusMusicProvider` ignores the embedded signed `stream.url` from search results and substitutes `GET /v1/tracks/{id}/stream?app_name=clibeats` for every track (documented rationale in `AudiusMusicProvider.kt`).
- Trigger: Any Audius playback; also affects `durationMs`/artwork fidelity since the substituted URL is the only stream source.
- Root cause: The discovery API mints signatures its own gateways reject (HTTP 401 "invalid signature").
- Workaround: None needed — the substitute endpoint is the verified-working path; revisit if Audius fixes signature minting.

**Queue restore can resurrect stale stream URLs after app data restore:**
- Symptoms: After a process-death restore, `PlaybackRepositoryImpl.restorePersistentQueue` rebuilds the queue from `QueueDao` — including any stale `streamUrl` values persisted in `songs` — with no expiry check before `PlayerAdapter.restoreQueue`.
- Files: `app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt`, `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`
- Workaround: None in-app; same 403 symptom as the expiry bug above.

## Security Considerations

**Backup exclusions only apply on API 31+:**
- Risk: `data_extraction_rules.xml` (`app/src/main/res/xml/data_extraction_rules.xml`) excludes secure prefs and DataStore, but `android:dataExtractionRules` is honored only on Android 12+. On API 26–30 (minSdk 26 = in scope), `allowBackup="true"` without `android:fullBackupContent` means auto-backup ships the Room DB (library/playlists/history), DataStore, AND `clibeats_secure_prefs.xml` to Google Drive.
- Files: `app/src/main/AndroidManifest.xml:10-11`, `app/src/main/res/xml/data_extraction_rules.xml`
- Current mitigation: None for API 26–30.
- Recommendations: Add a `fullBackupContent` XML with the same exclusions; also note restoring encrypted prefs to a new device yields unreadable ciphertext (Keystore key is not backed up).

**Search queries and telemetry-style data logged at INFO in all builds:**
- Risk: `DiagnosticLogger.logSearchRequest` logs the raw search query (`app/src/main/java/com/clibeats/util/DiagnosticLogger.kt`); `logTrackSelected` logs track ids/titles; `safeLog` runs in release too. OkHttp `HttpLoggingInterceptor` at `Level.BODY` in debug logs full request/response bodies (`app/src/main/java/com/clibeats/di/NetworkModule.kt:56`).
- Files: `app/src/main/java/com/clibeats/util/DiagnosticLogger.kt`, `app/src/main/java/com/clibeats/di/NetworkModule.kt`
- Current mitigation: None — DiagnosticLogger is not gated behind `BuildConfig.DEBUG`.
- Recommendations: Gate `DiagnosticLogger` behind `BuildConfig.DEBUG`, redact query strings, keep BODY logging out of any pre-release builds.

**Hidden WebView for PO-token scraping:**
- Risk: `PoTokenGenerator` (`app/src/main/java/com/clibeats/data/provider/youtube/PoTokenGenerator.kt`) spins up a hidden `WebView` with `javaScriptEnabled = true`, loads a YouTube embed page, and scrapes `window.ytcfg` for `PO_TOKEN`/`VISITOR_DATA` via a `@JavascriptInterface` bridge. YouTube's internal config keys changed before; every YouTube change can silently disable the InnerTube fallback.
- Current mitigation: 10 s timeout, cached token (12 h TTL), single-use on token failure.
- Recommendations: Keep NewPipeExtractor as the primary path (it is), monitor uptime, consider dropping the WebView path if it keeps breaking.

**Spoofed browser headers (anti-bot / ToS exposure):**
- Risk: `InnerTubeHeaderInterceptor` and `NewPipeExtractorResolver` impersonate desktop Chrome UAs plus `Origin`/`Referer` from music.youtube.com. Bot checks (`LOGIN_REQUIRED`) are treated as a normal fallback hop in `YouTubeMusicProvider.stream`.
- Files: `app/src/main/java/com/clibeats/data/provider/api/InnerTubeHeaderInterceptor.kt`, `app/src/main/java/com/clibeats/data/provider/youtube/NewPipeExtractorResolver.kt`
- Current mitigation: Multi-client `FALLBACK_CHAIN` + stream cache + NewPipe primary.
- Recommendations: Accept as inherent to the provider integration (ADR-005); keep the failure path user-friendly (see swallowed errors).

**Exported MediaSessionService without controller policy:**
- Risk: `PlaybackService` is `android:exported="true"` (`app/src/main/AndroidManifest.xml`) — required for MediaSessionService — and `onGetSession` returns the session to any controller. Any app could send `ACTION_PLAY_PAUSE`/`ACTION_NEXT`/`ACTION_PREVIOUS` intents to control playback.
- Current mitigation: Media3 default controller policy; actions are playback controls only.
- Recommendations: Acceptable for a media app; enforce `MediaSession.Callback.onConnect` policy checks if finer control is desired.

**Cache filename derived from unvalidated songId:**
- Risk: `File(cacheDir, "$songId.mp3")` construction in `CacheManager.saveTrackToCache` (`app/src/main/java/com/clibeats/data/cache/CacheManager.kt`) — a provider id containing `/`, `..`, or path separators could write outside the cache dir. Current providers emit safe composite ids, but nothing validates at the boundary.
- Files: `app/src/main/java/com/clibeats/data/cache/CacheManager.kt`, `app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt`
- Current mitigation: None structural.
- Recommendations: Sanitize songId to `[A-Za-z0-9._:-]` before use in file names.

## Performance Bottlenecks

**Main-thread Room query via `runBlocking` on every track item build:**
- Problem: `PlayerAdapter.Track.toMediaItem()` calls `runBlocking { cacheManager.getCachedFile(id) }` (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`) — a Room DAO query — synchronously on whatever thread builds media items (listener callbacks on the main thread during queue preparation).
- Cause: Media item construction needs async file lookup; `runBlocking` blocks the calling thread.
- Improvement path: Resolve cached-file existence before queue/play and pass it in, or make `toMediaItem` suspend and build media items off the main thread.

**Metadata N+1 during Internet Archive search:**
- Problem: Each IA search issues up to 8 metadata requests (`rankAndMap`, semaphore 4) per search (`app/src/main/java/com/clibeats/data/provider/InternetArchiveMusicProvider.kt:112-144`).
- Cause: Search returns docs without audio-file metadata; per-item `api.metadata()` fills the gap.
- Improvement path: Acceptable at current scale (8 items); cache metadata per identifier if search latency becomes visible.

**Stream resolution latency chain (YouTube):**
- Problem: `YouTubeMusicProvider.stream` tries cache → NewPipe extraction → PO-token WebView (up to 10 s) → up to N client configs. On a cold path this can take tens of seconds before the UI shows anything (the tap handler only updates state on success).
- Files: `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`, `app/src/main/java/com/clibeats/data/provider/youtube/PoTokenGenerator.kt`
- Cause: Layered fallbacks are sequential and blocking relative to playback start.
- Improvement path: Start playback with a "resolving" UI state (currently missing, see swallowed errors); parallelize client config attempts where safe.

**Position ticker:**
- Problem: `PlayerAdapter.startPositionTicker` polls `updateState()` every 500 ms while playing (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt:70-79`), recomposing player UI at 2 Hz.
- Improvement path: Fine for a single screen; avoid additional screens binding `playbackState` at high frequency.

## Fragile Areas

**YouTube provider stack (highest fragility):**
- Files: `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`, `data/provider/youtube/PoTokenGenerator.kt`, `NewPipeExtractorResolver.kt`, `StreamUrlDeobfuscator.kt`, `StreamCacheManager.kt`, `data/provider/api/InnerTubeHeaderInterceptor.kt`, `InnerTubeApi.kt`, `YouTubeClientStrategy.kt`
- Why fragile: Entirely reverse-engineered, undocumented APIs (InnerTube, ytcfg scraping, googlevideo signature params). Every YouTube front-end or policy change can break search, token generation, or stream extraction. Several suppression-heavy, low-coverage files (see Test Coverage Gaps).
- Safe modification: Change one stage at a time; keep the fallback chain intact; run `YouTubeMusicProviderTest` + `InnerTubeHeaderInterceptorTest`; manual smoke against real YouTube before trusting provider changes.
- Test coverage: `data/provider/YouTubeMusicProviderTest.kt` exists but there is no test for fallback ordering, the stream-cache expiry path, the WebView token path, or `StreamUrlDeobfuscator` cipher handling.

**Playback pipeline:**
- Files: `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`, `playback/StreamResolver.kt`, `data/repository/PlaybackRepositoryImpl.kt`, `playback/service/PlaybackService.kt`
- Why fragile: ExoPlayer is a DI singleton shared by app and service; errors are logged-not-surfaced; the notification is hand-rolled around MediaSession; queue persistence adds a DB write path that can race with player ops.
- Safe modification: Keep `PlayerAdapter` as the single owner of ExoPlayer; don't release the player from the service; add error-state plumbing before changing queue semantics.
- Test coverage: `PlayerAdapterTest.kt`/`PlayerAdapterQueueTest.kt`/`PlaybackRepositoryImplTest.kt`/`PlaybackIntegrationTest.kt` exist; `PlaybackService` has no tests.

**Playlist import/export:**
- Files: `app/src/main/java/com/clibeats/data/playlist/PlaylistExchangeManager.kt`, `data/playlist/CliBeatsFile.kt`
- Why fragile: Depends on `externalFilesDir` (user-cleared, device-dependent), `String.hashCode()` ids, silent upserts, and trusting file contents.
- Safe modification: Test with real exported files; change the ID scheme to a content hash before reuse spreads.
- Test coverage: Only `CliBeatsFileCodecTest.kt` (codec round-trip); no tests for import/export flows.

**Telemetry/crash reporters are stubs:**
- Files: `app/src/main/java/com/clibeats/telemetry/TimberTelemetryTracker.kt` (only `Log.d`), `TelemetryModule.kt`
- Why fragile: ADR-010/ADR-009 promise beta telemetry & crash reporting; today both interfaces are logcat-only no-ops. Any consumer believing events ship anywhere is mistaken.
- Safe modification: Keep the interfaces; implement real transports behind them.

## Scaling Limits

**In-memory stream cache:**
- Current capacity: Unbounded `ConcurrentHashMap` in `StreamCacheManager` (`app/src/main/java/com/clibeats/data/provider/youtube/StreamCacheManager.kt`) with per-entry expiry (6 h URL TTL + 60 s margin).
- Limit: Entries are dropped on process death and on expiry only; long sessions with many distinct tracks grow heap usage.
- Scaling path: Cap entry count or persist cache; acceptable for v1 volumes.

**Cache eviction is insertion-order, not true LRU:**
- Current capacity: 500 MB shared audio cache, eviction ordered by `cachedAt` (`CacheManager.evictLruIfNeeded`, `app/src/main/java/com/clibeats/data/cache/CacheManager.kt`).
- Limit: Frequently-played items evicted simply because they're old; `cachedAt` is never updated on access.
- Scaling path: Track `lastAccessedAt` on cache hit; also guard concurrent save+evict races (no locking today).

**Library/queue data growth:**
- Current capacity: Room DB unrestricted; `SongEntity` grows as tracks are imported/played (import path + queue persistence writes).
- Limit: No pagination in `getAllTracksAsFlow`; `LibraryViewModel` groups the entire track list on every emission — fine at hundreds of tracks, slow at tens of thousands.
- Scaling path: `PagingSource` in DAOs before library grows.

## Dependencies at Risk

**NewPipeExtractor v0.26.4 (JitPack):**
- Risk: Reverse-engineered YouTube extractor (`gradle/libs.versions.toml`, JitPack in `settings.gradle.kts`); pinned snapshot, updated only by upstream mirror; can break with YouTube changes.
- Impact: Primary stream-resolution path for the default provider (`NewPipeExtractorResolver`).
- Migration plan: Track upstream releases; the InnerTube + PO-token fallback is the backstop (itself fragile); consider periodic vendor bumps in CI.

**androidx.security:security-crypto 1.1.0-alpha06:**
- Risk: Alpha-quality, deprecated; known Keystore keyset-corruption and startup-crash reports on some OEMs; ADR-003 documents Tink migration as planned, unexecuted.
- Impact: Auth-token storage and app startup (`StorageModule.provideSecurePrefs`).
- Migration plan: Tink `KeystoreAesGcm` per ADR-003 with fallback handling.

**androidx.media3 1.4.1 / Room 2.6.1 / AGP 8.5.2 / Kotlin 2.0.21:**
- Risk: Media3 1.4.x predates several 1.5/1.6 bug fixes; Room 2.6.1 predates the 2.7 auto-migration improvements; AGP 8.5.2 supports compileSdk 34 only (`android.suppressUnsupportedCompileSdk=35` in `gradle.properties` hides the newer-SDK warning).
- Impact: Cumulative bug fixes and new-test APIs unavailable; compileSdk stuck at 34.
- Migration plan: Bump deliberately (Compose BOM 2024.09.03 + AGP 8.5.2 are internally consistent); verify notification behavior on Media3 bump.

## Missing Critical Features

**No user-facing playback error state:**
- Problem: Failing stream resolution, expired URLs, and provider outages produce only logs. Blocks diagnosis and trust.
- Blocks: Queue playback reliability work (queue tracks 2..n fail silently), offline UX.

**No notification permission request (Android 13+):**
- Problem: `POST_NOTIFICATIONS` neither declared nor requested (`app/src/main/AndroidManifest.xml`), while `PlaybackService` suppresses the lint.
- Blocks: Reliable media controls on modern devices.

**No content security scope for the WebView token scraper:**
- Problem: No network-security hardening on the hidden WebView (`PoTokenGenerator`).
- Blocks: Defense-in-depth hardening of the scraping path.

## Test Coverage Gaps

**Playback failure & retry paths (Highest priority):**
- What's not tested: `StreamResolver` expiry handling, `PlaybackRepositoryImpl` failure propagation to UI, `PlayerAdapter.onPlayerError` recovery, queue auto-advance with stale URLs.
- Files: `app/src/main/java/com/clibeats/playback/StreamResolver.kt`, `data/repository/PlaybackRepositoryImpl.kt`, `playback/PlayerAdapter.kt`
- Risk: The most visible user-facing bugs (silent failures) ship unnoticed.
- Priority: High

**YouTube fallback chain (High):**
- What's not tested: `YouTubeMusicProvider.stream` cache→NewPipe→PO-token→client-chain ordering, `StreamCacheManager` expiry borders, `StreamUrlDeobfuscator` cipher handling, `PoTokenGenerator` (WebView path).
- Files: `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`, `data/provider/youtube/*`
- Risk: Provider is the default; changes in YouTube can break it with no coverage.
- Priority: High

**Storage & security (Medium):**
- What's not tested: `CacheManager` eviction ordering/races, `TrackDownloadManager` failure modes, `AppPreferences` secure-prefs fallback, `PlaylistExchangeManager` import collision/validation.
- Files: `app/src/main/java/com/clibeats/data/cache/CacheManager.kt`, `data/download/TrackDownloadManager.kt`, `data/playlist/PlaylistExchangeManager.kt`
- Risk: Data-loss bugs (import IDs, backup) and crash-on-keyset issues found only by users.
- Priority: Medium

**Service layer (Medium):**
- What's not tested: `PlaybackService` notification lifecycle, action intents, `startForeground` behavior.
- Files: `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`
- Risk: Notification/foreground-service regressions on Android 13+ are invisible without permission UX tests.
- Priority: Medium

**CI cannot catch integration regressions:**
- What's not tested: No `androidTest` (Room DAO tests exist in `app/src/androidTest` but CI only runs `testDebugUnitTest` — DAO tests never execute in CI), no `assembleRelease`, no Paparazzi gate in CI, no dependency vulnerability scan (no Dependabot config, no OWASP plugin).
- Files: `.github/workflows/ci.yml`, `app/src/androidTest/java/com/clibeats/data/local/dao/*`
- Risk: Release-path and device-layer regressions ship unverified.
- Priority: Medium

---

*Concerns audit: 2026-08-12*
