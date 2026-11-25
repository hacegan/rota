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

Status after the green local P03 run (STATE.md, "Local verify (green, 2026-11-25)"): A1-A8
all compiled and their tests passed. They are not yet listed as CONFIRMED in
TIMEFOLD-VERIFIED.md; local should move them there with that run as evidence.

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

## P04 (2026-11-25, company Claude sandbox; nothing compiled or run)

### D — Timefold API used in P04 that is neither CONFIRMED nor exercised by the green P03 run

Each item is confined to one or two constraint methods, so a failure is local.

| # | Item | Used in | How to settle |
| --- | --- | --- | --- |
| D1 | `UniConstraintStream.ifNotExists(Class, BiJoiner...)` | `RotaConstraintProvider.preferredDateMissed`, `contractUnused` | compile; `SoftConstraintProviderTest.s1_*Preferred*`, `s4_*Seat` |
| D2 | Two-mapping `Joiners.equal(Function<A,T>, Function<B,T>)` (the one-mapping form is proven) | same two methods | compile |
| D3 | `ConstraintCollectors.toSet(Function)` signature and result type `Set<Integer>` (name CONFIRMED) | `RotaConstraintProvider.weekendFairness` | compile; `s2w_*` tests |
| D4 | `ConstraintFactory.forEach` over a **problem fact** class (`PlanningEmployee`, `PlanningPreference`), not an entity | `contractUnused`, `preferredDateMissed` | `s4_penalizesEmployeeWithNoSeat`, `s1_penalizesMissedPreferredDate` |
| D5 | A second `@ProblemFactCollectionProperty` on the solution (`preferences`) next to the value-range one | `RotaSchedule` | solve tests run |

If D1 or D2 does not compile, it is **not** a mechanical fix: the zero-seat and missed-preference
cases need another stream shape (Opus decides). If only an import or type name differs, it is
mechanical.

### E — `loadBalance` is not used

`ConstraintCollectors.loadBalance` exists (CONFIRMED) but its exact formula is UNVERIFIED
(TIMEFOLD-VERIFIED.md #4). P04 does not use it. Fairness uses our own convex formula from
CONSTRAINT-MODEL §4: per employee `count² × 1000 / max hours` (integer division), summed. The
constant `−A²/C` term is dropped, which is exact when the total count A is the same between
compared rosters. Because it is our formula, ConstraintVerifier tests assert exact values. If a
later phase switches to `loadBalance`, its tests must become relative (balanced < skewed) until
the formula is verified.

### F — Assumptions made in P04 code (please confirm or correct)

- **F1 Previous roster matched by shift id.** `previous-roster.json` covers 2026-11-16/22 but names
  shift ids (S02, S04) that also exist in the current week. P04 treats a shift id as a recurring
  slot: seat i of shift X compares with the i-th previous entry for X. If previous ids are meant to
  be different shifts, S3 needs a date-shift or pattern rule instead.
- **F2 `pinned` ignored.** Carried in `PreviousAssignment` but unused until incremental repair.
- **F3 Contract 0 = no target.** S4 applies only when `contract_hours_per_week > 0`. A missing field
  (tight) loads as 0.
- **F4 S4 zero-seat case is per horizon, not per week.** An employee with no seat at all gets
  `contract × weeks in horizon`. An employee with seats in some weeks but not others is not
  penalized for the empty weeks. Fixtures are one week, so this does not show yet.
- **F5 S1 PREFER/AVOID target is a date** (D4 fixture format). Matching uses the seat's start date in
  the site zone. A night shift starting 23:00 counts for its start date.
- **F6 Weekend unit = weekends touched**, not seats (CONSTRAINT-MODEL §4.5). Night = overlaps
  00:00-06:00 local on any day the seat spans (D3).
- **F7 Soft weights are placeholders** (`SoftWeights`): stability 1000/seat, contract 1/min,
  fairness 1 × scaled formula, preference 50. Order follows CONSTRAINT-MODEL §2; numbers are not
  calibrated.
- **F8 REQUIREMENTS mismatch.** REQUIREMENTS.md lists S2 as "balance scheduled hours" and S4 as
  "out of scope: cost". P04 implements night/weekend fairness and contract-hours deviation as
  instructed. REQUIREMENTS.md was not edited; Samet should decide whether to update it.

P04 status after the green local run (STATE.md, "Local verify (green, 2026-11-25, P04)"): D1-D5
compiled and their tests passed. Local should move them to CONFIRMED in TIMEFOLD-VERIFIED.md.

## P05 (2026-11-25, company Claude sandbox; nothing compiled or run)

### G — Timefold usage in P05

- **G1 `@PlanningPin` on a `boolean` field** (`ShiftSeat.pinned`). The annotation is CONFIRMED
  (marker, TIMEFOLD-VERIFIED.md corrected #3). That the solver never changes a pinned entity is
  **behaviour**, not shown by the jar. `SickRepairTest.pastPinnedAssignmentsNeverMove` proves or
  refutes it.
- **G2 Warm start.** `Solver.solve(schedule)` with seats already holding employees (proven API,
  same call as P03/P04). The assumption is that the solver keeps those values as its starting
  point instead of rebuilding from scratch. `SickRepairTest.unaffectedAssignmentsStayUnchanged`
  checks the outcome. Even a cold start would be pulled back by S3 stability, so that test proves
  the result, not the mechanism.
- **G3 Determinism: UNVERIFIED** (TIMEFOLD-VERIFIED.md #5, termination is wall-clock).
  `SickRepairTest.repeatedRepairIsDeterministic` is written but `@Disabled`. Enable it only after
  determinism is CONFIRMED.
- **No `ProblemChange`.** The repair is a batch re-solve (`RosterRepairer`).

### H — Assumptions in P05 code (please confirm or correct)

- **H1 "Now" = report time.** `SickCall.reportedAt` is the cut: a seat whose start is before it is
  pinned (in-progress seats included). There is no separate `now` input.
- **H2 The past is not rewritten.** Availability is removed only for the part of the sick period from
  the report onward (`SickCall.effectivePeriod`). Otherwise pinned past seats of the sick employee
  would show as H2 violations.
- **H3 Freed seats have no stability reference.** Reassigning them is the point of the repair, so
  they don't count as changes for S3. They do count in the change list.
- **H4 Stability reference = published roster**, not `previous-roster.json` (dropped during repair).
- **H5 No repair window** (CONSTRAINT-MODEL §5.2) and **no urgency weighting** (§5.4). All future seats
  can move, each change costs a flat 1000 (P04 `SoftWeights.STABILITY`).
- **H6 Pinned empty seats stay empty.** A seat published empty in the past keeps its medium penalty.
- **H7 `fixtures/sick` adds one E3 window (Wed 14-20).** Without it the repair is impossible
  (`fixtures/sick/README.md`).
- **H8 ROADMAP P05 was Ingestion.** It is now sick-call repair; ingestion is deferred, to be
  scheduled by Samet before P11 CLI.
