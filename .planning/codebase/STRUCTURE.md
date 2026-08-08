# Codebase Structure

**Analysis Date:** 2026-08-08

## Directory Layout

```
clibeats/                          # git repo root = BOTH codebases
├── app/                           # Android client (Kotlin, Compose, Hilt, Media3, Room)
├── gateway/                       # Provider gateway (Node.js, TypeScript, Fastify)
├── config/                        # Shared tooling config (detekt, etc.)
├── docs/                          # Project documentation / ADRs
├── scripts/                       # Root-level helper scripts
├── gradle/                        # Gradle wrapper + version catalog (libs.versions.toml)
├── gradlew, gradlew.bat, settings.gradle.kts, build.gradle.kts, gradle.properties
├── logo/                          # Branding assets
├── .github/workflows/ci.yml      # CI pipeline
├── .planning/                    # GSD workspace (phases, codebase docs)
└── README.md
```

## Directory Purposes

### Root

| Path | Purpose |
|------|---------|
| `app/` | Single-module Android Gradle project (`:app`) |
| `gateway/` | Standalone npm project with its own `package.json`, `tsconfig.json`, `vitest.config.ts`, `Dockerfile`, `docker-compose.yml` |
| `settings.gradle.kts` | Declares only `:app` module; rootProject `CLIBeats` |
| `config/` | Shared static-analysis config, e.g. `config/detekt/detekt.yml` |

## Android App Tree (`app/`)

```
app/
├── build.gradle.kts            # Android config: compileSdk 34, minSdk 26, Compose, Hilt,
│                               # Room (KSP), detekt/ktlint, Paparazzi; GATEWAY_BASE_URL build field
├── proguard-rules.pro
├── schemas/                    # Room schema exports (com.clibeats.data.local.CliBeatsDatabase/1.json)
└── src/
    ├── main/
    │   ├── AndroidManifest.xml    # MainActivity + PlaybackService (foreground mediaPlayback)
    │   ├── res/                   # values/, mipmap-*/ icons, font/, xml/ (bg data extraction rules)
    │   └── java/com/clibeats/
    │       ├── CLIBeatsApp.kt                  # @HiltAndroidApp
    │       ├── MainActivity.kt                 # Single-activity Compose root + nav state
    │       ├── core/logging/
    │       │   └── StructuredLogger.kt         # PlaybackEvent sealed events + log helper
    │       ├── data/
    │       │   ├── cache/
    │       │   │   └── CacheManager.kt         # audio_cache/ file cache + Room-backed index (500MB LRU)
    │       │   ├── download/
    │       │   │   ├── TrackDownloadManager.kt # OkHttp streaming downloads → StateFlow<Map<String,DownloadStatus>>
    │       │   │   └── DownloadStatus.kt       # Idle/Downloading/Completed/Failed
    │       │   ├── gateway/
    │       │   │   ├── GatewayMusicProvider.kt # MusicProvider impl → gateway REST
    │       │   │   ├── api/
    │       │   │   │   └── GatewayApi.kt       # Retrofit interface (search/stream/album/artist/playlist)
    │       │   │   ├── dto/
    │       │   │   │   └── GatewayDtos.kt      # kotlinx-serializable DTOs incl. error envelope
    │       │   │   └── mapper/
    │       │   │       ├── GatewayMapper.kt    # DTO → domain
    │       │   │       └── GatewayErrorMapper.kt # HTTP error → user message
    │       │   ├── network/
    │       │   │   └── NetworkMonitor.kt       # Connectivity callback → StateFlow<Boolean>
    │       │   ├── preferences/
    │       │   │   └── AppPreferences.kt       # DataStore (settings) + EncryptedSharedPreferences (auth)
    │       │   ├── local/
    │       │   │   ├── CliBeatsDatabase.kt     # Room @Database (6 entities, v1)
    │       │   │   ├── CliBeatsTypeConverters.kt
    │       │   │   ├── dao/                    # SongDao, PlaylistDao, HistoryDao, CacheIndexDao, QueueDao
    │       │   │   ├── entity/                 # SongEntity, PlaylistEntity, PlaylistSongCrossRef,
    │       │   │   │                              CacheIndexEntity, HistoryEntity, QueueEntity
    │       │   │   └── mapper/                 # SongMapper.kt, PlaylistMapper.kt (entity ↔ domain)
    │       │   └── repository/
    │       │       ├── SongRepositoryImpl.kt, PlaylistRepositoryImpl.kt, HistoryRepositoryImpl.kt
    │       │       └── PlaybackRepositoryImpl.kt   # Bridges PlayerAdapter + MusicProvider + QueueManager
    │       ├── di/                            # Hilt modules (all @InstallIn(SingletonComponent))
    │       │   ├── AppModule.kt                 # (empty placeholder)
    │       │   ├── DatabaseModule.kt            # Room db + DAOs
    │       │   ├── RepositoryModule.kt          # @Binds domain repository interfaces
    │       │   ├── ProviderModule.kt            # @Binds MusicProvider → GatewayMusicProvider
    │       │   ├── PlaybackModule.kt            # ExoPlayer + AudioAttributes
    │       │   ├── NetworkModule.kt             # Json, OkHttp (@Named gateway), Retrofit, GatewayApi, NetworkMonitor
    │       │   ├── CacheModule.kt               # CacheManager
    │       │   ├── DownloadModule.kt            # TrackDownloadManager
    │       │   ├── ImageLoaderModule.kt         # Coil ImageLoader (mem 25% / disk 2%)
    │       │   ├── StorageModule.kt             # DataStore + EncryptedSharedPreferences
    │       │   └── TelemetryModule.kt
    │       ├── domain/
    │       │   ├── model/                     # Track, Album, Artist, Playlist, PlaybackState, RepeatMode
    │       │   ├── provider/
    │       │   │   ├── MusicProvider.kt        # search/getTrack/stream/playlists/queue
    │       │   │   └── ProviderResult.kt       # sealed Success/Error/Loading
    │       │   ├── playback/
    │       │   │   └── QueueManager.kt         # in-memory queue + advance logic
    │       │   └── repository/                 # Song, Playlist, History, Playback repository interfaces
    │       ├── playback/
    │       │   ├── PlayerAdapter.kt            # ExoPlayer wrapper: queue, transport, PlaybackState flow
    │       │   └── service/
    │       │       └── PlaybackService.kt      # MediaSessionService (foreground media playback)
    │       ├── presentation/
    │       │   ├── component/                  # TUI primitives: PlayerBar, SongTableRow, TuiBlock, TuiTabBar
    │       │   ├── home/                       # HomeScreen.kt
    │       │   ├── search/                     # SearchScreen + SearchViewModel + SearchUiState
    │       │   ├── library/                    # LibraryScreen + LibraryViewModel + LibraryUiState
    │       │   ├── playlist/                   # PlaylistScreen + PlaylistViewModel + PlaylistUiState
    │       │   ├── queue/                      # QueueScreen + QueueViewModel + QueueUiState
    │       │   ├── player/                     # PlayerViewModel (+ playbackState observed by player bar)
    │       │   ├── settings/                   # SettingsScreen + SettingsViewModel + SettingsUiState
    │       │   ├── more/                       # MoreScreen.kt
    │       │   ├── layout/
    │       │   │   ├── MainLayout.kt           # NavigationSuiteScaffold shell + PlayerBar slot
    │       │   │   └── NavDestination.kt       # sealed destinations + mainTabs (Home/Search/Library/More)
    │       │   └── theme/                      # CliBeatsTheme/Colors/Typography/Shapes
    │       └── telemetry/                      # TelemetryTracker, CrashReporter, Timber impls, AnalyticsEvent
    ├── androidTest/java/com/clibeats/
    │   └── data/local/dao/                     # Room DAO instrumentation tests (SongDaoTest, ...)
    └── test/
        ├── java/com/clibeats/                  # JVM unit tests, mirroring main package layout
        │   ├── data/, domain/, presentation/, playback/, integration/, telemetry/, theme/, license/
        └── snapshots/                          # Paparazzi layout snapshots (images/, videos/)
```

## Gateway Tree (`gateway/`)

```
gateway/
├── package.json                 # scripts: build, start, dev, test, test:coverage, ci, openapi:*
├── tsconfig.json                 # ES2022 / CommonJS, strict, outDir dist/
├── vitest.config.ts             # test env node; coverage thresholds 70%; include tests/**/*.test.ts
├── vitest.config.ts             # node env
├── openapi.json                  # generated OpenAPI spec (swagger)
├── Dockerfile, docker-compose.yml
├── config/
│   └── gateway.yaml              # YAML config: server, providers (mock/youtube), cache TTLs, stream, health
├── scripts/
│   ├── generate-openapi.ts       # regenerates openapi.json from app routes
│   └── validate-openapi.ts       # spec validation (CI)
├── coverage/                     # vitest coverage output
├── dist/                         # tsc build output (runtime entry dist/server.js)
└── src/
    ├── server.ts                 # bootstrap: buildApp(), listen, SIGTERM/SIGINT shutdown
    ├── app.ts                    # Fastify factory: plugins, decorations, hooks, 9 routes, error handler
    ├── schemas.ts                # Fastify JSON-schema definitions (TrackSchema, ...) + route schemas
    ├── config/
    │   └── config.ts             # loadConfig(): gateway.yaml + env overrides (PORT, REDIS_URL, GATEWAY_CONFIG_PATH)
    ├── types/
    │   ├── adapter.ts            # ProviderAdapter interface + AdapterHealth
    │   ├── domain.ts             # Track, Album, Artist, Playlist, StreamResult, Lyrics
    │   ├── capabilities.ts       # ProviderCapabilities boolean flags
    │   ├── context.ts            # ProviderContext (country, lang, quality, traceId, ...)
    │   ├── error.ts              # ProviderError hierarchy + status codes
    │   └── declarations.d.ts
    ├── core/
    │   ├── registry/
    │   │   └── ProviderRegistry.ts          # Map<string, ProviderAdapter> + priority sort
    │   ├── selection/
    │   │   └── ProviderSelectionEngine.ts   # scoring + selectBestProvider + executeWithFailover
    │   ├── circuit/
    │   │   └── CircuitBreaker.ts            # CLOSED/OPEN/HALF_OPEN, failure threshold 3, cooldown 60s
    │   ├── cache/
    │   │   ├── CacheManager.ts               # owns 7 segregated caches
    │   │   ├── RedisCacheBase.ts             # fail-open primitive, key prefix clibeats:<ns>:
    │   │   └── segregated/
    │   │       ├── SearchCache.ts            # TTL 3600s
    │   │       ├── AlbumCache.ts, ArtistCache.ts, PlaylistCache.ts   # TTL 86400s
    │   │       ├── SessionCache.ts           # no TTL
    │   │       ├── ArtworkCache.ts           # TTL 604800s
    │   │       └── HealthCache.ts
    │   ├── events/
    │   │   └── EventBus.ts                   # globalEventBus singleton
    │   ├── health/
    │   │   └── RedisHealthChecker.ts        # PING + timeout → UP/DEGRADED/DOWN
    │   ├── logging/
    │   │   └── logger.ts                     # pino JSON logger, event-bus subscriber
    │   └── metrics/
    │       └── metrics.ts                    # prom-client register + EventBus → metrics wiring
    └── providers/
        ├── registerProviders.ts             # config-driven registration for mock + youtube
        ├── mock/
        │   └── MockProviderAdapter.ts      # seeded dataset + MockProviderState failure simulation
        └── youtube/
            ├── YouTubeProviderAdapter.ts   # youtubei.js client, dual session (music/ios), timeouts
            └── media.ts                     # YOUTUBE_PROVIDER_ID, parseRawItem, parseSubtitle, artwork
```

## Key File Locations

**App root config:**
- `app/build.gradle.kts` — all Android configuration; note `GATEWAY_BASE_URL` buildConfigField (emulator → host loopback) and testing framework deps (Paparazzi, MockWebServer, media3-test-utils)
- `gradle/libs.versions.toml` — the version catalog used by `build.gradle.kts` (new dependencies go here)

**Composition roots / DI:**
- App: `app/src/main/java/com/clibeats/CLIBeatsApp.kt` + the 11 files in `app/src/main/java/com/clibeats/di/`. Repository bindings are centralized in `di/RepositoryModule.kt` and `di/ProviderModule.kt` — any new repository/provider impl **must** be registered here
- Gateway: `gateway/src/app.ts` `buildApp()` — service instantiation (`CacheManager`, `ProviderRegistry`, `ProviderSelectionEngine`, `RedisHealthChecker`) and `app.decorate()` are all here

**Data mapping seams:**
- App: `data/local/mapper/SongMapper.kt` + `PlaylistMapper.kt` (Room ↔ domain), `data/gateway/mapper/GatewayMapper.kt` (gateway DTO ↔ domain)
- Gateway: `providers/*/media.ts` for raw payload → domain; `types/domain.ts` canonical shapes

## Naming Conventions

**Files:**
- **Android:** `PascalCase.kt` matching the public class — `SongRepository.kt`, `PlayerAdapter.kt`. Screens: `*Screen.kt`. ViewModels: `*ViewModel.kt`. UI state: `*UiState.kt`. DI: `*Module.kt`.
- **Gateway:** `PascalCase.ts` for classes (`MockProviderAdapter.ts`), `camelCase.ts` for instances/config (`config.ts`, `logger.ts`), `*Adapter.ts` for every provider class, `*Cache.ts` for cache namespaces, `*.test.ts` in `tests/`.

**Directories (both):** camelCase: `data/repository/`, `data/gateway/api/`, `core/cache/segregated/`, `providers/youtube/`.

**Android package structure:** one directory per clean-architecture layer — `presentation/`, `domain/`, `data/` — mirroring the public package name `com.clibeats`.

## Where to Add New Code

**New Android feature (e.g., "Favorites"):**
- UI/composables: `app/src/main/java/com/clibeats/presentation/favorites/FavoritesScreen.kt`
- ViewModel: `app/src/main/java/com/clibeats/presentation/favorites/FavoritesViewModel.kt` + `FavoritesUiState.kt` (annotated `@HiltViewModel`)
- Domain contract: `app/src/main/java/com/clibeats/domain/repository/FavoritesRepository.kt`
- Implementation: `app/src/main/java/com/clibeats/data/repository/FavoritesRepositoryImpl.kt` + binding in `app/src/main/java/com/clibeats/di/RepositoryModule.kt`
- Room: new entity in `data/local/entity/`, DAO in `data/local/dao/`, listed in `CliBeatsDatabase.kt` (bump `version`), and exported schema in `app/schemas/`
- Tests: mirror package under `app/src/test/java/com/clibeats/...` (JVM) — Room DAOs use `app/src/androidTest/`

**New gateway provider ("Bandcamp"):**
- Implement `ProviderAdapter`: `gateway/src/providers/bandcamp/BandcampProviderAdapter.ts`
- Register: `gateway/src/providers/registerProviders.ts`
- Enable/configure: `gateway/config/gateway.yaml` (`providers.bandcamp: { enabled, priority }`)
- Add capability flags if needed: `gateway/src/types/capabilities.ts`
- Tests: `gateway/tests/unit/bandcamp-adapter.test.ts`

**New gateway route:**
- Schema first: add to `gateway/src/schemas.ts` → define route in `gateway/src/app.ts` → regenerate `gateway/openapi.json` via `npm run openapi:generate`
- App-side client: mirror in `app/src/main/java/com/clibeats/data/gateway/api/GatewayApi.kt` + DTO in `gateway/dto/GatewayDtos.kt`

**New segregated cache namespace:**
- Extend `RedisCacheBase` in `gateway/src/core/cache/segregated/<Name>Cache.ts`, add instance in `gateway/src/core/cache/CacheManager.ts`, wire TTL in `gateway/config/gateway.yaml`.

## Special Directories

**`app/src/test/snapshots/`**
- Purpose: Paparazzi screenshot/layout golden images and videos
- Generated: Yes — by `paparazzi` test runs
- Committed: Yes

**`app/schemas/`**
- Purpose: Room schema export (`com.clibeats.data.local.CliBeatsDatabase/1.json`) for migrations
- Generated: Yes (KSP at build time)
- Committed: Yes

**`gateway/dist/`**
- Purpose: compiled JS output of `tsc`
- Generated: Yes (`npm run build`)
- Committed: No (runtime artifact; `.gitignore`)

**`gateway/coverage/`**
- Purpose: vitest v8 coverage reports
- Generated: Yes (`npm run test:coverage`)
- Committed: No

**`.planning/`**
- Purpose: GSD planning state (phases, roadmap, codebase docs)
- Generated: Yes (tooling)
- Committed: Yes (repo tracks it)

---

*Structure analysis: 2026-08-08*