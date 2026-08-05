---
schema_version: 1
open_count: 1
waived_count: 0
fixed_count: 0
total_count: 1
last_updated: 2026-08-05T08:27:24.613Z
---

# Broken Windows Ledger

> Cross-phase defect register. `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 3 | lint-warning | app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt | 30 | Pre-existing ktlint violations in 03-02 files (CliBeatsDatabase.kt 30-32, CliBeatsTypeConverters.kt 6/11, DatabaseModule.kt 1/16) - deferred from 03-03 per scope boundary; see deferred-items.md D-01 | open |  | 2026-08-05T08:27:24.613Z |  |

````json
[
  {
    "id": 1,
    "kind": "lint-warning",
    "phase": "3",
    "file": "app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt",
    "line": 30,
    "description": "Pre-existing ktlint violations in 03-02 files (CliBeatsDatabase.kt 30-32, CliBeatsTypeConverters.kt 6/11, DatabaseModule.kt 1/16) - deferred from 03-03 per scope boundary; see deferred-items.md D-01",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-08-05T08:27:24.613Z",
    "resolved_at": null
  }
]
````
