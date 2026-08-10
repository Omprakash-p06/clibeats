---
id: DEBUG-2026-08-10-home
title: "Home screen empty + reported streaming failure (post-recovery Audius provider)"
status: resolved
trigger: |
  User report after the pure-Kotlin recovery (commit 58c1d45): "the app isn't
  listing any songs nor does the song streaming work."
created: 2026-08-10
updated: 2026-08-10
---

# Debug Session: Home empty + streaming report

## Symptoms

- Home tab (default landing) showed only the label "Home" — no songs listed anywhere on launch.
- User reported song streaming "still fails" even after reinstall.

## Evidence (runtime, emulator-5554, current build)

- Search: HTTP 200 from `https://discoveryprovider.audius.co/v1/tracks/search?query=lofi&limit=20&app_name=clibeats` via the app's OkHttp (BODY logs). Full track list rendered (titles, artists, durations, artwork). ✅ works.
- Streaming: tap on a result → audio focus granted → `c2.android.mp3.decoder` initialized → AudioTrack active (48 kHz / stereo) → player bar shows "Pause <track>". ✅ works.
- Stream URLs (search-embedded AND track-endpoint) return 206 audio/mpeg with Range requests; UA-agnostic (okhttp/ExoPlayer/Dalvik all accepted). A 403 only occurred for `Python-urllib` bot UA (irrelevant to the app).
- `tracks/trending` endpoint verified: returns tracks with embedded `stream.url`. ✅
- Unit tests (112) + ktlint + detekt + lint all green after fix.

## Root Cause

1. **"Not listing any songs"** — the recovered baseline's `MainActivity` had no Home screen implementation; `NavDestination.Home` fell through to a bare `Text(selectedDestination.label)`. The library tab only lists local songs (empty on fresh install). So on launch the app showed no music. NOT a data-path bug.
2. **Streaming report** — could not be reproduced on the emulator (playback verified repeatedly). The likeliest causes are (a) the user could not reach any songs (empty Home), or (b) Audius content-node flakiness for specific tracks. `stream.mirrors` are returned by the API but not used for fallback — see follow-up note.

## Fix

Restored the TUI UI from commit `f3a2fdd7d79e873eaa1b8ff262746f1235fe487f` and adapted it to the Audius provider:

- Added `presentation/home/` — `HomeScreen` (spotify-tui style dashboard), `HomeViewModel`, `HomeUiState`.
- Added `MusicProvider.trending()` + `AudiusApi.trendingTracks()` + implementation; Home now lists a live trending feed on launch.
- Added `TuiBlock` component; `SongTableHeader` in `SongTableRow`; PlayerBar re-rendered as a TUI "Playing" panel.
- Restored the theme palette + `CliBeatsBorderActive/Inactive` tokens; updated `CliBeatsColorsTest`.
- Wired `MainActivity` Home branch; ported Library/Search/Settings screens to the TUI style; Settings default provider id → `audius`.
- Added `HomeViewModelTest`; fixed pre-existing ktlint/detekt violations so the quality gates pass.

## Follow-up (open)

- User reported streaming "still fails" in their environment but it works on the emulator. If it persists, capture the exact error (device vs emulator, which track, logcat). Candidate hardening: use Audius `stream.mirrors` as fallback URLs when the primary content node fails.
- Settings "Active Provider" list is cosmetic — `MusicProvider` is hard-bound to Audius in `ProviderModule`.
