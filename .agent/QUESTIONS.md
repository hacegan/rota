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

## P06 — REST API v1 (2026-11-25)

No new Timefold API. The bootstrap adapter only calls the solver module's own public classes
(`RosterSolver(Duration, Duration)`, `RosterSolver.solve`, `RosterRepairer(RosterSolver)`,
`RosterRepairer.repair`, `SolveResult`, `RepairResult`), all proven by the green P03-P05 runs.

### J. Spring Boot 4.1.2: UNVERIFIED (no sandbox build; confirm on the local run)

- **J1 Jackson major version.** Boot 4 is expected to ship Jackson 3 (`tools.jackson.*`), not
  Jackson 2 (`com.fasterxml.jackson.*`). Isolation:
  - No code imports Jackson.
  - The DTOs are plain records with String/long/boolean/List fields only.
  - Times are pre-formatted ISO strings.
  - Fixtures are parsed by `FixtureJson` (a copy of the test `MiniJson`), not by Jackson.

  Assumption: Spring MVC serializes records via their accessors, and `null` fields are written as
  JSON `null` (the default inclusion). If `changes[].before`/`after` nulls are omitted instead, the
  contract (required, nullable) and a future LEFT_EMPTY test need adjusting; current tests do not
  hit a null.
- **J2 Starter name.** `bootstrap/pom.xml` uses `spring-boot-starter-webmvc` (the Boot 4 name). If
  Maven cannot resolve it, the mechanical fix is `spring-boot-starter-web`. That is the same
  dependency set, so local opencode may make that change.
- **J3 Test setup.** `RosterApiTest` uses `@SpringBootTest` plus
  `MockMvcBuilders.webAppContextSetup(WebApplicationContext)` from spring-test.
  `@AutoConfigureMockMvc` is not used, because its package and module changed in Boot 4.

  Assumptions:
  - `spring-boot-starter-test` still brings spring-test, JUnit Jupiter, Hamcrest and json-path.
  - `@SpringBootTest` with the default `webEnvironment` gives a `WebApplicationContext` when
    webmvc is on the classpath.
- **J4 JUnit version mix.** domain, solver and application pin `junit-jupiter` 6.1.3. bootstrap
  takes JUnit from the Boot parent (`spring-boot-starter-test`). If surefire reports a JUnit
  Platform version clash, the mechanical fix is to align bootstrap on 6.1.3.

### K. P06 assumptions and decisions

- **K1 ROADMAP P06 was "Scheduler v1 (greedy)".** It was replaced by the REST API because Timefold
  already produces complete legal rosters. P07 "Scoring" and P08 "Search (beats greedy)" still
  describe a hand-written engine and probably need re-scoping by Samet too. Not changed here.
- **K2 Fixture reading is duplicated.** The main code has `bootstrap/FixtureFiles` + `FixtureJson`;
  the tests have `solver/src/test/.../fixture/FixtureLoader` + `MiniJson`.
  - Main code cannot use test classes.
  - The test copy sits 8 folders deep, which the company sandbox's file bridge cannot stage, so it
    was left untouched.
  - The later ingestion phase (H8) should replace both copies with one production reader.
- **K3 Fixture allowlist.** Solve accepts `mini` and `tight`; repair accepts `sick`. This is checked
  in the use cases (`SolveRoster.FIXTURES`, `RepairSickCall.FIXTURES`). `FixtureFiles` also rejects
  any name outside `[a-z0-9][a-z0-9-]*` and any path leaving `fixtures/`.
- **K4 Fixtures folder.** `rota.fixtures-dir` if set. Otherwise the first `fixtures/` found walking
  up from the working directory, which covers surefire in `bootstrap/` and the repo root.
  `run-api.ps1` passes it explicitly.
- **K5 Solve is synchronous.** A request blocks for the solver run: about 3-30 s, with the same
  termination as the solver tests (PT30S spent, PT3S unimproved). An async job API is a later phase.
- **K6 In-memory store.** Ids are `r1`, `r2`, … per process. Rosters are lost on restart; P13
  adds persistence.
- **K7 Status codes.** Solve and repair return **201 Created** with `Location: /api/rosters/{id}`.
  Errors use a simple `{error, message}` body, not problem+json; problem+json stays in P14.
- **K8 `feasible` = hard score 0.** Empty seats (medium < 0) do not make a roster infeasible.
  Tight is "feasible" with 3 unassigned seats, which are listed with reasons.

## P07 — Calendar demo UI (2026-11-27)

### L. Frontend toolchain: UNVERIFIED (no sandbox install or build; confirm on the local run)

- **L1 TypeScript 7.0.2 with Next.js 16.4.6.** TypeScript 7 is the native (Go) compiler. Two risks:
  - Whether its `tsc` accepts this `tsconfig.json` unchanged. Only long-standing options are used:
    `target ES2022`, `module esnext`, `moduleResolution bundler`, `strict`, `jsx preserve`, the
    `next` plugin. No `baseUrl`/`paths`, `esModuleInterop` or `downlevelIteration`.
  - Whether Next's own build-time type check can drive TypeScript 7. Isolation:
    `next.config.mjs` sets `typescript.ignoreBuildErrors: true`, and type checking runs as its own
    verify step (`npm run typecheck`) before `next build`. Next still needs the `typescript`
    package installed and may rewrite `tsconfig.json` on first build (`jsx`, `include`); such a
    rewrite is expected, not an error.

  Fallback if `tsc` 7 or `next build` refuses: Samet records a TypeScript 5.x/6.x GA version in
  VERSIONS.md and local changes the one pin in `web/package.json`. That is a mechanical change.
- **L2 `next typegen`.** The typecheck script runs `next typegen && tsc --noEmit`. `next typegen`
  writes `next-env.d.ts` (and the route types) without a full build, so `tsc` knows the CSS-import
  and Next types on a clean checkout. `next-env.d.ts` is gitignored because Next regenerates it.
  If `next typegen` does not exist in 16.4.6, the mechanical fix is to swap the order in
  `verify_p07.ps1` (build first, which also writes `next-env.d.ts`, then `tsc --noEmit`).
- **L3 `@types/react-dom` is not in the allowed list.** The app never imports `react-dom`
  directly (App Router renders it), so no types for it are needed. If Next's setup check insists
  on `@types/react-dom`, that is a new dependency: ask Samet, don't add it silently.
- **L4 package-lock.json.** It is not written by the sandbox. The first `npm install` on local
  creates `web/package-lock.json`; commit it from company Windows.

### M. P07 assumptions and decisions

- **M1 ROADMAP P07 was "Scoring".** It was replaced because the Timefold score (P03/P04) is that
  scoring function and the API already returns it. P08 "Search (beats greedy)" still describes a
  hand-written engine (K1); not changed here.
- **M2 CORS added to the backend** (`WebCorsConfiguration`, `rota.cors.allowed-origins`, default
  `http://localhost:3000`, GET/POST on `/api/**`, `Location` exposed). Without it the browser blocks
  every call from the Next dev server (port 3000) to the API (port 8080). The alternative was a
  Next rewrite proxy. It was not chosen because the task asks for a browser-side
  `NEXT_PUBLIC_API_BASE`. Covered by `CorsTest` (allowed origin passes, other origin 403).
- **M3 Columns come from the API only.** The API returns employee ids, not names, and no employee
  list. Columns are every employee with a seat plus every employee who lost a seat in a repair.
  An employee with no seat at all does not appear. A names/employees endpoint would be an API
  change (P14).
- **M4 Days use the local date of the seat's start, in the roster's zone.** The API already formats
  times in that zone, so the UI reads the date and time from the string prefix. There is no
  browser-timezone conversion. A seat ending after midnight shows "+1".
- **M5 Diff marking.** Changes come from the API's `changes` list.
  - In the new employee's cell, a seat with a change is "changed" (highlighted, note "was E2").
  - In the old employee's cell, the same seat is shown "removed" (struck through, note "now E3").
  - A whole cell is highlighted when it holds either.
  - LEFT_EMPTY seats appear in the Unassigned column and the change list.
  - Pinned seats show "pinned".
- **M6 No JS unit tests.** Only the six packages are allowed, so there is no test runner. The pure
  functions in `web/lib` are ready for one; adding it is a dependency decision for Samet.
- **M7 One request at a time.** Buttons are disabled while a solve runs (3-30 s, K5); a new click
  after it ends aborts nothing. Unmounting aborts the in-flight request.

## P08 — Browser E2E demo smoke (2026-11-27)

### N. Playwright toolchain: UNVERIFIED (no sandbox install or run; confirm on the local run)

- **N1 `@playwright/test` 1.65.1 config API.** The config uses long-standing options:
  `defineConfig`, `devices["Desktop Chrome"]`, `webServer` as an array with `command`, `cwd`, `url`,
  `timeout`, `reuseExistingServer`, `env`, `stdout`, `stderr`, and `expect.timeout`.
  - Playwright transpiles `playwright.config.ts` and the spec itself; no `typescript` package is
    needed in `e2e/`.
  - `__dirname` assumes the CommonJS loading Playwright uses when `package.json` has no
    `"type": "module"`.
- **N2 webServer commands on Windows.** Playwright runs each command through the shell:
  - API: `pwsh -NoProfile -File scripts/run-api.ps1 -SkipBuild -Port 8080` (cwd: repo root);
  - Web: `npm run build && npm run start -- --port 3000` (cwd `web/`).

  Playwright stops both when the run ends. Assumption: on Windows it kills the whole process tree,
  including the `java` child of `pwsh`. If a `java` process is left on port 8080, the next run's
  port check fails clearly. Stop it by hand (that is not a repo change).
- **N3 Chromium download.** `npx playwright install chromium` downloads into the user's
  Playwright cache (`%LOCALAPPDATA%\ms-playwright`), outside the repo. A proxy or offline machine
  makes this step fail. That is an environment problem, not a code one.

### O. P08 assumptions and decisions

- **O1 ROADMAP P08 was "Search (beats greedy)".** It was replaced because Timefold is the search
  engine and P06 removed the greedy baseline (K1, M1).
- **O2 Separate `e2e/` package.** It is separate rather than a devDependency in `web/`, so:
  - the P07-verified `web/package.json` (exactly six packages, checked by `verify_p07.ps1`) stays
    unchanged;
  - the browser tooling stays out of the app.
- **O3 Property rename `rota.cors.allowed-origins` → `rota.web.origin`.** This follows the P08
  instruction. Same default (`http://localhost:3000`), same behaviour, and a comma-separated list
  is still accepted. Anyone who set the old property in a local override must rename it; the
  repo has no other use of it.
- **O4 Tests use the page's existing classes and ARIA labels**: `table.week`, `.entry-assigned`,
  `.entry-empty`, `.entry-changed`, `.entry-removed`, `td.cell.changed`, the regions "Score",
  "Unassigned seats" and "Changes", and the button names. No `data-testid` was added, so the web
  app is unchanged apart from one error-hint string.
- **O5 Expected numbers come from the solver/API tests:**
  - mini: 14 seats, all assigned (P03);
  - tight: 3 unassigned (P03);
  - sick: exactly one change, S06#0 E2 → E3 (P05/P06), which gives 2 highlighted cells.

  If the solver ever finds a different equally good repair, test 3 fails. That is a solver
  question for Opus, not a test to loosen.
- **O6 `CI=1` in `verify_p08.ps1`.** Playwright then always starts fresh servers and never reuses
  a stale API or web app. Running `npx playwright test` by hand reuses running ones.
- **O7 Screenshot `docs/demo/rota-demo.png`** is full-page at 1440×900 after the sick repair. It
  is not gitignored: commit it from company Windows after a green run.

## P09 — PostgreSQL persistence (2026-11-27)

### P. UNVERIFIED (no sandbox build, no Docker; confirm on the local run)

- **P1 Boot-managed library versions.** Versions not in VERSIONS.md come from the Spring Boot
  4.1.2 parent's dependency management, like `spring-boot-starter-test` before:
  `spring-data-jpa`, `hibernate-core`, `HikariCP`, `org.postgresql:postgresql`.

  Explicit versions from VERSIONS.md: Flyway 13.13.0 (core + database-postgresql) and Testcontainers
  2.0.5. If the parent does not manage one of the four, the build names it. The mechanical fix is
  adding the version Samet records in VERSIONS.md.
- **P2 No Spring Boot persistence auto-configuration, on purpose.** `persistence` depends on plain
  libraries, not `spring-boot-starter-data-jpa`/`-flyway`. `PersistenceConfiguration`
  (`@Profile("postgres")`) builds:
  - a HikariDataSource;
  - Flyway (migrate at startup);
  - a `LocalContainerEntityManagerFactoryBean` with `HibernateJpaVendorAdapter`;
  - a `JpaTransactionManager`;
  - `@EnableJpaRepositories`.

  Assumption: Boot 4's auto-configuration is modular (JDBC/JPA/Flyway auto-config only with their
  `spring-boot-*` modules), so the default profile creates no DataSource even though Hikari,
  Hibernate and Spring Data are on the classpath. `DefaultProfileWiringTest` asserts exactly that.

  If Boot still auto-configures a DataSource in the default profile, the fix is an Opus decision:
  exclude it or restructure. It is not mechanical.
- **P3 Flyway 13.13.0 with PostgreSQL 19.0.** It uses only `Flyway.configure().dataSource(…)
  .locations(…).load().migrate()`, an API stable for many releases.
  - Flyway may warn that PostgreSQL 19 is newer than it was tested with. A warning is fine.
  - A hard "unsupported version" error needs Samet: Flyway or postgres versions in VERSIONS.md.
- **P4 Postgres image data path.** postgres 18+ images moved `PGDATA` under a versioned
  subdirectory of `/var/lib/postgresql`, so the volume `rota-pgdata` is mounted at
  `/var/lib/postgresql`. If the 19.0 image still uses `/var/lib/postgresql/data` directly, data
  still persists (it is inside the mount). Confirm the healthcheck passes with `dev-up.ps1`.
- **P5 Testcontainers 2.0.5 API.** Only core classes are used: `GenericContainer`,
  `DockerImageName`, `Wait.forLogMessage`, `getHost`, `getMappedPort`, and the started-in-static-init
  pattern. They come from the `testcontainers` artifact with no module artifacts, because 2.0
  renamed modules and moved `PostgreSQLContainer`. Spring wiring uses spring-test's
  `@DynamicPropertySource`. Containers are removed by Ryuk at JVM exit.
- **P6 Hibernate mapping.**
  - `Instant` → `timestamptz`.
  - `@ElementCollection` + `@OrderColumn(name = "position")` → collection tables with primary key
    `(roster_id, position)`.
  - Field access on `@Embeddable` row classes.
  - Hibernate never touches the schema (`hibernate.hbm2ddl.auto=none`); Flyway owns it.

  The round-trip tests compare full records, so a mapping mismatch fails loudly.
- **P7 Docker on local Windows.** verify_p09 needs Docker Desktop running: Testcontainers, then
  `docker compose config`. Without Docker, `test` is skipped and reported FAIL, not silently
  passed.

### Q. P09 assumptions and decisions

- **Q1 ROADMAP P09 was "Optimization (CP-SAT/ILP)".** It is not in the stack. P13 "Persistence
  (SQLite)" conflicts with STACK.md (PostgreSQL). Persistence moved to P09; P13 needs re-scoping.
- **Q2 Storage shape.**
  - Four tables (V1-V4) plus `roster_id_seq`; roster ids are `r<seq>`.
  - Reasons and pinned seat ids are stored as newline-separated text, keeping four tables as asked.
    A reason containing a newline is rejected, not split.
  - Order of seats, changes and reasons is preserved.
  - Saving an existing id replaces it.
- **Q3 `rota.datasource.*` properties**, not `spring.datasource.*`, because there is no Boot
  auto-configuration (P2). `application-postgres.properties` reads `POSTGRES_HOST/PORT/DB/USER/PASSWORD`
  from the environment. `run-api-postgres.ps1` loads them from `.env` into its own process only.
- **Q4 STACK.md modules list** does not name `persistence` yet. Adding a module inside the
  existing stack (Spring Data JPA, Flyway, PostgreSQL) is not a stack change, but Samet may want
  STACK.md updated. Not edited here.
- **Q5 Separate test databases.** Testcontainers uses throwaway containers with test-only
  credentials (`rota-test-only`). The dev database (`dev-up.ps1`, volume `rota-pgdata`) is never
  touched by tests or verify.
