# PORTABLE_LIBRARY_SPEC.md — Portable Library Format & Sync Specification

> **Milestone:** ARCHITECTURE-ROADMAP-01  
> **Status:** Specification Only — No Code Changes  
> **Date:** 2026-08-09

---

## Overview & Vision

CliBeats is local-first. User data must never be trapped in proprietary cloud databases or lost when upgrading devices.  
The **Portable Library Archive (`.clibeats`)** allows complete backup, restore, partial import, and device-to-device migration in seconds without requiring a server or account.

---

## File Format

A `.clibeats` file is an unencrypted (or optionally password-encrypted) **ZIP archive** containing standardized JSON manifests and optional media binary assets.

```
export_20260809_120000.clibeats (ZIP Archive)
├── manifest.json              ← Global archive metadata, version, checksums
├── library/
│   ├── liked_songs.json       ← Saved/liked tracks
│   ├── playlists.json         ← Custom playlists and track cross-references
│   ├── history.json           ← Playback history logs
│   └── queue.json             ← Saved queue state
├── preferences/
│   ├── settings.json          ← App settings (cache limits, theme, audio quality)
│   └── providers.json         ← Custom provider URLs & priorities
└── assets/                    ← Optional binary artwork cache
    ├── artwork_3f8a92.jpg
    └── artwork_9c1b4e.png
```

---

## 1. Schema Specifications

### `manifest.json`
```json
{
  "$schema": "https://clibeats.app/schemas/v1/manifest.json",
  "formatVersion": 1,
  "appVersion": "1.0.0",
  "createdAtEpochMs": 1786252800000,
  "exportedBy": "CliBeats Android v1.0.0",
  "deviceInfo": "Pixel 8 Pro (Android 14)",
  "contents": {
    "likedSongsCount": 142,
    "playlistsCount": 8,
    "historyEntriesCount": 1050,
    "hasSettings": true,
    "hasProviderPreferences": true,
    "includesArtworkAssets": false
  },
  "checksums": {
    "library/liked_songs.json": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "library/playlists.json": "f4015822b3b0d2d3851b9e2468f710f135b1c099351a021008e5c8e2a1b9f56e",
    "preferences/settings.json": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
  }
}
```

### `library/liked_songs.json`
```json
{
  "version": 1,
  "songs": [
    {
      "id": "rj5wZqReXQE",
      "providerId": "youtube",
      "title": "Wonderwall",
      "artist": "Oasis",
      "album": "(What's the Story) Morning Glory?",
      "durationSeconds": 259,
      "artworkUrl": "https://yt3.googleusercontent.com/...",
      "addedAtEpochMs": 1786200000000,
      "playCount": 42,
      "lastPlayedEpochMs": 1786240000000
    }
  ]
}
```

### `library/playlists.json`
```json
{
  "version": 1,
  "playlists": [
    {
      "id": "pl_rock_classics_9f2a",
      "name": "Rock Classics",
      "createdAtEpochMs": 1785000000000,
      "updatedAtEpochMs": 1786200000000,
      "description": "Essential rock tracks",
      "tracks": [
        {
          "trackId": "rj5wZqReXQE",
          "providerId": "youtube",
          "position": 0,
          "addedAtEpochMs": 1785000100000
        }
      ]
    }
  ]
}
```

---

## 2. Backward & Forward Compatibility

1. **Format Versioning:** `formatVersion` in `manifest.json` tracks structural breaking changes.
2. **Backward Compatibility:** Future CliBeats v2.x clients MUST support importing `formatVersion: 1`. Missing fields assume default values.
3. **Forward Compatibility:** Older CliBeats clients encountering unknown JSON fields MUST ignore unparsed properties (using `ignoreUnknownKeys = true` in `kotlinx.serialization`).

---

## 3. Conflict Resolution & Merge Strategies

When importing a `.clibeats` archive into an existing installation, the user can choose from three distinct strategies:

| Strategy | Behavior |
|---|---|
| **Merge & Keep Newest (Default)** | Deduplicates by `(providerId, trackId)`. Updates item if imported `addedAtEpochMs` / `updatedAtEpochMs` is newer. Preserves existing items. |
| **Overwrite All** | Wipes current Room tables (playlists, liked songs, history) and performs a clean load from archive. |
| **Skip Existing** | Only imports tracks/playlists whose IDs do not already exist in the database. Never overwrites local modifications. |

---

## 4. Incremental Sync & Partial Import

Users can selectively uncheck sections during the import flow:
- `[x] Playlists & Liked Songs`
- `[ ] Settings & Preferences`
- `[ ] Playback History`
- `[ ] Cached Artwork Files`

---

## 5. Security & Checksum Verification

1. **Integrity Validation:** Before applying database transactions, CliBeats computes the SHA-256 hash of each JSON file in the archive and matches it against `manifest.json`.
2. **Corrupt Archive Guard:** If any SHA-256 fails, import halts immediately without modifying local Room state.
3. **Sanitization:** String fields (title, artist, playlist name) are sanitized against SQL injection/malformed characters before Room insertion.
