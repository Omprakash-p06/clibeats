<!-- graphicle:start (added by graphicle setup; graphicle disconnect removes it) -->
## Graphicle
Use the `graphicle` MCP server for where or how something works, for questions across files or history, and before changing unfamiliar code: `recall_project` or `query_graph` with the question, then `get_node` and `get_neighbors` for one symbol; answers cite file and lines. For an exact identifier you already know, grep is enough.
Without the server, run `graphicle context "<question>"` in a terminal.
<!-- graphicle:end -->

# CLIBeats Agent Instructions

These are the canonical repository rules for Codex, Claude Code, Gemini CLI, OpenCode, and any AI agent working in CLIBeats. Native entry points (`CLAUDE.md`, `GEMINI.md`, `CODEX.md`, `OPENCODE.md`) refer here. These files guide agent behavior; they cannot guarantee that a host loaded or obeyed them.

---

## 1. Establish project and task context

- Before acting, read `.planning/STATE.md` and confirm the task belongs to the active phase. Read `.planning/PROJECT.md`, `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md`, and the applicable phase's spec, plan, research, context, and verification artifacts before implementation. Read relevant architecture docs under `docs/` and review all planning docs.
- Treat planned modules as plans, not proof that source code exists. Inspect the actual repository and ask only when a scope or contract is genuinely ambiguous.
- State a narrow goal and identify affected files, functions, callers/callees, data contracts, configuration, and side effects before changing code.

---

## 2. Graphicle and impact review before code changes

- Before changing code, always synchronize and inspect the project code graph using Graphicle:
  1. **Sync repository state**: Run `graphicle sync` (or `graphicle sync --quiet` / `graphicle sync --background`) to ensure on-disk symbols, dependencies, and imports are fully indexed in the graph store.
  2. **Inspect project status**: Run `graphicle status` to verify graph generation, node/edge counts, symbol coverage, and agent connectivity.
  3. **Assess symbol provenance and caller/callee context**:
     - Run `graphicle graph explain "<symbol>"` (or with `--all`) or MCP `get_node` to inspect node provenance, definitions, and connections.
     - Run `graphicle graph affected "<symbol>"` (`--depth N`, `--relation R`) or MCP `get_neighbors` to analyze downstream blast radius, callers, and reverse dependencies.
     - Run `graphicle graph path "<source>" "<target>"` (`--directed` / `--undirected`) to trace the shortest dependency path between components.
  4. **Extract targeted context**:
     - Run `graphicle context "<question>"` to extract cited code evidence without invoking an LLM.
     - Run `graphicle graph query "<question>"` (`--budget N`, `--depth N`, `--seed <node/path>`) or MCP `query_graph` / `recall_project` to traverse scoped subgraphs.
     - Check architecture hubs with `graphicle graph god-nodes` (`--top N`) when altering central modules.
- Treat Graphicle as static relationship evidence, not a runtime call graph or a complete inventory. Record gaps and stale/missing source coverage. Do not infer that an unlisted caller, side effect, or dependency does not exist.
- Read the complete touched functions and relevant source files. Cross-check likely callers/callees, configuration, schemas, tests, and side effects with source search and direct inspection.
- If Graphicle is unavailable or sync/query fails, report that limitation and continue with direct source reading and search; never present a failed query as a successful review.

---

## 3. Make safe, scoped changes

- **Git Branching, PR & Review Protocol (Mandatory Rule)**: Commit all changes to a new branch (never commit working code directly to `main`). Never merge branches into `main` without creating a Pull Request (PR) and conducting a comprehensive review. For each phase completion or change, push the working branch, open a PR with complete change context, verification evidence, and CI/CD results, and ensure thorough code and specification review before merging.
- Use an isolated sandbox/worktree when one is available. If not, record the starting worktree state, make the smallest reviewable change, and preserve a clear rollback path. Do not overwrite or revert pre-existing user work; revert only changes you made.
- Keep each change focused. Avoid unrelated files and unnecessary permanent catalogs or generated artifacts. Update relevant documentation and contracts when behavior changes.
- Never commit secrets, credentials, personal data, or sensitive API keys. Avoid destructive Git actions (including `reset --hard`, `clean -fd`, force-push, or broad checkout/revert commands); do not run them unless explicitly requested and the exact affected files are confirmed.

---

## 4. CLIBeats engineering & architecture constraints

- **Java Home & Environment**: MUST use JDK 17 (`JAVA_HOME=/home/omprakash/.jdks/jdk-17`). Prefix all Gradle commands with this environment variable.
- **Android SDK**: Configured via `local.properties` (`sdk.dir=/home/omprakash/Android/Sdk`). `app/build.gradle.kts` dynamically checks `local.properties` and falls back to `~/Android/Sdk`.
- **Clean Architecture Boundaries**:
  - `domain`: Pure Kotlin, business logic, entities (`Track`, `Playlist`, `PlaybackState`), zero Android or data dependencies.
  - `data`: Room persistence, ExoPlayer/Media3, OkHttp, Retrofit APIs (InnerTube, LRCLIB, Audius, Jamendo), DAOs, and repository implementations.
  - `presentation`: Jetpack Compose UI, ViewModels, navigation, and theme styling.
- **Detekt Clean Architecture Rules**:
  - Detekt enforces `ForbiddenImport` against referencing `com.clibeats.data.*` from outside `data`.
  - If a ViewModel or data-layer file legitimately requires an internal data import or `AppPreferences`, add `@file:Suppress("ForbiddenImport")` at the very top of the file.
- **Database Migrations (Room)**:
  - NEVER use destructive migration (`fallbackToDestructiveMigration()` is strictly forbidden).
  - Every database schema update requires:
    1. Increment `CliBeatsDatabase` version.
    2. Implement `MIGRATION_X_Y` Migration object in `CliBeatsDatabase.kt`.
    3. Register `MIGRATION_X_Y` in `DatabaseModule.kt`.
    4. Export Room schema to `app/schemas/com.clibeats.data.local.CliBeatsDatabase/<version>.json`.
    5. Write and verify migration test in `CliBeatsDatabaseMigrationTest.kt`.
- **UI & Theme Discipline**:
  - **Zero hardcoded colors**: Never instantiate `Color(0x...)` in UI composables. Always read from `com.clibeats.presentation.theme.CliBeatsColors` (`CliBeatsBackground`, `CliBeatsSurface`, `CliBeatsSurfaceVariant`, `CliBeatsAccent`, `CliBeatsTextPrimary`, `CliBeatsTextSecondary`, `CliBeatsDivider`, `CliBeatsBorder`) or `LocalAccentColor.current`.
  - **Typography**: Always use `CliBeatsTypography` (`JetBrainsMonoFamily`) and `FontFamily.Monospace`.
  - **TUI Style**: Follow CLIBeats terminal aesthetics (bracketed buttons `[ BUTTON ]`, prompt headers `> prompt`, `< cd ..` breadcrumbs).
  - **Paparazzi Snapshots**: Run `./gradlew verifyPaparazziDebug`. If layout dimensions intentionally change, update goldens via `./gradlew recordPaparazziDebug`.
- **Testing Framework**:
  - Use `org.mockito.kotlin.*` (Mockito Kotlin). Do NOT use MockK (`io.mockk` is not configured).

---

## 5. Device testing via ADB platform-tools

- **Platform-Tools Binary**: Use local `./platform-tools/adb` (or system `/usr/bin/adb`).
- **Git Ignore**: `platform-tools/` MUST remain in `.gitignore` and `.git/info/exclude` at all times. Never commit platform-tools binaries to git.
- **Conditional Device Verification Protocol**:
  Before concluding feature work or bug fixes, perform device smoke verification if hardware/emulator is connected:
  ```bash
  # Check for connected devices
  DEVICES=$(./platform-tools/adb devices | grep -v "List" | grep "device" | awk '{print $1}')
  if [ -z "$DEVICES" ]; then
      echo "[ADB] No device/emulator connected. Skipping on-device smoke test."
  else
      echo "[ADB] Device detected: $DEVICES. Executing on-device smoke test..."
      JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew assembleDebug
      ./platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
      ./platform-tools/adb shell am start -n com.clibeats/.MainActivity
      sleep 2
      # Verify process is alive
      PID=$(./platform-tools/adb shell pidof com.clibeats)
      if [ -n "$PID" ]; then
          echo "[ADB] SUCCESS: App is running with PID $PID"
      else
          echo "[ADB] FAILURE: App process crashed on launch!"
          ./platform-tools/adb logcat -d -s AndroidRuntime:E
          exit 1
      fi
  fi
  ```

---

## 6. Gradle cache & storage minimization

- **Storage Constraint**: The host development machine has strictly limited disk storage. The `.gradle` cache and build intermediates must not be allowed to accumulate.
- **Mandatory Cache Minimization Practices**:
  1. **Stop Background Daemons**: Always run `./gradlew --stop` after build or test execution. Lingering Gradle daemons lock RAM and prevent cache pruning.
  2. **Clean Build Outputs**: Run `JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew clean` when switching tasks or completing a PR wave. This frees multiple gigabytes of transient `app/build/` intermediate classes, transformed resources, and DEX files.
  3. **VFS Watcher Disabled**: `org.gradle.vfs.watch=false` is enforced in `gradle.properties` to stop the daemon file-system watcher from accumulating in-memory and on-disk snapshot trees.
  4. **Emergency Storage Reclamation**: If disk space runs low, clean Gradle caches:
     ```bash
     JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew --stop
     rm -rf .gradle/buildOutputCleanup .gradle/configuration-cache app/build/
     ```

---

## 7. Local file preservation protocol

- **Protected Assets**:
  - `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `CODEX.md`, `OPENCODE.md`
  - `.planning/` (GSD project roadmap, states, and specifications)
  - `platform-tools/` (local Android debug bridge binaries)
- **Preservation Across Branch Switches & Pulls**:
  - `platform-tools/` and `.planning/` are ignored in `.gitignore` and `.git/info/exclude`. They will not be touched by `git checkout` or `git pull`.
  - `.git/hooks/post-checkout` and `.git/hooks/post-merge` automatically maintain a mirror in `.git/preserved_agent_docs/`. Whenever a branch switch occurs (e.g., checking out older branches or pulling remote updates), these hooks automatically restore missing agent docs into the workspace root.
  - Agents must NEVER run destructive git commands (`git clean -fd`, `git checkout -- .`) that wipe local planning files or agent instructions.

---

## 8. Verify and report honestly

- **Mandatory Quality Gates** (All must pass with 0 errors before committing or pushing):
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintCheck detekt testDebugUnitTest verifyPaparazziDebug
  ```
- **Automatic Code Formatting**:
  ```bash
  JAVA_HOME=/home/omprakash/.jdks/jdk-17 ./gradlew ktlintFormat
  ```
- Add or update a targeted regression test when behavior changes.
- Prefer reproducible local data and mocks; do not depend on live network APIs for unit tests.
- Report exact commands and pass/fail results. If a check cannot run, say why and what remains unverified.
- Before finishing, review the scoped diff with `git status` and `git diff --stat`.

---

## 9. Graphicle Comprehensive Command & Feature Reference

Graphicle provides static code graph analysis, repository memory, change planning, and agent MCP integration. Below is the complete reference covering all features and commands exposed by `graphicle --help`, `graphicle sync --help`, and `graphicle graph --help`.

### 9.1 Top-Level CLI Commands & Global Options

**Global Options:**
- `--data-dir <DIR>`: Path to Graphicle data directory (defaults to `GRAPHICLE_DATA_DIR` or platform default).
- `--config <FILE>`: Model configuration file path (default: `models.json` in data directory).
- `--read-only`: Open store read-only (disallows mutations; also activated via `GRAPHICLE_READ_ONLY=1`).
- `-h, --help`: Display command assistance.
- `-V, --version`: Display Graphicle version.

**Start Commands:**
- `graphicle setup`: Complete initial repository setup (sync, connect coding agents, register git hooks).
- `graphicle ask "<question>"`: Answer codebase questions using synthesized code evidence and project documentation.
- `graphicle context "<question>"`: Extract cited code evidence for a question without running an LLM.
- `graphicle open`: Launch Graphicle interactive web interface in browser.
- `graphicle status`: Display repository sync status, code graph metrics, connected agents, and model configuration.
- `graphicle doctor`: Diagnose environment issues, verify dependencies, and repair common configuration faults.

**Agent & MCP Integration:**
- `graphicle connect`: Configure and connect an agent environment to Graphicle MCP tools.
- `graphicle disconnect`: Remove Graphicle MCP configuration and hooks from agent environments.
- `graphicle mcp`: Run the MCP server over standard I/O (stdio) for agent integration.

**Data Management:**
- `graphicle sessions`: Capture, inspect, browse, and prune Claude Code and Codex agent sessions.
- `graphicle backup`: Create an online hot backup of the Graphicle data store without stopping background processes.
- `graphicle restore <backup>`: Restore a previously created backup archive into a target data directory.
- `graphicle offboard`: Completely remove a project's data and backup archives, issuing a verification receipt.

**Server & Collaboration:**
- `graphicle serve`: Start the persistent application server and streamable HTTP MCP server.
- `graphicle init`: Initialize a shared workspace for team server installations.
- `graphicle login`: Authenticate and attach a local workspace to a remote server using an access key.

**Specialized Agent Utilities:**
- `graphicle pack "<question>"`: Assemble project code relevant to a question into a single token-budgeted prompt.
- `graphicle plan "<change>"`: Generate an implementation plan with ordered steps, affected files, and unified diff sketches.
- `graphicle review`: Analyze change diffs between the current `HEAD` and its merge base with the target branch.
- `graphicle fix "<issue>"`: Automated bug fixing workflow on a new branch with issue-focused token-budgeted context.
- `graphicle wiki`: Automatically generate and update markdown project documentation from code, docs, and memory.
- `graphicle usage`: Inspect token spend, API call counts, and cache performance logs (`ask --pack`, `plan`, `fix`).
- `graphicle pixel`: Visual-to-code mapping using SAM 3.5; points from UI screenshot coordinates to source code.
- `graphicle see`: Autonomous browser driving using model computer-use capabilities (Muse Wonder / Spark).
- `graphicle import-sqlite`: Migrate legacy SQLite stores (`reviews.sqlite3`, Graphicle <=0.1.12) to the current format.
- `graphicle config`: View or update configuration properties (e.g., answer models, token budgets).
- `graphicle update`: Check for and display instructions to upgrade Graphicle.
- `graphicle uninstall`: Completely remove Graphicle from agents and the local host.

---

### 9.2 Repository Synchronization (`graphicle sync`)

`graphicle sync` imports or refreshes project files, parses symbols, resolves imports, respects `.gitignore`, and generates or updates the code graph.

**Usage:** `graphicle sync [OPTIONS] [PATH]`

**Arguments & Flags:**
- `[PATH]`: Path to repository folder to synchronize (default: current project folder).
- `--project <PROJECT>`: Associate with an explicit project ID, key, or existing project folder.
- `--background`: Asynchronously build the code graph in a background worker and return immediately.
- `--no-graph`: Ingest files and update file metadata while deferring graph construction to next MCP call or sync.
- `--read-only`: Verify synchronization state without writing to disk.
- `--json`: Output synchronization progress and results as structured JSON (default in non-terminal environments).
- `--quiet`: Suppress output on success and exit immediately if no files changed since last sync (ideal for git hooks).
- `--data-dir <DIR>`: Explicit data directory location.
- `--config <FILE>`: Explicit model configuration file.

---

### 9.3 Code Graph Engine (`graphicle graph`)

The `graphicle graph` suite handles static analysis, semantic indexing, topological traversal, reverse impact assessment, and artifact generation.

#### 9.3.1 Graph Extraction & Maintenance
- `graphicle graph extract <path>`: Build code graph from source files and semantic material.
  - Flags: `--code-only`, `--force`, `--no-cluster`, `--no-dedup`, `--allow-partial`, `--timing`, `--allow-dedup-shrink`, `--backend <NAME>`, `--model <NAME>`, `--mode deep`, `--out <DIR>`, `--max-workers <N>`, `--token-budget <N>`, `--api-timeout <S>`, `--exclude <PATTERN>`, `--no-gitignore`, `--google-workspace`, `--resolution <N>`, `--exclude-hubs <N>`, `--dedup-llm`, `--postgres <DSN>`, `--cargo`, `--global`, `--as <TAG>`.
- `graphicle graph update [path]`: Fast incremental code refresh without invoking an LLM (`--force`, `--no-cluster`).
- `graphicle graph watch [path]`: Continuous file-system watcher to rebuild graph upon code changes (`--debounce <S>`).
- `graphicle graph check-update <path>`: Check and report files with pending semantic updates without rebuilding.
- `graphicle graph cluster-only [path]`: Recompute hierarchical Louvain community clustering on an existing graph.
- `graphicle graph label [path]`: Assign descriptive domain names to graph communities.

#### 9.3.2 Navigation, Impact & Topological Traversal
- `graphicle graph explain "<symbol>"`: Inspect node attributes, defining file/lines, docstrings, and relationships.
  - `--all`: Enumerate all incoming and outgoing connections (default: top 20 most connected).
  - `--graph <PATH>`: Target an explicit `graph.json` artifact.
- `graphicle graph affected "<symbol>"`: Perform reverse reachability analysis to discover blast radius and callers.
  - `--depth <N>`: Traversal depth limit (default: 2).
  - `--relation <R>`: Filter by edge type (e.g., `calls`, `imports`, `implements`, `references`).
- `graphicle graph path "<source>" "<target>"`: Compute shortest dependency path between two components (`--directed` / `--undirected`).
- `graphicle graph query "<question>"`: Query scoped graph context around a question or topic (`--budget N`, `--depth N`, `--dfs`, `--seed <X>`, `--context <C>`).
- `graphicle graph god-nodes`: Identify architectural hub nodes with high connectivity degrees (`--top <N>`, `--json`).

#### 9.3.3 Diagnostics, Visualization & Export
- `graphicle graph diagnose multigraph`: Detect and report multi-edge collapse opportunities.
- `graphicle graph benchmark [graph.json]`: Evaluate graph token savings ratio relative to the raw repository corpus.
- `graphicle graph tree`: Generate an interactive, collapsible HTML dependency tree.
- `graphicle graph export <format>`: Export graph to `html`, `callflow-html`, `obsidian`, `wiki`, `svg`, `graphml`, `neo4j`, `falkordb`.

#### 9.3.4 Memory, Results & Semantic Merging
- `graphicle graph save-result`: Persist Q&A pairs and execution outcomes into project work memory.
- `graphicle graph reflect`: Consolidate and decay episodic memories into long-term architectural facts.
- `graphicle graph cache-check <list>`: Validate entries against the semantic chunk cache.
- `graphicle graph merge-chunks <files>`: Combine validated parallel extraction chunks into one graph.
- `graphicle graph merge-semantic`: Reconcile cached fragments with newly extracted semantic nodes.
- `graphicle graph merge-graphs <files>`: Merge multiple subproject graphs into a unified repository graph.
- `graphicle graph merge-driver <base> <current> <other>`: Git-compatible 3-way union merge driver for `graph.json`.

#### 9.3.5 Remote Repositories, Providers & Global Subgraphs
- `graphicle graph add <url>`: Ingest remote documentation or web articles.
- `graphicle graph clone <github-url>`: Clone and index a remote repository.
- `graphicle graph global <subcommand>`: Manage cross-repository multi-project indices (`add`, `remove`, `list`, `path`, `--as TAG`).
- `graphicle graph provider <subcommand>`: Manage LLM provider credentials and endpoints.

#### 9.3.6 Git Hooks, Guards & Platform Skills
- `graphicle graph hook <subcommand>`: Manage automated git integration hooks (`install`, `uninstall`, `status`).
- `graphicle graph hook-guard <kind>`: Run assistant guard checks on tool calls (`assistant`, `read`, `search`, `gemini`).
- `graphicle graph hook-check`: Silent fast compatibility check executed by runtime hooks.
- `graphicle graph install [--platform <PLATFORM>]`: Copy the Graphicle skill definition into target agent environments.
- `graphicle graph uninstall [platform]`: Remove configuration and skills from target platforms.
- `graphicle graph pi guard <action>`: Manage Pi guard extension.

#### 9.3.7 Headless & HTTP MCP Server
- `graphicle graph serve [graph.json]`: Serve code graph and memory tools via Model Context Protocol (`--transport <stdio|streamable-http>`, `--port <N>`, `--api-key <KEY>`).

#### 9.3.8 Standard Navigation Exit Codes
- `0`: Success / Answer found.
- `3`: Ambiguous symbol name (candidate disambiguation list returned).
- `4`: No matching node or symbol found in graph store.

---

## 10. Model Context Protocol (MCP) tool catalog

When using coding agents with MCP capabilities, invoke the Graphicle MCP tools directly:
- `recall_project(query="...")`: Semantic search across codebase and architectural decisions.
- `query_graph(query="...", seed="...", depth=2)`: Graph traversal around a symbol or concept.
- `get_node(id="...")`: Fetch detailed properties, provenance, and relationships of a symbol node.
- `get_neighbors(id="...", relation="...")`: Inspect immediate callers, callees, or dependencies of a symbol.
- `save_memory(question="...", answer="...", nodes=[...])`: Record architectural findings and solutions for future sessions.
