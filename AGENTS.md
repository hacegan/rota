# AGENTS.md

Rules for AI agents working in this repository (project **Rota**, a shift scheduling engine).

## Hard rules

1. **No invented versions.** Only report versions you actually read from the environment
   (`$PSVersionTable.PSVersion`, `git --version`, `dotnet --version`, ...). Never guess a
   version number. Run `scripts/preflight.ps1` before claiming the environment is ready.
2. **Work only inside this repository.** All edits, scripts, and artifacts stay under
   `C:\dev\rota`. Do not create, modify, or delete files outside the repo.
3. **Safety policy (work autonomously inside the repo).** Allowed without asking: read/edit/create
   files in the repo; git status/diff/log/add/commit/pull --rebase/push (non-force)/stash/
   restore/checkout of tracked files; build/test with mvnw, npm install/ci/run, node, tsc,
   playwright; docker compose up/down/config/logs/ps; deleting build outputs only (target/,
   node_modules/, .next/, test-results/, playwright-report/); stopping a process ONLY after
   verifying its command line belongs to this repo. Forbidden (stop and ask): any OS change
   (system/user PATH or JAVA_HOME, execution policy, WindowsApps aliases, registry, services,
   firewall); installing/uninstalling software (winget, choco, npm -g); git push --force,
   history rewrite, git clean -xfd, deleting main; docker compose down -v, docker volume rm,
   docker system prune; deleting fixtures, docs, migrations, or anything outside build outputs;
   writing outside the repo, or "repairing" Windows/PowerShell. Commit npm lockfiles. Use npm ci
   when a lockfile exists.
4. **Phase-by-phase, with verify output.** Work one roadmap phase at a time
   (`docs/plan/ROADMAP.md`). A phase is not done until its documented verify command has
   been run and its **real output** is recorded. Never mark a phase complete from memory or
   assumption.
5. **Minimal code.** Write the smallest code that satisfies the phase. No speculative
   abstractions, no extra dependencies, no reformatting unrelated code, no "while I'm here"
   changes.
6. **PowerShell one-liners.** On Windows, keep shell checks to short PowerShell one-liners.
   No chained shell noise, no bash-ism workarounds.
7. **Checkpoint to `.agent/STATE.md`.** After every meaningful step, update
   `.agent/STATE.md` with: phase, status, files changed, verify command + output summary,
   next action.

## Workflow

1. Read `docs/plan/ROADMAP.md`; pick the next phase whose verify has not passed.
2. Run `pwsh -File scripts/preflight.ps1`; confirm the environment.
3. Implement the phase with minimal code.
4. Run the phase's verify command; capture actual output.
5. Update `.agent/STATE.md` with the output.
6. Stop at the phase boundary and report.

## Style

- Match the existing code style and naming. Do not introduce a new language, framework, or
  build tool unless a roadmap phase calls for it.
- Comments only where something is non-obvious.
- Keep commits small and scoped to one phase.

## Machine role

Read `.agent/MACHINE.md` before any work. File missing -> STOP and ask Samet. Never infer the
machine from the tool you run in.

| Machine | May | Must not |
| --- | --- | --- |
| **company Claude sandbox** (Linux sandbox; reaches the repo through the desktop bridge) | write repo files | run git, build, test or verify; install anything; change the global environment |
| **company Windows PowerShell** (`role=company`) | commit and push | run verify for the record; mark DONE |
| **local Windows** (`role=local`) | run verify; mark DONE | — |

Only local Windows runs verify and marks a phase DONE. A Claude session in the Linux sandbox
follows the sandbox row even though it reads the company `.agent/MACHINE.md` through the bridge;
Samet states the sandbox role in the session.

## Who changes what

- **Opus (Claude) writes most code**: domain, constraints, solver, tests, fixtures, scripts, docs.
- **Local opencode may only make minimal mechanical fixes** so an Opus change builds and runs:
  compile errors, type mismatches, missing or wrong imports, and script plumbing (paths, flags,
  dispatcher entries). The fix must not change what the code means.
- **Semantic changes require Opus**: anything that changes domain rules, constraint logic or
  weights, penalty shapes, test expectations, or fixture data. If a mechanical fix would need one
  of these, opencode stops, records the problem in `.agent/STATE.md`, and hands back to Opus.

## Company sandbox environment (read-only)

- The company Claude sandbox is a Linux container with **Java 11** and **no PowerShell** (`pwsh`).
- Outbound downloads are blocked (**HTTP 403**). Never attempt to download or install a JDK,
  PowerShell, Maven, or any tool there.
- In the sandbox: write repo files only. Do not run `git`, builds, tests, or verify; do not
  install anything; do not change the global environment.
- Local Windows (role=local) is the only machine that runs verify and marks a phase DONE.

## Stack

`docs/plan/STACK.md` is binding. Changing the stack requires an ADR and Samet's explicit approval.

## Machine environment

Never hardcode absolute machine paths in tracked files. Machine-specific values live in
`.agent/MACHINE.md` (gitignored). All build/test scripts must dot-source `scripts/env.ps1`.
