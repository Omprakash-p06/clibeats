<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

## Codebase Discipline & Engineering Rules

### 1. Codebase Exploration First (Graphicle & Navigation)
- ALWAYS query `graphicle` before editing unfamiliar components or architecture layers:
  - Query graph: `graphicle context "<question>"` or MCP `query_graph` / `recall_project`.
  - Check symbol neighbors: `get_neighbors` / `get_node` before changing public interfaces.
- Verify callers across modules before deleting or modifying shared types.

### 2. Build & Verification Environment
- **Java Home**: MUST use JDK 17 (`JAVA_HOME=/home/omprakash/.jdks/jdk-17`).
- **Android SDK**: Configured via `local.properties` (`sdk.dir=/home/omprakash/Android/Sdk`).
- **Quality Gates** (All must pass with 0 errors before committing or pushing):
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
  ```
- **Automatic formatting**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
  ```

### 3. Architecture & Clean Architecture Boundaries
- Layering: `domain` (pure Kotlin, business logic, zero Android/data dependencies) <- `data` (Room, OkHttp, Retrofit, ExoPlayer, repositories) <- `presentation` (Jetpack Compose, ViewModels).
- Detekt enforces `ForbiddenImport` against referencing `com.clibeats.data.*` from outside `data`.
- If a ViewModel or data-layer file legitimately requires an internal data import or `AppPreferences`, add `@file:Suppress("ForbiddenImport")` at the top of the file.

### 4. Database Migrations (Room)
- NEVER use destructive migration (`fallbackToDestructiveMigration()` is forbidden).
- Every database schema update requires:
  1. Increment `CliBeatsDatabase` version.
  2. Implement `MIGRATION_X_Y` Migration object.
  3. Export Room schema to `app/schemas/com.clibeats.data.local.CliBeatsDatabase/<version>.json`.
  4. Write migration verification test in `CliBeatsDatabaseMigrationTest.kt`.

### 5. UI & Theme Discipline
- **Zero hardcoded colors**: Never instantiate `Color(0x...)` in UI composables. Always read from `com.clibeats.presentation.theme.CliBeatsColors` (`CliBeatsBackground`, `CliBeatsSurface`, `CliBeatsAccent`, etc.) or `LocalAccentColor.current`.
- **Typography**: Always use `CliBeatsTypography` (`JetBrainsMonoFamily`) and `FontFamily.Monospace`.
- **TUI Style**: Follow CLIBeats terminal aesthetics (bracketed buttons `[ BUTTON ]`, prompt headers `> prompt`, `< cd ..` breadcrumbs).
- **Paparazzi Snapshots**: Run `./gradlew verifyPaparazziDebug`. If layout dimensions intentionally change, update goldens via `./gradlew recordPaparazziDebug`.

### 6. Testing Framework
- Use `org.mockito.kotlin.*` (Mockito Kotlin). Do NOT use MockK (`io.mockk` is not configured).
