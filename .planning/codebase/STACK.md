# Codebase Stack: CliBeats

## 1. Core Framework & Language
- **Android App**:
  - **Language**: Kotlin 2.0+
  - **Min SDK**: 26 (Android 8.0)
  - **Target/Compile SDK**: 34 (Android 14)
  - **UI Framework**: Jetpack Compose (Material3)
  - **Asynchronous & Flow**: Kotlin Coroutines 1.8.1, `kotlinx-coroutines-android`

- **Provider Gateway**:
  - **Runtime**: Node.js (v22+)
  - **Language**: TypeScript 5.7
  - **Framework**: Fastify 5.2
  - **Core Provider Adapter**: `youtubei.js` 17.2.0

---

## 2. Dependencies & Libraries

### Android Client (`app/build.gradle.kts`)
- **Dependency Injection**: Hilt 2.51.1 (`hilt-android`, `hilt-compiler`)
- **Networking**: Retrofit 2.11.0 + `kotlinx.serialization` (1.6.3), OkHttp 4.12.0
- **Audio Playback Engine**: Media3 ExoPlayer 1.4.1 (`androidx.media3:media3-exoplayer`, `media3-session`, `media3-ui`)
- **Local Persistence**:
  - **Database**: Room 2.6.1 + KSP (`room-runtime`, `room-ktx`, `room-compiler`)
  - **Settings**: DataStore Preferences 1.1.1 (`androidx.datastore:datastore-preferences`)
  - **Security**: Security Crypto 1.1.0-alpha06 (`androidx.security:security-crypto` - `EncryptedSharedPreferences`)
- **Testing**:
  - JUnit 4, Google Truth 1.4.2
  - Mockito Kotlin 5.4.0, Mockito Core 5.12.0
  - Paparazzi 1.3.4 (UI screenshot testing)
  - `androidx.test.ext:junit` 1.2.1

### Provider Gateway (`gateway/package.json`)
- **Server Framework**: Fastify 5.2.1 (`@fastify/cors`, `@fastify/swagger`, `@fastify/swagger-ui`)
- **Cache**: Redis via `ioredis` 5.4.2 (Mocked via `ioredis-mock` 5.9.0 for testing)
- **Metrics**: `prom-client` 15.1.3
- **Logging**: `pino` 9.6.0, `pino-pretty` 13.0.0
- **Testing**: Vitest 3.0.4, Fast Check 3.23.2, Autocannon 8.0.0

---

## 3. Build & CI Tools
- **Build Systems**: Gradle (Kotlin DSL `build.gradle.kts`), npm / `tsc` / `vitest`
- **Static Analysis**: Detekt, ktlint, Android Lint
- **CI Pipeline**: GitHub Actions (`.github/workflows/ci.yml`)