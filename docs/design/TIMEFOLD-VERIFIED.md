# Timefold 2.8.0 — Verified API

Verified on 2026-11-20 against the real jar
`ai/timefold.solver:timefold-solver-core:2.8.0` (Maven Central, fetched via `.\mvnw.cmd dependency:get`).
Every item below was either CONFIRMED or CORRECTED against the jar; nothing is guessed.
Items that cannot be settled from the jar are left UNVERIFIED and recorded in
`.agent/QUESTIONS.md`.

## CONFIRMED (class/method present in 2.8.0)

| Item | Evidence |
| --- | --- |
| `@PlanningSolution` | `ai/timefold/solver/core/api/domain/solution/PlanningSolution.class` (jar tf) |
| `@PlanningEntity` | `.../api/domain/entity/PlanningEntity.class` |
| `@PlanningEntityCollectionProperty`, `@PlanningEntityProperty` | `.../api/domain/solution/PlanningEntityCollectionProperty.class`, `PlanningEntityProperty.class` |
| `@PlanningId` | `.../api/domain/common/PlanningId.class` |
| `@PlanningPin`, `@PlanningPinToIndex` | `.../api/domain/entity/PlanningPin.class`, `PlanningPinToIndex.class` |
| `@PlanningScore` (bendableHardLevelsSize, bendableSoftLevelsSize) | `.../api/domain/solution/PlanningScore.class` (javap) |
| `@PlanningVariable(allowsUnassigned = ...)` | `.../api/domain/variable/PlanningVariable.class` has `allowsUnassigned()` (javap) |
| `@ProblemFactCollectionProperty`, `@ProblemFactProperty` | `.../api/domain/solution/ProblemFactCollectionProperty.class`, `ProblemFactProperty.class` |
| `@ValueRangeProvider` | `.../api/domain/valuerange/ValueRangeProvider.class` |
| `ConstraintFactory.forEach`, `forEachIncludingUnassigned`, `forEachUniquePair` | javap on `ConstraintFactory` (all present) |
| `Joiners.equal`, `overlapping`, `filtering`, `greaterThan` etc. | javap on `Joiners` (all present) |
| `ConstraintCollectors.sum`, `count`, `loadBalance`, `min`, `max`, `toSet`, `toMap`, ... | javap on `ConstraintCollectors` (all present) |
| `ConstraintVerifier` (lives in core, not a separate artifact) | `.../api/score/stream/test/ConstraintVerifier.class` |
| `ConstraintWeightOverrides` | `.../api/domain/solution/ConstraintWeightOverrides.class` |
| `ProblemChange` | `.../api/solver/change/ProblemChange.class` |
| `SolutionCloner` | `.../api/domain/solution/cloner/SolutionCloner.class` |
| `ConstructionHeuristicType` enum (FIRST_FIT, ...) | `.../config/constructionheuristic/ConstructionHeuristicType.class` |
| `TerminationConfig` | `.../config/solver/termination/TerminationConfig.class` |

Commands (same for all jar checks):

```powershell
. .\scripts\env.ps1
$jar = (Get-ChildItem "$env:USERPROFILE\.m2\repository\ai\timefold\solver\timefold-solver-core\2.8.0" -Filter 'timefold-solver-core-2.8.0.jar' | Select-Object -First 1).FullName
jar tf $jar | Select-String -Pattern '<ClassName>'
javap -p -classpath $jar <fully-qualified-class>
```

## CORRECTED (assumption in CONSTRAINT-MODEL.md was wrong)

### 1. Score type name

- Assumed: `HardMediumSoftLongScore`.
- Jar: only `HardMediumSoftScore` exists (package `ai.timefold.solver.core.api.score`).
- `HardMediumSoftScore` is a `record` with `long` fields `structuralScore`, `hardScore`,
  `mediumScore`, `softScore`. So it is already long-based; there is no separate `Long`
  variant. **Use `HardMediumSoftScore`.**

Evidence:

```text
public final class ai.timefold.solver.core.api.score.HardMediumSoftScore extends java.lang.Record implements ai.timefold.solver.core.api.score.Score<...> {
  private final long structuralScore;
  private final long hardScore;
  private final long mediumScore;
  private final long softScore;
  ...
}
```

### 2. ConstraintVerifier artifact

- Assumed: `ai.timefold.solver:timefold-solver-test` (separate artifact).
- Jar: `ConstraintVerifier` lives in `timefold-solver-core` at
  `ai/timefold/solver/core/api/score/stream/test/ConstraintVerifier.class`. The
  `timefold-solver-test` artifact only exists up to 1.35.0 on Maven Central, not 2.8.0.
- **Use `timefold-solver-core` only; no extra test dependency.**

Evidence: `jar tf` listed `ai/timefold/solver/core/api/score/stream/test/ConstraintVerifier.class`.

### 3. `@PlanningPin` attribute

- Assumed (1.x): `@PlanningPin`.
- Jar: `PlanningPin` is a marker annotation with no attributes
  (`public interface PlanningPin extends java.lang.annotation.Annotation {}`).
- **Use `@PlanningPin` as a marker; no `allowsUnassigned`-style flag on it.**

### 4. `@PlanningScore` attribute

- Assumed: `@PlanningScore` with bendable levels.
- Jar: `PlanningScore` has `bendableHardLevelsSize()` and `bendableSoftLevelsSize()`.
- **Use `@PlanningScore` (defaults 0/0 for HardMediumSoft).**

## UNVERIFIED (cannot be settled from the jar; left UNVERIFIED, recorded in `.agent/QUESTIONS.md`)

1. Whether 2.8.0 offers any non-annotation way to declare planning metadata.
2. Whether records are allowed as problem facts / whether entities need a no-arg constructor
   and mutable fields (assumed: mutable entity classes, record facts).
3. Whether `forEach` skips empty (unassigned) planning entities, or whether
   `forEachIncludingUnassigned` is required for H6. (Likely the latter; verify in code.)
4. Exact formula of `ConstraintCollectors.loadBalance` vs the model's `U` formula.
5. Termination config names (`unimprovedStepCount`, wall-clock vs step-count) and the
   determinism claim.
6. `ProblemChange` on a running solver (API exists; behaviour at scale unverified).
7. **Construction heuristic:** does 2.8.0's construction heuristic fill entities whose
   `allowsUnassigned` variable is empty in the first pass, or treat them as already
   initialized and skip them? `ConstructionHeuristicType` exists (FIRST_FIT, ...), but the
   fill-empty-seat behaviour is not derivable from the jar. The cross-cutting test
   `repair_sickE2Wednesday_freesOneSeatAndMovesNothingElse` (CONSTRAINT-MODEL §6) catches
   either outcome.
8. Whether `ConstraintWeightOverrides` (1.x name) is the right hook for runtime weight
   changes (class exists; usage unverified).

## Proven by local run (2026-11-25)

### F1 — `fixtures/mini` infeasibility is genuine, not a config bug

- **Answer:** genuine fixture infeasibility under H4 + H5, not solver configuration.
- **Evidence:** `RosterSolver` leaves S14 (2026-11-29 14:00-20:00 cashier) unassigned with
  explicit reasons for every candidate (E1 would reach 42 h > 40 h cap; E2 unavailable that day
  and would reach 36 h > 30 h; E3 rest 0 h after S13, needs 11 h). The solver assigns all 13
  other seats and reports 0 hard violations; raising the termination budget cannot create a
  feasible assignment because none exists: E1 must cover 5 weekday barista seats (30 h) and can
  take at most one weekend seat (6 h) under the 40 h cap, but the weekend needs two people per
  day (E1 + E3) to satisfy H4, forcing E1 to take 2 weekend seats (12 h) = 42 h.
- **Resolution (D1):** added one E3 availability window on Friday 2026-11-27 08:00-14:00 so E3
  covers S09, reducing E1 to 24 h weekdays + 12 h weekend = 36 h. Mini is now feasible
  (14/14 seats, 0 hard). `fixtures/tight` is unchanged and stays infeasible by design
  (0 hard, 3 unassigned with reasons). No constraint, weight, or hard rule changed.
- **Verified APIs exercised by this run:** construction heuristic fills all feasible seats in
  the first pass and leaves only genuinely infeasible seats empty; `forEach` skips unassigned
  seats and `forEachIncludingUnassigned` sees them (`TimefoldBehaviourTest`).

```powershell
. .\scripts\env.ps1
$jar = (Get-ChildItem "$env:USERPROFILE\.m2\repository\ai\timefold\solver\timefold-solver-core\2.8.0" -Filter 'timefold-solver-core-2.8.0.jar' | Select-Object -First 1).FullName
# download:
.\mvnw.cmd dependency:get "-Dartifact=ai.timefold.solver:timefold-solver-core:2.8.0:jar"
# inspect:
jar tf $jar | Select-String -Pattern '<name>'
javap -p -classpath $jar <fully-qualified-class>
```

Download exit code: **0**. All listed classes resolved from the jar.
