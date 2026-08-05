---
schema_version: 1
open_count: 2
waived_count: 0
fixed_count: 1
total_count: 3
last_updated: 2026-08-05T08:50:52.056Z
---

# Broken Windows Ledger

> Cross-phase defect register. `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 3 | lint-warning | app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt | 30 | Pre-existing ktlint violations in 03-02 files (CliBeatsDatabase.kt 30-32, CliBeatsTypeConverters.kt 6/11, DatabaseModule.kt 1/16) - deferred from 03-03 per scope boundary; see deferred-items.md D-01 | fixed |  | 2026-08-05T08:27:24.613Z | 2026-08-05T08:50:51.185Z |
| 2 | 3 | unrun-verify | app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt |  | connectedDebugAndroidTest instrumented DAO tests not run - no emulator/device in environment; files compile (compileDebugAndroidTestKotlin passed) | open |  | 2026-08-05T08:50:51.648Z |  |
| 3 | 3 | deviation | app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt |  | detekt @Suppress additions on 13 Phase-3 data-layer files (Indentation/ForbiddenImport/MaxLineLength) - first detekt run on data layer surfaced 131 issues from over-broad Phase 0 config and detekt 1.23.6 vs ktlint_official mismatch; see 03-04-SUMMARY.md | open |  | 2026-08-05T08:50:52.056Z |  |

````json
[
  {
    "id": 1,
    "kind": "lint-warning",
    "phase": "3",
    "file": "app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt",
    "line": 30,
    "description": "Pre-existing ktlint violations in 03-02 files (CliBeatsDatabase.kt 30-32, CliBeatsTypeConverters.kt 6/11, DatabaseModule.kt 1/16) - deferred from 03-03 per scope boundary; see deferred-items.md D-01",
    "status": "fixed",
    "reason": "",
    "recorded_at": "2026-08-05T08:27:24.613Z",
    "resolved_at": "2026-08-05T08:50:51.185Z"
  },
  {
    "id": 2,
    "kind": "unrun-verify",
    "phase": "3",
    "file": "app/src/androidTest/java/com/clibeats/data/local/dao/SongDaoTest.kt",
    "line": null,
    "description": "connectedDebugAndroidTest instrumented DAO tests not run - no emulator/device in environment; files compile (compileDebugAndroidTestKotlin passed)",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-08-05T08:50:51.648Z",
    "resolved_at": null
  },
  {
    "id": 3,
    "kind": "deviation",
    "phase": "3",
    "file": "app/src/main/java/com/clibeats/data/repository/SongRepositoryImpl.kt",
    "line": null,
    "description": "detekt @Suppress additions on 13 Phase-3 data-layer files (Indentation/ForbiddenImport/MaxLineLength) - first detekt run on data layer surfaced 131 issues from over-broad Phase 0 config and detekt 1.23.6 vs ktlint_official mismatch; see 03-04-SUMMARY.md",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-08-05T08:50:52.056Z",
    "resolved_at": null
  }
]
````
