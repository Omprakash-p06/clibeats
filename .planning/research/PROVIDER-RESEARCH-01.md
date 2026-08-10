---
id: PROVIDER-RESEARCH-01
title: Mainstream Music Provider Research
status: COMPLETE (research only — no code changed)
priority: P0
created: 2026-08-10
---

# PROVIDER RESEARCH — FINAL REPORT

Baseline: `f3a2fdd7d79e873eaa1b8ff262746f1235fe487f` (source of truth). Current
worktree == baseline + Audius provider swap + TUI UI restore (uncommitted).
Research-only session; no source code modified.

---

## 1. BASELINE AUDIT (commit f3a2fdd + current worktree)

Pure-Kotlin, single Android module `app`. No backend. No gateway (the
TypeScript/Fastify/Redis gateway architecture was removed in recovery-11).

| Layer | Contents |
|---|---|
| **App** | `CLIBeatsApp` (Hilt), `MainActivity` (Compose, `NavDestination` state switch: Home/Search/Queue/Library/Playlists/More/Settings) |
| **Domain** | `MusicProvider` interface (`providerId`, `displayName`, `search`, `trending`, `getTrack`, `stream`, `playlists`, `queue`); `ProviderResult` (Success/Error/Loading); models `Track`/`Album`/`Artist` (each carries `providerId`), `PlaybackState`, `Playlist`, `RepeatMode` |
| **Data** | Room (`Song`/`Playlist`/`Queue`/`History`/`CacheIndex` DAOs + entities + mappers); `CacheManager` (500 MB LRU file cache); `TrackDownloadManager` (streamUrl → OkHttp → cache); `AppPreferences` (DataStore + SecurePrefs); repositories |
| **Provider** | `AudiusMusicProvider` (search/trending/getTrack/stream; playlists/queue are empty stubs). `ProviderModule` **hard-binds** `MusicProvider` → `AudiusMusicProvider`; Settings "Active Provider" radio is **cosmetic** (persists a pref only, no rebind) |
| **Playback** | `PlayerAdapter` (singleton ExoPlayer; queue mgmt; `MediaItem` from `track.streamUrl` or cached file), `PlaybackService` (MediaSessionService; injects the *same* ExoPlayer and calls `player.release()` in `onDestroy`) |
| **Network** | Retrofit + OkHttp + kotlinx.serialization; base `https://discoveryprovider.audius.co/v1/`; debug body logging |
| **UI** | Home (trending feed), Search (debounced, results list), Queue, Library (local), Playlists, More, Settings; persistent TUI-style `PlayerBar` |

Gaps vs. product goal: no album/artist browsing (models exist, no provider
methods), playlists()/queue() stubbed empty, no favorites UI, History DAO
unused by UI, provider selection not functional, empty search shows only a
bare table header.

**Verified runtime problems (this session, 2026-08-10):**
- Emulator `recovery06` runs with `-no-audio` → no host sound ever (env, not app).
- Audius embedded `stream.url` (search/trending/getTrack) → **HTTP 401
  `{"error":"invalid signature"}` on 10/10 sampled URLs from both community
  `discoveryprovider.audius.co` and official `api.audius.co`**. Server-side
  regression in Audius's signed stream-gateway system.
- `GET /v1/tracks/{id}/stream?app_name=clibeats` → **302 → 206 audio/mpeg, real
  MP3 bytes (verified 4/4 tracks)**. This endpoint re-signs correctly → the
  working stream path.

## 2. PROVIDER RESEARCH (2026 landscape)

### Mainstream majors (all ruled out for free full-track streaming)
- **Spotify** — Web API/Playback SDK: OAuth + dev Premium + user Premium
  mandatory; free tier = 30s previews/blocked. ❌
- **Apple Music** — full tracks require Music User Token (paid sub); only
  30s previews available with a developer token. ❌ (previews only)
- **Deezer** — full streaming API limited to paid subscribers; free = 30s
  previews. ❌
- **TIDAL** — no free tier, no public playback API. ❌
- **Qobuz** — partner-gated API + paid sub. ❌

### Free / open (viable candidates)
- **Audius** — open decentralized catalog; search+trending+streams via REST,
  no account/key (only `app_name`). MP3 ~320 kbps, Range/206, seeking. Catalog
  = independent artists; **no mainstream majors** (covers/remixes only).
  Currently: embedded stream URLs broken server-side; `/tracks/{id}/stream`
  endpoint works. ✅ viable as primary (with fix).
- **Jamendo** — free dev `client_id` (registration); 500k+ CC-licensed tracks;
  API v3.0; MP3 (mp31 96k, mp32 VBR) / OGG / FLAC; no mainstream; REST-clean,
  stable. ✅ viable as secondary. (Live test blocked: my demo key guess was
  rejected — requires a real free registration; docs verified via research.)
- **Internet Archive** — `advancedsearch.php` open, no key; `archive.org/
  download/{id}/{file}` streams with Range (302 → mirrors). BUT mainstream
  hits are mostly **pirate re-uploads** (e.g., "PagalWorld" items) — legally
  unsuitable as a primary mainstream source. ⚠️ niche only (CC/public domain/
  live archives).
- **Free Music Archive** — API permanently shut down; hotlinking forbidden. ❌
- **SoundCloud** — official API gated (paid Artist Pro to register); unofficial
  `client_id` harvesting + `/tracks/{id}/stream` works-ish but tos-violating,
  quota'd, revocable, moving to AAC-HLS. ⚠️ high maintenance risk.
- **Bandcamp** — no public API; scraping with tokenized expiring MP3-128 URLs. ⚠️

### YouTube / YouTube Music (the mainstream hole)
- **Project evidence (this repo, recovery-06/11, 2026-08-08):** innertube
  `/youtubei/v1/player` from fresh IPs → 400/404 (PO-token/bot wall); IOS-signed
  googlevideo URLs enforce `rqh=1` Range cap ≈ 1 MiB → progressive full-track
  playback impossible; the entire gateway architecture (ADR-012..020) was
  abandoned after this.
- **2026 landscape:** yt-dlp / NewPipeExtractor / youtubei.js handle PO tokens
  & visitor data but **break repeatedly** (cipher changes, client-id revocations,
  bot waves); apps like InnerTune/ViMusic/Metrolist (YouTube Music innertube)
  go down in waves; Seal (yt-dlp wrapper) needs constant binary updates.
  Category E/F: technically possible, unstable, ToS-gray.
- **Verdict:** do NOT build the product on YouTube. If ever added: optional,
  experimental, behind a switch, using NewPipeExtractor (maintained) or yt-dlp.

### Reference open-source clients (architecture lessons)
| App | Provider | Stream resolution | Backend | What breaks |
|---|---|---|---|---|
| InnerTune / Metrolist / ViMusic | YTM innertube | cipher/signature deobfuscation | none | PO tokens, client-id revocations, cipher changes |
| Seal | yt-dlp (1,000+ sites) | local yt-dlp binary | none | yt-dlp breaks constantly; needs app updates |
| NewPipe / PipePipe | multi extractor | DOM/JSON scraping | none | layout/signature changes |
| SoundCloud clients | unofficial API | client_id + /stream | none | quota/token revocation |
| (Audius/Jamendo clients) | open REST | direct signed URLs | none | minimal — stable by design |

**Lesson:** single-provider apps die when their provider breaks (YouTube wave).
Multi-provider with a clean `MusicProvider` boundary survives.

## 3. RUNTIME EXPERIMENT RESULTS (live, 2026-08-10)

### Audius coverage — the 8 test tracks (query → top results)
| Track | Result |
|---|---|
| Blinding Lights | remix, "M.studio" re-upload, COVER — no original |
| Shape of You | exact-title track + remix + mashup (re-uploads) |
| Believer | Daydream Believer (wrong), generic "BELIEVER", rap ft — **miss** |
| Wonderwall | cover, mashup, low-quality upload — no original |
| Tum Hi Ho | exact-title + fingerstyle cover (re-uploads) |
| Kesariya | exact-title re-upload (+ noise from "Khesari Lal" pun) |
| Apna Bana Le | PaglaSongs re-upload + full-title re-upload |
| Chaleya | **miss** (sample challenge, generic sounds) |

⇒ Partial presence via covers/re-uploads; no reliable mainstream originals.
Search relevance is loose (title tokens, not artist-verified).

### Stream path (Audius)
- Embedded `stream.url` → **401 invalid signature** (10/10, both hosts).
- `GET /v1/tracks/{id}/stream` → **302 → 206 audio/mpeg** (4/4: 8Y9Al, 1106557,
  2004310021, 923232). MP3 frames verified (LAME/FFmpeg/TENC). ✅

### Internet Archive
- Search works (no key). Streams: `download/{id}/{file}` → 302 → Range-capable
  mirrors (verified 302; Range support documented). Mainstream items are pirate
  re-uploads → not a legal mainstream source.

### Jamendo
- Requires registered free `client_id`; my unauth attempt → `invalid client id`.
  Docs: 500k+ CC tracks, mp31/mp32/FLAC, stable REST. No live playback test
  possible without a key.

## 4. CATALOG COMPARISON

| Provider | Mainstream | Indian | Western | Catalog size | Search | Quality |
|---|---|---|---|---|---|---|
| Audius | ✗ (covers/re-uploads) | ~ (re-uploads) | ~ | 100k+ tracks | ✓ (loose) | MP3 ~320k 48kHz stereo, 206/Range |
| Jamendo | ✗ (indie/CC) | ✗ | ~ (global indie) | 500k+ | ✓ | MP3/OGG/FLAC |
| Internet Archive | ✗ (pirate uploads) | ~ | ~ | huge but messy | ✓ | VBR ~150-200k derived MP3 |
| YouTube (extractor) | ✓ (everything) | ✓ | ✓ | entire YTM | ✓ | opus/mp4a 128-160k (music) — unstable |
| SoundCloud (unofficial) | ~ (major re-uploads) | ~ | ~ | large | ✓ | mp3/aac-HLS — unstable |
| Spotify/Apple/Deezer/TIDAL/Qobuz | ✓ | ✓ | ✓ | entire | ✓ | — but **no free full-track streaming** |

## 5. PLAYBACK COMPARISON

| Provider | Auth/Key | Stream URL | Range/Seek | Stability | Backend |
|---|---|---|---|---|---|
| Audius | none (`app_name`) | signed cidstream (needs `/tracks/{id}/stream` fix) | ✓ 206 | good (current API glitch is server-side, has workaround) | none |
| Jamendo | free client_id | direct file URL | ✓ | excellent | none |
| Internet Archive | none | direct file URL | ✓ | excellent (content quality varies) | none |
| YouTube | none (fragile) | PO-token + cipher; Range caps | ✗ for full tracks (≤1 MiB) | poor (breaks in waves) | none |
| SoundCloud | extracted id | client_id + /stream | ~ | poor (revocable) | none |
| Majors | OAuth+premium | — | — | n/a | n/a |

## 6. LEGAL / ACCESS CLASSIFICATION

- **A. Official streaming API:** none are free full-track.
- **B. Official API w/ user auth:** Spotify (premium), Apple (MUT), Deezer
  (premium), TIDAL (premium), Qobuz (partner), SoundCloud (paid register).
- **C. Catalog API, restricted playback:** Spotify/Apple/Deezer 30s previews.
- **D. Openly licensed:** **Jamendo (CC)** ✅, **Audius (open catalog)** ✅,
  Internet Archive CC/public-domain subset ✅, FMA ✗ (dead).
- **E. Third-party extractor:** yt-dlp, NewPipeExtractor, youtubei.js — ToS-gray.
- **F. Unofficial/private:** SoundCloud client_id harvesting, Qobuz leaked keys,
  JioSaavn/Gaana/Wynk unofficial — fragile + ToS/legal risk.
- **G. Unsuitable:** TIDAL/Qobuz (no free tier), FMA (dead), Bandcamp (scrape-only).

## 7. ARCHITECTURE COMPARISON

| Option | Complexity | Catalog | Reliability | Privacy | Cost | Maintenance |
|---|---|---|---|---|---|---|
| **A. One provider** (today: Audius) | ★ | indie only | good | good | 0 | low |
| **B. Multi-provider, same interface** (Audius + Jamendo + local) | ★★ | broader indie | good (failover) | good | 0 | low |
| **C. Catalog/playback separation** (mainstream catalog + resolver) | ★★★★ | best if YT used | poor (YT fragile) | mixed | 0 | high |
| **D. Backend gateway** (tried & abandoned in this repo) | ★★★★★ | as upstream | poor | poor | hosting | very high |

**Verdict:** B is the sweet spot for the product goal (free/ad-free/open, no
backend). C (mainstream) is only achievable via YouTube-style extraction and is
not sustainable as the primary architecture; keep the `MusicProvider` boundary
so a future experimental YouTube provider can slot in behind a toggle.

## 8. RECOMMENDED PROVIDER

- **PRIMARY PROVIDER:** **Audius** — it already works end-to-end (search,
  trending, artwork, MP3 320k/48k stereo, 206 seeking). Fix the current stream
  break by resolving streams via `GET /v1/tracks/{id}/stream` (302→206) instead
  of the broken embedded `stream.url`.
- **SECONDARY PROVIDER:** **Jamendo** — free `client_id`, 500k+ CC tracks,
  stable REST, adds catalog breadth + quality tiers (mp32/FLAC). Broadens the
  "not available" problem meaningfully for non-mainstream listeners.
- **FALLBACK / TERTIARY:** **Local device media** (already in Settings) — always
  available, offline, zero network.
- **NOT chosen:** YouTube (unstable/ToS), SoundCloud unofficial (revocable),
  majors (no free streaming), FMA (dead), Bandcamp (scrape-only).

## 9. RECOMMENDED ARCHITECTURE

**Option B — multi-provider behind the existing `MusicProvider` interface,
with provider-owned stream resolution.**

```
Settings: activeProviderId (preference, now honored)
   ↓
ProviderRegistry (list of MusicProvider; selection by id)
   ├── AudiusMusicProvider   (primary — stream via /tracks/{id}/stream)
   ├── JamendoMusicProvider  (secondary — direct file URLs)
   └── LocalProvider         (local device media)
   ↓
Track { id, title, artist, album, durationMs, artworkUrl,
        streamUrl, providerId }   ← already provider-scoped ✓
   ↓
PlayerAdapter → Media3/ExoPlayer (unchanged; cache-first MediaItem)
```

Key rules:
1. **Provider owns its stream strategy** — `stream(trackId)` per provider;
   `Track.streamUrl` stays populated at search time as a fast path but each
   provider decides how (Audius: `/tracks/{id}/stream` URL string, no extra
   call; Jamendo: direct file URL).
2. **ID collision safety** — prefix provider ids (`audius:<id>`,
   `jamendo:<id>`) for queue/library keying.
3. **Settings becomes functional** — ProviderModule binds `ProviderRegistry`;
   Home/Search read `activeProviderId` (AppPreferences) and call the right
   provider. Default: audius.
4. **Library portability (Phase 7)** — persist `providerId` + `sourceId` +
   `sourceUrl` + `durationMs` + `artworkUrl` in Room (already mostly present in
   `Track`/`SongEntity`). Export/import as `clibeats.json` (no server).
   On import: resolve via current providers, fallback = keep metadata + mark
   "unavailable".
5. **No backend. Ever** (gateway architecture was abandoned for good reason).

## 10. IMPLEMENTATION PLAN (executable by a fresh session)

1. **Fix Audius streaming (P0):**
   - Expose `AUDIUS_BASE_URL` (internal) from `NetworkModule`.
   - In `AudiusMusicProvider.search/trending/getTrack`: set
     `streamUrl = "$AUDIUS_BASE_URL/tracks/${track.id}/stream?app_name=clibeats"`
     (302→206 verified); keep embedded `stream.url` only as comment/fallback.
   - Update `stream(trackId)` to return the same endpoint URL.
   - `TrackDownloadManager` then works unchanged (it uses `track.streamUrl`).
   - Add/update provider unit tests (URL shape; mapper keeps artwork/duration).
   - Verify on emulator (with audio enabled — see #6).
2. **Make provider selection real:** `ProviderRegistry` (ordered list from Hilt
   `@IntoSet` or manual map), `ProviderModule` binds it; Home/Search/Home VMs
   inject registry + `AppPreferences.activeProviderId`; Settings radio actually
   switches the active provider for next search/trending.
3. **Add JamendoMusicProvider (P1):**
   - `JamendoApi` (Retrofit): `GET /v3.0/tracks/?client_id=…&format=json&
     audioformat=mp32&search=…`, `GET /v3.0/tracks/{id}`; `tracks/trending` →
     `tracks/?order=popularity_week`.
   - Map `audiodownload`/`audiodownload_allowed` → `streamUrl`,
     `image` (album art), `duration`, `name`, `artist_name`.
   - `client_id` via `BuildConfig` gradle property `JAMENDO_CLIENT_ID`
     (fail-fast if missing, same pattern as the old `GATEWAY_URL` guard).
4. **UX:** empty-search "no results" message + provider name shown on
   Home/Search; "Audius/Jamendo are indie catalogs — mainstream hits may be
   missing" hint in Search idle state.
5. **Library portability:** ensure `SongEntity` persists `providerId`,
   `sourceUrl`; add export/import (`clibeats.json`) actions in Settings.
6. **Emulator audio:** relaunch `recovery06` **without `-no-audio`** so host
   sound is audible during all verification.
7. **Optional experimental YouTube provider (P2, OFF by default):** behind a
   Settings toggle, using NewPipeExtractor (or yt-dlp) for search+stream;
   clearly labeled experimental (ToS risk, breaks periodically). Do not make it
   the primary path.
8. **Test gates:** unit tests per provider + registry; run
   `./gradlew testDebugUnitTest ktlintCheck detekt lintDebug` (existing gates,
   all currently green) after each step.

---
*Research evidence collected live on 2026-08-10 (emulator-5554 + direct API
calls). No source code was modified in this session.*
