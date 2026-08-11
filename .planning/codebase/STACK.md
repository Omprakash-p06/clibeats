# Technology Stack

**Analysis Date:** 2026-08-12

## Languages

**Primary:**
- Kotlin 2.0.21 - All application code (`app/src/main/java/com/clibeats/**`)

**Secondary:**
- Groovy / Kotlin DSL (`.gradle.kts`) - Gradle build scripts
- XML - Android manifest & resources (`app/src/main/AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`)
- YAML - Detekt rules (`config/detekt/detekt.yml`), CI workflow (`.github/workflows/ci.yml`)

## Runtime

**Environment:**
- Android 8.0+ (API 26) to API 34 — `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`, JVM target 17 (`app/build.gradle.kts`)
- Java 17 (temurin) — required for the Android Gradle build

**Package Manager:**
- Gradle 8.x with version catalog — `gradle/libs.versions.toml`, wrapper in `gradle/wrapper/` (`gradlew`, `gradlew.bat`), JitPack repo configured in `settings.gradle.kts` for NewPipeExtractor

## Frameworks

**Core:**
- Jetpack Compose (BOM 2024.09.03) + Material 3 — all UI (`app/src/main/java/com/clibeats/presentation/**`); includes Material3 Adaptive Navigation Suite 1.3.1 (`NavigationSuiteScaffold` in `MainLayout.kt`) and material-icons-extended
- AndroidX Media3 1.4.1 (ExoPlayer, MediaSession, MediaSessionService) — playback engine (`app/src/main/java/com/clibeats/playback/**`)
- Hilt 2.51.1 (Dagger) + Hilt Navigation Compose 1.2.0 — dependency injection (`app/src/main/java/com/clibeats/di/**`)

**Persistence:**
- Room 2.6.1 (KSP) — local DB (`CliBeatsDatabase`, 9 entities, version 3 with `MIGRATION_1_2`/`MIGRATION_2_3`), schemas exported to `app/schemas/com.clibeats.data.local.CliBeatsDatabase/{1,2,3}.json`
- DataStore Preferences 1.1.1 — non-sensitive settings + queue metadata (`AppPreferences`)
- androidx.security:security-crypto 1.1.0-alpha06 (EncryptedSharedPreferences + Keystore MasterKey AES256_GCM) — `AUTH_TOKEN` storage only

**Networking:**
- OkHttp 4.12.0 + Retrofit 2.11.0 + kotlinx-serialization 1.7.1 (retrofit2-kotlinx-serialization-converter) — 4 provider API clients (`data/provider/api/**`)
- Coil 2.7.0 — artwork loading in Compose (`TrackArtwork`)

**Stream Extraction (YouTube):**
- NewPipeExtractor v0.26.4 (JitPack, `com.github.TeamNewPipe:NewPipeExtractor`) — primary YouTube stream extraction (`data/provider/youtube/NewPipeExtractorResolver.kt`)
- Direct InnerTube client (`data/provider/api/InnerTubeApi.kt`) + PO-token WebView generator (`PoTokenGenerator.kt`) — fallback chain

**Testing:**
- JUnit 4.13.2, Mockito 5.x (mockito-kotlin 5.4.0, mockito-core 5.12.0), kotlinx-coroutines-test 1.8.1 — unit tests (`app/src/test/**`)
- Paparazzi 1.3.4 — Compose screenshot rendering tests (`*ScreenshotTest.kt`)
- MockWebServer (OkHttp 4.12.0) — HTTP mocking
- Room testing (`room-testing`) — in-memory DB for DAO tests (`app/src/androidTest/**`)
- Media3 test-utils — ExoPlayer test doubles
- Espresso + androidx.test.ext:junit — instrumented test deps

**Build/Dev:**
- AGP 8.5.2, Kotlin 2.0.21, Compose compiler plugin, KSP 2.0.21-1.0.27
- ktlint 12.1.1 (`org.jlleitschuh.gradle.ktlint`) + Detekt 1.23.6 (`config/detekt/detekt.yml`) + Android Lint

## Key Dependencies

**Critical:**
- NewPipeExtractor v0.26.4 — YouTube stream extraction; the most fragile dependency (JitPack pinned snapshot, breaks when YouTube changes)
- Media3 1.4.1 — background playback, media session + notification, audio focus
- Room 2.6.1 — offline persistence: songs, playlists, history, queue, liked songs, saved albums/artists, cache index
- OkHttp/Retrofit/kotlinx-serialization — provider HTTP clients (InnerTube, Audius, Jamendo, Internet Archive)
- Compose BOM 2024.09.03 + Material3 Adaptive Navigation — TUI design system + adaptive nav layout

**Infrastructure:**
- Coil 2.7.0 — artwork caching/loading
- security-crypto 1.1.0-alpha06 — Keystore-backed encrypted prefs (deprecated API, Tink migration planned per ADR-003)
- DataStore 1.1.1 — preferences
- Hilt 2.51.1 — DI graph

## Configuration

**Environment:**
- `JAMENDO_CLIENT_ID` (Gradle property `-PJAMENDO_CLIENT_ID` or `JAMENDO_CLIENT_ID` env) — optional free Jamendo API key (`app/build.gradle.kts:16-18`); empty by default
- No other build-time secrets required; all providers either keyless (Audius, Internet Archive, YouTube) or optional-key (Jamendo)

**Build:**
- `gradle/libs.versions.toml` — version catalog (single source of truth)
- `settings.gradle.kts` — JitPack repo for NewPipeExtractor
- `config/detekt/detekt.yml`, `.github/workflows/ci.yml`
- ProGuard rules in `app/proguard-rules.pro` (dormant — `isMinifyEnabled = false` in release)
- `android.suppressUnsupportedCompileSdk=35` in `gradle.properties` hides the newer-SDK warning (compileSdk stays 34)

## Platform Requirements

**Development:**
- Windows (WINDOWS.md documents emulator + physical device setup) or any OS with JDK 17 + Android SDK 34
- Android Studio or command-line Gradle; emulator for `connectedDebugAndroidTest`

**Production:**
- Standalone APK — no backend required; all provider calls happen on-device (README explicitly states "does not require a separate application backend")
- Release buildType signs with the debug keystore (`signingConfigs.getByName("debug")`) — not distributable on Play as-is
- No analytics/CDN owned by the project; playback streams go direct to provider CDNs (googlevideo, archive.org mirrors, Jamendo/Audius CDNs)

---

*Stack analysis: 2026-08-12*
*Update after major dependency changes*
