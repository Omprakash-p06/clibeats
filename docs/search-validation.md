# CliBeats Phase 4 Search Pipeline Validation Report

This document records empirical verification of the YouTube Music search pipeline via the Fastify Provider Gateway (`YouTubeProviderAdapter`).

---

## 1. Verified Test Queries

| Query | Status | Track Count | Top Track Result | ID | Duration |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `Wonderwall` | **PASS (200 OK)** | 20 tracks | Wonderwall by Oasis | `rj5wZqReXQE` | 4:19 (259s) |
| `Believer` | **PASS (200 OK)** | 20 tracks | Believer by Imagine Dragons | `Kx7B-XvmFtE` | 3:25 (205s) |
| `Heat Waves` | **PASS (200 OK)** | 20 tracks | Heat Waves by Glass Animals | `XDjB9E3YtUE` | 3:59 (239s) |
| `Tum Hi Ho` | **PASS (200 OK)** | 20 tracks | Tum Hi Ho by Arijit Singh | `fsiPzT50ZiM` | 4:22 (262s) |

---

## 2. Sample Canonical Track JSON Payload

```json
{
  "id": "rj5wZqReXQE",
  "providerId": "youtube",
  "title": "Wonderwall",
  "artist": "Oasis",
  "album": "(What's The Story) Morning Glory? (Remastered)",
  "durationSeconds": 259,
  "artworkUrl": "https://yt3.googleusercontent.com/FoVQFdW6zBi3sNA_yZJSV3VTWmi0belhhFzleuEbn27utkirstj1woXHfWmWqkNyHla37ZFbk_F6jvVV=w120-h120-l90-rj"
}
```

---

## 3. Core Root Cause & Fix Summary

- **Root Cause**: Gateway `config/gateway.yaml` assigned `mock` provider `priority: 100` and `youtube` provider `priority: 60`. `ProviderSelectionEngine` sorted providers by priority descending, so `MockProviderAdapter` was selected for every search query and returned `[]`.
- **Fix**: Reordered priority in `gateway/config/gateway.yaml` (`youtube` = 100, `mock` = 10). Also updated `YouTubeProviderAdapter.ts` to query `yt.music.search(query, { type: 'song' })` and parse `MusicShelf` contents directly.
