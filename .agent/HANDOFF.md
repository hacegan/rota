# Handoff — P05 incremental sick-call repair (company Claude sandbox → local Windows)

Date: 2026-11-25. Status: **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox.
Nothing was compiled, tested, run or committed; no git command was run. P04 is DONE (see STATE.md).

## Scope

- **Input:** a `SchedulingProblem`, the published roster (`RosterAssignment` list) and a `SickCall`
  (employee, sick period as a whole local date or datetime range, report time).
- **`RosterRepairer.repair`:**
  - The sick employee loses availability for the sick period from the report onward.
  - Seats that started before the report are **pinned** (`@PlanningPin`).
  - The sick employee's seats in the sick period are **freed**.
  - Every other seat starts with its published employee, with S3 stability against the published roster.
  - Then one batch solve through the existing `RosterSolver` (`solveSchedule`). No `ProblemChange`.
- **Output:** `RepairResult` with the repaired roster (`SolveResult`: score, assignments, unassigned
  seats with reasons), the change list (REASSIGNED / LEFT_EMPTY / FILLED) and the pinned seats.
- **`fixtures/sick`:** mini plus one E3 window (Wed 14-20), the published roster, and E2 sick on Wed
  2026-11-25 reported at 07:00. Expected repair: one change, S06 E2 → E3. See
  `fixtures/sick/README.md`.
- **Impossible case:** `fixtures/mini` with the same roster and sick call leaves S06 empty, with reasons.
- **Not in scope:** repair window, urgency weighting, manager locks, DB, Docker, Spring, REST.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
pwsh -NoProfile -File scripts/verify.ps1 p04
pwsh -NoProfile -File scripts/verify.ps1 p05
```

Then:

1. Record the exact outputs and exit codes in `.agent/STATE.md`.
2. Compile errors: mechanical fixes only (AGENTS.md "Who changes what"). Any change to repair rules,
   fixture data or test expectations goes back to Opus.
3. After a green run:
   - Record in `docs/design/TIMEFOLD-VERIFIED.md` that the solver honours `@PlanningPin`
     (QUESTIONS.md G1), with `SickRepairTest.pastPinnedAssignmentsNeverMove` as evidence.
   - Also move the P04 D-items to CONFIRMED.
4. Mark P05 DONE only if `verify p05` exits 0.

## Expected result (predicted, not observed)

| Test | Prediction |
| --- | --- |
| P03/P04 tests | Unchanged and pass. `ShiftSeat` gained a pin field that defaults to false; `RosterSolver.solve` now delegates to `solveSchedule` |
| `SickCallTest` (5), `TimeRangeTest` (+2) | Pass (plain Java) |
| `RepairPreparationTest` (6) | Pass (no solver) |
| `SickRepairTest` (5 run + 1 disabled) | Pass. Repair = 1 change (S06 → E3); impossible case: hard 0, medium −360, S06 empty with "E2: not available …" |

## Known risks

- **G1 pin behaviour** is proven only by this run. If a pinned seat moves, that is a semantic
  problem: hand back, don't loosen the test.
- **`unaffectedAssignmentsStayUnchanged` asserts exactly one change.** This relies on stability
  (1000 per change) outweighing every other soft gain from moving a seat. I checked the fixture by
  hand: the biggest single-move gain is below 1000.
- **Determinism test is `@Disabled`** (UNVERIFIED, QUESTIONS.md G3).
- **Each solver-based repair test takes about 3 s or more** (wall-clock termination, 3 s unimproved).
  `repairChangesNoMoreThanFullResolve` runs two solves.
- **ROADMAP P05 was Ingestion.** It is now sick-call repair; ingestion is deferred (QUESTIONS.md H8).

## Changed files

Modified:

- `docs/plan/ROADMAP.md`: P05 row and section, with the reason for the change
- `scripts/verify.ps1`: register `p05`
- `.agent/STATE.md`, `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`
- `domain/src/main/java/dev/rota/domain/TimeRange.java`: `minus`
- `domain/src/main/java/dev/rota/domain/Employee.java`: `withoutAvailability`
- `domain/src/test/java/dev/rota/domain/TimeRangeTest.java`: two `minus` tests
- `solver/src/main/java/dev/rota/solver/ShiftSeat.java`: `@PlanningPin boolean pinned`
- `solver/src/main/java/dev/rota/solver/RosterSolver.java`: package-private `solveSchedule` (warm start)
- `solver/src/test/java/dev/rota/solver/fixture/FixtureLoader.java`: `publishedRoster`, `sickCall`

New:

- `domain/src/main/java/dev/rota/domain/SickCall.java`, `RosterAssignment.java`
- `domain/src/test/java/dev/rota/domain/SickCallTest.java`
- `solver/src/main/java/dev/rota/solver/RosterRepairer.java`, `RepairResult.java`
- `solver/src/test/java/dev/rota/solver/RepairPreparationTest.java`, `SickRepairTest.java`
- `fixtures/sick/`: `employees.json`, `availability.json`, `shifts.csv`, `preferences.json`,
  `published-roster.json`, `sick-call.json`, `README.md`
- `scripts/verify_p05.ps1`

Not changed: constraints, weights, `fixtures/mini`, `fixtures/tight`, REQUIREMENTS, STACK,
DECISIONS, TIMEFOLD-VERIFIED, AGENTS.md.
