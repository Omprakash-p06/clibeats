<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# GEMINI.md - Instructions for Google Gemini / Antigravity Agents

Canonical repository rules are defined in [AGENTS.md](AGENTS.md). This file outlines Gemini and Antigravity specific workflows, MCP integrations, and constraints.

## 1. Codebase Exploration First (Graphicle)
- ALWAYS run `graphicle sync` first to ensure the code graph is up-to-date with working files.
- Inspect symbols and caller impact before editing:
  - Query graph: `graphicle context "<question>"` or MCP `query_graph` / `recall_project`.
  - Node inspection: `graphicle graph explain "<symbol>"` or MCP `get_node`.
  - Reverse blast radius: `graphicle graph affected "<symbol>"` or MCP `get_neighbors`.

## 2. Build & Verification Environment
- **JDK 17**: MUST use `JAVA_HOME=/home/omprakash/.jdks/jdk-17` for all Gradle commands.
- **Android SDK**: Read from `local.properties` (`/home/omprakash/Android/Sdk`).
- **Quality Gates** (All must pass with 0 errors):
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
  ```
- **Automatic formatting**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
  ```

## 3. Architecture & Standards
- Clean Architecture: `domain` (pure Kotlin logic) <- `data` (Room, ExoPlayer, OkHttp, Retrofit) <- `presentation` (Jetpack Compose, ViewModels).
- Detekt: Suppress `ForbiddenImport` with `@file:Suppress("ForbiddenImport")` when justified in ViewModels/data.
- Mocking: `org.mockito.kotlin.*` only (never MockK).
- Room Migrations: Export schemas to `app/schemas/.../<ver>.json`, zero destructive migrations.
- UI Theme: Strict TUI aesthetic. Use `CliBeatsColors` and `JetBrainsMonoFamily`. No hardcoded hex colors.

## 4. Device Smoke Testing via ADB
- Binary: `./platform-tools/adb`.
- If a device is attached, install and launch the debug build to ensure no crash on startup. If no device is attached, skip without error.

## 5. Storage Preservation & Cleanup
- Stop daemons after execution: `./gradlew --stop`
- Clean build cache: `./gradlew clean`
- Preserve `.planning/`, `platform-tools/`, and agent instruction markdown files across branch switches.
