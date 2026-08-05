# Codebase Structure

**Analysis Date:** 2026-08-05

## Directory Layout

```
clibeats/
├── .github/
│   └── workflows/
│       └── ci.yml                 # GitHub Actions: ktlint, detekt, lint, build, unit tests
├── .planning/                     # GSD planning artifacts (PROJECT, ROADMAP, phased plans)
│   ├── PROJECT.md
│   ├── REQUIREMENTS.md
│   ├── ROADMAP.md
│   ├── STATE.md
│   └── phases/                    # 00-engineering-foundation, 01-architecture-core-provider-api
├── app/
│   ├── build.gradle.kts           # App module build config (Compose, Hilt, KSP, detekt)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/clibeats/
│       │       ├── CLIBeatsApp.kt     # @HiltAndroidApp Application
│       │       ├── MainActivity.kt    # @AndroidEntryPoint, Compose host
│       │       ├── data/              # EMPTY (placeholder)
│       │       ├── di/AppModule.kt    # Hilt @SingletonComponent module
│       │       ├── domain/
│       │       │   ├── model/         # Track, Album, Artist, Playlist, PlaybackState
│       │       │   └── provider/      # MusicProvider, ProviderResult
│       │       └── presentation/      # EMPTY (placeholder)
│       └── test/
│           └── java/com/clibeats/domain/model/TrackTest.kt
├── config/
│   └── detekt/detekt.yml          # Detekt rules (ForbiddenImport layer guard)
├── docs/
│   ├── adr/                       # Architecture Decision Records (ADR-000, ADR-001)
│   └── *.docx                     # Original design/SRS/SDD/FSD/Research specs
├── gradle/
│   ├── libs.versions.toml         # Version catalog (single source of dependency versions)
│   └── wrapper/
├── scripts/
│   └── check-quality-gates.sh     # Local 4-step quality gate runner
├── build.gradle.kts               # Root plugin declarations (`apply false`)
├── settings.gradle.kts            # Plugin repos, module includes, rootProject "CLIBeats"
├── gradle.properties              # Gradle JVM args, AndroidX flags
├── gradlew / gradlew.bat          # Wrapper launchers
├── local.properties               # Local SDK path (machine-specific)
└── .gitignore
```

## Directory Purposes

**`app/src/main/java/com/clibeats/domain`:**
- Purpose: Pure-Kotlin business models and provider contracts — the core of the app.
- Contains: File-per-model data classes in `model/`; provider interface + result type in `provider/`.
- Key files: `model/Track.kt`, `model/Album.kt`, `model/Artist.kt`, `model/Playlist.kt`, `model/PlaybackState.kt`, `provider/MusicProvider.kt`, `provider/ProviderResult.kt`.

**`app/src/main/java/com/clibeats/data`:**
- Purpose: Reserved for data-layer implementations (provider adapters, Room DAOs, cache).
- Contains: Empty — only `data/.gitkeep`. Placeholder for planned phases.

**`app/src/main/java/com/clibeats/presentation`:**
- Purpose: Reserved for Compose TUI UI components and ViewModels.
- Contains: Empty — only `presentation/.gitkeep`. Placeholder for planned phases.

**`app/src/main/java/com/clibeats/di`:**
- Purpose: Hilt DI modules.
- Contains: `AppModule.kt` (`@SingletonComponent`, currently empty body).

**`config/detekt`:**
- Purpose: Static-analysis rule configuration shared across modules.
- Contains: `detekt.yml` (maxIssues 0, complexity rules, MagicNumber, `ForbiddenImport` for `com.clibeats.data.*`).

**`gradle`:**
- Purpose: Version catalog + wrapper.
- Contains: `libs.versions.toml` (all dependency/library/plugin versions); `wrapper/` (wrapper jar/properties).

**`docs`:**
- Purpose: Architecture Decision Records and original project specification documents.
- Contains: `adr/ADR-000-template.md`, `adr/ADR-001-architecture-and-di-strategy.md`; `.docx` specs (Design Brief, UI Spec, SRS, SDD, FSD, Research Report).

**`.planning`:**
- Purpose: GSD workflow state and phase artifacts (not part of the shipped app).
- Contains: `PROJECT.md`, `REQUIREMENTS.md`, `ROADMAP.md`, `STATE.md`, `config.json`, and `phases/` with `PLAN.md`/`SUMMARY.md`/`UAT.md` per unit.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/clibeats/CLIBeatsApp.kt`: Application + Hilt root.
- `app/src/main/java/com/clibeats/MainActivity.kt`: Single Compose Activity.
- `app/src/main/AndroidManifest.xml`: Launcher activity + `INTERNET` permission.

**Configuration:**
- `app/build.gradle.kts`: Module plugins, SDK versions (compile/target 35, min 26), detekt config wiring.
- `build.gradle.kts`: Root plugin version declarations.
- `settings.gradle.kts`: Repositories, module includes, root project name.
- `gradle/libs.versions.toml`: All dependency/plugin/library versions.
- `gradle.properties`: JVM args, AndroidX flags, compile-SDK suppression.
- `config/detekt/detekt.yml`: Static-analysis rules.

**Core Logic:**
- `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`: Provider strategy interface.
- `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt`: Sealed result type.
- `app/src/main/java/com/clibeats/domain/model/*.kt`: Immutable domain models.

**Testing:**
- `app/src/test/java/com/clibeats/domain/model/TrackTest.kt`: JUnit4 model tests (mirrors package structure).

## Naming Conventions

**Files:**
- Kotlin files: PascalCase matching the primary type — `Track.kt`, `MusicProvider.kt`, `MainActivity.kt`.
- Test files: `<ClassUnderTest>Test.kt` — `TrackTest.kt`.

**Directories:**
- Package dirs flow from `com.clibeats.` + layer (`domain`/`data`/`presentation`/`di`).
- Layer sub-packages use lowercase plural nouns reflecting content: `domain/model`, `domain/provider`.

## Where to Add New Code

**New Feature (e.g. a provider implementation or player):**
- Primary code: `app/src/main/java/com/clibeats/data/` (repo + provider adapters), `app/src/main/java/com/clibeats/presentation/` (ViewModels + TUI screens), new use-cases in `app/src/main/java/com/clibeats/domain/` if business logic is added.
- Tests: `app/src/test/java/com/clibeats/` mirroring the source package (e.g. `domain/model/TrackTest.kt`).

**New Component/Module:**
- Implementation: `app/src/main/java/com/clibeats/presentation/` for UI, `data/` for persistence/network.

**Utilities:**
- No shared utility directory exists yet; shared helper code would be introduced in `domain/` (if pure Kotlin) or a new `util` package under `com.clibeats`, keeping the layer guard in mind.

## Special Directories

**`.planning`:**
- Purpose: GSD planning state + phase artifacts.
- Generated: Yes (by workflow).
- Committed: Yes (planning artifacts are part of the repo).

**`docs`:**
- Purpose: Architecture Decision Records and source specifications (`.docx`).
- Generated: No.
- Committed: Yes.

**`build/` and `.gradle/`:**
- Purpose: Gradle build outputs and caches.
- Generated: Yes (build tooling).
- Committed: No (git-ignored).

**`local.properties`:**
- Purpose: Machine-specific Android SDK path.
- Generated: Yes (IDE/Gradle).
- Committed: No (git-ignored).

---

*Structure analysis: 2026-08-05*