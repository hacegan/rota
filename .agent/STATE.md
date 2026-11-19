# Agent State — Rota

Checkpoint file. Update after every meaningful step. Latest entry first.

## Current

- **Phase:** P00 — Bootstrap
- **Status:** DONE
- **Updated:** 2026-11-18

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

## Verify log

| Phase | Command | Result | Date |
| --- | --- | --- | --- |
| P00 | `pwsh -File scripts/preflight.ps1` | DONE — exit code 0, 11 OK / 4 WARN / 0 FAIL | 2026-11-18 |

## Files changed

- `AGENTS.md`
- `.agent/STATE.md`
- `docs/plan/ROADMAP.md`
- `scripts/preflight.ps1`
- `.gitignore`
- `.gitattributes`
- `LICENSE`

## Next action

- P00 is DONE. P01 has not been started, per instruction. Await direction before starting P01.
