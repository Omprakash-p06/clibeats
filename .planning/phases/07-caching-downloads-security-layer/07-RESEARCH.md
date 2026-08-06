# Phase 7: Caching, Downloads & Security Layer — Technical Research

## Objective
Research architecture, state management, Media3 cache integration, Room index synchronization, and security hardening for Phase 7.

## 1. Requirements Mapping
- **`REQ-OFF-01`**: LRU Cache Manager for offline audio streaming (`CacheManager`, `CacheIndexEntity`, `CacheIndexDao`).
- **`REQ-OFF-02`**: Background Track Download Manager (`DownloadManager`, download queue, progress flows).
- **`REQ-OFF-03`**: Auto-fallback to offline cached media when network connection is lost (`NetworkMonitor`, `PlayerAdapter` cache URI resolver).
- **`REQ-ENG-09`**: Security Hardening (secret protection, no plaintext credential logging, ProGuard/R8 rules, CI vulnerability checks).

## 2. Technical Architecture

### LRU Cache Manager (`CacheManager`)
- Storage directory: `context.cacheDir/audio_cache/`.
- Configurable maximum capacity (e.g. 500 MB).
- When capacity exceeds threshold, query `CacheIndexDao` ordered by `cached_at ASC` and delete oldest files until under capacity limit.
- Expose `getCacheSizeBytes()`, `clearAll()`, `getCachedFile(songId): File?`.

### Track Download Manager (`TrackDownloadManager`)
- Downloads track audio streams via OkHttp to disk.
- Exposes `downloadStatus: StateFlow<Map<String, DownloadStatus>>` (`Idle`, `Downloading(progress)`, `Completed`, `Failed`).
- Upon completion, registers file details in `CacheIndexDao` via `CacheManager`.

### Offline Playback Fallback (`NetworkMonitor` & `PlayerAdapter` integration)
- `NetworkMonitor` uses `ConnectivityManager.NetworkCallback` to expose `isOnline: StateFlow<Boolean>`.
- In `PlayerAdapter.toMediaItem()`, check `CacheManager.getCachedFile(track.id)`. If present, set URI to local file URI `file://...`. Otherwise use remote `streamUrl`. If offline and file not cached, notify UI gracefully.

### Security Hardening
- ProGuard rules (`proguard-rules.pro`) configured to obscure internal implementation details.
- Secret protection audit: verify interceptors (`InnerTubeHeaderInterceptor`) sanitize log outputs and do not expose sensitive API keys or authorization tokens in plaintext logs.

## 3. Quality Gate Targets
- 0 compile errors (`assembleDebug`).
- 0 Android Lint errors.
- 0 Detekt critical issues.
- 0 ktlint formatting errors.
- 100% passing unit test suite in `testDebugUnitTest`.
