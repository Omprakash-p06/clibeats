<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# CLAUDE.md - Instructions for Claude Code / Anthropic Models

Canonical repository rules are defined in [AGENTS.md](AGENTS.md). This file outlines Claude Code specific entry points, MCP integration, and required workflows.

## 1. Codebase Exploration First (Graphicle MCP)
- Claude Code connects to Graphicle via `.mcp.json`.
- ALWAYS run `graphicle sync` first to ensure the graph reflects the latest working copy.
- Before modifying unfamiliar code, inspect nodes using Graphicle MCP tools:
  - `recall_project(query="...")`: Semantic query across symbols and architectural decisions.
  - `query_graph(query="...", depth=2)`: Subgraph traversal around specific classes or functions.
  - `get_node(id="...")`: Inspect node definitions, provenance, and relationships.
  - `get_neighbors(id="...")`: Trace downstream callers and reverse dependencies.
- Terminal fallback without MCP:
  - `graphicle context "<question>"`
  - `graphicle graph explain "<symbol>"`
  - `graphicle graph affected "<symbol>"`

## 2. Environment & Quality Gates
- MUST use JDK 17: Prefix all gradle commands with `JAVA_HOME=/home/omprakash/.jdks/jdk-17`.
- Android SDK is configured in `local.properties` (`~/Android/Sdk`).
- **Mandatory Quality Gates** (must pass with 0 errors):
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
  ```
- **Code Auto-formatting**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
  ```

## 3. Device Testing with ADB Platform-Tools
- Platform tools are located at `./platform-tools/adb`.
- If a device/emulator is connected (`./platform-tools/adb devices | grep "device"`):
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew assembleDebug
  ./platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
  ./platform-tools/adb shell am start -n com.clibeats/.MainActivity
  ./platform-tools/adb shell pidof com.clibeats
  ```
- If no device is connected, skip device testing cleanly.

## 4. Gradle Cache & Storage Discipline
- Stop daemons after execution: `JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew --stop`
- Clean transient build outputs: `JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew clean`
- Do not let `.gradle/caches/` accumulate.

## 5. Local File Preservation
- Never delete or modify `.planning/`, `platform-tools/`, `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `CODEX.md`, `OPENCODE.md`.
- Preservation hooks in `.git/hooks/post-checkout` auto-restore local agent files if missing.
