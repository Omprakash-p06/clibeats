# Codebase Architecture: CliBeats

## 1. High-Level Architecture Overview
CliBeats follows a **Decoupled Gateway Architecture** combined with **Clean Architecture + MVVM** on Android.

```text
Jetpack Compose UI (Screens / Components)
       ↓
PlayerViewModel / SearchViewModel / HomeViewModel
       ↓
PlaybackRepositoryImpl / SongRepositoryImpl
       ↓
GatewayMusicProvider (implements MusicProvider)
       ↓
GatewayApi (Retrofit Client)
       ↓  (HTTP / REST JSON)
CliBeats Provider Gateway (Fastify Server)
       ↓
YouTubeProviderAdapter
       ↓
YouTube.js (Server-side InnerTube)
       ↓
GoogleVideo Direct Stream URL
       ↓  (Returned to GatewayMusicProvider)
PlayerAdapter (Media3 ExoPlayer)
       ↓
Audio Playback & Background MediaSession
```

---

## 2. Layer Responsibilities

### Android Application Layer (`app/`)
1. **Presentation Layer (`presentation/`)**:
   - Built with Jetpack Compose (Material3 design system).
   - ViewModels manage screen state and interact with domain repositories.
2. **Domain Layer (`domain/`)**:
   - Defines core models (`Track`, `Playlist`, `PlaybackState`, `RepeatMode`).
   - Defines provider interface (`MusicProvider`) and repository contracts (`PlaybackRepository`, `SongRepository`, `PlaylistRepository`, `HistoryRepository`).
3. **Data Layer (`data/`)**:
   - `data/gateway/`: Implements `GatewayMusicProvider` calling `GatewayApi`. Maps gateway error codes using `GatewayErrorMapper`.
   - `data/local/`: Room database (`CliBeatsDatabase`), DAOs, mappers, and DataStore/EncryptedSharedPreferences wrappers (`AppPreferences`).
   - `data/repository/`: Implementations of domain repository interfaces delegating to Gateway or Room DAOs.
4. **Playback Layer (`playback/`)**:
   - `PlayerAdapter`: Wraps Media3 `ExoPlayer`, manages queue transitions, state listener callbacks, position tickers, and caching layer lookup.
   - `PlaybackService`: `MediaSessionService` for background playback and system notification controls.

### Provider Gateway Layer (`gateway/`)
1. **Core Gateway (`gateway/src/app.ts`, `server.ts`)**: Fastify web server handling REST endpoints, swagger docs, metrics, and error schema mapping.
2. **Provider Adapters (`gateway/src/providers/`)**: `YouTubeProviderAdapter` abstracts YouTube extraction via `youtubei.js`.
3. **Cache & Resiliency (`gateway/src/core/`)**: Redis caching layer, circuit breakers, rate limiting, and structured logging.
