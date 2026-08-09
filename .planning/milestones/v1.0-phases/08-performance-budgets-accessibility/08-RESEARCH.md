# Phase 8: Performance Budgets & Accessibility — Technical Research

## Objective
Research settings state management, cold start optimization, Compose rendering performance, and TalkBack accessibility compliance for Phase 8.

## 1. Requirements Mapping
- **`REQ-SET-02`**: Settings Screen (`SettingsUiState`, `SettingsViewModel`, `SettingsScreen` UI for theme, provider, cache limits, audio quality).
- **`REQ-NFR-01`**: Cold start performance budget (<2s startup initialization budget).
- **`REQ-NFR-02`**: 60 FPS list scrolling budget (`LazyColumn` key/contentType optimizations, Coil memory cache limits).
- **`REQ-NFR-03`**: Accessibility compliance (TalkBack `contentDescription`, 48dp touch targets, >= 4.5:1 text contrast).
- **`REQ-ENG-08`**: Unit test coverage for `SettingsViewModel` + ADR-008 documentation.

## 2. Technical Architecture

### Settings Screen & Preferences (`SettingsViewModel` & `SettingsScreen`)
- Uses `AppPreferences` DataStore + Keystore `EncryptedSharedPreferences`.
- `SettingsUiState`:
  - `activeProviderId: String` (e.g. `"ytmusic"`, `"local"`)
  - `cacheMaxMb: Int` (e.g. `512`)
  - `highQualityStreaming: Boolean` (e.g. `true`)
  - `isAuthenticated: Boolean`
- TUI styled Settings UI with section cards:
  - Active Music Provider selection
  - Max Disk Cache Capacity (256 MB, 512 MB, 1 GB, 2 GB)
  - Audio Quality Toggle (High Quality vs Standard)
  - Clear Audio Cache & Clear Session Credentials buttons

### Cold Start Optimization (`REQ-NFR-01`)
- Review `CliBeatsApplication.kt` and Hilt startup graph.
- Lazy-initialize non-critical singletons (e.g., download manager, network listeners) outside the immediate launch path.

### 60 FPS List Scrolling (`REQ-NFR-02`)
- Explicit `key = { song.id }` and `contentType = { "song_row" }` in all `LazyColumn` lists (`SongTable`, `QueueScreen`, `PlaylistScreen`, `LibraryScreen`).
- Configure Coil `ImageLoader` in Hilt `ImageLoaderModule` with 25% memory cache limit and disabled unnecessary crossfade transitions during fast flings.

### Accessibility Audit (`REQ-NFR-03`)
- Every `IconButton`, `Icon`, and interactive image element across `PlayerBar`, `NavigationDrawer`, `SongTableRow`, `QueueScreen`, and `SettingsScreen` MUST have explicit, localized `contentDescription`.
- Touch target sizes verified to be >= 48dp.
- High contrast dark theme (`#0D0D0D` background vs `#FFFFFF` text = 21:1 contrast ratio) exceeds 4.5:1 WCAG AA standards.

## 3. Quality Gate Targets
- 0 compile errors (`assembleDebug`).
- 0 Android Lint errors.
- 0 Detekt critical issues.
- 0 ktlint formatting errors.
- 100% passing unit test suite in `testDebugUnitTest`.
