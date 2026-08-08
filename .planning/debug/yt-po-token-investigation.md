# Debug Session: YouTube PO Token Investigation

**Status:** in_progress  
**Priority:** P1  
**Trigger:** The debug endpoint has been deployed. YouTube streaming fails on Render deployment — all clients return LOGIN_REQUIRED, stream endpoint returns 500.  
**Created:** 2026-08-09  

## Symptoms

- `/debug-yt` on Render: ALL client types (MUSIC/WEB_REMIX, ANDROID, ANDROID_VR, WEB, TVHTML5, YTKIDS) return `LOGIN_REQUIRED` status with 0 audio formats. `TV_EMBEDDED` returns "This video is unavailable".
- `/api/v1/stream` on Render: Returns 500 `{"error":{"code":"INTERNAL_ERROR","message":"All provider failover candidates failed for playback: undefined"}}`.
- `/api/v1/search` on Render: Works — returns 20 real YouTube tracks. Search does NOT require PO token.
- `/api/v1/bootstrap` on Render: Works — YouTube provider HEALTHY.
- Local gateway (192.168.0.106:8080, per recovery-06): Streaming worked with ANDROID_VR + Range-safe relay.

## Hypothesis

YouTube's InnerTube API has introduced a PO Token (Proof of Origin) requirement for playback/streaming requests. Fresh sessions created from Render's IP (shared datacenter) are now blocked with `LOGIN_REQUIRED` for ALL InnerTube client types when attempting to resolve stream URLs. The YouTubeProviderAdapter does NOT pass a `po_token` to youtubei.js `getBasicInfo()` or `Innertube.create()`.

youtubei.js v17.2.0 exposes optional `po_token` support:
- `Innertube.create({ po_token: '...' })` — session-level PO token
- `yt.getBasicInfo(videoId, { po_token: '...' })` — per-request PO token

## Investigation Steps

1. [x] Call /debug-yt on Render deployment → all LOGIN_REQUIRED
2. [x] Call /api/v1/stream on Render → 500 all providers failed
3. [x] Verify /api/v1/search and /api/v1/bootstrap work (no PO token needed for search)
4. [ ] Enhance debug-yt to test with PO token + HTTP status + Range >1MiB + 403 detection
5. [ ] Obtain or generate a PO token
6. [ ] Deploy enhanced debug-yt and test with PO token
7. [ ] Apply PO token fix to YouTubeProviderAdapter
8. [ ] Redeploy to Render and verify stream playback end-to-end

## Evidence

- 2026-08-09 — /debug-yt raw response: all clients LOGIN_REQUIRED with 0 formats
- 2026-08-09 — /api/v1/stream returns 500 "All provider failover candidates failed for playback: undefined"
- 2026-08-09 — youtubei.js v17.2.0 confirmed to accept `po_token` option on both `Innertube.create()` and `getBasicInfo()`
