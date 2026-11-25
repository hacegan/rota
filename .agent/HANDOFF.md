# Handoff — P03 (company Claude sandbox → local Windows)

Date: 2026-11-25. Status: **RESOLVED**. F1 fixed in fixture (E3 Friday availability). Mini now feasible.

## Fix round 2 (2026-11-25): H5 compile error

Local verify (recorded in `.agent/STATE.md`, "Local verify (2026-11-25)") failed to compile:

```text
RotaConstraintProvider.java:[69,80] incompatible types: java.lang.Long cannot be converted to java.lang.Integer
RotaConstraintProvider.java:[70,17] no suitable method found for penalize(HardMediumSoftScore, RotaConstr[...]nutes)
  method BiConstraintStream.<Score_>penalize(Score_,java.util.function.ToLongBiFunction<EmployeeWeek,java.lang.Long>) is not applicable
```

What the compiler proved about 2.8.0:

- `groupBy(keyMapper, ConstraintCollectors.sum(ToIntFunction-compatible))` produces a **`Long`** result.
- `BiConstraintStream.penalize` has two overloads: `penalize(Score_)` and
  `penalize(Score_, ToLongBiFunction<A, B>)`. The weight is **`long`**, not `int`.

Fix (one method, `solver/src/main/java/dev/rota/solver/RotaConstraintProvider.java`):

- `excessMinutes(EmployeeWeek, Integer) : int` became `excessMinutes(EmployeeWeek, Long) : long`.
- No cast, and no `Math.toIntExact`: the weight is already `long`, so nothing is narrowed.
- The meaning is unchanged: the penalty is the minutes above the weekly cap, filtered to `> 0`.
  `h5_penalizesWeeklyExcess` still expects 120.

No other compile errors were reported. H1-H4 and M6 use `int`-returning method references,
which `ToLong*Function` accepts by widening. The compiler did not flag those lines.

`scripts/verify_p02.ps1` was **not changed**. In the local run it printed
`OK layout aggregator + domain, application, bootstrap` (it doesn't reject the extra `solver`
module). Its `build` and `test` FAILs were caused only by this compile error.

After a green run, the local machine should record in `docs/design/TIMEFOLD-VERIFIED.md`
(CONFIRMED) the two compiler-proven facts above, with this compile output as evidence.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes this fix
pwsh -NoProfile -File scripts/verify.ps1 p02
pwsh -NoProfile -File scripts/verify.ps1 p03
```

Then:

1. Record the exact output and exit code in `.agent/STATE.md` (P03 section).
2. If the build fails on another Timefold signature, report it. Fix only what
   `.agent/QUESTIONS.md` §A lists.
3. After a green run, move the A-items that compiled and passed to **CONFIRMED** in
   `docs/design/TIMEFOLD-VERIFIED.md`, with the command and output. Also record the answer of
   `TimefoldBehaviourTest` for UNVERIFIED #3.
4. Mark P03 DONE only if verify exits 0.

## Expected result (predicted, not observed)

| Check | Prediction | Why |
| --- | --- | --- |
| p02 | OK, 4/4 | only the compile error broke it |
| layout, build, archunit | OK if the remaining A-items compile | — |
| constraints, timefold-probe | OK | expected penalties are hand-computed (§6 of CONSTRAINT-MODEL) |
| solve-tight | OK: hard 0, medium -1080, 3 unassigned with reasons | ADR 0001 |
| **solve-mini** | **FAIL: 1 unassigned seat, medium -360** | `fixtures/mini` is infeasible under H4+H5. See `.agent/QUESTIONS.md` F1. `test` also FAILs because `MiniFixtureSolveTest` fails |

So p03 is still expected to exit 1 until Samet resolves F1. The code is not patched around
it: the test encodes D1 as written.

## Known risks

- **Timefold API (QUESTIONS §A).** Now proven by the compiler: the Bi `penalize` overloads, and
  that the `sum` result in `groupBy` is `Long`. Still unproven until a green compile: the Uni
  `penalize` weigher type, `SolverFactory`/`SolverConfig`, `TerminationConfig` methods, and the
  `ConstraintVerifier` methods. They are isolated in `RotaConstraintProvider`, `Scores`,
  `RosterSolver` and the two test classes.
- **`HardMediumSoftScore` constructor (A4).** The `structuralScore` meaning is unknown; 0 is used.
- **Determinism (A7).** Termination is wall-clock (30 s max, 3 s unimproved). Each solve test
  takes at least about 3 s.
- **`.gitignore` changed.** `.agent/HANDOFF.md` and `.agent/QUESTIONS.md` are un-ignored so they
  travel with git.
- **ROADMAP mismatch.** `docs/plan/ROADMAP.md` still calls P03 "Core domain model". This P03
  is the solver module. ROADMAP was not changed.
- **Fixture loader is test-only** (`solver/src/test/.../fixture`). Real ingestion is P05.
- **Line endings.** Files were written with LF, matching `.gitattributes`.

## Changed files

Fix round 2:

- `solver/src/main/java/dev/rota/solver/RotaConstraintProvider.java`: H5 weigher `Long` → `long`
- `.agent/HANDOFF.md`
- `.agent/STATE.md`: status and next action

Round 1 (P03 code), modified:

- `AGENTS.md`: Machine role section only
- `.gitignore`: un-ignore `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`
- `.agent/STATE.md`
- `pom.xml`: `<module>solver</module>`
- `scripts/verify.ps1`: register `p03`
- `domain/src/main/java/dev/rota/domain/Employee.java`
- `domain/src/test/java/dev/rota/domain/ArchitectureTest.java`: `domainMustNotDependOnTimefold`

Round 1, new:

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

Not changed: `scripts/verify_p02.ps1`, `scripts/verify_p03.ps1`, tests,
`docs/design/TIMEFOLD-VERIFIED.md`, fixtures, ROADMAP, STACK, REQUIREMENTS.
