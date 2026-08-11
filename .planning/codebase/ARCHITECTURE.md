# Architecture

**Analysis Date:** 2026-08-12

## Pattern Overview

**Overall:** Single-module native Android app (Clean Architecture + MVVM). All music providers run **on-device** via a common `MusicProvider` contract — there is no backend/gateway (the old Fastify gateway was removed; the project is now self-contained). Playback uses AndroidX Media3 with a foreground MediaSessionService.

**Key Characteristics:**
- Layered Clean Architecture: `presentation` → `domain` ← `data`, enforced by Detekt `ForbiddenImport` (presentation must not import `com.clibeats.data.*`; 40+ files carry targeted `@file:Suppress` with justification comments)
- Provider plugin architecture on-device: 5 providers registered in display-priority order (`MusicProviderRegistry`), active provider persisted in `AppPreferences`, lookup by id — no runtime failover engine like the old gateway's `ProviderSelectionEngine`
- Collision-safe composite track ids (`ProviderId.composite`: `"<provider>:<sourceId>"`) so library/queue/cache keys stay unique across providers
- Room DB v3 with explicit migrations (liked songs; saved albums/artists)
- Persistent queue: queue + playback metadata (index, position, repeat, shuffle) survive process death
- Media3 `MediaSessionService` with `foregroundServiceType="mediaPlayback"` for background playback

## Layers

### Presentation Layer
- Purpose: Compose UI, ViewModels, navigation, theme
- Contains: Screens (`presentation/home|search|library|playlist|queue|settings|more/**`), ViewModels exposing `*UiState` sealed interfaces, TUI theme system (`CliBeatsColors/Typography/Shapes/Theme`, `AccentColor`, `CliBeatsThemeMode`), shared components (`PlayerBar`, `SongTableRow`, `TrackArtwork`, `TuiBlock`, `TuiTextField`)
- Location: `app/src/main/java/com/clibeats/presentation/**`
- Depends on: `domain` (models, repositories, providers, playback state) via ViewModels; Hilt-injected
- Used by: `MainActivity` (single-activity Compose app, manual destination switching via `NavDestination` enum — no Navigation library)

### Domain Layer
- Purpose: Pure business models and contracts, no Android/network dependencies
- Contains: `model/` (Track, Album, Artist, Playlist, PlaybackState, RepeatMode, PlaybackException), `provider/` (`MusicProvider` interface, `ProviderResult` sealed type, `ProviderRegistry`, `ProviderId`), `repository/` interfaces (Song, Playlist, History, Playback, Library)
- Location: `app/src/main/java/com/clibeats/domain/**`
- Depends on: nothing external (pure Kotlin) — one deliberate exception: `HistoryRepository` returns `HistoryEntity` (data-layer type) per Plan 03-03 spec, suppressed

### Data Layer
- Purpose: Provider clients, local persistence, cache, downloads, preferences
- Contains: `provider/` (5 `MusicProvider` impls + registry + `api/` Retrofit clients + `dto/` + `mapper/` + `youtube/` extraction stack), `local/` (Room DB v3, 8 DAOs, 9 entities, mappers), `repository/` (Song, Playlist, History, Playback, Library impls), `cache/CacheManager`, `download/TrackDownloadManager`, `preferences/AppPreferences`, `network/NetworkMonitor`, `playlist/PlaylistExchangeManager`
- Location: `app/src/main/java/com/clibeats/data/**`
- Depends on: domain contracts; Hilt modules in `di/**`; Retrofit/OkHttp/Room/DataStore

### Playback Engine
- Purpose: Media3 ExoPlayer wrapper exposing reactive state
- Contains: `PlayerAdapter` (singleton wrapper: StateFlow of `PlaybackState` + queue, restore/reorder/shuffle/repeat, position ticker), `service/PlaybackService` (MediaSessionService), `StreamResolver` (per-track stream URL resolution)
- Location: `app/src/main/java/com/clibeats/playback/**`
- Depends on: domain models, `data/cache/CacheManager` (offline file preference over stream URL), `domain/provider/ProviderRegistry`
- Used by: `PlaybackRepositoryImpl`, ViewModels (PlayerViewModel), `MainLayout` via `PlayerBar`

## Data Flow

**Search Request (in-app → provider API):**
1. User types in `SearchScreen` → `SearchViewModel` calls the domain repository
2. `SongRepositoryImpl` → `MusicProviderRegistry` lookup → active provider's `search()` (e.g. `YouTubeMusicProvider.search` → `InnerTubeApi.search(SearchRequest.forQuery(q))`, or Jamendo/Audius/IA Retrofit calls)
3. Provider maps DTOs → domain `Track` models (`TrackMapper`/provider-specific mappers); `ProviderResult.Success/Error` returned (never thrown to UI)
4. UI renders `SearchUiState`

**Playback Request (stream resolution):**
1. User taps a track → `PlayerViewModel.playTrack()` → `PlaybackRepositoryImpl.playTrack(track)`
2. `StreamResolver.resolve(track)` — if `track.streamUrl` is blank, calls the track's provider `stream(id)`; else returns the track untouched (no expiry-aware re-resolution — see CONCERNS)
3. `PlayerAdapter.playTrack(resolved)` — starts the foreground service, sets MediaItem (cached file if present, else streamUrl), prepares + plays
4. ExoPlayer issues requests; `PlayerAdapter` listeners + 500 ms position ticker update `PlaybackState` → `PlayerBar`/UI
5. Queue changes are collected by `PlaybackRepositoryImpl` and persisted to `QueueDao` + `AppPreferences` (index/position/repeat/shuffle); `restorePersistentQueue()` on startup restores the last queue

**YouTube stream resolution fallback chain (`YouTubeMusicProvider.stream`):**
1. Check `StreamCacheManager` (6 h TTL)
2. `NewPipeExtractorResolver` (primary)
3. InnerTube player API across `YouTubeClientStrategy.FALLBACK_CHAIN` clients with PO token from `PoTokenGenerator` (10 s timeout, 12 h cached token)
4. `StreamUrlDeobfuscator.deobfuscateStreamUrl` extracts a non-ciphered format

**State Management:**
- `StateFlow`-driven MVVM (`*UiState` data classes); queue + playback state in singleton `PlayerAdapter`; theme state in `ThemeViewModel`; provider selection in `AppPreferences.activeProviderId`

## Key Abstractions

**MusicProvider (domain):**
- Purpose: Canonical contract every provider implements — `search/trending/getTrack/stream/playlists/queue` returning `ProviderResult`
- Examples: `YouTubeMusicProvider`, `JamendoMusicProvider`, `AudiusMusicProvider`, `InternetArchiveMusicProvider`, `LocalMusicProvider`
- Pattern: Strategy/plugin, registered via `MusicProviderRegistry` (explicit ordered list from `ProviderModule`)

**ProviderRegistry / ProviderId:**
- Purpose: Single provider-selection mechanism; composite ids keep keys collision-safe
- Pattern: Registry + id-namespacing; `DEFAULT_PROVIDER_ID = "youtube_music"`

**ProviderResult (domain):**
- Purpose: Kotlin sealed result (`Success`/`Error`/`Loading`) avoiding exceptions across layers
- Pattern: Result monad

**PlayerAdapter (playback):**
- Purpose: Singleton facade over ExoPlayer exposing `playbackState`/`queueFlow` StateFlows and queue ops (play, setQueue, addToQueue, playNext, move, remove, clear, shuffle, repeat, seek, restoreQueue)
- Pattern: Facade + observer (Player.Listener)

**StreamResolver:**
- Purpose: Lazy per-track stream URL resolution with `PlaybackException.StreamResolutionFailed` on failure
- Pattern: Service; called by `PlaybackRepositoryImpl`

**Repository interfaces (domain) → impls (data):**
- Purpose: Data-access contracts; Hilt binds `*Impl` to interfaces in `RepositoryModule`
- Examples: `SongRepository`, `PlaylistRepository`, `HistoryRepository`, `PlaybackRepository`, `LibraryRepository`

## Entry Points

- `CLIBeatsApp.onCreate()` (`app/src/main/java/com/clibeats/CLIBeatsApp.kt`) — `@HiltAndroidApp`
- `MainActivity` — single activity, Compose `setContent`, destination switching, theme application (`CliBeatsTheme`)
- `PlaybackService` — Media3 `MediaSessionService`, declared in manifest with `foregroundServiceType="mediaPlayback"` and `androidx.media3.session.MediaSessionService` intent filter

## Error Handling

**Strategy:** `ProviderResult.Success/Error` returned from data layer via `runCatching { }`; `PlaybackException` subclasses (`StreamResolutionFailed`) for stream failures; UI renders error states in `*UiState` rather than throwing.

**Patterns:**
- Providers wrap all network calls in `runCatching`/`try-catch` → `ProviderResult.Error(message, cause)`
- `PlaybackRepositoryImpl` launches on an internal `repositoryScope` and swallows failures into `DiagnosticLogger` logs (see CONCERNS: playback failures are not surfaced to UI)
- `PlayerAdapter.onPlayerError` logs `MEDIA_ERROR` (HTTP status extraction) but does not re-resolve or notify the user (see CONCERNS)
- `PlaylistExchangeManager` wraps import/export in `runCatching` with `PlaylistExchangeException`
- `GatewayErrorMapper` no longer exists — errors are provider-native now

## Cross-Cutting Concerns

**Logging:**
- `DiagnosticLogger` (logcat, trace-id prefixed) at every provider/stream/playback boundary
- `StructuredLogger` abstraction + Timber telemetry/crash stubs (ADR-010)

**Validation:**
- Room DAO constraints + FK cascades (e.g. `liked_songs` FK → `songs` CASCADE); ktlint/detekt/lint gates
- Imported `clibeats.json` stream URLs are trusted without scheme validation (see CONCERNS)

**Authentication:**
- None enforced; `AUTH_TOKEN` stored but unused; PO tokens are YouTube anti-bot credentials only

**Security:**
- EncryptedSharedPreferences (Keystore AES256_GCM) for `AUTH_TOKEN`; backup exclusions via `data_extraction_rules.xml` (API 31+ only — see CONCERNS)

---

*Architecture analysis: 2026-08-12*
*Update when major patterns change*
