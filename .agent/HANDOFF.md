# Handoff — P09 PostgreSQL persistence (company Claude sandbox → local Windows)

Date: 2026-11-27. Status: **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox.
Nothing was built, tested, run or committed; no git or docker command was run. P08 is DONE (see
STATE.md).

## Scope

- **Local database:** `platform/compose/postgres.yml`.
  - postgres only, `postgres:19.0` (VERSIONS.md), `127.0.0.1:5432:5432`.
  - `POSTGRES_*` from the repo-root `.env` via `--env-file`.
  - Named volume `rota-pgdata`, `pg_isready` healthcheck, `name: rota`, container `rota-postgres`,
    no `version:` key.
- **Secrets:** `.env.example` holds the three keys with a placeholder password. `.env` was already
  gitignored and stays local.
- **Module `persistence`** (`dev.rota.persistence`, depends on application + domain):
  - `RosterEntity` with ordered element collections `AssignmentRow`, `UnassignedSeatRow` and
    `ChangeRow`.
  - Spring Data `RosterJpaRepository`, with `nextval('roster_id_seq')`.
  - `RosterMapper` (application `Roster` ↔ entity).
  - `PostgresRosterRepository` implements the application `RosterRepository` port, with one
    `TransactionTemplate` transaction per call.
  - `PersistenceConfiguration` (`@Profile("postgres")`) wires HikariCP, Flyway, the
    EntityManagerFactory, `JpaTransactionManager` and `@EnableJpaRepositories` by hand, with no
    Boot persistence auto-configuration (QUESTIONS.md P2).
  - Flyway `db/migration`: V1 `roster` (+ `roster_id_seq`), V2 `assignment`, V3 `unassigned_seat`,
    V4 `roster_change`.
- **Bootstrap:**
  - `RotaConfiguration` imports `PersistenceConfiguration`, and `InMemoryRosterRepository` is now
    `@Profile("!postgres")`.
  - `application-postgres.properties` sets `rota.datasource.*` from the `POSTGRES_*` environment
    variables.
  - Default profile: unchanged behaviour, no DataSource.
- **Scripts:**
  - `dev-up.ps1`: compose up, then wait for `healthy`.
  - `dev-down.ps1`: compose down without `-v`; the volume is kept.
  - `run-api-postgres.ps1`: dot-sources `env.ps1`, loads `.env` into its own process, builds,
    then runs the jar with `--spring.profiles.active=postgres`.
- **Tests:**
  - `persistence/PostgresRosterRepositoryTest` (Testcontainers `postgres:19.0`, 7 tests):
    - Flyway tables exist;
    - id sequence;
    - solve and repair round trips equal the original (order, nulls, pinned seats);
    - replace on the same id;
    - unknown id;
    - a multi-line reason is rejected.
  - `persistence/PersistenceArchitectureTest` (2).
  - `bootstrap/PostgresProfileApiTest` (Testcontainers, 3): the repository is Postgres; solve
    mini stores 14 assignment rows and GET returns identical JSON; the repair change list is
    stored.
  - `bootstrap/DefaultProfileWiringTest` (2): in-memory, no DataSource.
  - ArchUnit:
    - `domain/ArchitectureTest` gains `domainMustNotDependOnJpa`;
    - `application/ApplicationArchitectureTest` gains `applicationMustNotDependOnJpa` and now also
      forbids `dev.rota.persistence`.
- **`scripts/verify_p09.ps1`:** checks layout, compose rules, versions against VERSIONS.md
  (compose tag, Flyway, Testcontainers, test image constants), secrets (`.env` ignored and not
  tracked), the docker daemon and `docker compose config --quiet`. Then it runs build and test
  and checks the reports (persistence, wiring, archunit). Registered as `p09`. It never starts or
  stops the compose stack.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
# Docker Desktop must be running (Testcontainers)
pwsh -NoProfile -File scripts/verify.ps1 p06
pwsh -NoProfile -File scripts/verify.ps1 p08   # ports 8080/3000 free; default profile unchanged
pwsh -NoProfile -File scripts/verify.ps1 p09   # first run pulls postgres:19.0
```

Then:

1. Record the exact outputs and exit codes in `.agent/STATE.md`.
2. Optional manual check:
   - Copy `.env.example` to `.env` (set a password; never commit it).
   - Run `dev-up.ps1`, then `run-api-postgres.ps1`, then
     `POST /api/rosters/solve?fixture=mini`.
   - Stop and restart the API; `GET /api/rosters/<id>` still returns the roster.
   - Run `dev-down.ps1`.
3. Failures:
   - Mechanical fixes are allowed:
     - a missing Boot-managed version (QUESTIONS.md P1);
     - a moved import;
     - script plumbing;
     - compose flag quoting.
   - These go back to Opus:
     - the schema, mapping or test expectations;
     - Boot auto-configuring a DataSource in the default profile (P2);
     - Flyway refusing PostgreSQL 19 (P3);
     - a Testcontainers 2.0 API break (P5).
4. Mark P09 DONE only if `verify p09` exits 0 and p06/p08 stay green.

## Expected result (predicted, not observed)

| Check | Prediction |
| --- | --- |
| p06 | Green; `RotaConfiguration` change is invisible in the default profile |
| p08 | Green; the API under the default profile is still in-memory |
| p09 layout, compose, versions, secrets, docker, config | OK |
| p09 build, test | OK; the Testcontainers tests add about 10-30 s (two postgres containers, Flyway, Spring contexts) |
| p09 persistence, wiring, archunit | OK |

## Known risks

- **Boot 4 auto-configuration** is assumed modular (P2). `DefaultProfileWiringTest` is the guard.
- **Flyway 13.13.0 with PostgreSQL 19.0** compatibility and the **postgres 19 data path** (P3, P4).
- **Testcontainers 2.0.5** core API (P5); only `GenericContainer` is used to avoid the renamed
  modules.
- **Reasons and pinned seat ids are newline-joined text** (Q2): fine for the explainer's
  one-line reasons, and enforced on write.
- **ROADMAP P13** (SQLite persistence) is now covered by P09 and needs re-scoping. STACK.md's
  module list does not name `persistence` yet (Q4).

## Changed files

Modified:

- `pom.xml`: module `persistence`
- `bootstrap/pom.xml`: `rota-persistence`; test `testcontainers` 2.0.5
- `bootstrap/src/main/java/dev/rota/bootstrap/RotaConfiguration.java`: `@Import(PersistenceConfiguration)`,
  in-memory repository `@Profile("!postgres")`
- `domain/src/test/java/dev/rota/domain/ArchitectureTest.java`: JPA rule
- `application/src/test/java/dev/rota/application/ApplicationArchitectureTest.java`: JPA rule, persistence package
- `docs/plan/ROADMAP.md`: P09 row and section with the reason for the change; note on P13
- `scripts/verify.ps1`: register `p09`
- `.agent/STATE.md`, `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`

New:

- `platform/compose/postgres.yml`, `.env.example`
- `persistence/pom.xml`
- `persistence/src/main/java/dev/rota/persistence/`: `RosterEntity.java`, `AssignmentRow.java`,
  `UnassignedSeatRow.java`, `ChangeRow.java`, `RosterJpaRepository.java`, `RosterMapper.java`,
  `PostgresRosterRepository.java`, `PersistenceConfiguration.java`
- `persistence/src/main/resources/db/migration/`: `V1__create_roster.sql`, `V2__create_assignment.sql`,
  `V3__create_unassigned_seat.sql`, `V4__create_roster_change.sql`
- `persistence/src/test/java/dev/rota/persistence/`: `PostgresRosterRepositoryTest.java`,
  `PersistenceArchitectureTest.java`
- `bootstrap/src/main/resources/application-postgres.properties`
- `bootstrap/src/test/java/dev/rota/bootstrap/`: `PostgresProfileApiTest.java`, `DefaultProfileWiringTest.java`
- `scripts/dev-up.ps1`, `scripts/dev-down.ps1`, `scripts/run-api-postgres.ps1`, `scripts/verify_p09.ps1`

Not changed: domain and application main code, solver, web, e2e, fixtures, VERSIONS.md,
STACK.md, AGENTS.md, `.gitignore` (already ignores `.env`), `run-api.ps1`, earlier verify scripts.
