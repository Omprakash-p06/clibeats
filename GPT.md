<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# GPT.md - Instructions for OpenAI GPT / Codex / Copilot Agents

## Codebase Exploration & Discipline
1. **Graphicle First**: Always run `graphicle context "<question>"` or query `graphicle` MCP tools before editing unfamiliar codebase files.
2. **Environment Requirements**:
   - Android build environment requires JDK 17: `JAVA_HOME=/home/omprakash/.jdks/jdk-17`.
   - Android SDK location: `/home/omprakash/Android/Sdk` (specified in `local.properties`).
3. **Mandatory Quality Gates**:
   Always execute and pass all validation checks:
   ```bash
   JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
   ```
   To format code according to Ktlint:
   ```bash
   JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
   ```
4. **Architecture & Design Principles**:
   - 3-Layer Clean Architecture: `presentation` -> `data` -> `domain`.
   - Detekt linting: Use `@file:Suppress("ForbiddenImport")` for data imports in ViewModels where needed.
   - Mocking framework: `org.mockito.kotlin.*` (Mockito). Do not use MockK.
   - Persistence: Schema versioning via Room migrations with zero data loss.
   - Theme & UI: Strict TUI monospace styling, bracketed badges (`[ ... ]`), and dynamic semantic tokens (`CliBeatsColors`).
