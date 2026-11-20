# CLIBeats — User Guide

Welcome to CLIBeats, the terminal-styled music player for Android.

## First run

New installs open a short setup: pick a theme, optionally choose your music folder, and choose the default
source (Online or Local). Everything can be changed later in Settings. Reinstalling? The music step offers
**RESTORE FROM A BACKUP FILE**: pick `clibeats-backup.json` from `Download/CLIBeats`.

## Tabs

Swipe left / right anywhere outside a song row to move between tabs.

| Tab | What it does |
|---|---|
| **Home** | Source switch (`ONLINE ▾` / `LOCAL`), Recently Played (last 5, with `[ CLEAR ]`), trending songs. Tap the tab again to switch Online ↔ Local. |
| **Search** | Search the active source. With the box empty it lists songs you played from earlier searches (`[ CLEAR ]`). Tap the tab again to switch Online ↔ Local; double-tap it to jump into the search box. |
| **Library** | `[ ONLINE ]` = songs you've played, `[ OFFLINE ]` = music on your device (with the track count). Tap the tab again to switch. Tracks / Artists / Albums / **Playlists**. |
| **Settings** | Theme & accent, music folders, playback, provider, storage, playlist exchange, about. |

**Playlists** (Library → Playlists): ♥ Favorites, Most Played (✕ removes a song,
`[ CLEAR ALL ]` asks first), your playlists, `[ + NEW ]` and local M3U playlists. Drag a song's ≡ handle to
reorder ♥ Favorites or your own playlists.

**Queue** (≡ in the top bar, the mini player, or `[ QUEUE ]` in Now Playing): the playing song, then Up Next. Hold
a song (or drag its ≡ handle) and move it to reorder; remove, shuffle or clear upcoming songs; tap one to jump to it.

## Song gestures

- **Tap** — play (with "Auto-queue related songs" on in Settings → Playback, the rest of the list is queued after it; off by default).
- **Swipe right** — add to queue.
- **Swipe left** — add to ♥ Favorites.
- **Long-press** — add to a playlist (or create a new one).

## Player

- **Mini player** — tap the song to open Now Playing.
- **Now Playing** — a frosted-glass background made from the album art, with matching control colours. The waveform
  behind the controls shows the song's real loudness (local songs within seconds, streamed songs as they play; dim
  bars aren't measured yet). Drag across it to scrub: playback keeps going, and dragging back near the current time
  snaps onto it. Repeat (off → all → one), previous, play/pause,
  next, shuffle, sleep timer,
  queue, add to playlist, ♥. Drag the artwork down to close.
- **Previous** — more than 3 seconds into a song it restarts it; press again to play the song that played
  before, local or online, even from an earlier queue (also from the notification, lock screen and headphones).
- **Sleep timer** — 15 / 30 / 45 / 60 minutes or end of the current song (works with repeat-one too).
- If a song can't be played, CLIBeats skips to the next one and shows a short notice above the player.

## Local music

Settings → Local music → add one or more folders. Titles, artists, albums and covers come from the file tags.
Use **Rescan** after adding files. Playlist files (`.m3u`, `.m3u8`, `.pls`) in your folders appear as
read-only `[LOCAL]` playlists; use **Save as app playlist** to get an editable copy.

## Storage

Settings → Storage has two caches, each with a size limit and `[ CLEAR ]`:

- **Songs** — streamed songs replay instantly without using data (local files are never copied).
- **Artwork** — cover images. Changing the limit applies immediately.

## Controls outside the app

Playback continues in the background with notification and lock-screen controls. After the app is closed,
the media card in Quick Settings resumes the last song from where you left off.

## Updates

When a new version is released, a pop-up offers **DOWNLOAD** or **LATER** (hides it for a day). The APK is
saved to your Downloads folder and checked against the release's checksum; then tap **INSTALL** and confirm
Android's update screen. The first time, Android asks you to allow installs from CLIBeats.

Settings → Updates sets how often the app checks (**Daily**, **Weekly** or **Off**) and has **CHECK NOW** to
check, download and install by hand. The downloaded APK is deleted after the update is installed.

## Playlist exchange

Settings → Playlist exchange exports your playlists and Favorites to a `clibeats.json` file anywhere you
choose, and imports them back (for example on a new phone or after reinstalling).

## Backup

CLIBeats saves a backup of your library once a day: playlists, Favorites, saved albums and artists, and
settings. On Android 10+ a copy goes to `Download/CLIBeats/clibeats-backup.json`, which stays after an
uninstall. Settings → **SHARE BACKUP** sends it to Drive, email or another phone. Android's own backup
(Google account) also brings the library back when you set up a new phone.
