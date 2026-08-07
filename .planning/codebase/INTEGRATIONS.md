---
title: External Integrations
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

# External Integrations

**Analysis Date:** 2026-08-07

## APIs & External Services

**Media / Streaming:**
- **YouTube Music InnerTube API** — the only network service. Unofficial internal API used for search and stream-URL resolution.
  - Base URL: `https://music.youtube.com/youtubei/v1/` (hardcoded in `app/src/main/java/com/clibeats/di/NetworkModule.kt`).
  - Client: Retrofit `InnerTubeApi` (`app/src/main/java/com/clibeats/data/provider/api/InnerTubeApi.kt`) — `POST search` and `POST player` endpoints.
  - Auth: none. Unauthenticated browser-client impersonation via `InnerTubeHeaderInterceptor` (`app/src/main/java/com/clibeats/data/provider/api/InnerTubeHeaderInterceptor.kt`): `X-YouTube-Client-Name: 67` (WEB_REMIX), `X-YouTube-Client-Version: 1.20240101.01.00`, plus `Origin`/`Referer` to `https://music.youtube.com`.
  - Serialization: `kotlinx.serialization` with `Json { ignoreUnknownKeys = true; isLenient = true }`; responses parsed defensively via `JsonElement` tree navigation in `app/src/main/java/com/clibeats/data/provider/mapper/TrackMapper.kt`.
  - Request/response DTOs: `app/src/main/java/com/clibeats/data/provider/dto/{SearchRequest,SearchResponse,PlayerRequest,PlayerResponse}.kt`.
  - Consumer: `YouTubeMusicProvider` (`app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`), bound as the single `MusicProvider` in `app/src/main/java/com/clibeats/di/ProviderModule.kt`.
  - Risks documented in `docs/adr/ADR-005-provider-integration-innertube.md`: unofficial API, response shape churn, ~6h stream-URL expiry, no OAuth (no personalized library).

**Image / Artwork:**
- **Thumbnail CDN** — album artwork URLs embedded in InnerTube search responses are loaded directly by Coil (`coil-compose`), which routes through the shared OkHttp client (`app/src/main/java/com/clibeats/di/ImageLoaderModule.kt`). No dedicated image service integration.

## Data Storage

**Databases:**
- Room SQLite database `clibeats.db` (local only, no server sync).
  - Entities: `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `HistoryEntity`, `CacheIndexEntity`, `QueueEntity` — see `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt`.
  - Version 1, `exportSchema = true`, schema JSON in `app/schemas/com.clibeats.data.local.CliBeatsDatabase/1.json`.
  - Built in `app/src/main/java/com/clibeats/di/DatabaseModule.kt`; DAOs `SongDao`, `PlaylistDao`, `HistoryDao`, `CacheIndexDao`, `QueueDao`.

**File Storage:**
- **Audio cache** — `context.cacheDir/audio_cache/` managed by `CacheManager` (`app/src/main/java/com/clibeats/data/cache/CacheManager.kt`): 500 MB LRU limit, eviction tracked via `CacheIndexDao`, files named `<songId>.mp3`.
- **Image cache** — Coil disk cache at `context.cacheDir/image_cache/` (2% of device storage).
- **Downloads** — `TrackDownloadManager` (`app/src/main/java/com/clibeats/data/download/TrackDownloadManager.kt`) streams via OkHttp into the audio cache; `PlayerAdapter` (`app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`) prefers cached files over remote URLs. Cache lives in app cache dir, so OS can reclaim it; design per `docs/adr/ADR-007-caching-downloads-security.md`.

**Caching:**
- In-memory: Coil memory cache (25% of device memory) — `ImageLoaderModule.kt`.
- On-disk LRU as above. No HTTP response caching layer beyond Interceptor-level headers (none configured).

## Authentication & Identity

**Auth Provider:**
- None. No OAuth, no API keys, no Firebase Auth, no user accounts.
- InnerTube requests are anonymous; telemetry/crash reporting redacts any `Bearer ...` tokens defensively (`docs/adr/ADR-010-beta-telemetry-privacy.md`, `CrashReporter` in `app/src/main/java/com/clibeats/telemetry/`).

**Local secure storage:**
- `EncryptedSharedPreferences` file `clibeats_secure_prefs` (AES256_GCM via Android Keystore `MasterKey`, AES256_SIV key-encryption) — `app/src/main/java/com/clibeats/di/StorageModule.kt`. Pinned to deprecated security-crypto 1.1.0-alpha06; Tink-based `KeystoreAesGcm` migration tracked in `docs/adr/ADR-003-encrypted-storage-local-persistence.md`.
- Non-sensitive preferences in DataStore `clibeats_prefs` (`app/src/main/java/com/clibeats/di/StorageModule.kt`).

## Monitoring & Observability

**Error Tracking:**
- None external. `TimberCrashReporter` + `TimberTelemetryTracker` (`app/src/main/java/com/clibeats/telemetry/`) log to `android.util.Log` with the tag `CLIBeatsTelemetry` — **no Firebase Crashlytics, Sentry, or remote backend**.
- Telemetry is privacy-first: `TelemetryTracker.trackEvent(AnalyticsEvent)` logs structured, non-PII payloads; `CrashReporter` regex-redacts auth tokens (`docs/adr/ADR-010-beta-telemetry-privacy.md`). Note: the classes are named "Timber*" but do not use the Timber library — plain `android.util.Log`.

**Logs:**
- OkHttp `HttpLoggingInterceptor` at `Level.BODY`, enabled only in `BuildConfig.DEBUG` builds (`NetworkModule.kt`). ADR-007 notes log sanitization protects sensitive tokens.

## CI/CD & Deployment

**Hosting:**
- None. App distributed as an APK; release build currently signs with the debug key (`app/build.gradle.kts`) — not Play-Store ready (see `docs/adr/ADR-011-production-release-distribution.md`).

**CI Pipeline:**
- GitHub Actions — `.github/workflows/ci.yml` (repo `Omprakash-p06/clibeats`, badge in `README.md`).
  - Runs on push/PR to `main`, `master`, `develop`.
  - JDK 17 (Temurin) via `actions/setup-java@v5`, `gradle/actions/setup-gradle@v3`.
  - Gates: `ktlintCheck`, `detekt`, `lintDebug`, `assembleDebug`, `testDebugUnitTest` (single job, 20-min timeout, cancels in-progress on new commits).
  - Uploads test/lint reports as artifacts.

## Environment Configuration

**Required env vars:**
- None at runtime — no API keys, no OAuth secrets (by design, `docs/adr/ADR-005-provider-integration-innertube.md`).
- CI: no secrets configured in `.github/workflows/ci.yml`.
- Tests fall back to `ANDROID_HOME`/`ANDROID_SDK_ROOT` env or `C:\Android\Sdk` (`app/build.gradle.kts` tasks block).

**Secrets location:**
- None stored in repo. `local.properties` (SDK path) is dev-local and gitignored.

## Webhooks & Callbacks

**Incoming:**
- None.

**Outgoing:**
- None. All API traffic is pull-based requests to the InnerTube endpoints.

## Platform Capabilities Used

**Permissions** (`app/src/main/AndroidManifest.xml`): `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- Media session integration with the Android system: `PlaybackService` (`app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`) extends `androidx.media3.session.MediaSessionService` with `foregroundServiceType="mediaPlayback"` — supports Media3 session controllers / notification integration.
- Network state awareness via `ConnectivityManager` in `NetworkMonitor` (`app/src/main/java/com/clibeats/data/network/NetworkMonitor.kt`) — no external connectivity API.

---

*Integration audit: 2026-08-07*