# Handoff — P04 soft constraints and fairness (company Claude sandbox → local Windows)

Date: 2026-11-25. Status: **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox.
Nothing was compiled, tested, run or committed; no git command was run. P03 is DONE (see STATE.md).

## Scope

- S1 preferences: AVOID (seat on an avoided date) and PREFER (preferred date with no seat), from
  `preferences.json`.
- S2n night fairness and S2w weekend fairness: per employee `count² × 1000 / max hours`
  (CONSTRAINT-MODEL §4). Weekend counts distinct weekends touched. Night = overlaps 00:00-06:00
  local (D3). `loadBalance` is not used (formula UNVERIFIED).
- S3 stability: one penalty per seat whose employee differs from `previous-roster.json`
  (matched by shift id and seat order).
- S4 contract hours: per (employee, week) `|assigned − contract|` minutes, plus the full
  contract for an employee with no seat at all.
- Not in scope: incremental repair/pinning, DB, Docker, Spring, REST.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
pwsh -NoProfile -File scripts/verify.ps1 p03
pwsh -NoProfile -File scripts/verify.ps1 p04
```

Then:

1. Record the exact outputs and exit codes in `.agent/STATE.md`.
2. Compile errors: fix only mechanically (imports, type names, overload plumbing), per AGENTS.md
   "Who changes what". If `ifNotExists` / two-mapping `Joiners.equal` / `toSet` need a different
   stream shape, stop and hand back (QUESTIONS.md P04 §D).
3. After a green run, add D1-D5 (QUESTIONS.md P04 §D) and the P03 A-items to CONFIRMED in
   `docs/design/TIMEFOLD-VERIFIED.md`, with the run as evidence.
4. Mark P04 DONE only if `verify p04` exits 0.

## Expected result (predicted, not observed)

| Check | Prediction | Why |
| --- | --- | --- |
| p03 | still passes | hard/medium unchanged; soft rules cannot change medium or hard |
| p04 layout, archunit | OK | no Timefold in domain |
| p04 build | OK if D1-D3 compile | see risks |
| `SoftConstraintProviderTest` (16 tests) | OK | expected values hand-computed from our own formulas |
| `SoftSolveTest` (7 tests) | OK | each problem has one unique soft optimum; about 3 s per solve |
| `PlanningMapperCalendarTest` (3 tests) | OK | D3 calendar arithmetic |
| `MiniFixtureSolveTest`, `TightFixtureSolveTest` | OK | they assert hard/medium/unassigned only |

## Known risks

- **Unverified API (QUESTIONS.md P04 §D):** `ifNotExists` with two-mapping `Joiners.equal`
  (in `preferredDateMissed` and `contractUnused`), the `toSet` signature (in `weekendFairness`),
  and `forEach` over problem facts.
- **Weekend solver test needs local search to accept worse moves.** Going from (all to E1) to
  (one weekend each) passes through a worse state. Timefold's default local search should do it
  within the 3 s unimproved limit. If `weekendFairness_solverGivesEachEmployeeOneWholeWeekend`
  is flaky, report it. Don't loosen the assertion; that is a semantic change.
- **Domain record changes:** `Employee` has a new `contractHoursPerWeek` component (6-arg canonical
  constructor; the 2-arg constructor is kept). `SchedulingProblem` has `preferences` and
  `previousAssignments` (5-arg canonical constructor; the 3-arg constructor is kept). Every
  call site in the repo is updated.
- **Soft weights are placeholders** (`SoftWeights`).
- **REQUIREMENTS.md not updated** for S2/S4 wording (QUESTIONS.md P04 F8).

## Changed files

Modified:

- `AGENTS.md`: new "Who changes what" section (Opus vs local opencode); nothing else changed
- `docs/plan/ROADMAP.md`: P04 row and section, with the reason for the change
- `scripts/verify.ps1`: register `p04`
- `.agent/STATE.md`, `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`
- `domain/src/main/java/dev/rota/domain/Employee.java`: `contractHoursPerWeek`
- `domain/src/main/java/dev/rota/domain/SchedulingProblem.java`: `preferences`, `previousAssignments`
- `solver/src/main/java/dev/rota/solver/PlanningEmployee.java`: contract minutes, horizon weeks, avoid dates
- `solver/src/main/java/dev/rota/solver/ShiftSeat.java`: local date, night, weekend key, previous employee
- `solver/src/main/java/dev/rota/solver/RotaSchedule.java`: `preferences` fact collection
- `solver/src/main/java/dev/rota/solver/PlanningMapper.java`: calendar facts, preferences, previous roster
- `solver/src/main/java/dev/rota/solver/Scores.java`: `soft(weight)`
- `solver/src/main/java/dev/rota/solver/RotaConstraintProvider.java`: 7 soft constraints added; hard/medium unchanged
- `solver/src/test/java/dev/rota/solver/fixture/FixtureLoader.java`: contract hours, preferences, previous roster

New:

- `domain/src/main/java/dev/rota/domain/Preference.java`, `PreviousAssignment.java`
- `domain/src/test/java/dev/rota/domain/SchedulingProblemTest.java`
- `solver/src/main/java/dev/rota/solver/PlanningPreference.java`, `SoftWeights.java`
- `solver/src/test/java/dev/rota/solver/SoftConstraintProviderTest.java`, `SoftSolveTest.java`,
  `PlanningMapperCalendarTest.java`
- `scripts/verify_p04.ps1`

Not changed: fixtures, REQUIREMENTS, STACK, DECISIONS, TIMEFOLD-VERIFIED, P03 tests, `verify_p03.ps1`.
