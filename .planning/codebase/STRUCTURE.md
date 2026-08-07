---
title: CLIBeats Codebase Structure
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

# Codebase Structure

**Analysis Date:** 2026-08-07

## Directory Layout

```
clibeats/
├── app/                          # Single Gradle module (:app)
│   ├── build.gradle.kts          # Android app config, deps, detekt/ktlint setup
│   ├── schemas/                  # Room schema exports (ksp arg room.schemaLocation)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/clibeats/
│       │   │   ├── CLIBeatsApp.kt        # @HiltAndroidApp Application
│       │   │   ├── MainActivity.kt       # single-activity host + nav state
│       │   │   ├── presentation/         # Compose UI, ViewModels, UiState
│       │   │   ├── domain/               # pure-Kotlin models, interfaces, provider
│       │   │   ├── data/                 # Room, Retrofit, cache, downloads, prefs
│       │   │   ├── playback/             # PlayerAdapter + MediaSessionService
│       │   │   ├── di/                   # Hilt modules
│       │   │   └── telemetry/            # TelemetryTracker / CrashReporter
│       │   └── res/                      # launcher icons, fonts (JetBrains Mono), xml
│       ├── test/java/com/clibeats/       # unit + Paparazzi screenshot tests
│       └── androidTest/java/com/clibeats/  # Room DAO instrumentation tests
├── config/detekt/detekt.yml      # detekt ruleset (custom ForbiddenImport rule)
├── docs/                         # Design docs (.docx) + docs/adr/*.md
├── gradle/libs.versions.toml     # Version catalog (single source of versions)
├── settings.gradle.kts           # rootProject "CLIBeats", includes :app
├── build.gradle.kts              # root build config
└── scripts/                      # repo scripts (release/build tooling)
```

## Directory Purposes

**`app/src/main/java/com/clibeats/presentation/`:**
- Purpose: MVVM presentation layer — one subpackage per feature
- Contains: `home/`, `search/`, `library/`, `playlist/`, `queue/`, `settings/`, `player/`, `more/` (each: `*Screen.kt`, `*ViewModel.kt`, `*UiState.kt`); shared `component/` (`TuiBlock.kt`, `SongTableRow.kt`, `PlayerBar.kt`); `layout/` (`MainLayout.kt`, `NavDestination.kt`); `theme/` (`CliBeatsColors.kt`, `CliBeatsTypography.kt`, `CliBeatsShapes.kt`, `CliBeatsTheme.kt`)
- Key files: `layout/MainLayout.kt`, `player/PlayerViewModel.kt`

**`app/src/main/java/com/clibeats/domain/`:**
- Purpose: pure-Kotlin contracts and models (zero Android imports per ADR-001)
- Contains: `model/` (`Track.kt`, `Playlist.kt`, `Artist.kt`, `Album.kt`, `PlaybackState.kt`), `repository/` (`SongRepository.kt`, `PlaylistRepository.kt`, `HistoryRepository.kt`, `PlaybackRepository.kt`), `provider/` (`MusicProvider.kt`, `ProviderResult.kt`)
- Key files: `provider/MusicProvider.kt`, `repository/PlaybackRepository.kt`

**`app/src/main/java/com/clibeats/data/`:**
- Purpose: concrete implementations of domain contracts + local persistence + network
- Contains: `repository/` (`*Impl.kt`), `local/` (`CliBeatsDatabase.kt`, `CliBeatsTypeConverters.kt`, `dao/`, `entity/`, `mapper/`), `provider/` (`YouTubeMusicProvider.kt`, `api/` (`InnerTubeApi.kt`, `InnerTubeHeaderInterceptor.kt`), `dto/`, `mapper/TrackMapper.kt`), `network/NetworkMonitor.kt`, `preferences/AppPreferences.kt`, `cache/CacheManager.kt`, `download/` (`TrackDownloadManager.kt`, `DownloadStatus.kt`)
- Key files: `local/CliBeatsDatabase.kt`, `provider/YouTubeMusicProvider.kt`, `preferences/AppPreferences.kt`

**`app/src/main/java/com/clibeats/playback/`:**
- Purpose: Media3 integration (outside the three core layers)
- Contains: `PlayerAdapter.kt`, `service/PlaybackService.kt`
- Key file: `PlayerAdapter.kt` (ExoPlayer facade, playback `StateFlow`s)

**`app/src/main/java/com/clibeats/di/`:**
- Purpose: all Hilt modules — `AppModule` (empty), `DatabaseModule`, `RepositoryModule` (@Binds), `ProviderModule` (@Binds MusicProvider), `PlaybackModule`, `NetworkModule`, `StorageModule`, `CacheModule`, `DownloadModule`, `ImageLoaderModule`, `TelemetryModule`
- Key files: `RepositoryModule.kt`, `ProviderModule.kt`, `StorageModule.kt`, `NetworkModule.kt`

**`app/src/main/java/com/clibeats/telemetry/`:**
- Purpose: analytics/crash abstractions + Timber impls
- Contains: `TelemetryTracker.kt`, `CrashReporter.kt`, `TimberTelemetryTracker.kt`, `TimberCrashReporter.kt`, `AnalyticsEvent.kt`

**`app/src/test/` and `app/src/androidTest/`:**
- Purpose: mirror main package structure. Unit/Paparazzi tests in `test/java/com/clibeats/` (e.g., `presentation/search/SearchViewModelTest.kt`, `playback/PlayerAdapterTest.kt`, `theme/PlayerBarScreenshotTest.kt`, `data/provider/YouTubeMusicProviderTest.kt`, `integration/PlaybackIntegrationTest.kt`); Room DAO instrumentation in `androidTest/java/com/clibeats/data/local/dao/`.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/clibeats/MainActivity.kt`: single launcher activity, nav state holder
- `app/src/main/java/com/clibeats/CLIBeatsApp.kt`: Hilt Application
- `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`: media-session service
- `app/src/main/AndroidManifest.xml`: component registration + permissions

**Configuration:**
- `gradle/libs.versions.toml`: version catalog (AGP 8.5.2, Kotlin 2.0.21, Hilt 2.51.1, Room 2.6.1, Media3 1.4.1, Compose BOM 2024.09.03)
- `app/build.gradle.kts`: compileSdk 34, minSdk 26, targetSdk 34; detekt/ktlint/Paparazzi plugins
- `config/detekt/detekt.yml`: detekt rules (incl. custom `ForbiddenImport`)
- `docs/adr/*.md`: architecture decision records (ADR-001 through ADR-011)

**Core Logic:**
- `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`: playback engine facade
- `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`: provider impl
- `app/src/main/java/com/clibeats/data/provider/api/InnerTubeApi.kt`: Retrofit endpoints
- `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt`: Room DB (6 entities, 5 DAOs, version 1)
- `app/src/main/java/com/clibeats/data/preferences/AppPreferences.kt`: DataStore + EncryptedSharedPreferences
- `app/src/main/java/com/clibeats/data/cache/CacheManager.kt`: audio cache with LRU eviction

## Naming Conventions

**Files:**
- Screens: `{Feature}Screen.kt` (e.g., `SearchScreen.kt`)
- ViewModels: `{Feature}ViewModel.kt` (e.g., `SearchViewModel.kt`)
- UiState: `{Feature}UiState.kt` (e.g., `LibraryUiState.kt`)
- Domain interfaces: `{Name}Repository.kt`, `MusicProvider.kt`
- Data impls: `{Name}RepositoryImpl.kt` (e.g., `SongRepositoryImpl.kt`)
- Room: entities `{Name}Entity.kt`, DAOs `{Name}Dao.kt`
- Providers: `{Provider}Provider.kt` (e.g., `YouTubeMusicProvider.kt`), DTOs `{Name}Request/Response.kt`
- Mappers: `{Name}Mapper.kt` (`SongMapper.kt`, `PlaylistMapper.kt`, `TrackMapper.kt`)

**Directories:**
- Feature subpackages under `presentation/` are lowercase (e.g., `presentation/queue/`)
- Layer packages: `presentation`, `domain`, `data`, `playback`, `di`, `telemetry`
- Class-name casing inside each package dir (e.g., `data/local/dao/`, `data/provider/api/`, `data/provider/dto/`)

## Where to Add New Code

**New Feature (screen):**
- UI + ViewModel + UiState: `app/src/main/java/com/clibeats/presentation/<feature>/`
- Register destination: add `data object` to `presentation/layout/NavDestination.kt` and a branch in `MainActivity.kt`'s `when`
- Tests: `app/src/test/java/com/clibeats/presentation/<feature>/`

**New Repository (domain contract):**
- Interface: `app/src/main/java/com/clibeats/domain/repository/`
- Impl: `app/src/main/java/com/clibeats/data/repository/`
- Binding: `app/src/main/java/com/clibeats/di/RepositoryModule.kt`

**New Music Provider:**
- Impl + API: `app/src/main/java/com/clibeats/data/provider/`
- Bind as `MusicProvider` in `app/src/main/java/com/clibeats/di/ProviderModule.kt` (replace/alias `YouTubeMusicProvider`)

**New DAO / Entity:**
- `app/src/main/java/com/clibeats/data/local/dao/` and `entity/`; register in `CliBeatsDatabase.kt`; bump DB version + export schema to `app/schemas/`

**Playback changes:**
- Engine logic: `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`
- Background audio: `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`

**Utilities/helpers:**
- Pure helpers used by data layer: `app/src/main/java/com/clibeats/data/<area>/mapper/` or `data/<area>/`; UI helpers: `presentation/component/`

## Special Directories

**`app/schemas/`:**
- Purpose: exported Room schemas (from `ksp { arg("room.schemaLocation", ...) }`)
- Generated: Yes — committed for migration history

**`app/src/main/res/font/`:**
- Purpose: bundled JetBrains Mono fonts (regular/medium/semibold/bold) for the TUI aesthetic
- Generated: No — Committed: Yes

**`docs/adr/`:**
- Purpose: architecture decision records (ADR-001..ADR-011) — source of truth for architectural choices (Hilt+Clean Arch, Media3 background audio, InnerTube provider, encrypted storage, caching)
- Generated: No — Committed: Yes

**`config/detekt/`:**
- Purpose: detekt baseline/ruleset including the custom `ForbiddenImport` rule referenced by `@file:Suppress("ForbiddenImport")` comments
- Generated: No — Committed: Yes

---

*Structure analysis: 2026-08-07*
