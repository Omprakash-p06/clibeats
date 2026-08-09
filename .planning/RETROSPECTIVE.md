# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — Engineering Foundation & Core TUI Client

**Shipped:** 2026-08-10
**Phases:** 12 | **Plans:** 44 | **Commits:** 145 (Aug 4 – Aug 9)

### What Was Built
- Production Android TUI music client: monochrome Material3 theme, JetBrains Mono typography, dense song tables, persistent player bar, 6 Paparazzi golden baselines.
- Full data layer: Room v1 (5 entities, 4 DAOs, schema export), repository pattern, Keystore-backed EncryptedSharedPreferences (AES256_GCM) with backup exclusions.
- Media3 background playback (PlaybackService + PlayerAdapter + StateFlow), queue persistence, library and playlist CRUD, offline cache/download layer with network fallback.
- Provider architecture evolved from direct InnerTube client to the CliBeats Gateway (Fastify + Redis, ADR-012–ADR-020) with search, streaming, auth/session, canonical models, and API versioning.
- Quality culture: Detekt + ktlint + Android Lint + GitHub Actions CI, 109 unit tests green, telemetry with PII redaction, release signing + R8 + license audit + release docs.

### What Worked
- Quality-gate-first engineering (DoD enforced from Phase 0) kept every phase landing with zero lint/static-analysis regressions — test count climbed monotonically 27 → 40 → 84 → 93 → 96 → 100 → 106 → 108 → 109 with 0 failures throughout.
- Phase execution in small plans (2–5 per phase) with per-plan commits made the milestone auditable end-to-end.
- Gateway migration (ADR-012+) cleanly replaced the fragile InnerTube client while preserving the provider abstraction and search UX.

### What Was Inefficient
- STATE.md was not kept current after Phase 3 — the completion matrix and frontmatter drifted from disk reality for 9 phases. Closing the milestone required back-filling verification artifacts (0, 1, 5) and refreshing stale ones (9, 10, 11).
- Missing VERIFICATION.md for phases 0/1/5 delayed a clean `verified_closeout`; these should have been produced by the phase execute/verify step at the time.
- Debug sessions (recovery-01/02/06, yt-po-token-investigation) were never formally closed, and phase-02 UAT has 4 pending scenarios — paperwork lag behind real progress.

### Patterns Established
- Per-phase verification reports (`XX-VERIFICATION.md`) with frontmatter `status: passed` are the canonical readiness signal; they must be refreshed whenever summary files land afterward (staleness is mtime-based).
- `gsd-tools query progress` / `init.manager` are the source of truth for phase completion — STATE.md matrix should be regenerated from them, not hand-maintained.

### Key Lessons
1. Keep STATE.md updated at each phase close (or regenerate from `init.manager`) — stale state silently blocks milestone closeout.
2. Generate VERIFICATION.md during the execute/verify step, not later; back-filling is more work than doing it in the flow.
3. Run the milestone pre-close audit (`audit-open`) early so debug sessions and UAT scenarios get closed as they finish, not en masse at closeout.

### Cost Observations
- Timeline: 6 days (2026-08-04 → 2026-08-09), 145 commits, 29 `feat(` commits, ~7,700 Kotlin LOC in `app/src`.
- Sessions: multiple per phase; execution was wave-parallelized within phases.
- Notable: verification/paperwork lag (STATE.md, VERIFICATION.md, UAT, debug sessions) was the main closeout bottleneck, not implementation.

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | ~12 | 12 | Engineering-first GSD with per-phase quality gates; gateway provider architecture |

### Cumulative Quality

| Milestone | Tests | Zero-Dep Additions | Release |
|-----------|-------|--------------------|---------|
| v1.0 | 109 unit tests, 0 failures | 0 (all from version catalog) | v1.0 APK with signing, R8, license audit |

### Top Lessons (Verified Across Milestones)

1. Quality gates from Phase 0 prevent regression accumulation — keep them.
2. Planning/state artifacts must be maintained in the same commit as the code they describe.
