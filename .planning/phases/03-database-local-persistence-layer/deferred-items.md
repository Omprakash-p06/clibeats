# Deferred Items — Phase 03

Out-of-scope discoveries logged during plan execution. These are **not** fixed here because they originate from earlier waves/plans; they are tracked so a future wave or the audit can address them.

## Open

### D-01: ktlint violations in wave-2 files (Plan 03-02 deliverables)

- **Found during:** Plan 03-03, final `ktlintCheck` (after `ktlintFormat` on plan-03-03 files).
- **Files:**
  - `app/src/main/java/com/clibeats/data/local/CliBeatsDatabase.kt` (lines 30-32: "Expected a blank line for this declaration" — abstract DAO accessors not separated by blank lines)
  - `app/src/main/java/com/clibeats/data/local/CliBeatsTypeConverters.kt` (line 6: "Class body should not start with blank line"; line 11: "First line of body expression fits on same line as function signature")
  - `app/src/main/java/com/clibeats/di/DatabaseModule.kt` (line 1: "File must end with a newline"; line 16: "Class body should not start with blank line")
- **Why deferred:** These files were committed by Plan 03-02 with content-grep acceptance (no build/lint gate existed then — the build was intentionally deferred to 03-03). They are pre-existing violations unrelated to Plan 03-03's changes; per the executor scope boundary they are not auto-fixed here.
- **Resolution:** Run `.\gradlew.bat ktlintFormat` (or manual edits) and commit as a `style(03-03)` or `style(03-02)` follow-up — recommend folding into Plan 03-04's test wave, which already runs lint gates. Reverting the formatter's changes kept Plan 03-03 commits atomic to its own deliverables.
