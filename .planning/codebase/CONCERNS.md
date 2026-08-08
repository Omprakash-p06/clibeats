# Codebase Concerns & Known Risks: CliBeats

## 1. End-to-End Validation & Gateway Runtime Readiness
- **Current State**: Phase 2 migration on Android is complete and unit tests pass cleanly (`BUILD SUCCESSFUL`).
- **Concern**: Runtime validation on a physical device connected to a live Fastify Gateway instance (`READY FOR END-TO-END VALIDATION`) must verify real stream URL playback with Media3 ExoPlayer.

---

## 2. Upstream YouTube / Provider Health
- **Concern**: Changes to YouTube's web interface or InnerTube APIs can impact `youtubei.js` server-side extraction.
- **Mitigation**: Caching stream URLs in Redis, circuit breaking, and server-side gateway updates allow fixing provider logic without pushing app store updates for the Android app.

---

## 3. Local Persistence Security & Migration Integrity
- **Room Database**: Room version 1 schema is committed in `app/schemas/`. Any future table or column changes require written migration paths.
- **Encrypted Preference Storage**: `AUTH_TOKEN` is persisted using `EncryptedSharedPreferences` backed by Android Keystore (`AES256_GCM`). Devices without hardware-backed Keystore fall back safely to software KeyStore without crashing.