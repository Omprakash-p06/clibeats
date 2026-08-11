# Codebase Structure

**Analysis Date:** 2026-08-12

## Directory Layout

```
clibeats/
├── app/                          # Android application module (the only module)
│   ├── build.gradle.kts          # App build config (SDK, JAMENDO_CLIENT_ID, quality tools)
│   ├── proguard-rules.pro        # R8/ProGuard keep rules (dormant: minify disabled)
│   ├── schemas/                  # Exported Room DB schemas (1.json, 2.json, 3.json)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/clibeats/   # All Kotlin source (see below)
│       │   └── res/                # Fonts (JetBrains Mono), launcher icons, extraction rules
│       ├── test/java/com/clibeats/  # JVM unit + Paparazzi screenshot tests
│       └── androidTest/java/com/clibeats/  # Instrumented Room DAO tests
├── config/
│   └── detekt/detekt.yml         # Detekt rules (clean-architecture ForbiddenImport)
├── docs/                         # ADRs, release notes, user guide, docx design docs
├── gradle/                       # Version catalog (libs.versions.toml) + wrapper
├── logo/                         # App logo asset
├── scripts/
│   └── check-quality-gates.sh    # Local quality-gate runner
├── .github/workflows/ci.yml      # GitHub Actions CI (single Android job)
├── .planning/                    # GSD planning docs, codebase map, debug sessions
└── settings.gradle.kts, build.gradle.kts, gradle.properties
```

## Directory Purposes

**`app/src/main/java/com/clibeats/` (Android package root):**
- `CLIBeatsApp.kt` — `@HiltAndroidApp` Application class
- `MainActivity.kt` — single Compose activity with `NavDestination` switching
- `data/` — provider clients (`provider/`), Room DB (`local/`), repositories (`repository/`), cache (`cache/`), downloads (`download/`), preferences (`preferences/`), network monitor (`network/`), playlist import/export (`playlist/`)
- `di/` — Hilt modules (`AppModule`, `CacheModule`, `DatabaseModule`, `DownloadModule`, `ImageLoaderModule`, `NetworkModule`, `PlaybackModule`, `ProviderModule`, `RepositoryModule`, `StorageModule`, `TelemetryModule`)
- `domain/` — pure models (`model/`), provider contracts (`provider/`), repository interfaces (`repository/`)
- `playback/` — `PlayerAdapter` (ExoPlayer facade), `StreamResolver`, `service/PlaybackService` (MediaSessionService)
- `presentation/` — Compose screens (`home`, `search`, `library`, `playlist`, `queue`, `settings`, `more`), layout shell (`layout/`), shared components (`component/`), TUI theme (`theme/`)
- `telemetry/` — `AnalyticsEvent`, `CrashReporter`, `TelemetryTracker` interfaces + Timber implementations
- `util/` — `DiagnosticLogger`

**`app/src/test/` and `app/src/androidTest/`:**
- JVM unit + Paparazzi tests in `app/src/test/java/com/clibeats/` (47 files total across both trees); instrumented Room DAO tests in `app/src/androidTest/java/com/clibeats/data/local/dao/`

**`config/detekt/detekt.yml`:**
- Static-analysis rules incl. `ForbiddenImport` (`com.clibeats.data.*` banned from presentation), `maxIssues: 0`

**`docs/`:**
- `adr/` (ADR-000..011), `RELEASE_NOTES.md`, `USER_GUIDE.md`, `LICENSES.md`, `evidence/` (screenshots), and docx design docs (`01_Design_Brief.docx` .. `06_User_Research_Report_Template.docx`)

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/clibeats/MainActivity.kt`: Android UI entry (single activity)
- `app/src/main/java/com/clibeats/playback/service/PlaybackService.kt`: Background media entry

**Configuration:**
- `gradle/libs.versions.toml`: Android version catalog (single source of truth)
- `app/build.gradle.kts`: Build config incl. `JAMENDO_CLIENT_ID` handling
- `config/detekt/detekt.yml`: Static-analysis rules
- `.github/workflows/ci.yml`: CI pipeline

**Core Logic:**
- `app/src/main/java/com/clibeats/playback/PlayerAdapter.kt`: playback state engine + queue
- `app/src/main/java/com/clibeats/playback/StreamResolver.kt`: stream URL resolution
- `app/src/main/java/com/clibeats/data/provider/YouTubeMusicProvider.kt`: default provider + fallback chain
- `app/src/main/java/com/clibeats/data/provider/youtube/`: extraction stack (NewPipe, PO token, deobfuscator, stream cache, client strategy)
- `app/src/main/java/com/clibeats/data/repository/PlaybackRepositoryImpl.kt`: queue persistence + playback orchestration
- `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt`: Room DB v3 + migrations

**Testing:**
- `app/src/test/java/com/clibeats/**`: unit + screenshot tests
- `app/src/androidTest/java/com/clibeats/**`: instrumented DAO tests

## Naming Conventions

**Files:**
- Kotlin: `PascalCase.kt`, one primary class/object per file; `*Test.kt` for unit tests, `*ScreenshotTest.kt` for Paparazzi; DAOs `*Dao.kt`, entities `*Entity.kt`, Hilt modules `*Module.kt`, DTOs `*Dtos.kt`/`*Response.kt`
- Android resources: `snake_case.xml` (`data_extraction_rules.xml`)

**Directories:**
- Kotlin: lowercase single-context dirs matching package structure (`data/local/dao`, `domain/model`, `presentation/settings`)

**Special Patterns:**
- Room: entity → `*Entity.kt`, DAO → `*Dao.kt`, cross-ref → `*CrossRef.kt`; schema exported under `app/schemas/com.clibeats.data.local.CliBeatsDatabase/{1,2,3}.json`
- Hilt: `di/*Module.kt` (object or class with `@InstallIn`)
- Provider APIs: `*Api.kt` (Retrofit interface) + `*Dtos.kt` + `*Mapper.kt`
- Provider ids: `ProviderId.composite(providerId, sourceId)` → `"youtube_music:VIDEO_ID"`

## Where to Add New Code

**New Android feature (e.g., a new screen):**
- UI + ViewModel: `app/src/main/java/com/clibeats/presentation/<feature>/`
- State models: same dir as `*UiState.kt`
- Domain models/contracts: `app/src/main/java/com/clibeats/domain/model` + `domain/repository`
- Data implementation: `app/src/main/java/com/clibeats/data/**`
- DI wiring: `app/src/main/java/com/clibeats/di/`
- Tests: `app/src/test/java/com/clibeats/presentation/<feature>/` (unit) + `app/src/androidTest/` (instrumented)
- Navigation: register destination in `presentation/layout/NavDestination.kt` + `MainActivity.kt` (main tabs vs More menu)

**New music provider:**
- Adapter: `app/src/main/java/com/clibeats/data/provider/<Name>MusicProvider.kt` implementing `MusicProvider`
- API client: `data/provider/api/<Name>Api.kt` + DTOs + mapper
- Registration: `ProviderModule.provideProviderRegistry` (ordered list) + `ProviderId` usage for ids
- Tests: `app/src/test/java/com/clibeats/data/provider/<Name>MusicProviderTest.kt` + mapper tests

**New domain repository:**
- Interface: `domain/repository/<Name>Repository.kt`; impl: `data/repository/<Name>RepositoryImpl.kt`; binding: `di/RepositoryModule.kt`

**Utilities:**
- Shared helpers: `app/src/main/java/com/clibeats/core/**` (logging), `util/` (DiagnosticLogger)

## Special Directories

**`app/schemas/`:**
- Purpose: Exported Room migration schemas (1.json, 2.json, 3.json)
- Source: Auto-generated by Room KSP (`room.schemaLocation` arg in `app/build.gradle.kts`)
- Committed: Yes (needed for verified migrations)

**`docs/evidence/`:**
- Purpose: Screenshots used in README and release docs (final validation + recovery evidence)
- Committed: Yes

**`logo/`:**
- Purpose: App logo source asset
- Committed: Yes

---

*Structure analysis: 2026-08-12*
*Update when directory structure changes*
