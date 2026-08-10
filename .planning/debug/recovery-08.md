# Debug Session: RECOVERY-08

- **id:** RECOVERY-08
- **title:** Implement On-Device YouTube Music Playback
- **status:** implementing
- **trigger:** User brief (P0 implementation). Investigation (recovery-07) complete; no re-research.
- **baseline:** f3a2fdd7d79e873eaa1b8ff262746f1235fe487f (HEAD is ahead: 58c1d45)
- **objective:** CliBeats plays mainstream music on-device via YouTube Music Innertube + on-device PO-token/BotGuard → stream resolution → Media3. NO backend/gateway.

## Phase 0 — Recon (evidence)

- HEAD (58c1d45) retains full InnerTube stack; worktree has them STAGED-DELETED:
  - `data/provider/YouTubeMusicProvider.kt`, `api/InnerTubeApi.kt`, `api/InnerTubeHeaderInterceptor.kt`,
    `dto/{PlayerRequest,PlayerResponse,SearchRequest,SearchResponse}.kt`, `mapper/TrackMapper.kt`,
    tests: `YouTubeMusicProviderTest.kt`, `InnerTubeHeaderInterceptorTest.kt`, `TrackMapperTest.kt`.
- Worktree other providers (current): InternetArchive, Audius, Jamendo, Local; IA currently PRIMARY via ProviderModule registration order.
- Worktree `MusicProvider` interface gained `trending()` vs HEAD (HEAD provider must be adapted).
- Worktree `NetworkModule.kt` does NOT wire InnerTubeApi; HEAD NetworkModule had `INNERTUBE_BASE_URL = https://music.youtube.com/youtubei/v1/` + InnerTubeHeaderInterceptor.
- Current deps: compose, hilt, room, datastore, security-crypto, media3-1.4.1, okhttp-4.12, retrofit-2.11, kotlinx-serialization-json-1.7.1. No newpipeextractor/webkit dep yet.
- live experiment (recovery-07.md): Innertube WEB_REMIX SEARCH → 200 real results. Player w/o PO-token fails (UNPLAYABLE / LOGIN_REQUIRED / 400 / 404).
- env: SDK at C:\Android\Sdk (local.properties). Emulator `emulator-5554` (Nox) attached via adb. gradle-8.9, AGP? JDK? (verify).
- Known runtime issue: subagent handoff returns EMPTY task_result (proven 4x in recovery-07). Orchestrator drives implementation directly; session-manager spawn skipped to avoid wasted cycles (documented in recovery-07.md).

## Plan (from brief Phases 1-16)

1. Clean provider base — restore InnerTube stack, adapt to current interfaces, YouTubeMusic primary, IA demoted.
2. YouTube search via Innertube WEB_REMIX (real "Attention" → Charlie Puth). streamUrl=null at search.
3. Play-time resolution: StreamResolver + PlayerAdapter lazy resolve (outside UI).
4. Existing extractor logic: use current NewPipe Extractor stable for stream URL deobfuscation where applicable; verify APIs.
5. PO-token/BotGuard via Android WebView (no login, on-device, not logged/persisted).
6. Ordered client fallback strategy (current versions).
7. Stream extraction: playability → streamingData → audio-only → settings quality → signature/n-param → validate URL → Media3.
8. Media3 integration: Track → PlaybackRepository → StreamResolver → MusickProvider.stream → URL → MediaItem → ExoPlayer.
9. Short-lived stream URL cache (providerId+videoId → url+expiry); 403 → invalidate → resolve → retry once.
10. Explicit errors: SEARCH_FAILED, TRACK_UNAVAILABLE, PO_TOKEN_FAILED, PLAYER_REQUEST_FAILED, STREAM_RESOLUTION_FAILED, STREAM_EXPIRED, MEDIA_PLAYBACK_FAILED, BOT_CHECK_FAILED.
11. Structured logging with traceId; never log token/cookies/auth/signed URLs.
12. Fixture-based unit tests (no live YouTube).
13. Real-device/emulator test (no -no-audio) — 6 mandatory tracks, full pass matrix.
14. Screenshots → .planning/evidence/recovery-08/ (real only).
15. Failure rule: fix first failing stage only.
16. Quality gates: testDebugUnitTest, ktlintCheck, detekt, lintDebug, assembleDebug, assembleRelease.

## next_action (current)

Phase 1: restore InnerTube stack from HEAD → adapt to current interfaces → wire NetworkModule/ProviderModule.