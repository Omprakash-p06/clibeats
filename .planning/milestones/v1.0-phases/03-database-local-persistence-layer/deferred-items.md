# Deferred Items — Phase 03

Out-of-scope discoveries logged during plan execution. These are **not** fixed here because they originate from earlier waves/plans; they are tracked so a future wave or the audit can address them.

## Open

### D-02 (recommendation): detekt 1.23.6 vs ktlint_official incompatibility & over-broad ForbiddenImport

- **Found during:** Plan 03-04, Task 4 quality gate (first-ever detekt run on the Phase 3 data layer).
- **Issues:** `detekt` exited 1 with 131 issues — 94 `Indentation` (detekt 1.23.6 misparses the ktlint_official `@Inject` on-separate-line constructor style the project mandates; false positives), 31 `ForbiddenImport` (the Phase 0 pattern `com.clibeats.data.*` flags legitimate data-layer self-imports in addition to the presentation boundary it protects), 4 `MaxLineLength` + 2 `MaximumLineLength` + 2 `Wrapping` (detekt's 120-char default vs ktlint_official's relaxed line-length tolerance).
- **Resolution applied (03-04):** targeted `@Suppress` annotations with justification comments, per plan Task 4 prescription — `@file:Suppress` for import-level violations, class-level for style rules. `detekt` now exits 0.
- **Recommendation for a future wave (NOT done here):** consider refining `config/detekt/detekt.yml` — disable the duplicate `Indentation` rule (ktlint is the formatting authority) and replace the over-broad `ForbiddenImport` pattern with an explicit-import list (e.g. `com.clibeats.presentation.*` imports of data classes) so future data-layer files don't each need suppressions.

## Resolved

### D-01: ktlint violations in wave-2 files (Plan 03-02 deliverables) — RESOLVED 2026-08-05 (Plan 03-04, Task 4)

- **Found during:** Plan 03-03, final `ktlintCheck` (after `ktlintFormat` on plan-03-03 files).
- **Files:**
  - `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt` (lines 30-32: "Expected a blank line for this declaration" — abstract DAO accessors not separated by blank lines)
  - `app/src/main/java/com/clibeats/data/local/CliBeatsTypeConverters.kt` (line 6: "Class body should not start with blank line"; line 11: "First line of body expression fits on same line as function signature")
  - `app/src/main/java/com/clibeats/di/DatabaseModule.kt` (line 1: "File must end with a newline"; line 16: "Class body should not start with blank line")
- **Why deferred:** These files were committed by Plan 03-02 with content-grep acceptance (no build/lint gate existed then — the build was intentionally deferred to 03-03). They are pre-existing violations unrelated to Plan 03-03's changes; per the executor scope boundary they are not auto-fixed here.
- **Resolution:** Fixed in Plan 03-04 Task 4 via `.\gradlew.bat ktlintFormat` (formatting only: blank lines between DAO accessors, class-body leading blank line removed, expression-body joined, EOF newline added). Committed as part of `6e2d0e4`. WINDOWS.md ledger entry #1 marked fixed. `ktlintCheck` exits 0 on the whole project.
