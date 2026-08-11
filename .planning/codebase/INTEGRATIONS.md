# External Integrations

**Analysis Date:** 2026-08-12

## APIs & External Services

**YouTube Music (InnerTube + NewPipe):**
- What it's used for: Default provider (`youtube_music`) — search and stream URL resolution. Everything runs **on-device**; there is no gateway/proxy (the old gateway service was removed).
  - SDK/Client: `NewPipeExtractor` v0.26.4 (`app/src/main/java/com/clibeats/data/provider/youtube/NewPipeExtractorResolver.kt`) — primary path; maintained reverse-engineered extractor.
  - Fallback: direct InnerTube player API (`app/src/main/java/com/clibeats/data/provider/api/InnerTubeApi.kt`, base `https://music.youtube.com/youtubei/v1/`) with a `FALLBACK_CHAIN` of client configs (`YouTubeClientStrategy.kt`) and an optional PO token from `PoTokenGenerator.kt` (hidden WebView scraping `window.ytcfg` for `PO_TOKEN`/`VISITOR_DATA`).
  - Auth: None (anonymous sessions); PO token is a Proof-of-Origin anti-bot credential, cached 12 h, not user auth.
  - Rate limits: No API key; subject to YouTube bot detection — handled by the fallback chain (`LOGIN_REQUIRED`/`UNPLAYABLE`/`ERROR` statuses hop to the next client config).
  - Spoofed headers: `InnerTubeHeaderInterceptor` impersonates a desktop Chrome UA + music.youtube.com `Origin`/`Referer`.

**Jamendo:**
- What it's used for: Creative Commons music catalog provider (`jamendo`). Search, trending (`order=popularity_week`), track lookup, and stream URLs.
  - SDK/Client: Retrofit `JamendoApi` (`app/src/main/java/com/clibeats/data/provider/api/JamendoApi.kt`), base `https://api.jamendo.com/v3.0/`.
  - Auth: Free `client_id` from developer.jamendo.com, passed as a Gradle property `JAMENDO_CLIENT_ID` → `BuildConfig.JAMENDO_CLIENT_ID` (`ProviderModule`). Empty by default — provider returns a clear configuration error when unset.

**Audius:**
- What it's used for: Open music catalog provider (`audius`). Search, trending, track lookup, stream.
  - SDK/Client: Retrofit `AudiusApi` (`app/src/main/java/com/clibeats/data/provider/api/AudiusApi.kt`), base `https://discoveryprovider.audius.co/` (shared Audius discovery endpoint).
  - Auth: None — only an `app_name=clibeats` query param.
  - Stream resolution uses `GET /v1/tracks/{id}/stream?app_name=clibeats` (302 → 206 audio/mpeg) instead of the embedded signed `stream.url` from search results, which the discovery API mints with signatures its own gateways reject (documented in `AudiusMusicProvider.kt`).

**Internet Archive:**
- What it's used for: Public audio catalog provider (`internet_archive`). Search (`advancedsearch.php` with `mediatype:audio` filter), trending (downloads-sorted `opensource_audio` collection), metadata, and direct-download stream URLs.
  - SDK/Client: Retrofit `InternetArchiveApi` (`app/src/main/java/com/clibeats/data/provider/api/InternetArchiveApi.kt`), base `https://archive.org/`.
  - Auth: None.
  - Streams use archive.org direct download URLs (`https://archive.org/download/{id}/{file}`), which support HTTP Range and 302 → mirror redirects — no proxy.

**Local device media:**
- What it's used for: Local library provider (`local`), currently backed by the persisted `SongRepository` (tracks saved via playlist import or future features). MediaStore scanning is explicitly future work (needs `READ_MEDIA_AUDIO`, noted in `LocalMusicProvider.kt:17-19`).
  - Stream URLs are `file://` URIs rewritten by `LocalMusicProvider.withLocalFileUri()`.

## Data Storage

**Databases:**
- Room (SQLite) — on-device only. 9 entities: `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`, `QueueEntity`, `LikedSongEntity`, `SavedAlbumEntity`, `SavedArtistEntity` (`app/src/main/java/com/clibeats/data/local/**`). Schema **version 3** with `MIGRATION_1_2` (liked_songs) and `MIGRATION_2_3` (saved_albums, saved_artists) in `CliBeatsDatabase.kt`, exported to `app/schemas/`.
  - No cloud DB, no backend storage.

**Caching:**
- Disk cache: 500 MB LRU cache at `context.cacheDir/audio_cache` via `CacheManager` (`app/src/main/java/com/clibeats/data/cache/CacheManager.kt`), indexed in Room `CacheIndexEntity`.
- In-memory stream cache: `StreamCacheManager` (`app/src/main/java/com/clibeats/data/provider/youtube/StreamCacheManager.kt`) — per-track resolved stream URLs with ~6 h expiry.
- Coil memory/disk cache for artwork.

## Authentication & Identity

**Auth Provider:**
- None external. `AUTH_TOKEN` is stored in EncryptedSharedPreferences backed by a Keystore `MasterKey` (AES256_GCM) (`StorageModule`), excluded from cloud backup via `res/xml/data_extraction_rules.xml`. It is not currently sent as a real credential to any provider.

**OAuth Integrations:**
- None. No user accounts, no Google sign-in.

## Monitoring & Observability

**Logs:**
- `DiagnosticLogger` (`app/src/main/java/com/clibeats/util/DiagnosticLogger.kt`) — structured logcat lines with 8-char trace IDs (`SEARCH_REQUEST`, `STREAM_RESOLUTION_STARTED`, `PLAYER_REQUEST`, `STREAM_URL_RESOLVED`, ...), tag `CliBeatsDiagnostic`. Runs in all builds (including release).
- `TimberTelemetryTracker` / `TimberCrashReporter` (`app/src/main/java/com/clibeats/telemetry/**`) — ADR-010/ADR-009 telemetry & crash-reporting interfaces; currently logcat-only no-op implementations.
- OkHttp `HttpLoggingInterceptor` at `Level.BODY` in debug builds only (`NetworkModule.kt:56`).

**Debug endpoints:**
- None — no server-side diagnostics (gateway `/debug-yt` no longer exists).

## CI/CD & Deployment

**Hosting:**
- None — pure client app. No backend to host.

**CI Pipeline:**
- GitHub Actions — `.github/workflows/ci.yml`, single job `quality-and-test` on push/PR to main/master/develop:
  `ktlintCheck` → `detekt` → `lintDebug` → `assembleDebug` → `testDebugUnitTest`, with test/lint/detekt report artifacts uploaded.
- No release build (`assembleRelease`) in CI, no instrumented tests (`connectedDebugAndroidTest`) in CI, no dependency-vulnerability scan.

## Environment Configuration

**Development:**
- Optional `JAMENDO_CLIENT_ID` gradle property for the Jamendo provider; everything else works out of the box.
- No cleartext-HTTP config: `network_security_config.xml` was removed along with the gateway-era dev hosts; all provider base URLs are HTTPS.
- Tests: unit tests are hermetic (MockWebServer, mocked DAOs); DAO tests use in-memory Room (`androidTest`).

**Production:**
- Standalone APK signed with the debug keystore (release buildType); `versionName = "0.1.0"` despite a `v1.0` release tag.
- Provider reliability is the app's only external dependency; YouTube changes can break the default provider's extraction (NewPipe + InnerTube fallback).

## Webhooks & Callbacks

**Incoming:**
- None.

**Outgoing:**
- None. Pull-based provider APIs only; outbound traffic is limited to provider search/stream/artwork requests.

---

*Integrations analysis: 2026-08-12*
*Update when external services change*
