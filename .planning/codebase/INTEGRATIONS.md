# External Integrations

**Analysis Date:** 2026-08-05

## APIs & External Services

**Music Provider APIs (planned, not implemented):**
- None implemented. The project is at foundation stage; no HTTP client library (Retrofit/OkHttp/Ktor) is present in `gradle/libs.versions.toml` or `app/build.gradle.kts`.
- The extension point is the `MusicProvider` interface at `app/src/main/java/com/clibeats/domain/provider/MusicProvider.kt`, which defines the contract future provider adapters must implement:
  - `search(query, limit)`, `getTrack(trackId)`, `stream(trackId)`, `playlists()`, `queue()` — all suspend functions returning `ProviderResult<T>`.
  - Each provider is keyed by `providerId` + `displayName`; domain models (`Track.kt`, `Album.kt`, `Artist.kt`, `Playlist.kt` in `app/src/main/java/com/clibeats/domain/model/`) carry a `providerId` field to support multi-provider routing.
- Result envelope `ProviderResult` at `app/src/main/java/com/clibeats/domain/provider/ProviderResult.kt` models `Success` / `Error(message, cause)` / `Loading` states for provider calls.

**Other third-party APIs:**
- None detected. No SDK imports for Spotify, YouTube Music, SoundCloud, Last.fm, or similar found in the codebase.

## Data Storage

**Databases:**
- None implemented. No Room dependency in `gradle/libs.versions.toml`. ADR-001 (`docs/adr/ADR-001-architecture-and-di-strategy.md`) plans Room DAOs in a future `Data` layer; the `Data` package (`com.clibeats.data.*`) does not exist yet and is explicitly protected by a Detekt `ForbiddenImport` rule in `config/detekt/detekt.yml`.

**File Storage:**
- None beyond standard Android app sandbox storage. No explicit storage abstraction exists.

**Caching:**
- None.

## Authentication & Identity

**Auth Provider:**
- None. No OAuth library, no account manager integration, no auth SDKs. `MainActivity` (`app/src/main/java/com/clibeats/MainActivity.kt`) hosts an empty Compose root pending TUI wiring.

## Monitoring & Observability

**Error Tracking:**
- None (no Sentry/Firebase Crashlytics).

**Logs:**
- None configured beyond Android default `Log`/`println` (no source usages yet). No logging framework dependency.

## CI/CD & Deployment

**Hosting:**
- Not applicable (mobile application; no server-side deployment).

**CI Pipeline:**
- GitHub Actions — `.github/workflows/ci.yml`.
  - Triggers: push / pull_request on `main` and `develop`; concurrency group with cancel-in-progress.
  - Runner: `ubuntu-latest`, JDK 17 (Temurin via `actions/setup-java@v4`), Gradle setup via `gradle/actions/setup-gradle@v3`.
  - Steps: `ktlintCheck` → `detekt` → `lintDebug` → `assembleDebug` → `testDebugUnitTest`, with test/lint/detekt reports uploaded via `actions/upload-artifact@v4`.
- Local quality gate script mirrors the CI steps: `scripts/check-quality-gates.sh`.

## Environment Configuration

**Required env vars:**
- None. The app currently requires no secrets or API keys.

**Secrets location:**
- None. No `.env` files, credential files, or keystores are present in the repository. `local.properties` (gitignored) contains only the machine-specific `sdk.dir` path.

## Webhooks & Callbacks

**Incoming:**
- None.

**Outgoing:**
- None.

---

*Integration audit: 2026-08-05*
