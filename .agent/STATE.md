# Agent State — Rota

Checkpoint file. Update after every meaningful step. Latest entry first.

## Current

- **Phase:** P02 — Stack & skeleton
- **Status:** DONE
- **Updated:** 2026-11-20

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

First open P02 step: `docs/plan/STACK.md` created — Java 27 (Temurin), Maven wrapper,
Spring Boot, Spring Web, Spring Data JPA/Hibernate, Bean Validation, Flyway, PostgreSQL,
Timefold Solver, Kafka, RabbitMQ, Redis, MongoDB, Elasticsearch, Spring AI, gRPC,
Testcontainers, JUnit, Mockito, ArchUnit; Next.js/React/TypeScript is frontend-only.

Earlier verify boundary (before the Maven skeleton existed):
`pwsh -NoProfile -File scripts/build.ps1` and `scripts/test.ps1` exited **64** (files missing).
That boundary is now cleared; see "P02 verify evidence".

## P02 verify evidence

Command run (exact):

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p02
```

Exact full output:

```text
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
Rota P02 verify
Repository: C:\dev\rota

OK    layout             aggregator + domain, application, bootstrap
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
OK    build              mvnw -q -DskipTests package
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
OK    test               mvnw -q test
OK    archunit           domain does not depend on Spring

P02 verify: 4 OK, 0 WARN, 0 FAIL
P02 verify passed.
```

Exit code: **0**

## P02 stack evidence

`docs/plan/STACK.md` rewritten: Java backend (Java 27 Temurin, Maven wrapper, Spring Boot,
Spring Web, Spring Data JPA/Hibernate, Bean Validation, Flyway, PostgreSQL, Timefold Solver,
Kafka, RabbitMQ, Redis, MongoDB, Elasticsearch, Spring AI, gRPC, Testcontainers, JUnit,
Mockito, ArchUnit); Next.js/React/TypeScript is frontend-only. `ROADMAP.md` had no Node-backend
lines to fix.

`VERSIONS.md` resolved from Maven Central (GA only) on 2026-11-19:

- Spring Boot `org.springframework.boot:spring-boot-starter-parent` — **4.1.2**
- Timefold `ai.timefold.solver:timefold-solver-core` — **2.8.0**
- Testcontainers `org.testcontainers:testcontainers` — **2.0.5**
- JUnit `org.junit.jupiter:junit-jupiter` — **6.1.3**

Java: Samet confirmed binding **Java 27 (Temurin)** on 2026-11-20. Now resolved per-user at
`C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot` via
`scripts/env.ps1` (see P02 env evidence).

## P02 env evidence

`scripts/env.ps1` created: reads `java_home` from `.agent/MACHINE.md`, else scans the
per-user/machine Adoptium dirs for `jdk-27*`, records the highest, and sets `JAVA_HOME` plus
the session `PATH` (no admin, no system changes). `scripts/preflight.ps1` dot-sources it and
gates `java` on resolved major version 27.

Command run (exact):

```powershell
pwsh -NoProfile -File scripts/preflight.ps1
```

Exact full output:

```text
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
Rota preflight
Repository: C:\dev\rota

OK    pwsh               PowerShell 7.7.0
OK    git                git version 2.56.0.windows.4
OK    java               openjdk version "27.0.1.0.2" 2026-10-21
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
OK    ram                19681 MB free / 32406 MB total
OK    disk               143,55 GB free on C:\

Preflight: 11 OK, 4 WARN, 0 FAIL
Preflight passed.
```

Exit code: **0**

## Verify log

| Phase | Command | Result | Date |
| --- | --- | --- | --- |
| P00 | `pwsh -File scripts/preflight.ps1` | DONE — exit code 0, 11 OK / 4 WARN / 0 FAIL | 2026-11-18 |
| P01 | `pwsh -NoProfile -File scripts/verify_p01.ps1` | DONE — exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-19 |
| P02 | `pwsh -NoProfile -File scripts/verify.ps1 p02` | DONE — exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-20 |

## Files changed

- `AGENTS.md`
- `.agent/STATE.md`
- `docs/plan/ROADMAP.md`
- `scripts/preflight.ps1`
- `scripts/env.ps1`
- `docs/domain/GLOSSARY.md`
- `docs/domain/REQUIREMENTS.md`
- `fixtures/mini/employees.json`
- `fixtures/mini/shifts.csv`
- `fixtures/mini/availability.json`
- `scripts/verify_p01.ps1`
- `scripts/verify.ps1`
- `scripts/verify_p02.ps1`
- `docs/plan/STACK.md`
- `VERSIONS.md`
- `.mvn/wrapper/maven-wrapper.properties`
- `mvnw`
- `mvnw.cmd`
- `pom.xml`
- `domain/pom.xml`
- `domain/src/main/java/dev/rota/domain/Employee.java`
- `domain/src/test/java/dev/rota/domain/EmployeeTest.java`
- `domain/src/test/java/dev/rota/domain/ArchitectureTest.java`
- `application/pom.xml`
- `bootstrap/pom.xml`
- `bootstrap/src/main/java/dev/rota/bootstrap/RotaApplication.java`
- `scripts/build.ps1`
- `scripts/test.ps1`
- `.gitignore`
- `.gitattributes`
- `LICENSE`

## Next action

- P02 DONE (verify exit code 0). P03 not started; awaiting direction.
