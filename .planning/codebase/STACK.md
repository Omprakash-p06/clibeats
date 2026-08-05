# Technology Stack

**Analysis Date:** 2026-08-05

## Languages

**Primary:**
- Kotlin 2.0.21 - All application source code in `app/src/main/java/com/clibeats/` and tests in `app/src/test/java/com/clibeats/`. Version pinned via `kotlin` in `gradle/libs.versions.toml` and applied through the `org.jetbrains.kotlin.android` plugin.

**Secondary:**
- Java 17 - JVM target only, not used for source authoring. Configured via `compileOptions` (`sourceCompatibility`/`targetCompatibility = JavaVersion.VERSION_17`) and `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }` in `app/build.gradle.kts:35-55`.

## Runtime

**Environment:**
- Android - minSdk 26 (Android 8.0), targetSdk 35 (Android 15), compileSdk 35 (`app/build.gradle.kts:15-23`). SDK location is machine-specific via `sdk.dir` in `local.properties` (gitignored).

**Package Manager:**
- Gradle 8.9 - via wrapper (`gradle/wrapper/gradle-wrapper.properties` -> `gradle-8.9-bin.zip`; `gradlew` / `gradlew.bat` present).
- Dependencies centralized in the version catalog `gradle/libs.versions.toml`; lockfile: none (Gradle dependency locking not enabled).

## Frameworks

**Core:**
- Jetpack Compose (BOM 2024.09.03) - Declarative UI. Compose enabled via `buildFeatures { compose = true }` (`app/build.gradle.kts:40-42`), using the `org.jetbrains.kotlin.plugin.compose` compiler plugin. Material 3 (`androidx.compose.material3:material3`) is the UI component set.
- Hilt 2.51.1 - Compile-time dependency injection (Dagger). Applied via `com.google.dagger.hilt.android` plugin + `com.google.devtools.ksp` processor. `@HiltAndroidApp` on `app/src/main/java/com/clibeats/CLIBeatsApp.kt`, `@AndroidEntryPoint` on `app/src/main/java/com/clibeats/MainActivity.kt`, scoping module at `app/src/main/java/com/clibeats/di/AppModule.kt`.
- AndroidX - `core-ktx` 1.13.1, `lifecycle-runtime-ktx` 2.8.6, `activity-compose` 1.9.2.

**Testing:**
- JUnit 4.13.2 - Unit tests (`testImplementation`), e.g. `app/src/test/java/com/clibeats/domain/model/TrackTest.kt`.
- Espresso 3.6.1 - Instrumented UI tests (`androidTestImplementation`), no test sources yet.

**Build/Dev:**
- Android Gradle Plugin 8.5.2 - Android build tooling (`com.android.application`).
- KSP 2.0.21-1.0.27 - Annotation processing for Hilt.
- Detekt 1.23.6 - Static analysis, config at `config/detekt/detekt.yml`, wired via `detekt { buildUponDefaultConfig = true }` in `app/build.gradle.kts:57-61`.
- ktlint 12.1.1 - Code style enforcement (`org.jlleitschuh.gradle.ktlint` plugin), `ktlintCheck` task.

## Key Dependencies

**Critical:**
- `com.google.dagger:hilt-android` 2.51.1 + `hilt-compiler` - DI framework; the app's architectural backbone (ADR-001 mandates Hilt).
- `androidx.compose:compose-bom` 2024.09.03 - Manages Compose artifact versions (ui, material3, ui-tooling-preview, ui-tooling).
- `androidx.compose.material3:material3` - Material 3 components for the TUI views.
- `androidx.activity:activity-compose` 1.9.2 - `setContent` host for `MainActivity`.
- `androidx.core:core-ktx` 1.13.1 - AndroidX core extensions.
- `androidx.lifecycle:lifecycle-runtime-ktx` 2.8.6 - Lifecycle-aware coroutine support.

**Infrastructure:**
- `org.jetbrains.kotlin.plugin.compose` (2.0.21) - Compose compiler Gradle plugin (Kotlin 2.0+ compiler integration).
- `com.google.devtools.ksp` 2.0.21-1.0.27 - KSP plugin for Hilt codegen.
- `io.gitlab.arturbosch.detekt:detekt-formatting` 1.23.6 - Formatting rule set bundled into detekt (`detektPlugins`).
- `androidx.test.espresso:espresso-core` 3.6.1 - Instrumented test framework.

## Configuration

**Environment:**
- No environment variables required. `local.properties` contains only machine-specific `sdk.dir=C:\Android\Sdk` (gitignored). No `.env` files present.
- `gradle.properties` sets `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `android.suppressUnsupportedCompileSdk=35`.

**Build:**
- `settings.gradle.kts` - Repositories `google()`, `mavenCentral()`, `gradlePluginPortal()`; `RepositoriesMode.FAIL_ON_PROJECT_REPOS`; single module `:app`; project name "CLIBeats".
- `build.gradle.kts` (root) - Declares plugin versions via catalog with `apply false`.
- `app/build.gradle.kts` - Module build config: namespace `com.clibeats`, applicationId `com.clibeats`, versionCode 1, versionName "0.1.0", release build type with `isMinifyEnabled = false` and ProGuard files (`proguard-android-optimize.txt` + `app/proguard-rules.pro`).
- `config/detekt/detekt.yml` - Detekt rule config: `maxIssues: 0`, complexity thresholds, and a `ForbiddenImport` guard blocking `com.clibeats.data.*` imports from the presentation layer.
- `app/proguard-rules.pro` - Stub rules file (Hilt components handled by plugin).

## Platform Requirements

**Development:**
- JDK 17 (enforced by `compileOptions` + `jvmTarget`; CI uses Temurin 17).
- Android SDK with platform 35 installed (`sdk.dir` in `local.properties`).
- Gradle 8.9 wrapper (`gradlew.bat` on Windows).

**Production:**
- Android 8.0 (API 26) or higher (minSdk 26).
- `INTERNET` permission declared in `app/src/main/AndroidManifest.xml:4` (required for future streaming providers).
- APK built via `assembleDebug`/`assembleRelease`; CI target is `ubuntu-latest` (`.github/workflows/ci.yml`).

---

*Stack analysis: 2026-08-05*
