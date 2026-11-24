<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# OPENCODE.md - Instructions for OpenCode / OSS Agent Runners

Canonical repository rules are defined in [AGENTS.md](AGENTS.md). This file guides OpenCode and open-source autonomous coding agents.

## 1. Graphicle MCP & Exploration
- Connect to Graphicle MCP server via stdio:
  ```json
  {
    "mcpServers": {
      "graphicle": {
        "command": "graphicle",
        "args": ["mcp", "--project", ".", "--tools", "essentials"]
      }
    }
  }
  ```
- Run `graphicle sync` before analyzing code.
- Query graph using `query_graph`, `get_node`, `get_neighbors`, or CLI `graphicle context "<question>"`.

## 2. Environment & Quality Gates
- **JDK 17**: All Gradle commands must run with `JAVA_HOME=/home/omprakash/.jdks/jdk-17`.
- **Quality Gates** (0 errors required):
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
  ```
- **Code Formatting**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
  ```

## 3. Architecture & Standards
- Layering: `domain` <- `data` <- `presentation`.
- Detekt: Suppress `ForbiddenImport` with `@file:Suppress("ForbiddenImport")` when needed.
- Mocking: `org.mockito.kotlin.*` (Mockito).
- Database: Strict incremental migrations tested via `CliBeatsDatabaseMigrationTest`.
- UI: Monospace typography, bracketed TUI buttons, zero hardcoded hex colors.

## 4. ADB Platform-Tools Testing
- Use `./platform-tools/adb`.
- If a device/emulator is connected: build, install, launch `MainActivity`, and verify process PID.
- If no device is connected: skip device testing cleanly.

## 5. Storage & Daemon Management
- Stop Gradle daemons after work: `./gradlew --stop`
- Clean `app/build/` when appropriate to conserve disk space.
- Preserve `.planning/`, `platform-tools/`, and agent docs across branch checkouts.
