# AGENTS.md

Rules for AI agents working in this repository (project **Rota**, a shift scheduling engine).

## Hard rules

1. **No invented versions.** Only report versions you actually read from the environment
   (`$PSVersionTable.PSVersion`, `git --version`, `dotnet --version`, ...). Never guess a
   version number. Run `scripts/preflight.ps1` before claiming the environment is ready.
2. **Work only inside this repository.** All edits, scripts, and artifacts stay under
   `C:\dev\rota`. Do not create, modify, or delete files outside the repo.
3. **No destructive commands without my ONAY.** Deletions, `git reset --hard`, `git clean`,
   force pushes, dropping data, `Remove-Item -Recurse -Force`, disk/format commands, and
   similar require an explicit "ONAY" from the user in chat first. Prefer non-destructive
   alternatives.
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

Read `.agent/MACHINE.md` before any work. `role=local` -> may run verify and mark DONE.
`role=company` -> write code/docs only, never verify, never mark DONE.
File missing -> STOP and ask Samet. Never infer the machine from the tool you run in.

## Stack

`docs/plan/STACK.md` is binding. Changing the stack requires an ADR and Samet's explicit approval.

