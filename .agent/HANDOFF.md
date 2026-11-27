# Handoff — P07 calendar demo UI (company Claude sandbox → local Windows)

Date: 2026-11-27. Status: **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox.
Nothing was installed, type-checked, built, run or committed; no git or npm command was run.
P06 is DONE (see STATE.md).

## Scope

- **`web/`:** Next.js App Router + React + TypeScript, one page (`/`).
  - Dependencies are exactly `next` 16.4.6, `react` 19.3.1 and `react-dom` 19.3.1; devDependencies
    `typescript` 7.0.2, `@types/react` 19.3.3 and `@types/node` 26.6.11, all from VERSIONS.md.
  - No UI or state library; plain CSS in `app/globals.css`.
- **Screen:**
  - Buttons "Solve mini", "Solve tight" and "Sick repair" POST to the real API.
  - Week grid: days as rows, employees as columns, shift cells (time, skill, seat id), plus an
    Unassigned column.
  - Repair diff: the cell that gained a seat is highlighted ("was E2"); the cell that lost it is
    highlighted with the seat struck through ("now E3"); a change list sits beside it.
  - Side panel: hard / medium / soft score, a feasible flag, and unassigned seats with every reason.
  - Loading text while the solver runs (3-30 s); an error box with the API's `{error, message}` or
    a "cannot reach the API" hint.
- **API base:** `NEXT_PUBLIC_API_BASE`, default `http://localhost:8080`. It is inlined at build
  time, so a change needs a rebuild or dev restart.
- **Pure logic in `web/lib`:**
  - `grid.ts`: week grid plus diff.
  - `format.ts`: dates, times, score, change text, without timezone or locale APIs.
  - `api.ts`: base URL, paths, error text, plus the one `fetch` call.
  - `types.ts`: the `rota.yaml` shapes.
- **Backend (small, needed for the browser):** `WebCorsConfiguration` allows `/api/**` GET/POST
  from `rota.cors.allowed-origins` (default `http://localhost:3000`) and exposes `Location`.
  `CorsTest` covers it. A one-line CORS note was added to `rota.yaml`. No endpoint, DTO or solver
  change.
- **`scripts/verify_p07.ps1`:**
  - Checks layout and that `web/.gitignore` ignores node_modules and .next.
  - Checks that `package.json` has exactly those six packages at the VERSIONS.md pins.
  - Checks node/npm, then runs `npm install`, `npm run typecheck` (`next typegen && tsc --noEmit`)
    and `npm run build` (`next build`).
  - The header notes that the first run downloads packages.
  - Registered as `p07`.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
pwsh -NoProfile -File scripts/verify.ps1 p06   # includes the new CorsTest
pwsh -NoProfile -File scripts/verify.ps1 p07   # first run downloads npm packages
```

Then:

1. Record the exact outputs and exit codes in `.agent/STATE.md`.
2. Manual walkthrough, recorded in STATE.md:
   - Run `pwsh -NoProfile -File scripts/run-api.ps1`; in a second shell, `cd web; npm run dev`;
     open http://localhost:3000.
   - Solve mini: full grid with an empty Unassigned column and hard 0.
   - Solve tight: 3 unassigned seats with reasons and medium −1080.
   - Sick repair: S06#0 E2 → E3 highlighted in both cells, and 1 change listed.
   - Stop the API and click a button: an error box appears.
3. Toolchain failures: mechanical fixes only (AGENTS.md "Who changes what"). See QUESTIONS.md §L:
   - L1: if TypeScript 7 is refused, Samet picks another GA version in VERSIONS.md first;
   - L2: if `next typegen` is missing, swap to build then `tsc`.

   Adding any package (for example `@types/react-dom`, L3) needs Samet's approval.
4. Commit `web/package-lock.json` created by the first `npm install` (L4).
5. Mark P07 DONE only if `verify p07` exits 0 and p06 stays green.

## Expected result (predicted, not observed)

| Check | Prediction |
| --- | --- |
| p06 | Still green; `CorsTest` (3) passes. Preflight from `http://localhost:3000` returns 200 with Allow-Origin; other origin returns 403 |
| p07 layout, versions, tools | OK |
| p07 install | OK after downloading packages (first run takes several minutes) |
| p07 typecheck | OK, unless TypeScript 7 rejects a `tsconfig` option or `next typegen` is missing (QUESTIONS L1/L2) |
| p07 build | OK; Next may rewrite `tsconfig.json` (`jsx`, `include`) on the first build. That is expected |

## Known risks

- **TypeScript 7.0.2 + Next 16.4.6 is untried** (QUESTIONS.md L1). Next's own type check is off
  (`ignoreBuildErrors`); `tsc` runs separately.
- **No JS unit tests** (M6): the allowed packages include no test runner. `web/lib` is pure so
  tests can be added once a runner is approved.
- **Employee columns come from roster data only** (M3); the API has no employee list or names.
- **The backend gained CORS** (M2). It is a small semantic change in bootstrap, made by Opus,
  scoped to `/api/**` and configurable.
- **ROADMAP P08** still mentions greedy (K1/M1).

## Changed files

Modified:

- `bootstrap/src/main/resources/application.properties`: `rota.cors.allowed-origins`
- `docs/contracts/openapi/rota.yaml`: CORS note in the description
- `docs/plan/ROADMAP.md`: P07 row and section, with the reason for the change
- `scripts/verify.ps1`: register `p07`
- `.agent/STATE.md`, `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`

New:

- `web/package.json`, `web/tsconfig.json`, `web/next.config.mjs`, `web/.gitignore`
- `web/app/layout.tsx`, `web/app/page.tsx`, `web/app/roster-demo.tsx`, `web/app/globals.css`
- `web/lib/types.ts`, `web/lib/format.ts`, `web/lib/grid.ts`, `web/lib/api.ts`
- `bootstrap/src/main/java/dev/rota/bootstrap/WebCorsConfiguration.java`
- `bootstrap/src/test/java/dev/rota/bootstrap/CorsTest.java`
- `scripts/verify_p07.ps1`

Not changed: domain, solver, application, the other bootstrap classes, fixtures, VERSIONS.md,
STACK.md, DECISIONS.md, REQUIREMENTS.md, AGENTS.md, `verify_p06.ps1`.
