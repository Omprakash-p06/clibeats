# Phase 6: Playlists, Queue Management & Library — Technical Research

## Objective
Research architecture, state management, Room persistence, and Compose UI patterns for Phase 6: Playlists, Queue Management & Library.

## 1. Domain Requirements & Requirements Mapping
- **`REQ-MUS-03`**: Interactive Queue panel (reorder tracks, remove track, clear queue, persist queue across app restarts).
- **`REQ-LIB-01`**: Library Screen (browse saved tracks, artists, albums from local Room database).
- **`REQ-LIB-02`**: Playlist Screen (create, edit, delete playlists, add/remove tracks, view playlist details).

## 2. Technical Strategy

### Queue Management & Persistence Strategy
- **PlayerAdapter enhancements:**
  - Add `moveTrack(fromIndex: Int, toIndex: Int)` using `ExoPlayer.moveMediaItem()`.
  - Add `removeTrack(index: Int)` using `ExoPlayer.removeMediaItem()`.
  - Add `clearQueue()` using `ExoPlayer.clearMediaItems()`.
  - Expose `queueFlow: StateFlow<List<Track>>` from `PlayerAdapter` / `PlaybackRepository`.
- **Queue Persistence in Room:**
  - Create `QueueEntity(position: Int, songId: String)` in Room database.
  - `QueueDao` with `replaceQueue(items: List<QueueEntity>)`, `getQueueFlow()`, `clearQueue()`.
  - On queue change in `PlayerAdapter`, persist current queue items and current playing index to Room DB / DataStore.
  - On app launch, restore queue into ExoPlayer.

### Library Browsing Strategy
- **`SongRepository` & `SongDao`:**
  - Query all tracks ordered by title, artist, or date added (`getAllTracksAsFlow()`).
  - Query distinct artists and albums from Room `songs` table.
- **`LibraryViewModel` & `LibraryScreen`:**
  - Tab state: `Tracks`, `Artists`, `Albums`.
  - Dense `SongTableRow` for track lists, compact card/row lists for artists and albums.

### Playlist CRUD Strategy
- **`PlaylistRepositoryImpl` & `PlaylistDao`:**
  - `getAllPlaylistsAsFlow()`, `upsertPlaylist()`, `deletePlaylist()`, `addSongToPlaylist()`, `removeSongFromPlaylist()`.
  - `getSongsForPlaylistAsFlow(playlistId)` returning `Flow<List<Track>>`.
- **`PlaylistViewModel` & `PlaylistScreen` / `PlaylistDetailScreen`:**
  - `PlaylistScreen`: displays list of playlists, "Create Playlist" dialog (title + description), swipe/click delete.
  - `PlaylistDetailScreen`: displays songs inside selected playlist, play all / play track, remove track from playlist.

## 3. UI Design Specifications
- Monospaced JetBrains Mono typography.
- Dark theme (`#0D0D0D` background, `#151515` surface, `#1DB954` accent).
- 48dp dense song table rows (`SongTableRow`).
- Navigation integration via `NavDestination.Queue`, `NavDestination.Library`, `NavDestination.Playlists`.

## 4. Quality Gate Targets
- 0 compile errors (`assembleDebug`).
- 0 Android Lint errors.
- 0 Detekt critical issues.
- 0 ktlint formatting errors.
- 100% passing unit tests in `testDebugUnitTest`.
