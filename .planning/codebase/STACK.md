---
title: Technology Stack
last_mapped_commit: f4a1654be402779424fc4b3c06f20e1023327e0d
mapped_on: 2026-08-07
---

# Technology Stack

**Analysis Date:** 2026-08-07

## Languages

**Primary:**
- Kotlin 2.0.21 — all production and test source (`app/src/main/java/com/clibeats`, `app/src/test`, `app/src/androidTest`). Declared in `gradle/libs.versions.toml`.

**Secondary:**
- Kotlin DSL (`.kts`) — all Gradle build scripts: `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`.
- XML — Android manifest and resources: `app/src/main/AndroidManifest.xml`, `app/src/main/res/`.
- YAML — CI pipeline: `.github/workflows/ci.yml`.

## Runtime

**Environment:**
- Android — `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` (Android 8.0), `JVM 17` target/source compatibility. See `app/build.gradle.kts`.
- Gradle 8.9 — pinned via `gradle/wrapper/gradle-wrapper.properties`. JDK 17 required (Temurin on CI, `.github/workflows/ci.yml`).
- No JVM/JDK vendor constraint for local builds; `local.properties` present (SDK path, not committed).

**Package Manager:**
- Gradle with version catalog `gradle/libs.versions.toml`.
- Repository sources in `settings.gradle.kts`: `google()`, `mavenCentral()` (via version catalog plugin + dependency resolution, `RepositoriesMode.FAIL_ON_PROJECT_REPOS`).
- Uphill wrapper `gradlew` / `gradlew.bat` committed. No `gradle/verification-metadata.xml` found.

## Frameworks

**Core:**
- Jetpack Compose (BOM `2024.09.03`) — declarative UI. Used with the Compose compiler plugin `org.jetbrains.kotlin.plugin.compose` (Kotlin 2.0.21, from `libs.versions.toml`).
- Material3 + Material3 Adaptive Navigation Suite (`material3-adaptive-navigation-suite` 1.3.1) + `material-icons-extended`.
- Hilt / Dagger 2.51.1 — DI, with `androidx.hilt:hilt-navigation-compose` 1.2.0 and KSP (`com.google.devtools.ksp` 2.0.21-1.0.27).
- Coroutines / Flow — reactive state throughout (`StateFlow` in `PlayerAdapter.kt`, `NetworkMonitor.kt`, view models).

**Audio / Media:**
- AndroidX Media3 1.4.1 — `media3-exoplayer`, `media3-session`, `media3-common`. ExoPlayer instance + `MediaSessionService` in `app/src/main/java/com/clibeats/playback/`.

**Networking:**
- OkHttp 4.12.0 (+ `logging-interceptor`), Retrofit 2.11.0, `retrofit2-kotlinx-serialization-converter` 1.0.0, `kotlinx-serialization-json` 1.7.1. See `app/src/main/java/com/clibeats/di/NetworkModule.kt`.

**Imaging:**
- Coil 2.7.0 (`coil-compose`) — artwork loading. Configured in `app/src/main/java/com/clibeats/di/ImageLoaderModule.kt` (custom memory 25% / disk 2% cache, reuses OkHttp client, crossfade disabled).

**Data Persistence:**
- Room 2.6.1 (`room-runtime`, `room-ktx`, `room-compiler`, `room-testing`) with KSP. Schema exported to `app/schemas/`.
- DataStore Preferences 1.1.1 (`datastore-preferences`).
- AndroidX security-crypto 1.1.0-alpha06 (`EncryptedSharedPreferences` + `MasterKey`). Deprecated legacy API pinned intentionally (see `app/src/main/java/com/clibeats/di/StorageModule.kt`).

**Testing:**
- JUnit 4.13.2, Mockito `mockito-core` 5.12.0 + `mockito-kotlin` 5.4.0, `kotlinx-coroutines-test` 1.8.1, Room testing, Media3 test-utils, OkHttp `mockwebserver`, AndroidX test-ext-junit 1.2.1, Espresso 3.6.1, Compose `ui-test-junit4` + `ui-test-manifest`, and Paparazzi 1.3.4 (screenshot regression, `app.cash.paparazzi` plugin).

**Build/Dev:**
- Android Gradle Plugin 8.5.2, Kotlin 2.0.21, KSP 2.0.21-1.0.27, Detekt 1.23.6 (`detekt-formatting` plugin), ktlint 12.1.1 (`org.jlleitschuh.gradle.ktlint` plugin), Paparazzi.

## Key Dependencies

**Critical:**
- `androidx.media3:media3-exoplayer/session/common:1.4.1` — the audio engine and media-session contract behind `PlaybackService.kt` / `PlayerAdapter.kt`.
- `com.squareup.retrofit2:retrofit:2.11.0` + OkHttp — the InnerTube HTTP stack (`InnerTubeApi.kt`, `NetworkModule.kt`).
- `com.google.dagger:hilt-android:2.51.1` + KSP — dependency graph wiring across all `com.clibeats.di.*` modules.
- `androidx.room:room-*:2.6.1` — local library/queue/cache metadata persistence (`CliBeatsDatabase.kt`).
- `androidx.compose.material3` + adaptive navigation — the UI layer.

**Infrastructure:**
- `androidx.security:security-crypto:1.1.0-alpha06` — secure settings storage.
- `androidx.datastore:datastore-preferences:1.1.1` — non-sensitive preferences.
- `io.coil-kt:coil-compose:2.7.0` — artwork rendering.

## Configuration

**Source set structure:** single `:app` module under `app/`, with `app/src/main`, `app/src/test` (JVM unit tests), and `app/src/androidTest` (instrumented DAO tests).

**Environment:**
- `app/build.gradle.kts` — SDK versions, applicationId `com.clibeats`, versionCode `1`, `versionName "0.1.0"`. Release build: `isMinifyEnabled = false`, `proguard-android-optimize.txt` + `app/proguard-rules.pro`, and crucially `signingConfig = signingConfigs.getByName("debug")` (release APK signs with the debug key — beta/dev distribution only).
- `gradle.properties` — `org.gradle.jvmargs=-Xmx2048m`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `android.suppressUnsupportedCompileSdk=35`.
- `local.properties` — SDK location (dev-only, gitignored), not part of build logic.

**Build:**
- Single module. `ksp` args configure Room schema export dir: `room.schemaLocation=$projectDir/schemas` (`app/build.gradle.kts`).
- Lint: `abortOnError = true`, `checkDependencies = true`, `warningsAsErrors = false`.
- Detekt baseline config: `config/detekt/detekt.yml`, `buildUponDefaultConfig = true`.
- `scripts/check-quality-gates.sh` orchestrates `ktlintCheck`, `detekt`, `lintDebug`, `testDebugUnitTest`.

## Platform Requirements

**Development:**
- JDK 17, Android SDK 34, Gradle wrapper 8.9. Build via `./gradlew assembleDebug`. Tests via `./gradlew testDebugUnitTest`, screenshots via `./gradlew verifyPaparazziDebug`.

**Production:**
- Android 8.0 (API 26) and up; targets Android 14 (API 34). No cloud backend is self-hosted; delivery is via APK (CI builds `assembleDebug`).

---

*Stack analysis: 2026-08-07*