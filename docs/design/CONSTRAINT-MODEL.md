# Rota — Constraint Model (Timefold Solver)

Status: design draft written on the company machine. Nothing here has been compiled or verified.

**UNVERIFIED convention.** Every Timefold class, annotation, attribute, collector or config name
below is from Timefold 1.x knowledge and is **UNVERIFIED against `timefold-solver-core` 2.8.0**
(the version pinned in `VERSIONS.md`). Each one is tagged `⚠UNVERIFIED`. The local machine
checks them against the real jar and corrects this document before implementation.

---

## 1. Domain model

### 1.1 Layering decision

Timefold reads its planning metadata from annotations on the classes it solves
(⚠UNVERIFIED: whether 2.8.0 offers any non-annotation way to declare it). Putting those annotations
on domain classes would break "pure Java domain". So there are two models with a mapper between them:

| Layer | Module | Depends on | Contains |
| --- | --- | --- | --- |
| Domain | `rota-domain` (exists) | JDK only | Records, invariants, calendar policy, `RosterValidator` |
| Planning | `rota-solver` (**new, proposed**, Q5) | `rota-domain`, `timefold-solver-core` | Planning solution/entity, `ConstraintProvider`, domain↔planning mapper |
| Use cases | `rota-application` (exists) | domain, solver | `SolveRoster`, `RepairRoster`, `PublishRoster` |

Add an ArchUnit rule next to the existing Spring rule: `dev.rota.domain` must not depend on
`ai.timefold..`.

### 1.2 Pure domain (`rota-domain`)

**Value objects** (immutable records; invariants checked in the compact constructor):

| Type | Content | Invariant |
| --- | --- | --- |
| `EmployeeId`, `ShiftId` | non-blank string | — |
| `SeatId` | `ShiftId` + seat index | `0 ≤ index < headcount` |
| `Skill` | code | non-blank, normalized lowercase |
| `TimeRange` | `Instant start`, `Instant end`, half-open `[start, end)` | `end > start`; `overlaps`, `contains`, `gapTo`, `duration` |
| `WorkLimits` | `Duration maxPerWeek` (later: `contractedPerWeek`, S4) | non-negative |
| `AvailabilityWindow` | `TimeRange` | — |
| `Unavailability` | `TimeRange`, reason (`SICK`, `LEAVE`, …) | — |
| `Preference` | kind (`PREFER`/`AVOID`), target (date / weekday / shift) | shape unknown (Q4) |
| `RosterPolicy` | `ZoneId zone`, `Duration minRest = 11h`, week rule, night rule, weekend rule | all calendar rules live here, none hardcoded |

Times are stored as `Instant` and turned into calendar facts (week, weekend, night) only
through `RosterPolicy.zone`, so DST cannot distort durations for H4 and H5.

**Aggregates**

| Aggregate root | Owns | Key behaviour / invariant |
| --- | --- | --- |
| `Employee` | skills, `WorkLimits`, availability windows, unavailabilities, preferences | `isAvailable(TimeRange)`: inside the union of windows **and** not intersecting any unavailability |
| `Shift` | `TimeRange`, required `Skill`, `headcount ≥ 1` | derives its seats `SeatId(shift, 0..headcount-1)` |
| `Roster` | period, version, status (`DRAFT`/`PUBLISHED`), `Assignment` per seat | every seat of every shift in the period appears exactly once; `publish()` makes version n+1; `revise(changes)` returns a `RosterRevision` (diff) |

`Assignment` = (`SeatId`, `EmployeeId` or empty, `locked` flag set by a manager).

**Domain service.** `RosterValidator` checks H1–H6 in plain Java and returns
`Violation(constraintId, seats, employee, magnitude, message)`. It is the independent checker for
P10 and the readable report for NFR4. It shares no code with the Timefold constraints, so the
two can be cross-checked (see §6).

### 1.3 Planning model (`rota-solver`)

| Timefold role | Class / member | Annotation |
| --- | --- | --- |
| **Planning solution** | `RotaSchedule` | `@PlanningSolution` ⚠UNVERIFIED |
| Problem facts | `List<PlanningEmployee>`, `RosterPolicy` snapshot | `@ProblemFactCollectionProperty`, `@ProblemFactProperty` ⚠UNVERIFIED |
| Value range | `List<PlanningEmployee>` (whole solution) | `@ValueRangeProvider` ⚠UNVERIFIED |
| **Planning entity** | `ShiftSeat` (one per headcount seat) | `@PlanningEntity`, `@PlanningId` on `seatId` ⚠UNVERIFIED |
| **Planning variable** | `ShiftSeat.employee : PlanningEmployee`, may be empty | `@PlanningVariable(allowsUnassigned = true)` ⚠UNVERIFIED (1.x name; 2.x may differ) |
| Pinning | `ShiftSeat.pinned` | `@PlanningPin` ⚠UNVERIFIED |
| Score | `RotaSchedule.score` | `@PlanningScore`, `HardMediumSoftLongScore` ⚠UNVERIFIED |

`ShiftSeat` carries copies of facts that the mapper works out once: `seatId`, `shiftId`, `start`,
`end`, `durationMinutes`, `requiredSkill`, `weekKey`, `nightMinutes`, `weekendKey`,
`publishedEmployeeId` (for stability, §5), `pinned`. Working these out in the mapper keeps
`ZoneId` logic out of constraint streams and makes them cheap.

`PlanningEmployee` wraps a domain `Employee`: id, skill set, max minutes per week, capacity weight,
and an availability index for fast `isAvailable` checks.

**Choices made**

- **Basic variable on a per-seat entity, not a list variable on `Employee`.** Seats turn headcount
  into separate entities. An unfilled seat is simply an empty value, pinning works seat by seat,
  and H6 becomes a simple filter. A list variable was rejected because order has no meaning
  in rostering, and partial-list pinning (`@PlanningPinToIndex` ⚠UNVERIFIED) is too coarse.
- **The value range covers the whole solution (all employees), not one filtered per entity by skill.** H1 stays an
  explicit, explainable constraint, which manual edits in P15 need. Revisit at P18 if the
  search space is too large.
- **Two seats of one shift given to the same employee** are caught by H3, because identical ranges
  overlap. No extra rule is needed. Swapping seats within a shift is a harmless symmetry.
- ⚠UNVERIFIED: whether 2.8.0 needs a no-arg constructor or mutable fields on entities, and whether
  records are allowed as problem facts. The design assumes mutable entity classes and record facts.

---

## 2. Constraint table

Hard and medium penalties are measured in **minutes**, so every violation has a gradient: a 10-minute
rest shortfall scores better than a 6-hour one.

| ID | Rule (one sentence) | Level | Penalty shape | Weight rationale | Missing / ambiguous |
| --- | --- | --- | --- | --- | --- |
| H1 Qualification | An assigned employee holds the seat's required skill. | Hard | `durationMinutes` per violating seat | Hard must reach 0. Minutes keep it on the same scale as H2–H5. | Only one skill per shift. Fine per the glossary. |
| H2 Availability | The seat's range lies inside the employee's availability windows and touches no unavailability. | Hard | Minutes of the seat not covered | Partial coverage gives a gradient. | Windows are stored per day, so availability **cannot cross midnight**. Night seats can never be staffed with the current format (Q2). The glossary mentions unavailability blocks but no fixture has any. §5 needs them, so the design adds `Unavailability`. Containment is taken as inclusive (`08:00–14:00` fits inside `08:00–20:00`). |
| H3 No overlap | One employee never holds two seats whose half-open ranges intersect. | Hard | Overlap minutes per unique pair | — | Back-to-back seats (`end == start`) do not overlap, but see H4. |
| H4 Rest | For one employee, any two seats `a`, `b` with `a.end ≤ b.start` are at least 11 h apart. | Hard | `(11h − gap)` minutes per pair | Penalizing each pair overcounts when there are 3+ seats in a day. That is acceptable because hard must reach 0. | Read literally, this **forbids same-day double shifts** (mini S11+S12 have a 0 h gap) (Q3). |
| H5 Max hours | An employee's assigned minutes per week stay ≤ `max_hours_per_week × 60`. | Hard | Excess minutes per (employee, week) | — | The week boundary is not defined. Assumed: ISO Mon–Sun in the site zone. A seat that crosses the boundary counts toward the week it **starts** in (alternative: split it) (Q3). |
| H6 Headcount | Every seat has an employee. | **Medium** (REQUIREMENTS says hard) | `durationMinutes` per empty seat | Coverage ranks below legality and above quality. See §3. | **Deviates from REQUIREMENTS.** Under H1–H5 the mini fixture cannot be fully staffed (§3), so H6 cannot be hard (Q1). |
| S1 Preference | Assign employees to the shifts/days they asked for and away from those they want to avoid. | Soft | `w1` per violated preference | Lowest priority: the data is self-reported and lowest-stakes. | **No preference data or schema** in the fixtures. Only the shape can be built (Q4). |
| S2 Fairness (hours) | Spread assigned hours in proportion to each employee's capacity. | Soft | Quadratic, §4: `Σ m_e² / c_e − A²/C` | Convex shape, so moving work from a heavy to a light employee always improves the score. | "Balance" is not defined. **Assumed: proportional to `max_hours_per_week`**, not equal hours (a 40 h and a 30 h contract should not be pushed to equal). |
| S2n Night fairness | Spread night seats in proportion to capacity among eligible employees. | Soft | Quadratic, §4 | Same as S2 | **Not in REQUIREMENTS.** Added because this task asks for it. "Night" is not defined (Q2). |
| S2w Weekend fairness | Spread weekends worked in proportion to capacity among eligible employees. | Soft | Quadratic, §4, counted per weekend touched | Same as S2 | **Not in REQUIREMENTS.** "Weekend" and the site zone are not defined (Q2). |
| S3 Continuity | Prefer that an employee keeps last week's pattern. | Soft | `w3` per assigned seat whose (weekday, start time) the employee did not work in the previous published week | Below cost and fairness | **No previous roster is an input.** "Stable pattern" is not defined. The rule shown is an assumption (Q4). |
| S4 Cost | Minimize overtime and premium-pay minutes. | Soft | Minutes above contracted weekly hours × premium factor, plus premium minutes × rate | Money, so above fairness | **No contracted hours, pay rates or premium definition.** Because H5 caps hours as hard and there is no contracted level below the cap, overtime is always 0 and S4 does nothing (Q4). |
| R1 Stability (repair only) | During repair, avoid changing published seats. | Soft | `W_stab × urgency` per changed seat, §5 | Top soft priority. Coverage still outranks it. | **Not in REQUIREMENTS.** §5 needs it. |

**Default soft priority:** R1 ≫ S4 > S2 ≥ S2n/S2w > S3 > S1.

- R1 is first: changing a published seat breaks a promise people have already planned around.
- S4 is real money.
- Fairness (S2, S2n, S2w) is long-term wellbeing.
- Continuity (S3) and preference (S1) are nice-to-haves.

The numeric weights are **placeholders** until P07 calibrates them on known-better/known-worse
pairs. The soft constraints mix units (minutes, squared hours, counts), so raw numbers are not
comparable yet. Weights should be adjustable without code changes via
`ConstraintWeightOverrides` ⚠UNVERIFIED (1.x name).

**Implementation notes** (constraint streams, all ⚠UNVERIFIED in 2.8.0):

- `forEach` skips empty seats; H6 needs `forEachIncludingUnassigned`.
- H3 and H4 use `forEachUniquePair` with `Joiners.equal(employee)`. H3 adds `Joiners.overlapping`.
- H5 and S2 use `groupBy` with `ConstraintCollectors.sum`.
- H2 needs no join: it calls `PlanningEmployee.isAvailable(start, end)` on fact data.

---

## 3. Score type

**Chosen: `HardMediumSoftLongScore`** ⚠UNVERIFIED (class name and package in 2.8.0).
Priority order: **legality (hard) > coverage (medium) > quality (soft)**.

**Why a medium level: the mini fixture is infeasible if H6 is hard.**

- **Barista seats.** Only E1 holds `barista`. The 7 barista seats need 42 h, but E1's cap is 40 h (H5).
- **Weekend cashier seats.** E2 has no weekend availability (H2). E1 would have to work S11+S12
  and S13+S14 back to back with 0 h rest (H4).
- **Result.** E1 can take at most 6 seats (36 h) of the 9 that only E1 can fill. At least **3 seats
  (1080 min) must stay empty**. A legal roster with exactly 3 empty seats exists
  (E1: S01, S03, S05, S07, S09, S11; E2: the five weekday cashier seats).
- **Consequence for P06.** The roadmap check "fully assigned, 0 hard violations" cannot be met (Q1).

**Rejected: HardSoft with H6 as hard.** Breaking a rest rule and leaving a seat empty would sit on the
same level. The solver would accept 120 min of H5 overrun to avoid a 360 min empty seat. The result
is an illegal roster that scores as "less bad", which the planner can neither publish nor act on.

**Rejected: HardSoft with H6 as soft and a very large weight.** Coverage would only dominate if
its weight exceeded any possible soft total. Quadratic fairness grows with instance size, so at
100k shifts the order can silently flip. Explanations also get muddy.

**Rejected for now: Bendable score** (e.g. hard / coverage / stability / soft). It would make
stability strictly outrank everything else at every distance in time, which is too rigid: a change
three weeks out should be cheap. Revisit if R1 weight tuning proves fragile.

**Why `Long`.** Quadratic fairness overflows `int` at scale. 40 h is 2400 min; 2400² ≈ 5.8·10⁶ per
employee, and 10⁴ employees reach about 5.8·10¹⁰, well above 2³¹.

---

## 4. Fairness

**Definitions** (horizon `P` = roster period, optionally extended by `H` weeks of published history):

- **Night seat**: a seat with at least θ minutes inside the night band `[b_start, b_end)` in the
  site zone. The band and θ are `RosterPolicy` parameters with no agreed value yet (Q2).
- **Weekend**: the Saturday and Sunday of one ISO week in the site zone (assumption, Q2).
- For each employee `e`:
  - `n_e` = number of night seats assigned in `P`, plus history.
  - `w_e` = number of **distinct weekends** in which `e` works at least one seat. Losing a weekend is
    the human cost, whether it holds one shift or two.
- **Eligible pool** `E_N` (and `E_W`): employees who are qualified and available for at least one night
  (weekend) seat in `P`. Employees who could never take such a seat are left out; otherwise they would add
  unfairness that no roster can remove. Policy caveat: an employee could game this through declared
  availability. That is out of scope.
- **Capacity weight** `c_e = max_hours_per_week_e`. `C = Σ_{e∈E} c_e`. `A = Σ_{e∈E} n_e`.
- **Target share** `t_e = A · c_e / C`.

**Unfairness (night):**

```
U_N = Σ_{e∈E_N} (n_e − t_e)² / c_e   =   Σ_{e∈E_N} n_e² / c_e  −  A² / C
```

`U_W` is the same with `w_e` over `E_W`. S2 (hours) is the same with assigned hours `m_e` over all
employees. The soft penalty is `round(K · U)` with a fixed integer scale `K`.

**How it is computed.**

- The first form needs a global mean. The second form needs only per-employee sums plus one global `A`.
- Precise implementation: a global `groupBy` for `A` plus a per-employee `groupBy`, or
  `ConstraintCollectors.loadBalance` ⚠UNVERIFIED (its exact formula must be checked against the jar
  before using it).
- If P18 shows the global aggregate is too slow: drop the `−A²/C` term. This is exact whenever two
  rosters have the same `A`, and slightly favours covering fewer night seats otherwise.

**Why this is not just "minimize total penalty"**

1. **A linear per-seat penalty cannot see distribution.** If every night seat costs 1, the total is
   `A` no matter who works the nights. A 4–0 split and a 2–2 split score the same. Fairness is a
   property of the distribution, not of the sum.
2. **A convex (squared) penalty makes each additional night cost more** (`2n_e + 1` marginal). Moving
   one night from a heavy to a light employee always strictly improves the score: 4–0 → 16,
   3–1 → 10, 2–2 → 8. Every move gives local search a gradient.
3. **Min-max (minimize the worst-off) was rejected.** Only the top employee affects the score, so most
   moves leave it unchanged. Local search hits plateaus, and the second-worst employee is ignored.
4. **Capacity weighting.** A 30 h employee should not get the same nights as a 40 h employee.
5. **Counting weekends touched gives the right behaviour within a weekend.** On one weekend, E1 on both
   days scores `1/40`; E1 Saturday plus E2 Sunday scores `1/40 + 1/30`. The model keeps one person's
   weekend whole rather than ruining two, and rotates *across* weekends through the squared term.
6. **Horizon.** Fairness within a single week is noise. It only means something over time, so history
   counts are included. That requires previous rosters as input, the same input S3 needs (Q4).

**On the mini fixture:** there are no night seats (everything is within 08–20), so `U_N ≡ 0`. Only E1 is
eligible for weekends, so `U_W ≡ 0`. Fairness tests therefore use fixture variants (§6).

---

## 5. Incremental re-solve (sick call on a published roster)

**Input:** published `Roster` version *v*, `SickReport(employee, from, to?)`, `now`.

1. **Domain step.** Add `Unavailability(SICK, [from, to ?? end of horizon))` to the employee.
   From then on H2 makes it a hard violation to put them in that range.
2. **Classify every seat.**

   | Class | Rule | Planning state |
   | --- | --- | --- |
   | FROZEN | Seat already started or ended (`start < now + minNotice`), **or** manager-locked, **or** outside the repair window | `pinned = true`, keeps its employee |
   | FREED | Belongs to the sick employee, overlaps the sick range, not frozen | `employee = empty`, unpinned |
   | MOVABLE | Every other seat inside the repair window | unpinned, starts at its published employee, `publishedEmployeeId` set |

   **Repair window:** every ISO week the sick range touches (H5 is weekly, so only whole weeks work),
   plus 11 h on each side (H4 ripple). Everything else is pinned. The search space stays small, so
   a repair finishes in seconds (NFR2).
3. **What must never move:**
   - Seats in the past or in progress. History cannot be rewritten. They stay in the score as pinned
     entities, so H4 and H5 still count them.
   - Seats a manager has locked.
   - Seats outside the repair window.
   - The sick employee is never placed inside the sick range (enforced by H2 as hard).
   - The repair never trades a hard violation for coverage: the levels guarantee this.
4. **Stability penalty R1** (soft):
   - Applies to each MOVABLE seat whose employee is not its published employee: `W_stab × u(seat)`.
   - Urgency `u`: 3 if the seat starts within 48 h, 2 if within 7 days, else 1 (placeholders).
   - FREED seats are excluded, because changing them is the point of the repair.
   - Filling a seat that was empty in *v* costs nothing.
   - **Level reasoning:** coverage (medium) outranks stability, so moving one colleague to close a gap
     is accepted and leaving a gap to avoid a move is not. Stability outranks every other soft rule,
     so the repair does not re-optimize fairness.
   - **Alternative considered:** penalize the number of distinct employees whose schedule changes (each
     one gets a notification). This is closer to the human cost but harder to compute incrementally.
     Revisit after P17.
5. **Solve.**
   - Batch warm start: build `RotaSchedule` from *v* with the pinning above and run the solver.
   - Stop on unimproved-step or step-count limits, **not wall-clock time**, so NFR1 determinism holds
     (termination config names ⚠UNVERIFIED).
   - ⚠UNVERIFIED: whether the 2.8.0 construction heuristic tries to fill entities whose
     `allowsUnassigned` variable is empty, or treats them as already initialized and skips them.
     If it skips them, local search change moves alone must fill FREED seats. The test in §6
     catches either outcome.
   - Later option: `ProblemChange` on a running solver ⚠UNVERIFIED, once a long-running solver
     service exists (Kafka-triggered). The same classification rules apply.
6. **Output.**
   - Publish `Roster` *v+1* plus a `RosterRevision` listing only the changed seats (old → new).
   - Notify only the affected employees.
   - If medium is still below 0, the remaining empty seats go to a manager. Hard rules are never
     relaxed automatically.

Determinism: the same *v*, sick report and seed produce the same revision (seed and environment-mode
config ⚠UNVERIFIED).

---

## 6. Test plan

**Tooling:**

- `ConstraintVerifier` from `ai.timefold.solver:timefold-solver-test` ⚠UNVERIFIED (artifact and API in
  2.8.0). This is a new test-scoped dependency; the local machine resolves its version into
  `VERSIONS.md`.
- Base data is `fixtures/mini`, loaded through the P05 reader.
- Variants are built **in test code** from mini; the fixture files stay unchanged.
- Units are minutes. Each test is written first and must fail before its constraint exists.
- ISO week of the fixture: 2026-W48 (Mon 2026-11-23).

| Constraint | Test name | Given | When | Then |
| --- | --- | --- | --- | --- |
| H1 | `h1_penalizesUnqualifiedEmployee` | mini; S01 (barista) → E2 (cashier only) | verify H1 | penalizes 360 |
| H2 | `h2_penalizesSeatOutsideAvailability` | mini; S12 (Sat) → E2 (no Saturday window) | verify H2 | penalizes 360 |
| H3 | `h3_penalizesOverlap` | variant: S01 headcount 2; both seats → E1 | verify H3 | penalizes 360 (one pair) |
| H4 | `h4_penalizesBackToBackWithoutRest` | mini; S11 + S12 → E1 (Sat 08–14, 14–20) | verify H4 | penalizes 660 (11 h − 0). Guard: S12 + S13 → E1 (12 h gap) → 0 |
| H5 | `h5_penalizesWeeklyExcess` | mini; all 7 barista seats → E1 (42 h) | verify H5 | penalizes 120 for (E1, 2026-W48) |
| H6 | `h6_penalizesEmptySeatOnMediumLevel` | mini; S14 empty, rest legal | verify H6 | medium −360, hard 0 |
| S1 | `s1_penalizesAvoidedDay` | mini + synthetic preference "E1 AVOID 2026-11-29"; S13 → E1 | verify S1 | penalizes 1 match *(blocked by Q4)* |
| S2 | `s2_prefersCapacityProportionalHours` | weekday cashier seats only. A: all 5 → E1 (30, 0). B: all 5 → E2 (0, 30). C: 3 → E1, 2 → E2 (18, 12) | verify S2 on A, B, C | penalty(C) < penalty(A) < penalty(B); each equals `K·U` from §4 |
| S2n | `s2n_penalizesNightConcentration` | variant: two 22:00–06:00 seats; both employees qualified and available; both seats → E1 vs one each | verify S2n | concentrated > split *(blocked by Q2)* |
| S2w | `s2w_countsWeekendsNotSeats` | variant: E2 available Sat/Sun. X: S11 + S13 → E1. Y: S11 → E1, S13 → E2 | verify S2w | `w_E1 = 1` in X; penalty(X) < penalty(Y) |
| S3 | `s3_penalizesPatternBreak` | previous week = mini shifted −7 d with E2 on Mon 14:00 cashier; now S02 → E1 | verify S3 | penalizes 1 *(blocked by Q4)* |
| S4 | `s4_penalizesOvertimeAboveContract` | synthetic contracted E1 = 30 h; E1 on 6 barista seats (36 h) | verify S4 | penalizes 360 × premium factor *(blocked by Q4)* |
| R1 | `r1_penalizesChangedPublishedSeatButNotFreedSeat` | published S04 = E2 now → E1; freed S06 now empty | verify R1 | penalizes 1 × urgency for S04 only |

**Cross-cutting tests (one each):**

- **`solve_mini_isHardFeasibleWithThreeEmptySeats`.** Fixed seed, full solve. Expect hard = 0 and
  medium = −1080. Depends on Q1.
- **`repair_sickE2Wednesday_freesOneSeatAndMovesNothingElse`.**
  - Given: the published mini solution; `now = 2026-11-25T07:00`; E2 sick for 2026-11-25.
  - When: repair runs.
  - Then: S01–S04 are unchanged (frozen); only S06 is freed. E1 cannot legally cover S06: S05
    08–14 leaves 0 h rest, and E1 would reach 42 h. So S06 stays empty: medium −360 more, hard 0,
    zero other changes, R1 = 0.
  - This proves there is no gratuitous ripple, and it exposes the construction-heuristic question in §5.
- **`validatorAgreesWithScore`.** Random rosters on mini (fixed seeds). The total hard minutes from
  `RosterValidator` equal the negated hard score, and the same holds for medium. This keeps the
  independent validator (P10) and the solver honest with each other.

---

## 7. Open questions for Samet

1. **H6 as medium, not hard?** Under H1–H5 the mini fixture cannot be fully staffed (at least 3 seats
   empty, §3), so P06's "fully assigned, 0 hard violations" cannot pass. Approve H6 → medium and
   change P06's check to "hard 0, medium −1080"? Or change the fixture instead (for example give E2
   `barista` and weekend availability)?
2. **Night, weekend and time zone.** What is the night band and threshold θ? Which days are the
   weekend? Which `ZoneId` is the site in? How should availability that crosses midnight be written,
   given that today's per-day windows cannot express it? Also: night and weekend fairness are **not in
   REQUIREMENTS**. Add them as S2n/S2w?
3. **H4/H5 edge cases.** Does H4 really forbid same-day double shifts (0 h gap, as written)? Is the
   week ISO Mon–Sun in the site zone, and does a shift crossing the week boundary count toward the week
   it starts in or get split?
4. **Inputs for S1/S3/S4.** There is no preference schema, no previous roster (needed for S3 and
   fairness history), and no contracted hours or premium rates (S4 does nothing without them). Add these
   fields now, or defer S1/S3/S4 and ship hard + H6 + S2 first? Also confirm the soft priority
   R1 ≫ S4 > S2 ≥ S2n/S2w > S3 > S1.
5. **Module and dependency approval.** Is a new `rota-solver` Maven module (Timefold kept out of
   `rota-domain`) plus a test-scoped `timefold-solver-test` acceptable? Related: should ROADMAP
   P07/P08/P09 (hand-written penalty function, simulated annealing, CP-SAT oracle) become Timefold
   constraints and solver config, keeping a brute-force oracle only for ≤ 6 shifts?
