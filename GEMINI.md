<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# GEMINI.md - Instructions for Google Gemini / Antigravity Agents

## Codebase Exploration & Discipline
1. **Graphicle First**: Always query `graphicle` MCP or run `graphicle context "<question>"` before modifying unfamiliar files, refactoring, or adding architecture changes.
2. **Environment Requirements**:
   - MUST use JDK 17: `JAVA_HOME=/home/omprakash/.jdks/jdk-17`.
   - Android SDK is at `/home/omprakash/Android/Sdk` (via `local.properties`).
3. **Mandatory Quality Gates**:
   Before proposing or committing changes, verify:
   ```bash
   JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
   ```
   Autoformat command:
   ```bash
   JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
   ```
4. **Architecture & Standards**:
   - Clean Architecture: `domain` pure Kotlin logic; `data` handles APIs, ExoPlayer, Room; `presentation` Compose UI and ViewModels.
   - Detekt: Suppress `ForbiddenImport` with `@file:Suppress("ForbiddenImport")` when justified in ViewModels/data.
   - Mocking: `org.mockito.kotlin.*` only.
   - Room Migrations: Export schemas to `app/schemas/.../<ver>.json` and never allow destructive migrations.
   - UI Theme: Strict TUI aesthetic. Use `CliBeatsColors` and `JetBrainsMonoFamily`. No hardcoded hex colors.
