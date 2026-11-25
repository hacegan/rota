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
| P05 | Ingestion | Parse JSON/CSV employees, shifts, availability | Fixture parse tests pass |
| P06 | Scheduler v1 | Greedy assignment | Mini fixture fully assigned, 0 hard violations |
| P07 | Scoring | Penalty/cost function | Score improves on known cases |
| P08 | Search | Local search / simulated annealing | Beats greedy baseline on mini fixture |
| P09 | Optimization (optional) | CP-SAT / ILP exact solve | Matches brute force on small fixture |
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

## P05 — Ingestion

**Goal:** Load input data from disk.

**Deliverables**

- Readers for the fixture formats (JSON/CSV) into the domain model.
- Validation of required fields; clear parse errors.

**Verify**

```powershell
pwsh -File scripts/test.ps1
```

Expected: mini fixture round-trips; malformed fixtures produce readable errors.

---

## P06 — Scheduler v1

**Goal:** Produce a complete, legal assignment.

**Deliverables**

- Greedy assignment: sort shifts, assign best-fit available qualified employee.
- Hard-constraint checks during assignment.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p06
```

Expected: mini fixture fully assigned with zero hard violations.

---

## P07 — Scoring

**Goal:** One number that says how good a schedule is.

**Deliverables**

- Penalty function combining soft-constraint weights (fairness, preferences, cost).
- Deterministic and pure.

**Verify**

```powershell
pwsh -File scripts/test.ps1
```

Expected: known-better schedule scores strictly lower than known-worse on every case.

---

## P08 — Search

**Goal:** Improve on greedy.

**Deliverables**

- Local search over assignments (swap / move / replace) with simulated annealing or hill
  climbing; budgeted iterations.
- Seedable RNG for reproducibility.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p08
```

Expected: final score <= greedy score on the mini fixture; hard violations stay at zero.

---

## P09 — Optimization (optional)

**Goal:** Exact optimum on small instances.

**Deliverables**

- CP-SAT or ILP model of hard constraints and weighted soft penalties.
- Used only as a test oracle, not the default runtime path.

**Verify**

```powershell
pwsh -File scripts/verify.ps1 p09
```

Expected: matches brute-force optimum on a tiny fixture (<= 6 shifts).

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
