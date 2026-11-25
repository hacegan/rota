# Handoff — P03 (company Claude sandbox → local Windows)

Date: 2026-11-25. Status: **CODE_WRITTEN_UNVERIFIED**. Nothing was compiled, tested, run or
committed. No git commands were run. All files were written straight into the working tree.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
pwsh -NoProfile -File scripts/verify.ps1 p03
```

Then:

1. Record the exact output and exit code in `.agent/STATE.md` (P03 section).
2. If the build fails on a Timefold signature, fix only what `.agent/QUESTIONS.md` §A lists,
   then re-run.
3. After a green run, move the A-items that compiled and passed to **CONFIRMED** in
   `docs/design/TIMEFOLD-VERIFIED.md`, with the command and output. Also record the answer of
   `TimefoldBehaviourTest` for UNVERIFIED #3.
4. Mark P03 DONE only if verify exits 0.

## Expected result (predicted, not observed)

| Check | Prediction | Why |
| --- | --- | --- |
| layout, build, archunit | OK if A1-A8 compile | — |
| constraints, timefold-probe | OK | expected penalties are hand-computed (§6 of CONSTRAINT-MODEL) |
| solve-tight | OK: hard 0, medium -1080, 3 unassigned with reasons | ADR 0001 |
| **solve-mini** | **FAIL: 1 unassigned seat, medium -360** | `fixtures/mini` is infeasible under H4+H5. See `.agent/QUESTIONS.md` F1. `test` also FAILs because `MiniFixtureSolveTest` fails |

So the first local run is expected to exit 1 until Samet resolves F1. The code is not
patched around it: the test encodes D1 as written.

## Known risks

- **Timefold API (QUESTIONS §A).** Only annotations, `forEach*`, `Joiners.equal`, `sum`,
  `HardMediumSoftScore`, `ConstraintVerifier` (class) and `TerminationConfig` (class) are
  CONFIRMED. Stream terminal ops, `SolverFactory`/`SolverConfig`, and the `ConstraintVerifier`
  methods are not. They are isolated in `RotaConstraintProvider`, `Scores`, `RosterSolver` and
  the two test classes.
- **`HardMediumSoftScore` constructor (A4).** The `structuralScore` meaning is unknown; 0 is used.
- **Determinism (A7).** Termination is wall-clock (30 s max, 3 s unimproved). Each solve test
  takes at least about 3 s.
- **`.gitignore` changed.** `.agent/HANDOFF.md` and `.agent/QUESTIONS.md` are now un-ignored so
  they travel with git. If local already has an untracked `.agent/QUESTIONS.md`, `git pull`
  stops with "untracked working tree file would be overwritten". Rename the local copy first,
  then merge by hand.
- **ROADMAP mismatch.** `docs/plan/ROADMAP.md` still calls P03 "Core domain model". This P03
  is the solver module. ROADMAP was not changed.
- **Fixture loader is test-only** (`solver/src/test/.../fixture`). Real ingestion is P05.
  `preferences.json` and `previous-roster.json` are not read; they are out of P03 scope.
- **Line endings.** Files were written with LF, matching `.gitattributes`.

## Changed files

Modified:

- `AGENTS.md`: Machine role section only
- `.gitignore`: un-ignore `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`
- `.agent/STATE.md`: P03 status CODE_WRITTEN_UNVERIFIED
- `pom.xml`: `<module>solver</module>`
- `scripts/verify.ps1`: register `p03`
- `domain/src/main/java/dev/rota/domain/Employee.java`: skills, max hours, availability (2-arg constructor kept)
- `domain/src/test/java/dev/rota/domain/ArchitectureTest.java`: `domainMustNotDependOnTimefold`

New:

- `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`
- `scripts/verify_p03.ps1`
- `domain/src/main/java/dev/rota/domain/TimeRange.java`, `Shift.java`, `SchedulingProblem.java`
- `domain/src/test/java/dev/rota/domain/TimeRangeTest.java`, `ShiftTest.java`
- `solver/pom.xml`
- `solver/src/main/java/dev/rota/solver/`: `Scores`, `PlanningEmployee`, `ShiftSeat` (entity),
  `RotaSchedule` (solution), `HardRules`, `RotaConstraintProvider`, `PlanningMapper`,
  `RosterSolver`, `SolveResult`, `UnassignedSeatExplainer`
- `solver/src/test/java/dev/rota/solver/`: `RotaConstraintProviderTest`, `TimefoldBehaviourTest`,
  `MiniFixtureSolveTest`, `TightFixtureSolveTest`, `Seats`, `Reports`,
  `fixture/FixtureLoader`, `fixture/MiniJson`

Not changed: `docs/design/TIMEFOLD-VERIFIED.md` (nothing could be verified here), fixtures,
ROADMAP, STACK, REQUIREMENTS.
