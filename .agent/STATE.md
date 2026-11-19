# Agent State — Rota

Checkpoint file. Update after every meaningful step. Latest entry first.

## Current

- **Phase:** P02 — Stack & skeleton
- **Status:** in_progress
- **Updated:** 2026-11-19

## Environment

Preflight passes: exit code 0, 11 OK / 4 WARN / 0 FAIL. Required tools present:
pwsh, git, java, node, npm, docker, gh, opencode. Optional WARNs: dotnet (probe exit
-2147450725), kubectl (probe exit 1), helm/kind not found.

## P00 evidence

Command run (exact):

```powershell
pwsh -NoProfile -File scripts/preflight.ps1
```

Exact full output:

```text
Rota preflight
Repository: C:\dev\rota

OK    pwsh               PowerShell 7.7.0
OK    git                git version 2.56.0.windows.4
OK    java               openjdk version "21.0.10" 2026-01-20
OK    node               v26.13.1
OK    npm                11.20.0
OK    docker             Docker version 29.9.3, build 5457199
OK    gh                 gh version 2.105.0 (2026-11-17)
OK    opencode           opencode v2.2.7
WARN  dotnet             version probe exited with code -2147450725
OK    python             Python 3.13.17
WARN  kubectl            version probe exited with code 1
WARN  helm               not found
WARN  kind               not found
OK    ram                17541 MB free / 32406 MB total
OK    disk               143,91 GB free on C:\

Preflight: 11 OK, 4 WARN, 0 FAIL
Preflight passed.
```

Exit code: **0**

Repo: **https://github.com/hacegan/rota**
P00 commit: **3fef683** — `chore: bootstrap rules, roadmap, preflight [P00]` (branch `main`, pushed to `origin`)

### Fixes applied to `scripts/preflight.ps1` (only file changed besides this checkpoint)

- Probe args preserved as an array: `$probeArgs = @($versionArgs[$Name])` then `& $tool @probeArgs`
  (previously the single-element array was unrolled to a string and splatted character-by-character,
  causing `git - - v e r s i o n` and `python -` hanging on stdin).
- 5-second timeout per tool probe via `Start-Job` + `Wait-Job -Timeout 5`; a timeout is FAIL for a
  required tool and WARN for an optional tool, and the run continues.
- OK only when the probe exit code is 0.
- Required: `pwsh, git, java, node, npm, docker, gh, opencode`.
  Optional: `dotnet, python, kubectl, helm, kind`.

Earlier hung processes (verified by command line, then terminated with ONAY): `pwsh.exe` 33044,
`python.exe` 30032, wrapper `pwsh.exe` 34552.

## P01 evidence

Deliverables: `docs/domain/GLOSSARY.md`, `docs/domain/REQUIREMENTS.md`, and
`fixtures/mini/` (`employees.json`, `shifts.csv`, `availability.json` — synthetic data only,
no real names or personal data).

Verifier: `scripts/verify_p01.ps1` (P01-only). The roadmap's original P01 verify called
`scripts/verify.ps1`, which is a P02 deliverable; per instruction it was not created, and the
P01 verify line was updated to `pwsh -NoProfile -File scripts/verify_p01.ps1`.

Command run (exact):

```powershell
pwsh -NoProfile -File scripts/verify_p01.ps1
```

Exact full output:

```text
Rota P01 verify
Repository: C:\dev\rota

OK    glossary           all 8 terms present
OK    requirements       2355 bytes
OK    fixtures           3 files
OK    json-parse         2 JSON files parsed

P01 verify: 4 OK, 0 WARN, 0 FAIL
P01 verify passed.
```

Exit code: **0**

## P02 prep evidence

Two-machine rule appended to `AGENTS.md`.

`scripts/verify.ps1` dispatcher created; dispatches `p01` to `scripts/verify_p01.ps1`.
Unknown or unimplemented phases exit 2 with a clear message.

Command run (exact):

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p01
```

Exact full output:

```text
Rota P01 verify
Repository: C:\dev\rota

OK    glossary           all 8 terms present
OK    requirements       2355 bytes
OK    fixtures           3 files
OK    json-parse         2 JSON files parsed

P01 verify: 4 OK, 0 WARN, 0 FAIL
P01 verify passed.
```

Exit code: **0**

Unknown-phase check: `pwsh -NoProfile -File scripts/verify.ps1 p99` printed
`Unknown or unimplemented phase: p99` / `Supported phases: p01` and exited **2**.

## P02 step evidence

First open P02 step: `docs/plan/STACK.md` created — Node.js v26.13.1 / npm 11.20.0, plain ES
modules, zero external runtime deps (`node:sqlite`, `node:test`, `node:http` built in).

P02 verify boundary (real output; `scripts/build.ps1` and `scripts/test.ps1` are later P02
deliverables and do not exist yet):

```powershell
pwsh -NoProfile -File scripts/build.ps1
pwsh -NoProfile -File scripts/test.ps1
```

```text
The argument 'scripts/build.ps1' is not recognized as the name of a script file. Check the spelling of the name, or if a path was included, verify that the path is correct and try again.
BUILD_EXITCODE=64
The argument 'scripts/test.ps1' is not recognized as the name of a script file. Check the spelling of the name, or if a path was included, verify that the path is correct and try again.
TEST_EXITCODE=64
```

(pwsh also printed its usage help after each error.) Both exit **64** — script files missing.
Stopped at the verify boundary; P03 not started.

## Verify log

| Phase | Command | Result | Date |
| --- | --- | --- | --- |
| P00 | `pwsh -File scripts/preflight.ps1` | DONE — exit code 0, 11 OK / 4 WARN / 0 FAIL | 2026-11-18 |
| P01 | `pwsh -NoProfile -File scripts/verify_p01.ps1` | DONE — exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-19 |
| P02 | `pwsh -File scripts/build.ps1` / `pwsh -File scripts/test.ps1` | in_progress — exit 64, both scripts missing | 2026-11-19 |

## Files changed

- `AGENTS.md`
- `.agent/STATE.md`
- `docs/plan/ROADMAP.md`
- `scripts/preflight.ps1`
- `docs/domain/GLOSSARY.md`
- `docs/domain/REQUIREMENTS.md`
- `fixtures/mini/employees.json`
- `fixtures/mini/shifts.csv`
- `fixtures/mini/availability.json`
- `scripts/verify_p01.ps1`
- `scripts/verify.ps1`
- `docs/plan/STACK.md`
- `.gitignore`
- `.gitattributes`
- `LICENSE`

## Next action

- P02 in progress. First step done (`docs/plan/STACK.md`). Remaining P02 deliverables: project
  files / solution layout, `scripts/build.ps1`, `scripts/test.ps1`. The P02 verify boundary is
  not reachable until those exist. P03 not started.
