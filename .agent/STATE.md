# Agent State — Rota

Checkpoint file. Update after every meaningful step. Latest entry first.

## Current

- **Phase:** P05 - Incremental sick-call repair
- **Status:** CODE_WRITTEN_UNVERIFIED
- **Updated:** 2026-11-25
- **Previous phase:** P04 - DONE (local verify green, 2026-11-25)

## P05 code (company Claude sandbox, 2026-11-25)

Status **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox (write files only).
Nothing was compiled, tested or run, and no git command was run. Verify has **not** been run;
there is no verify output to record.

Scope: `RosterRepairer` does a batch repair of a published roster after a `SickCall`. Seats that
started before the report are pinned (`@PlanningPin`). The sick employee's seats in the sick period
are freed. All other seats start from the published roster, with S3 stability against it. One solve
runs through the existing `RosterSolver`; no `ProblemChange`. Output: `RepairResult` (repaired
roster, change list, unassigned seats with reasons, pinned seats). New `fixtures/sick` (mini plus E3
Wed 14-20; expected repair S06 E2 → E3). The determinism test is written but `@Disabled`
(UNVERIFIED). ROADMAP P05 changed from Ingestion to this scope (reason recorded there).

Timefold usage and assumptions: `.agent/QUESTIONS.md` P05 §G/§H. Details, risks and the file list:
`.agent/HANDOFF.md`.

Verify command for local Windows:

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p05
```

## P04 code (company Claude sandbox, 2026-11-25)

Status **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox (write files only).
Nothing was compiled, tested or run, and no git command was run. Verify has **not** been run;
there is no verify output to record.

Scope: S1 preferences (PREFER/AVOID from `preferences.json`), S2n night and S2w weekend
fairness (own convex formula from CONSTRAINT-MODEL §4; `loadBalance` not used because its formula
is UNVERIFIED), S3 stability against `previous-roster.json`, S4 contract-hours deviation. No
incremental repair/pinning, DB, Docker, Spring or REST. ROADMAP P04 updated to this scope (reason
recorded there). AGENTS.md: new "Who changes what" rule (Opus vs local opencode).

Unverified Timefold API used, isolated to single constraint methods: `.agent/QUESTIONS.md` P04 §D.
Assumptions to confirm: QUESTIONS.md P04 §F. Details, risks and the file list: `.agent/HANDOFF.md`.

Verify command for local Windows:

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p04
```

## P03 code (company Claude sandbox, 2026-11-25)

Status **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox (write files only).
Nothing was compiled, tested or run, and no git command was run. Verify has **not** been run;
there is no verify output to record.

Scope: new `solver` module (Timefold only here), hard H1-H5 plus medium M6 coverage,
ConstraintVerifier tests, solver runs on `fixtures/mini` and `fixtures/tight`, `scripts/verify_p03.ps1`
registered as `p03`. No soft constraints, fairness, repair, DB, Docker or Spring wiring.

Predicted (not observed): `solve-mini` FAILs with 1 empty seat, because `fixtures/mini` is
infeasible under H4 + H5. See `.agent/QUESTIONS.md` F1.

Details, risks and the full file list: `.agent/HANDOFF.md`. Unverified Timefold API used:
`.agent/QUESTIONS.md` §A.

Verify command for local Windows:

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p03
```

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

## P01/P02 verify (2026-11-20)

### P01

Command: `pwsh -NoProfile -File scripts/verify.ps1 p01` — exit code **0**

```text
Rota P01 verify
Repository: C:\dev\rota

OK    glossary           all 8 terms present
OK    requirements       3166 bytes
OK    fixtures           5 files
OK    json-parse         4 JSON files parsed

P01 verify: 4 OK, 0 WARN, 0 FAIL
P01 verify passed.
```

### P02

Command: `pwsh -NoProfile -File scripts/verify.ps1 p02` — exit code **0**

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

## Local verify (2026-11-25)

Phase: P03. Machine role: local. Commands run in order. p01 exit 0; p02 exit 1; p03 exit 1.

### p01 - exit code 0

```text
Rota P01 verify
Repository: C:\dev\rota

OK    glossary           all 8 terms present
OK    requirements       3166 bytes
OK    fixtures           5 files
OK    json-parse         4 JSON files parsed

P01 verify: 4 OK, 0 WARN, 0 FAIL
P01 verify passed.
```

### p02 - exit code 1 (FAILED)

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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 4.022 s <<< FAILURE! -- in dev.rota.solver.MiniFixtureSolveTest
[ERROR] dev.rota.solver.MiniFixtureSolveTest.everySeatAssignedWithZeroHard -- Time elapsed: 3.992 s <<< FAILURE!
org.opentest4j.AssertionFailedError: 
unassigned seats
fixture: mini
zone: Europe/Istanbul
score: 0hard/-360medium/0soft
hard: 0
medium: -360
seats: 14
assigned: 13
unassigned: 1
ASSIGNED S01#0 2026-11-23 08:00-14:00 barista -> E1
ASSIGNED S02#0 2026-11-23 14:00-20:00 cashier -> E2
ASSIGNED S03#0 2026-11-24 08:00-14:00 barista -> E1
ASSIGNED S04#0 2026-11-24 14:00-20:00 cashier -> E2
ASSIGNED S05#0 2026-11-25 08:00-14:00 barista -> E1
ASSIGNED S06#0 2026-11-25 14:00-20:00 cashier -> E2
ASSIGNED S07#0 2026-11-26 08:00-14:00 barista -> E1
ASSIGNED S08#0 2026-11-26 14:00-20:00 cashier -> E2
ASSIGNED S09#0 2026-11-27 08:00-14:00 barista -> E1
ASSIGNED S10#0 2026-11-27 14:00-20:00 cashier -> E2
ASSIGNED S11#0 2026-11-28 08:00-14:00 barista -> E1
ASSIGNED S12#0 2026-11-28 14:00-20:00 cashier -> E3
ASSIGNED S13#0 2026-11-29 08:00-14:00 barista -> E3
UNASSIGNED S14#0 2026-11-29 14:00-20:00 cashier | E1: week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h; E3: rest 0h next to S13#0 (min 11h)
 ==> expected: <0> but was: <1>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:158)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:139)
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:201)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:152)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:590)
	at dev.rota.solver.MiniFixtureSolveTest.everySeatAssignedWithZeroHard(MiniFixtureSolveTest.java:23)

[ERROR] Failures: 
[ERROR]   MiniFixtureSolveTest.everySeatAssignedWithZeroHard:23 unassigned seats
fixture: mini
zone: Europe/Istanbul
score: 0hard/-360medium/0soft
hard: 0
medium: -360
seats: 14
assigned: 13
unassigned: 1
ASSIGNED S01#0 2026-11-23 08:00-14:00 barista -> E1
ASSIGNED S02#0 2026-11-23 14:00-20:00 cashier -> E2
ASSIGNED S03#0 2026-11-24 08:00-14:00 barista -> E1
ASSIGNED S04#0 2026-11-24 14:00-20:00 cashier -> E2
ASSIGNED S05#0 2026-11-25 08:00-14:00 barista -> E1
ASSIGNED S06#0 2026-11-25 14:00-20:00 cashier -> E2
ASSIGNED S07#0 2026-11-26 08:00-14:00 barista -> E1
ASSIGNED S08#0 2026-11-26 14:00-20:00 cashier -> E2
ASSIGNED S09#0 2026-11-27 08:00-14:00 barista -> E1
ASSIGNED S10#0 2026-11-27 14:00-20:00 cashier -> E2
ASSIGNED S11#0 2026-11-28 08:00-14:00 barista -> E1
ASSIGNED S12#0 2026-11-28 14:00-20:00 cashier -> E3
ASSIGNED S13#0 2026-11-29 08:00-14:00 barista -> E3
UNASSIGNED S14#0 2026-11-29 14:00-20:00 cashier | E1: week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h; E3: rest 0h next to S13#0 (min 11h)
 ==> expected: <0> but was: <1>
[ERROR] Tests run: 17, Failures: 1, Errors: 0, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project rota-solver: There are test failures.
[ERROR] 
[ERROR] See C:\dev\rota\solver\target\surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :rota-solver
FAIL  test               exit 1
OK    archunit           domain does not depend on Spring

P02 verify: 3 OK, 0 WARN, 1 FAIL
P02 verify FAILED.
```

### p03 - exit code 1 (FAILED)

```text
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
Rota P03 verify
Repository: C:\dev\rota

OK    layout             domain, solver, application, bootstrap; no Timefold in domain pom
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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 3.844 s <<< FAILURE! -- in dev.rota.solver.MiniFixtureSolveTest
[ERROR] dev.rota.solver.MiniFixtureSolveTest.everySeatAssignedWithZeroHard -- Time elapsed: 3.812 s <<< FAILURE!
org.opentest4j.AssertionFailedError: 
unassigned seats
fixture: mini
zone: Europe/Istanbul
score: 0hard/-360medium/0soft
hard: 0
medium: -360
seats: 14
assigned: 13
unassigned: 1
ASSIGNED S01#0 2026-11-23 08:00-14:00 barista -> E1
ASSIGNED S02#0 2026-11-23 14:00-20:00 cashier -> E2
ASSIGNED S03#0 2026-11-24 08:00-14:00 barista -> E1
ASSIGNED S04#0 2026-11-24 14:00-20:00 cashier -> E2
ASSIGNED S05#0 2026-11-25 08:00-14:00 barista -> E1
ASSIGNED S06#0 2026-11-25 14:00-20:00 cashier -> E2
ASSIGNED S07#0 2026-11-26 08:00-14:00 barista -> E1
ASSIGNED S08#0 2026-11-26 14:00-20:00 cashier -> E2
ASSIGNED S09#0 2026-11-27 08:00-14:00 barista -> E1
ASSIGNED S10#0 2026-11-27 14:00-20:00 cashier -> E2
ASSIGNED S11#0 2026-11-28 08:00-14:00 barista -> E1
ASSIGNED S12#0 2026-11-28 14:00-20:00 cashier -> E3
ASSIGNED S13#0 2026-11-29 08:00-14:00 barista -> E3
UNASSIGNED S14#0 2026-11-29 14:00-20:00 cashier | E1: week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h; E3: rest 0h next to S13#0 (min 11h)
 ==> expected: <0> but was: <1>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:158)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:139)
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:201)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:152)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:590)
	at dev.rota.solver.MiniFixtureSolveTest.everySeatAssignedWithZeroHard(MiniFixtureSolveTest.java:23)

[ERROR] Failures: 
[ERROR]   MiniFixtureSolveTest.everySeatAssignedWithZeroHard:23 unassigned seats
fixture: mini
zone: Europe/Istanbul
score: 0hard/-360medium/0soft
hard: 0
medium: -360
seats: 14
assigned: 13
unassigned: 1
ASSIGNED S01#0 2026-11-23 08:00-14:00 barista -> E1
ASSIGNED S02#0 2026-11-23 14:00-20:00 cashier -> E2
ASSIGNED S03#0 2026-11-24 08:00-14:00 barista -> E1
ASSIGNED S04#0 2026-11-24 14:00-20:00 cashier -> E2
ASSIGNED S05#0 2026-11-25 08:00-14:00 barista -> E1
ASSIGNED S06#0 2026-11-25 14:00-20:00 cashier -> E2
ASSIGNED S07#0 2026-11-26 08:00-14:00 barista -> E1
ASSIGNED S08#0 2026-11-26 14:00-20:00 cashier -> E2
ASSIGNED S09#0 2026-11-27 08:00-14:00 barista -> E1
ASSIGNED S10#0 2026-11-27 14:00-20:00 cashier -> E2
ASSIGNED S11#0 2026-11-28 08:00-14:00 barista -> E1
ASSIGNED S12#0 2026-11-28 14:00-20:00 cashier -> E3
ASSIGNED S13#0 2026-11-29 08:00-14:00 barista -> E3
UNASSIGNED S14#0 2026-11-29 14:00-20:00 cashier | E1: week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h; E3: rest 0h next to S13#0 (min 11h)
 ==> expected: <0> but was: <1>
[ERROR] Tests run: 17, Failures: 1, Errors: 0, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project rota-solver: There are test failures.
[ERROR] 
[ERROR] See C:\dev\rota\solver\target\surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :rota-solver
FAIL  test               exit 1
OK    archunit           domain does not depend on Spring or Timefold
OK    constraints        H1-H5 + M6 ConstraintVerifier tests passed
OK    timefold-probe     forEach skips empty seats; forEachIncludingUnassigned sees them
FAIL  solve-mini         1 unassigned seat(s) (expected 0)
OK    solve-tight        0 hard, medium -1080, 3 unassigned with reasons

--- C:\dev\rota\solver\target\p03\mini-report.txt ---
fixture: mini
zone: Europe/Istanbul
score: 0hard/-360medium/0soft
hard: 0
medium: -360
seats: 14
assigned: 13
unassigned: 1
ASSIGNED S01#0 2026-11-23 08:00-14:00 barista -> E1
ASSIGNED S02#0 2026-11-23 14:00-20:00 cashier -> E2
ASSIGNED S03#0 2026-11-24 08:00-14:00 barista -> E1
ASSIGNED S04#0 2026-11-24 14:00-20:00 cashier -> E2
ASSIGNED S05#0 2026-11-25 08:00-14:00 barista -> E1
ASSIGNED S06#0 2026-11-25 14:00-20:00 cashier -> E2
ASSIGNED S07#0 2026-11-26 08:00-14:00 barista -> E1
ASSIGNED S08#0 2026-11-26 14:00-20:00 cashier -> E2
ASSIGNED S09#0 2026-11-27 08:00-14:00 barista -> E1
ASSIGNED S10#0 2026-11-27 14:00-20:00 cashier -> E2
ASSIGNED S11#0 2026-11-28 08:00-14:00 barista -> E1
ASSIGNED S12#0 2026-11-28 14:00-20:00 cashier -> E3
ASSIGNED S13#0 2026-11-29 08:00-14:00 barista -> E3
UNASSIGNED S14#0 2026-11-29 14:00-20:00 cashier | E1: week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h; E3: rest 0h next to S13#0 (min 11h)

--- C:\dev\rota\solver\target\p03\tight-report.txt ---
fixture: tight
zone: Europe/Istanbul
score: 0hard/-1080medium/0soft
hard: 0
medium: -1080
seats: 14
assigned: 11
unassigned: 3
ASSIGNED S01#0 2026-11-23 08:00-14:00 barista -> E1
ASSIGNED S02#0 2026-11-23 14:00-20:00 cashier -> E2
ASSIGNED S03#0 2026-11-24 08:00-14:00 barista -> E1
ASSIGNED S04#0 2026-11-24 14:00-20:00 cashier -> E2
ASSIGNED S05#0 2026-11-25 08:00-14:00 barista -> E1
ASSIGNED S06#0 2026-11-25 14:00-20:00 cashier -> E2
ASSIGNED S07#0 2026-11-26 08:00-14:00 barista -> E1
ASSIGNED S08#0 2026-11-26 14:00-20:00 cashier -> E2
ASSIGNED S09#0 2026-11-27 08:00-14:00 barista -> E1
ASSIGNED S10#0 2026-11-27 14:00-20:00 cashier -> E2
ASSIGNED S11#0 2026-11-28 08:00-14:00 barista -> E1
UNASSIGNED S12#0 2026-11-28 14:00-20:00 cashier | E1: rest 0h next to S11#0 (min 11h), week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h
UNASSIGNED S13#0 2026-11-29 08:00-14:00 barista | E1: week total would be 42h > max 40h; E2: lacks skill barista, not available for 360 of 360 min, week total would be 36h > max 30h
UNASSIGNED S14#0 2026-11-29 14:00-20:00 cashier | E1: week total would be 42h > max 40h; E2: not available for 360 of 360 min, week total would be 36h > max 30h

P03 verify: 6 OK, 0 WARN, 2 FAIL
P03 verify FAILED.
```

Status: BLOCKED. Root cause: solve-mini expects 0 unassigned seats but the mini fixture is infeasible under H4 + H5 (S14 2026-11-29 14:00-20:00 cashier unassigned: E1 would be 42h > max 40h; E2 unavailable that day and would be 36h > max 30h; E3 rest 0h after S13, needs 11h). Matches predicted F1 in .agent/QUESTIONS.md. No code fixed (per instruction: do not fix silently). Awaiting Samet decision on making mini feasible per D1 or adjusting the assertion.

## Local verify (green, 2026-11-25)

Phase: P03. Machine role: local. All three exit 0. P03 marked DONE.

### p01 - exit code 0

```text
Rota P01 verify
Repository: C:\dev\rota

OK    glossary           all 8 terms present
OK    requirements       3166 bytes
OK    fixtures           6 files
OK    json-parse         4 JSON files parsed

P01 verify: 4 OK, 0 WARN, 0 FAIL
P01 verify passed.
```

### p02 - exit code 0

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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
OK    test               mvnw -q test
OK    archunit           domain does not depend on Spring

P02 verify: 4 OK, 0 WARN, 0 FAIL
P02 verify passed.
```

### p03 - exit code 0

```text
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
Rota P03 verify
Repository: C:\dev\rota

OK    layout             domain, solver, application, bootstrap; no Timefold in domain pom
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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
OK    test               mvnw -q test
OK    archunit           domain does not depend on Spring or Timefold

P03 verify: 4 OK, 0 WARN, 0 FAIL
P03 verify passed.
```

## Local verify (green, 2026-11-25, P04)

Phase: P04. Machine role: local. All four exit 0. P04 marked DONE.

### p01 - exit code 0

```text
Rota P01 verify
Repository: C:\dev\rota

OK    glossary           all 8 terms present
OK    requirements       3166 bytes
OK    fixtures           6 files
OK    json-parse         4 JSON files parsed

P01 verify: 4 OK, 0 WARN, 0 FAIL
P01 verify passed.
```

### p02 - exit code 0

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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
OK    test               mvnw -q test
OK    archunit           domain does not depend on Spring

P02 verify: 4 OK, 0 WARN, 0 FAIL
P02 verify passed.
```

### p03 - exit code 0

```text
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
Rota P03 verify
Repository: C:\dev\rota

OK    layout             domain, solver, application, bootstrap; no Timefold in domain pom
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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
OK    test               mvnw -q test
OK    archunit           domain does not depend on Spring or Timefold

P03 verify: 4 OK, 0 WARN, 0 FAIL
P03 verify passed.
```

### p04 - exit code 0

```text
JAVA_HOME: C:\Users\samet\AppData\Local\Programs\Eclipse Adoptium\jdk-27.0.1.0-hotspot
openjdk version "27.0.1.0.2" 2026-10-21
OpenJDK Runtime Environment Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1)
OpenJDK 64-Bit Server VM Temurin-27.0.1.0.2+1 (build 27.0.1.0.2+1, mixed mode, sharing)
Rota P04 verify
Repository: C:\dev\rota

OK    layout             domain, solver, application, bootstrap; no Timefold in domain pom
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
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
OK    test               mvnw -q test
OK    archunit           domain does not depend on Spring or Timefold

P04 verify: 4 OK, 0 WARN, 0 FAIL
P04 verify passed.
```

## Verify log

| Phase | Command | Result | Date |
| --- | --- | --- | --- |
| P00 | `pwsh -File scripts/preflight.ps1` | DONE — exit code 0, 11 OK / 4 WARN / 0 FAIL | 2026-11-18 |
| P01 | `pwsh -NoProfile -File scripts/verify_p01.ps1` | DONE — exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-19 |
| P02 | `pwsh -NoProfile -File scripts/verify.ps1 p02` | DONE — exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-20 |
| P03 | `pwsh -NoProfile -File scripts/verify.ps1 p03` | DONE - exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-25 |
| P04 | `pwsh -NoProfile -File scripts/verify.ps1 p04` | DONE - exit code 0, 4 OK / 0 WARN / 0 FAIL | 2026-11-25 |
| P05 | `pwsh -NoProfile -File scripts/verify.ps1 p05` | CODE_WRITTEN_UNVERIFIED — not run | 2026-11-25 |

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
- `fixtures/mini/preferences.json`
- `fixtures/mini/previous-roster.json`
- `fixtures/tight/availability.json`
- `fixtures/tight/employees.json`
- `fixtures/tight/shifts.csv`
- `scripts/verify_p01.ps1`
- `scripts/verify.ps1`
- `scripts/verify_p02.ps1`
- `docs/plan/STACK.md`
- `VERSIONS.md`
- `docs/design/DECISIONS.md`
- `docs/adr/0001-coverage-as-medium.md`
- `docs/design/TIMEFOLD-VERIFIED.md`
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
- P03 files: see `.agent/HANDOFF.md` history (P03 commit)
- P04 files: see `.agent/HANDOFF.md` history (P04 commit)
- P05 files: see `.agent/HANDOFF.md`

## Next action

- **P05 CODE_WRITTEN_UNVERIFIED.** Not DONE. (P04 DONE: all four verifies exit 0.)
- **Company Windows PowerShell:** review, then commit and push the P05 files (list in
  `.agent/HANDOFF.md`).
- **Local Windows:** `git pull --rebase origin main`, then
  `pwsh -NoProfile -File scripts/verify.ps1 p04` and `pwsh -NoProfile -File scripts/verify.ps1 p05`;
  record the exact output and exit codes here. Mechanical compile fixes only (AGENTS.md "Who
  changes what"); semantic problems go back to Opus. Mark P05 DONE only if `verify p05` exits 0.
