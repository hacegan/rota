# Rota Roadmap

Shift scheduling engine, built in 20 phases (P00–P19). Each phase is small, ends with a
verify command, and is checkpointed in `.agent/STATE.md`.

**Rules of the road**

- No phase is done until its verify command has been run and its real output recorded in
  `.agent/STATE.md`.
- Never skip ahead. If a verify fails, fix the current phase before moving on.
- The stack is chosen in P02; every later phase uses that stack.
- Keep each phase minimal. Do not build beyond the deliverables listed.

## Phase index

| Phase | Name | Goal | Verify |
| --- | --- | --- | --- |
| P00 | Bootstrap | Repo hygiene, agent rules, roadmap, preflight, license | `pwsh -File scripts/preflight.ps1` exits 0 |
| P01 | Domain & requirements | Glossary, entities, constraint taxonomy, sample data | Fixtures parse; glossary reviewed |
| P02 | Stack & skeleton | Pick stack, project files, build + test commands | Clean build and empty test run pass |
| P03 | Core domain model | Entities, value objects, invariants | Unit tests pass |
| P04 | Soft constraints & fairness | S1 preferences, S2n/S2w night/weekend fairness, S3 stability, S4 contract hours | `pwsh -NoProfile -File scripts/verify.ps1 p04` exits 0 |
| P05 | Sick-call repair | Repair a published roster after a sick call with minimum changes | `pwsh -NoProfile -File scripts/verify.ps1 p05` exits 0 |
| P06 | REST API v1 | Runnable Spring Boot API around the solver: solve, repair, get roster | `pwsh -NoProfile -File scripts/verify.ps1 p06` exits 0 |
| P07 | Calendar demo UI | Next.js week grid over the P06 API: solve, sick repair diff, score, unassigned | `pwsh -NoProfile -File scripts/verify.ps1 p07` exits 0 |
| P08 | Browser E2E demo smoke | Playwright drives the demo UI against the real API; screenshot | `pwsh -NoProfile -File scripts/verify.ps1 p08` exits 0 |
| P09 | PostgreSQL persistence | Rosters in PostgreSQL (JPA, Flyway) under profile `postgres`; local compose; Testcontainers tests | `pwsh -NoProfile -File scripts/verify.ps1 p09` exits 0 |
| P10 | Validation | Invariant checks and reports | Validator catches seeded errors |
| P11 | CLI | `validate`, `solve`, `export` commands | End-to-end run on fixture |
| P12 | Export | CSV / PDF / ICS roster output | Exports re-parse and validate |
| P13 | Persistence | Repository + SQLite storage | Round-trip tests pass |
| P14 | API | REST endpoints for schedules | Integration tests pass |
| P15 | UI | Schedule board | Smoke test + manual walkthrough |
| P16 | Auth & tenants | Accounts, roles, isolation | Authz tests pass |
| P17 | Notifications | Email / iCal publishing | Template + dry-run tests pass |
| P18 | Scale & performance | Large-dataset benchmarks | Meets recorded thresholds |
| P19 | Observability & release | Logging, metrics, versioning, changelog | Release checklist passes |

---

## P00 — Bootstrap

**Goal:** Repo hygiene and agent workflow; nothing that compiles yet.

**Deliverables**

- `AGENTS.md` — agent rules (no invented versions, in-repo only, ONAY for destructive
  commands, phase-by-phase with verify output, minimal code, PowerShell one-liners,
  checkpoint to `.agent/STATE.md`).
- `docs/plan/ROADMAP.md` — this file.
- `scripts/preflight.ps1` — versions, free RAM, disk budget.
- `.gitignore`, `.gitattributes` (LF), `LICENSE` (Apache-2.0).
- `.agent/STATE.md` — checkpoint.

**Verify**

```powershell
pwsh -File scripts/preflight.ps1
```

Expected: exit code 0, all required checks OK.

---

## P01 — Domain & requirements

**Goal:** Agree on what is being scheduled before writing code.

**Deliverables**

- `docs/domain/GLOSSARY.md` — shift, employee, skill, availability, roster, constraint,
  penalty, publication.
- `docs/domain/REQUIREMENTS.md` — functional + non-functional requirements, hard vs soft
  constraint taxonomy.
- `fixtures/mini/` — small hand-made JSON/CSV input (2 employees, 1 week of shifts).

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify_p01.ps1
```

Expected: fixtures parse with a tiny loader; glossary terms all used in requirements.

---

## P02 — Stack & skeleton

**Goal:** Choose and pin the runtime/stack; create the project skeleton.

**Deliverables**

- Stack decision recorded in `docs/plan/STACK.md` with exact versions from preflight.
- Project file(s), solution layout, `scripts/build.ps1`, `scripts/test.ps1`.
- `scripts/verify.ps1` dispatcher used by later phases.

**Verify**

```powershell
pwsh -File scripts/build.ps1
pwsh -File scripts/test.ps1
```

Expected: clean build; test runner executes zero tests without error.

---

## P03 — Core domain model

**Goal:** Entities and value objects with invariants.

**Deliverables**

- Employee, Shift, Skill, Availability, Schedule, Roster types.
- Invariants enforced at construction (e.g. shift end after start, non-negative counts).

**Verify**

```powershell
pwsh -File scripts/test.ps1
```

Expected: domain unit tests pass, including invariant rejection cases.

---

## P04 — Soft constraints & fairness

**Goal:** Quality rules on top of the P03 hard rules and coverage, in the Timefold `solver` module.

**Scope changed 2026-11-25 (Samet).** The original P04 was "Constraints engine: constraint
interface `Evaluate(schedule) -> violations`, initial hard rule set". P03 already delivered the hard
rules (H1-H5) and coverage (M6) as Timefold constraints with ConstraintVerifier tests, and
Timefold is the binding constraint engine (STACK.md), so a hand-written evaluator would duplicate
it. P04 therefore covers the soft rules. No incremental repair or pinning, no DB, Docker, Spring
or REST.

**Deliverables**

- S1 preferences: PREFER / AVOID dates from `preferences.json`.
- S2n night fairness and S2w weekend fairness (D3 definitions; convex formula from
  CONSTRAINT-MODEL §4, not `loadBalance`).
- S3 stability: penalize changes from `previous-roster.json`.
- S4 contract-hours deviation (`contract_hours_per_week`, D4).
- ConstraintVerifier test per soft rule; solver tests showing balanced beats skewed, preferred beats
  avoided when hard rules allow, contract deviation is penalized, and changing the previous roster
  costs a stability penalty.
- `scripts/verify_p04.ps1`: build + tests + ArchUnit.

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p04
```

Expected: exit code 0; all P03 and P04 tests pass; ArchUnit rules hold.

---

## P05 — Incremental sick-call repair

**Goal:** When someone calls in sick, repair the published roster with as few changes as possible
(CONSTRAINT-MODEL §5, simplified).

**Scope changed 2026-11-25 (Samet).** The original P05 was "Ingestion: readers for the fixture
formats into the domain model; clear parse errors". Repair was moved ahead because it builds
directly on the P03/P04 solver and is core product value. Ingestion is not dropped: fixtures
are already read by the test-only `FixtureLoader`, and production ingestion (validation and
readable parse errors) moves to a later phase, to be scheduled by Samet before P11 CLI.

**Deliverables**

- Input: published roster + sick call (employee; whole local date or datetime range; report time).
- The sick employee loses availability for the sick period from the report onward; their seats in
  that period are freed.
- Batch repair through the existing solver harness, with the published roster as warm start. Seats
  that started before the report are pinned (`@PlanningPin`) and never move. Future seats move only
  if needed; S3 stability (published roster as reference) discourages changes. No live
  `ProblemChange`.
- Output: repaired roster, change list against the published roster, unassigned seats with reasons.
- `fixtures/sick` (from mini, repairable) with README; mini plus the same sick call is the
  impossible-repair case.
- Tests: sick seats reassigned, unaffected seats unchanged, pinned seats never move, repair has no
  more changes than a full re-solve, impossible repair reports reasons with 0 hard. Determinism
  test written but disabled (UNVERIFIED).
- `scripts/verify_p05.ps1`: build + tests + ArchUnit.

**Not in scope:** repair window and urgency-weighted stability (CONSTRAINT-MODEL §5.2/§5.4), manager
locks, DB, Docker, Spring, REST.

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p05
```

Expected: exit code 0; all P03-P05 tests pass; ArchUnit rules hold.

---

## P06 — REST API v1

**Goal:** A runnable REST API around the solver, so the P03-P05 engine can be used over HTTP.

**Scope changed 2026-11-25 (Samet).** The original P06 was "Scheduler v1: greedy assignment with
hard-constraint checks; mini fixture fully assigned, 0 hard violations". The Timefold solver
(P03-P05) already produces complete, legal assignments and the mini fixture is already fully
assigned with hard 0 (P03 verify), so a hand-written greedy scheduler would duplicate the binding
engine (STACK.md) and add nothing. P06 instead exposes the existing engine through Spring Web
(the stack's REST choice). P14 keeps the fuller API (list/create schedules, request validation,
problem+json errors) on top of P13 persistence.

**Deliverables**

- Contract first: `docs/contracts/openapi/rota.yaml`.
- Application use cases `SolveRoster`, `RepairSickCall`, `GetRoster` with pure-Java ports
  (`SchedulingPort`, `FixturePort`, `RosterRepository`); no Spring, no Timefold in `application`.
- Bootstrap Spring Boot app wiring the use cases to a solver adapter, an in-memory roster
  repository, and a fixture adapter over `fixtures/`.
- Endpoints: `GET /api/health`; `POST /api/rosters/solve?fixture=mini|tight`;
  `POST /api/rosters/repair?fixture=sick`; `GET /api/rosters/{id}`. JSON DTOs: roster, score
  summary, unassigned seats with reasons, change list. Unknown fixture 400, unknown id 404.
- `scripts/run-api.ps1`: dot-sources `env.ps1`, starts the bootstrap app.
- Tests: MockMvc integration tests for every endpoint, application unit tests with fakes,
  ArchUnit (domain and application: no Spring, no Timefold).
- `scripts/verify_p06.ps1`: build + tests + MockMvc report + ArchUnit.

**Not in scope:** database, Docker, Kafka, Next.js/UI, auth, uploading problems over HTTP.

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p06
```

Expected: exit code 0; all P03-P06 tests pass; ArchUnit rules hold.

---

## P07 — Calendar demo UI

**Goal:** See the solver's work in a browser: a week roster, a sick-call repair diff, the score
and every empty seat with its reasons, all from the real P06 API.

**Scope changed 2026-11-27 (Samet).** The original P07 was "Scoring: a penalty function combining
soft-constraint weights, deterministic and pure". That already exists: the Timefold
`HardMediumSoftScore` with H1-H5, M6 and S1-S4 (P03/P04) is the score, and the API returns it
(P06). A second hand-written scoring function would duplicate the binding engine (STACK.md). P07
instead adds the frontend demo (Next.js is the stack's frontend). P15 keeps the full schedule board
(manual edits with revalidation, violation highlights).

**Deliverables**

- `web/`: Next.js App Router + React + TypeScript. Dependencies are exactly `next`, `react`,
  `react-dom`, `typescript`, `@types/react` and `@types/node`, at the versions in VERSIONS.md. No
  UI library and no state library; plain CSS.
- Week grid: days as rows, employees as columns, shift cells; empty seats in their own column.
- Buttons "Solve mini", "Solve tight" and "Sick repair" call
  `POST /api/rosters/solve?fixture=…` and `POST /api/rosters/repair?fixture=sick`.
- Sick repair diff: changed cells highlighted (new seat marked "was …", lost seat struck through
  "now …"), plus the change list.
- Panel: score summary (hard / medium / soft, feasible) and unassigned seats with reasons.
- API base URL from `NEXT_PUBLIC_API_BASE`, default `http://localhost:8080`. Loading and error
  states; no fake data.
- Diff and format logic as pure functions in `web/lib` (`grid.ts`, `format.ts`, `api.ts` helpers).
- Backend: CORS for `/api/**` from `rota.cors.allowed-origins` (default `http://localhost:3000`) so
  the browser may call the API; MockMvc `CorsTest`.
- `scripts/verify_p07.ps1`: layout, exact version pins against VERSIONS.md, `npm install`,
  typecheck (`next typegen && tsc --noEmit`), `next build`.

**Not in scope:** manual edits, auth, persistence, a JS test runner (no extra dependencies allowed;
the `web/lib` functions are pure so a runner can be added later), Docker.

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p07
```

Expected: exit code 0. The first run downloads npm packages. The P06 verify also stays green with
the CORS test.

---

## P08 — Browser E2E demo smoke

**Goal:** Prove the whole demo works end to end in a real browser (UI → API → solver) and keep a
screenshot of the sick-call repair.

**Scope changed 2026-11-27 (Samet).** The original P08 was "Search: local search / simulated
annealing with a seedable RNG; beats the greedy baseline on mini". Timefold (P03-P05) already is
the search engine: construction heuristic plus local search, configured in `RosterSolver`. There
is no greedy baseline any more: P06 replaced the greedy scheduler. A hand-written search would
duplicate the binding engine (STACK.md). P08 instead locks in the demo with browser tests.

**Deliverables**

- API CORS for the web origin `http://localhost:3000`, configurable via `rota.web.origin`
  (renames P07's `rota.cors.allowed-origins`). `CorsTest` covers the default origin and
  `WebOriginPropertyTest` covers the property.
- `e2e/`: Playwright with `@playwright/test` at the VERSIONS.md version only, Chromium only.
  `playwright.config.ts` starts the API (`scripts/run-api.ps1 -SkipBuild`) and the web app
  (`next build` + `next start`) automatically.
- Tests:
  1. Solve mini: the grid renders, 14 assigned cells, score feasible.
  2. Solve tight: the unassigned panel shows 3 seats with reasons.
  3. Sick repair: changed cells highlighted (2 cells: E2 lost and E3 gained S06#0) and the
     change list visible.
- Screenshot `docs/demo/rota-demo.png` after the sick repair.
- `scripts/verify_p08.ps1`: build API, `npm install` (web, e2e),
  `npx playwright install chromium`, run E2E, check the screenshot was written.

**Not in scope:** DB, Docker, Kafka, other browsers, visual diffing, CI pipeline.

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p08
```

Expected: exit code 0. The first run downloads npm packages and Chromium. Ports 8080 and 3000
must be free.

---

## P09 — PostgreSQL persistence

**Goal:** Stored rosters survive restarts: PostgreSQL behind the existing application roster port,
without touching domain, application or the default in-memory demo.

**Scope changed 2026-11-27 (Samet).** The original P09 was "Optimization (optional): a CP-SAT /
ILP exact solve as a test oracle". The binding solver is Timefold (STACK.md); CP-SAT/ILP is not
in the stack and would need an ADR. The original P13 "Persistence: repository + SQLite" conflicts
with STACK.md (PostgreSQL, Flyway, Spring Data JPA). Persistence moves here, on the stack's
database. P13 is now covered by P09 and needs re-scoping by Samet.

**Deliverables**

- `platform/compose/postgres.yml`:
  - postgres only, image tag from VERSIONS.md, ports `127.0.0.1:5432:5432`, env from `.env`;
  - named volume `rota-pgdata`, healthcheck, no `version:` key.
- `.env.example` with `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`. `.env` stays gitignored.
- New Maven module `persistence`: JPA entities, a Spring Data repository, and an adapter
  implementing the application `RosterRepository` port. Domain and application stay pure.
- Flyway migrations V1-V4: `roster`, `assignment`, `unassigned_seat`, `roster_change`.
- Bootstrap wires the PostgreSQL adapter only under Spring profile `postgres`. The default profile
  stays in-memory, so the P08 E2E is unaffected.
- Scripts:
  - `scripts/dev-up.ps1`: start the compose stack and wait for health;
  - `scripts/dev-down.ps1`: stop it without `-v`, never deleting volumes;
  - `scripts/run-api-postgres.ps1`: API with profile `postgres`.
- Testcontainers integration tests (real PostgreSQL) for the adapter and for the API under
  profile `postgres`; a default-profile wiring test.
- ArchUnit: domain and application must not depend on Spring, Timefold or JPA.
- `scripts/verify_p09.ps1`: compose checks + `docker compose config` + build + tests + ArchUnit.

**Not in scope:** Docker for the API or web app, Kafka, Redis, employees/shifts tables, data
retention, backups.

**Verify**

```powershell
pwsh -NoProfile -File scripts/verify.ps1 p09
```

Expected: exit code 0 with Docker running. The first run pulls the postgres image.

---

## P10 — Validation

**Goal:** Independent checker for any schedule.

**Deliverables**

- Validator covering all hard constraints plus data integrity (unknown ids, duplicates).
- Human-readable violation report.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p10
```

Expected: catches every seeded error class; passes a known-good schedule.

---

## P11 — CLI

**Goal:** Usable command line.

**Deliverables**

- Commands: `validate <input>`, `solve <input> [--seed N]`, `export <schedule>`.
- Exit codes: 0 ok, 1 validation failure, 2 usage error.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p11
```

Expected: end-to-end run on the mini fixture; exit codes match the contract.

---

## P12 — Export

**Goal:** Get rosters out.

**Deliverables**

- CSV roster, iCal feed, print-friendly/PDF report.
- Export preserves ids and times exactly.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p12
```

Expected: exports re-parse (CSV/ICS) and match the source schedule.

---

## P13 — Persistence

> **Note 2026-11-27:** roster persistence on PostgreSQL (the stack's database, not SQLite) is
> delivered by P09. This phase needs re-scoping by Samet (for example employees/shifts storage).

**Goal:** Save and reload state.

**Deliverables**

- Repository interface + SQLite implementation.
- Migrations/versioned schema.

**Verify**

```powershell
pwsh -File scripts/test.ps1
```

Expected: round-trip tests for employees, shifts, schedules.

---

## P14 — API

**Goal:** HTTP surface for schedules.

**Deliverables**

- REST endpoints: list/create schedules, get roster, trigger solve.
- Request validation and problem+json errors.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p14
```

Expected: integration tests against an in-process server.

---

## P15 — UI

**Goal:** See and tweak the roster.

**Deliverables**

- Schedule board: shifts by day, employee list, violation highlights.
- Manual assignment change that revalidates.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p15
```

Expected: smoke test loads board; manual walkthrough recorded in `.agent/STATE.md`.

---

## P16 — Auth & tenants

**Goal:** Multi-user, isolated data.

**Deliverables**

- Accounts, roles (admin/scheduler/viewer), tenant isolation.
- Authn/authz on API and UI.

**Verify**

```powershell
pwsh -File scripts/test.ps1
```

Expected: authz tests prove cross-tenant access is denied.

---

## P17 — Notifications

**Goal:** Tell people their shifts.

**Deliverables**

- Email templates + iCal publishing.
- Dry-run mode; no real sends without explicit configuration.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p17
```

Expected: template rendering tests pass; dry run produces expected payloads.

---

## P18 — Scale & performance

**Goal:** Hold up on real data.

**Deliverables**

- Benchmarks: 1k / 10k / 100k shifts.
- Recorded thresholds and regression check in verify.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p18
```

Expected: solve time and memory within recorded thresholds.

---

## P19 — Observability & release

**Goal:** Operable and shippable.

**Deliverables**

- Structured logging, basic metrics, health check.
- Versioning policy, `CHANGELOG.md`, release checklist.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p19
```

Expected: release checklist passes; health endpoint responds.
