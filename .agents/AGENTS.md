# Project Rules & Process Standards

## Process Standards
- Global Process Standard: Always commit changes as soon as a debug session or phase is completed, verified, and all test cases pass.
- Global Process Standard: Always propose a commit with a descriptive message after a debug session (gsd-debugger) is completed and resolved.
- Global Process Standard: Always ensure GitHub workflow files (.github/workflows/) are synchronized with the project structure and that all tests pass before completing a task.
- Global Process Standard: All AI agents (Gemini, DeepSeek, Claude, etc.) MUST format phase plan files (`{phase}-{plan}-PLAN.md`) using extensive research and the exact detailed template exemplified in `05-05-PLAN.md`. Every plan MUST include:
  1. Complete YAML Frontmatter (`id`, `phase`, `wave`, `title`, `depends_on`, `files_modified`, `autonomous`, `requirements_addressed`).
  2. `## Objective` & `## must_haves` sections specifying observable verification targets.
  3. `### Task N:` sections containing:
     - `<read_first>` with exact source files to inspect first.
     - `<action>` with detailed, concrete implementation guidance and explicit code blocks/snippets.
     - `<acceptance_criteria>` with grep-verifiable or command-testable conditions.
  4. `## Verification` section with exact executable shell commands.
