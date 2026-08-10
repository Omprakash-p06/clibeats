---
id: DEBUG-2026-08-10-no-audio-audius-catalog
title: "No audible audio while playback appears to stream + pop songs 'not available' in Audius"
status: investigating
trigger: |
  User report: "although it looks like the song is being streamed, i don't hear
  any audio at all, why is that? also all the songs, indian and western pop are
  not availabe in Audius."
created: 2026-08-10
updated: 2026-08-10
---

# Debug Session: Silent playback + Audius catalog gaps

Continuation of the open follow-up in `2026-08-10-home-empty-and-streaming.md`
("user reported streaming still fails in their environment").

## Symptoms

1. Player bar shows the track as playing (Pause state, progress advancing) but
   **zero audible audio** on the host.
2. Searching Indian / Western pop returns few or no relevant songs — the
   requested mainstream tracks "are not available in Audius".

## Evidence (collected 2026-08-10, emulator-5554 + live Audius API)

### Audio path (emulator)

- `adb devices` → `emulator-5554` (Android 14) running; `com.clibeats` installed
  and running (pid 9210).
- **Host emulator cmdline (wmic):**
  `C:\Android\Sdk\emulator\emulator.exe -avd recovery06 -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-audio`
  ⇒ **the emulator was launched with `-no-audio`** — guest audio is rendered to
  a null host sink. No sound can ever reach the host speakers, regardless of app.
- Guest audio subsystem is otherwise healthy: `media.audio_flinger` shows the
  app's `AudioTrack` (uid 10197, `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC`, 48 kHz
  stereo) created → started → device updated to `0x2 (AUDIO_DEVICE_OUT_SPEAKER)`.
- Bluetooth: state ON but `ConnectionState: STATE_DISCONNECTED`, zero bonded
  devices. The `ERROR(7) "Bluetooth audio disconnected"` MediaSession states in
  `dumpsys media_session` belong to other packages (not com.clibeats).
- UI: `PlayerBar` shows "Pause <track>" only when `player.isPlaying == true`
  (ExoPlayer), with progress from `positionMs`. So "looks like it's streaming"
  ⇒ real audio bytes were being decoded and written to AudioTrack ⇒ the only
  missing link is host output, i.e. `-no-audio`.

### Stream URLs (live API, both community and official hosts)

- Search / trending / getTrack responses embed `stream.url` (signed
  `cidstream` URLs on gateway domains: figment.io, staked.cloud,
  open-audio-validator.com, monophonic.digital, zeogrid.com, theblueprint.xyz,
  creatornode.audius.co, …).
- **10/10 sampled embedded URLs → HTTP 401** `{"detail":"invalid character '\"'
  after top-level value","error":"invalid signature"}` — independent of
  HEAD/GET, Range, User-Agent, and of discovery host
  (`discoveryprovider.audius.co` AND official `api.audius.co`).
  Appending `&skip_play_count=true` does not help.
- **`GET /v1/tracks/{id}/stream?app_name=clibeats` → HTTP 302** to a freshly
  re-signed gateway URL → following with `Range: bytes=0-…` → **HTTP 206
  `audio/mpeg`, real MP3 bytes** (LAME/FFmpeg/TENC frames). Verified for
  tracks `8Y9Al`, `1106557`, `2004310021`, `923232` (4/4).
- Conclusion: the embedded `stream.url` handed out by the Audius discovery API
  is currently rejected by the gateways (server-side signature/encoding
  regression). The dedicated stream endpoint re-signs correctly and works.

### Catalog (Indian / Western pop)

- Queries `indian pop`, `western pop`, `pop`, `bollywood`, `shape of you`,
  `arijit singh`, `taylor swift`, `ed sheeran`, `despacito` all return results
  from the API (20/10 each), all `is_streamable: true` with embedded streams.
- BUT the results are community covers / remixes / mashups / re-uploads
  ("Tum Hi Ho (fingerstyle cover)", "Taylor Swift - Opalite (Remix)",
  "Perfect - Cover (Violin)") — **Audius is a decentralized catalog of
  independent artists; it does not license major-label music**, so the original
  mainstream recordings the user wants are genuinely absent. This is a catalog
  limitation, not an app bug.
- Relevance is also loose ("indian pop" → first hit is a Powfu track tagged
  "popeisarockstar"). The app's `SearchResultsList` renders an empty result as
  just the table header with no "no results" message.

## Root Cause

1. **"No audio at all":** The emulator `recovery06` runs with `-no-audio`, so
   the Android guest's audio output never reaches the host. Playback in-app is
   real (stream → decode → AudioTrack → SPEAKER), which is why the UI shows a
   playing track with advancing progress. This is an environment issue, not an
   app defect.
2. **"Streaming doesn't actually produce audio data for many tracks":** the
   Audius discovery API currently embeds broken signed stream URLs (HTTP 401
   "invalid signature" on every sampled gateway). Tracks whose URLs 401 never
   start playing (ExoPlayer errors before READY). The dedicated
   `/v1/tracks/{id}/stream` endpoint works (302 → 206 audio/mpeg) and is the
   reliable path. The app (`AudiusMusicProvider.search/trending/stream` +
   `PlayerAdapter.toMediaItem` + `TrackDownloadManager`) trusts the embedded
   URL only.
3. **"Songs not available":** Audius has no mainstream/major-label catalog —
   Indian and Western pop originals (and most requested pop songs) are not
   there; only covers/remixes/independent artists. Fundamental to the provider.

## Candidate Fixes

- **F1 (app, stream resolution):** resolve playback/download streams via
  `GET /v1/tracks/{id}/stream?app_name=clibeats` (302-follow) instead of the
  embedded `stream.url` — at mapping time (construct URL from track id, no
  extra call) and/or in `MusicProvider.stream(trackId)`. Also fix
  `TrackDownloadManager` which uses `track.streamUrl`.
- **F2 (environment):** relaunch the emulator WITHOUT `-no-audio` (or test on a
  real device) so host audio is wired up.
- **F3 (UX):** add an empty-results message in `SearchResultsList`; optionally
  surface "Audius is an indie catalog — mainstream tracks may be missing".
- **F4 (bigger):** evaluate a second open provider for mainstream music
  (requires research; providers with major-label catalogs need auth/API keys).

## Next Action

Present root-cause report to user; ask whether to apply F1 (+ F2 restart) or
keep this as diagnose-only.
