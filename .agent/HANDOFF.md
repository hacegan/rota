# Handoff — P06 REST API v1 (company Claude sandbox → local Windows)

Date: 2026-11-25. Status: **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox.
Nothing was compiled, tested, run or committed; no git command was run. P05 is DONE (see STATE.md).

## Scope

- **Contract first:** `docs/contracts/openapi/rota.yaml` (OpenAPI 3.1). Controllers and tests follow it.
- **application module (pure Java; no Spring, no Timefold):**
  - Use cases `SolveRoster`, `RepairSickCall`, `GetRoster`.
  - Ports `SchedulingPort`, `FixturePort`, `RosterRepository`.
  - Result types `Roster` and `RosterPlan` (score summary, assignments, unassigned seats with
    reasons, changes, pinned seats).
  - Exceptions `UnknownFixtureException` and `RosterNotFoundException`.
- **bootstrap module (Spring Boot):**
  - `RotaConfiguration` wires the use cases to:
    - `SolverSchedulingAdapter`, which maps `SolveResult`/`RepairResult` to `RosterPlan`;
    - `InMemoryRosterRepository` (ConcurrentHashMap);
    - `FixtureFiles` + `FixtureJson`, which read `fixtures/` (the same formats as the test `FixtureLoader`).
  - Controllers: `HealthController`, `RosterController`.
  - DTOs `RosterResponse` and `ErrorResponse`.
  - `ApiExceptionHandler`: unknown fixture or missing parameter returns 400; unknown id returns 404.
- **Endpoints:**
  - `GET /api/health`
  - `POST /api/rosters/solve?fixture=mini|tight` (201 + Location)
  - `POST /api/rosters/repair?fixture=sick` (201 + Location)
  - `GET /api/rosters/{id}`
- **Scripts:**
  - `scripts/run-api.ps1`: dot-sources `env.ps1`, runs `mvnw -q -DskipTests package` (never
    `clean`), then `java -jar` on the bootstrap jar with `--rota.fixtures-dir`.
  - `scripts/verify_p06.ps1`: layout, build, test, MockMvc report, ArchUnit. Registered as `p06`.
- **Not in scope:** DB, Docker, Kafka, Next.js, auth, uploading problems. No solver or constraint
  change.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
pwsh -NoProfile -File scripts/verify.ps1 p05
pwsh -NoProfile -File scripts/verify.ps1 p06
```

Then:

1. Record the exact outputs and exit codes in `.agent/STATE.md`.
2. Compile or dependency errors: mechanical fixes only (AGENTS.md "Who changes what"). Expected
   candidates, all listed in QUESTIONS.md §J:
   - J2: starter name `spring-boot-starter-webmvc` → `spring-boot-starter-web`;
   - J4: JUnit version alignment in `bootstrap/pom.xml`;
   - J3: a moved Spring test import.

   Any change to endpoints, status codes, JSON shape, use-case rules or test expectations goes back
   to Opus.
3. Optional smoke test:
   - Run `pwsh -NoProfile -File scripts/run-api.ps1`.
   - In a second shell:
     `Invoke-RestMethod -Method Post 'http://localhost:8080/api/rosters/solve?fixture=mini'`
     and `Invoke-RestMethod 'http://localhost:8080/api/rosters/r1'`.
   - Stop the first shell with Ctrl+C.
4. Mark P06 DONE only if `verify p06` exits 0.

## Expected result (predicted, not observed)

| Test | Prediction |
| --- | --- |
| P03-P05 tests | Unchanged and pass; no domain or solver file was modified |
| `UseCasesTest` (8), `ApplicationArchitectureTest` (3) | Pass (plain Java + ArchUnit, no solver) |
| `FixtureFilesTest` (5) | Pass (reads repo fixtures) |
| `RosterApiTest` (8) | Pass. mini: hard 0, medium 0, 0 unassigned. tight: medium −1080, 3 unassigned with reasons. sick repair: 1 change S06#0 E2 → E3. Four real solves, about 3 s or more each |

## Known risks

- **Spring Boot 4 specifics are UNVERIFIED** (QUESTIONS.md §J): the starter name, the Jackson 3
  serialization of records and nulls, and what `spring-boot-starter-test` contains.
- **`RosterApiTest` repeats solver expectations** (tight medium −1080, sick = exactly one change).
  They rely on the same solver runs the solver tests already assert. If they fail here but pass in
  the solver module, that points to wiring or fixture loading, not the solver.
- **Synchronous solve.** Each POST blocks for about 3-30 s (QUESTIONS.md K5).
- **Duplicate fixture reader** (QUESTIONS.md K2). The test `FixtureLoader` was not touched (the
  sandbox can't stage files 8 folders deep).
- **ROADMAP P07/P08** still mention greedy (QUESTIONS.md K1); Samet to re-scope.

## Changed files

Modified:

- `application/pom.xml`: junit-jupiter 6.1.3 and archunit 1.5.1 (test)
- `bootstrap/pom.xml`: `rota-solver`, `spring-boot-starter-webmvc`, `spring-boot-starter-test`
  (test); `spring-boot-starter` removed (included by webmvc)
- `scripts/verify.ps1`: register `p06`
- `docs/plan/ROADMAP.md`: P06 row and section, with the reason for the change
- `.agent/STATE.md`, `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`

New:

- `docs/contracts/openapi/rota.yaml`
- `application/src/main/java/dev/rota/application/`: `Roster.java`, `RosterPlan.java`,
  `SchedulingPort.java`, `FixturePort.java`, `RosterRepository.java`, `SolveRoster.java`,
  `RepairSickCall.java`, `GetRoster.java`, `UnknownFixtureException.java`,
  `RosterNotFoundException.java`
- `application/src/test/java/dev/rota/application/`: `UseCasesTest.java`,
  `ApplicationArchitectureTest.java`
- `bootstrap/src/main/java/dev/rota/bootstrap/`: `RotaConfiguration.java`,
  `SolverSchedulingAdapter.java`, `InMemoryRosterRepository.java`, `FixtureFiles.java`,
  `FixtureJson.java`, `RosterController.java`, `HealthController.java`, `ApiExceptionHandler.java`,
  `RosterResponse.java`, `ErrorResponse.java`
- `bootstrap/src/main/resources/application.properties`
- `bootstrap/src/test/java/dev/rota/bootstrap/`: `RosterApiTest.java`, `FixtureFilesTest.java`
- `scripts/run-api.ps1`, `scripts/verify_p06.ps1`

Not changed: domain, solver (main and test), fixtures, constraints, weights, REQUIREMENTS, STACK,
DECISIONS, TIMEFOLD-VERIFIED, AGENTS.md, `RotaApplication.java`, root `pom.xml`.
