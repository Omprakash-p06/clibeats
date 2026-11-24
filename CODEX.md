<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# CODEX.md - Instructions for OpenAI Codex / Cursor / ChatGPT Agents

Canonical repository rules are defined in [AGENTS.md](AGENTS.md). This file guides Codex, Cursor, and ChatGPT agents working in CLIBeats.

## 1. Code Graph & MCP Integration
- Codex connects to Graphicle via `~/.codex/config.toml` (`[mcp_servers.graphicle-clibeats]`).
- Cursor connects via `~/.cursor/mcp.json` (`graphicle-clibeats`).
- ALWAYS run `graphicle sync` first to ensure the graph reflects the latest working tree.
- Use MCP tools `recall_project`, `query_graph`, `get_node`, and `get_neighbors` before changing public interfaces, data classes, or components.
- Terminal fallback: `graphicle context "<question>"` and `graphicle graph explain "<symbol>"`.

## 2. Environment & Quality Gates
- **JDK 17**: MUST run with `JAVA_HOME=/home/omprakash/.jdks/jdk-17`.
- **Quality Gates**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
  ```
- **Autoformat**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
  ```

## 3. Engineering Constraints
- Clean Architecture: 3-tier structure (`domain` <- `data` <- `presentation`).
- Detekt rule: Add `@file:Suppress("ForbiddenImport")` for data imports in ViewModels where needed.
- Mocking: `org.mockito.kotlin.*` only.
- Room migrations: Export schemas and verify via `CliBeatsDatabaseMigrationTest`.
- UI: Zero hardcoded colors; use `CliBeatsColors`, JetBrains Mono typography, and bracketed TUI buttons (`[ BUTTON ]`).

## 4. Device Verification with ADB Platform-Tools
- Run `./platform-tools/adb devices`. If connected, deploy debug APK and launch `MainActivity`. If no device connected, skip device test.

## 5. Storage & Local Preservation
- Run `./gradlew --stop` after tasks to prevent daemon pile-up.
- Do not delete or overwrite `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `CODEX.md`, `OPENCODE.md`, `.planning/`, or `platform-tools/`.
