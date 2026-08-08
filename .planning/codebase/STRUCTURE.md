# Codebase Structure: CliBeats

## Root Directory Tree
```text
clibeats/
├── .agents/                    # Customization & agent skills
├── .github/workflows/          # CI pipeline definitions
├── .planning/                  # Project roadmap, state, and codebase maps
├── app/                        # Android application source code
│   ├── schemas/                # Exported Room database schema versioning
│   └── src/
│       ├── main/
│       │   ├── java/com/clibeats/
│       │   │   ├── core/       # Logging & structured error handling
│       │   │   ├── data/       # Gateway, local DB, repositories
│       │   │   ├── di/         # Hilt Dependency Injection modules
│       │   │   ├── domain/     # Models, provider interfaces, repositories
│       │   │   ├── playback/   # ExoPlayer adapter & MediaSession service
│       │   │   ├── presentation/# Jetpack Compose UI & ViewModels
│       │   │   └── MainActivity.kt
│       │   └── res/            # Android resources (XML layout, values, icons)
│       └── test/               # Android unit tests & Paparazzi UI tests
├── docs/                       # Architecture Decision Records (ADRs) & documentation
└── gateway/                    # CliBeats Fastify Provider Gateway service
    ├── src/
    │   ├── config/             # Gateway configuration
    │   ├── core/               # Redis cache, circuit breaker, metrics
    │   ├── providers/          # YouTubeProviderAdapter & provider registry
    │   ├── app.ts              # Fastify application setup
    │   └── server.ts           # Server entry point
    └── tests/                  # Gateway unit, integration, and load tests
```

---

## Detailed Component Directory Structure

### `app/src/main/java/com/clibeats/`
- **`data/gateway/`**:
  - `GatewayMusicProvider.kt`: Implements `MusicProvider` targeting gateway REST API.
  - `api/GatewayApi.kt`: Retrofit interface definitions.
  - `dto/GatewayDtos.kt`: Serialized DTO data contracts.
  - `mapper/GatewayMapper.kt` & `GatewayErrorMapper.kt`: Domain & error mapping.
- **`data/local/`**:
  - `database/CliBeatsDatabase.kt`: Room database versioning and type converters.
  - `dao/`: `SongDao.kt`, `PlaylistDao.kt`, `HistoryDao.kt`.
  - `entity/`: Room database entities (`SongEntity`, `PlaylistEntity`, `HistoryEntity`, etc.).
  - `preferences/AppPreferences.kt`: Encrypted DataStore / EncryptedSharedPreferences wrapper.
- **`di/`**:
  - `AppModule.kt`, `DatabaseModule.kt`, `NetworkModule.kt`, `PlaybackModule.kt`, `ProviderModule.kt`, `StorageModule.kt`.
- **`playback/`**:
  - `PlayerAdapter.kt`: ExoPlayer wrapper and queue manager interface.
  - `service/PlaybackService.kt`: `MediaSessionService` implementation.
