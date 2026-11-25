# Open questions — Rota

Written by company Claude sandbox, 2026-11-25 (P03). Nothing here was compiled or run.

## F1 — RESOLVED (2026-11-25): `fixtures/mini` infeasibility was genuine, not a config bug

Resolved by adding one E3 availability window on Friday 2026-11-27 08:00-14:00
(Europe/Istanbul) so E3 covers S09 (barista), reducing E1 to 24 h weekdays + 12 h weekend = 36 h.
Mini now assigns all 14 seats with 0 hard violations. `fixtures/tight` unchanged and stays
infeasible by design (0 hard, 3 unassigned with reasons). No constraint/weight/hard rule
changed. See `fixtures/mini/README.md` and `docs/design/TIMEFOLD-VERIFIED.md` (F1 section).

D1 says mini = every shift assigned, 0 hard violations. Under the hard rules as written,
that is impossible. `MiniFixtureSolveTest` and `verify p03 / solve-mini` are expected to
**fail with 1 empty seat (medium -360)**.

Derivation (week 2026-W48, all shifts 6 h):

- Weekday barista S01, S03, S05, S07, S09: only E1 has `barista` and weekday availability
  (E3 is Sat/Sun only). So E1 = 30 h on weekdays.
- Weekday cashier S02..S10: E2 = 5 × 6 h = 30 h = E2 max. Fine.
- Weekend S11, S12 (Sat) and S13, S14 (Sun): E2 has no weekend availability. Same-day
  08-14 + 14-20 gives 0 h rest, so H4 allows **one seat per person per day**. That means
  2 people per day: E1 and E3 on both days.
- E1 would then reach 30 + 12 = **42 h > 40 h** (H5). Only 3 of the 4 weekend seats can be filled.

Ways to fix it (Samet decides; nothing changed here):

1. Give E3 one weekday barista window (e.g. Fri 08-14). E1 drops to 24 h on weekdays plus 12 h at the weekend = 36 h.
2. Raise E1 `max_hours_per_week` to 42.
3. Exempt same-day double shifts from H4. Not recommended: it changes a hard (legal) rule.

## A — Timefold API used but not in the CONFIRMED list of TIMEFOLD-VERIFIED.md

These can't be avoided if the solver is to run. Each is confined to the file named.
The local compile settles the signatures (A1-A8); the local test run settles behaviour.
After a green local run, move each item to CONFIRMED in TIMEFOLD-VERIFIED.md with the
command and output.

| # | Item | Used in | How to settle |
| --- | --- | --- | --- |
| A1 | `ConstraintProvider` interface, `defineConstraints(ConstraintFactory)` returning `Constraint[]`, type `Constraint` | `RotaConstraintProvider`, `TimefoldBehaviourTest` | compile; `javap ai.timefold.solver.core.api.score.stream.ConstraintProvider` |
| A2 | Stream ops: `filter`, `groupBy(Function, UniConstraintCollector)`, `penalize(Score)`, `penalize(Score, ToIntFunction)`, `penalize(Score, ToIntBiFunction)`, `asConstraint(String)` | `RotaConstraintProvider` | compile; javap `UniConstraintStream`, `BiConstraintStream` |
| A3 | `ConstraintCollectors.sum(ToIntFunction)`: the name is CONFIRMED, the parameter type is not | `RotaConstraintProvider.maxHoursPerWeek` | compile |
| A4 | `new HardMediumSoftScore(structural, hard, medium, soft)`: canonical record constructor. Order comes from the javap field order; it is public by JLS. The meaning of `structuralScore` is unknown, so 0 is assumed neutral | `Scores` | compile; test that `H1` on an unqualified seat yields `-360hard` in a solve |
| A5 | `ConstraintVerifier.build(provider, solutionClass, entityClass...)`, `verifyThat(BiFunction)`, `given(Object...)`, `penalizesBy(int)` | `RotaConstraintProviderTest`, `TimefoldBehaviourTest` | compile + test run |
| A6 | `SolverFactory.create(SolverConfig)`, `buildSolver()`, `Solver.solve(solution)`; `SolverConfig.withSolutionClass/withEntityClasses/withConstraintProviderClass/withTerminationConfig` | `RosterSolver` | compile + solve tests |
| A7 | `TerminationConfig.withSpentLimit(Duration)`, `withUnimprovedSpentLimit(Duration)`. Wall-clock limits were chosen because step-count limits may be phase-only (unverified). **NFR1 risk:** the stopping point is time-based, so results may differ between runs if the optimum isn't reached within 3 s unimproved | `RosterSolver` | compile; run the solve tests twice and compare the reports |
| A8 | `@ValueRangeProvider` / `@PlanningVariable` linked by type, without `id` / `valueRangeProviderRefs` | `RotaSchedule`, `ShiftSeat` | solve test runs without a "no value range" error |

## B — Existing UNVERIFIED items (TIMEFOLD-VERIFIED.md) and how P03 handles them

| # | Item | P03 handling |
| --- | --- | --- |
| 2 | Records as facts / entity needs no-arg ctor + mutable fields | Avoided: `PlanningEmployee` is a final class; `ShiftSeat` and `RotaSchedule` have public no-arg constructors and setters |
| 3 | Does `forEach` skip unassigned? | Probed by `TimefoldBehaviourTest.forEachSkipsUnassignedSeats` (expects yes). The hard constraints also filter `employee != null` themselves, so they are correct either way |
| 5 | Termination names / determinism | See A7 |
| 7 | Does the construction heuristic fill empty seats in its first pass? | **Not answered in P03.** It needs a construction-heuristic-only phase config (`ConstructionHeuristicPhaseConfig`, unverified), which is outside this scope |
| 1, 4, 6, 8 | non-annotation metadata, loadBalance, ProblemChange, ConstraintWeightOverrides | Not used in P03 |

## C — Assumptions made in code (not decided in DECISIONS.md)

- **C1 Week for H5:** ISO Mon-Sun in the site zone. A shift crossing the week boundary counts in its **start** week (`PlanningMapper.weekKey`). CONSTRAINT-MODEL Q3 is still open.
- **C2 H4 counts every pair**, not only consecutive shifts. With 3+ shifts close together it overcounts, but hard must reach 0 anyway.
- **C3 tight has no timezone** and uses the `day + HH:mm` format. The test loader defaults it to Europe/Istanbul (D3).
