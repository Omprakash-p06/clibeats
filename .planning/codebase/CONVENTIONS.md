# Codebase Conventions: CliBeats

## 1. Coding Standards & Code Style
- **Android / Kotlin**:
  - Official Kotlin Code Style enforced via `ktlint` and `detekt`.
  - Static analysis threshold: **0 critical issues**.
  - `ForbiddenImport` rule enforced globally: No low-level playback engine imports outside designated player engine and DI packages (`@file:Suppress("ForbiddenImport")`).
- **Gateway / TypeScript**:
  - TypeScript strict mode enabled (`tsconfig.json`).
  - ESLint / Prettier formatting rules.

---

## 2. Dependency Injection (Hilt) Conventions
- Every DI module MUST be annotated with `@Module` and `@InstallIn(SingletonComponent::class)`.
- Interface bindings MUST use `@Binds` + `@Singleton` (e.g. `GatewayMusicProvider` -> `MusicProvider` in `ProviderModule.kt`).
- Provider methods MUST use `@Provides` + `@Singleton` (e.g. `GatewayRetrofit` & `GatewayApi` in `NetworkModule.kt`).

---

## 3. Data & Error Handling Patterns
- Data flow uses `runCatching` blocks or `ProviderResult` sealed interfaces (`ProviderResult.Success` / `ProviderResult.Error`).
- Gateway HTTP exceptions are mapped to clean user-facing error messages via `GatewayErrorMapper.message(throwable)`.
- Database operations enforce Room entity mapping decoupling: Room entities (`SongEntity`) live in `data/local/entity/` and are converted to domain models (`Track`) via explicit extension mappers.
