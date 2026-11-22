<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# CLAUDE.md - Instructions for Claude Code / Anthropic Models

## Codebase Exploration & Discipline
1. **Graphicle First & Node Inspection**:
   - ALWAYS run `graphicle sync` first to ensure the graph reflects the latest working tree.
   - Inspect nodes before editing: run `graphicle graph explain "<symbol>"` or MCP `get_node` for connections, and `graphicle graph affected "<symbol>"` or MCP `get_neighbors` for reverse caller impact.
   - For architecture queries, run `graphicle context "<question>"` or MCP `query_graph`.
2. **Environment Requirements**:
   - `JAVA_HOME=/home/omprakash/.jdks/jdk-17` must prefix all `./gradlew` commands.
   - Sdk path is located at `/home/omprakash/Android/Sdk` (read from `local.properties`).
3. **Mandatory Quality Gates**:
   Before submitting changes, run:
   ```bash
   JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
   ```
   To fix formatting:
   ```bash
   JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
   ```
4. **Architecture Guidelines**:
   - Clean Architecture: `domain` -> `data` -> `presentation`.
   - Detekt Clean Architecture Rule: Add `@file:Suppress("ForbiddenImport")` if ViewModels or data layers require cross-package data imports.
   - Testing: Use `org.mockito.kotlin.*` (never MockK).
   - Database: Always write incremental migrations (`MIGRATION_X_Y`) and test with `CliBeatsDatabaseMigrationTest`.
   - UI / Theme: Zero hardcoded colors. Use `CliBeatsColors` and `LocalAccentColor`. Monospace typography and TUI bracketed conventions.
