# Handoff — P08 browser E2E demo smoke (company Claude sandbox → local Windows)

Date: 2026-11-27. Status: **CODE_WRITTEN_UNVERIFIED**. Written by the company Claude sandbox.
Nothing was installed, built, run or committed; no git, npm or Playwright command was run. P07 is
DONE (see STATE.md).

## Scope

- **API CORS:** the web origin is now set by `rota.web.origin` (default `http://localhost:3000`,
  comma-separated list accepted). It replaces P07's `rota.cors.allowed-origins`, with the same
  behaviour.
  - Existing `CorsTest` covers the default origin.
  - New `WebOriginPropertyTest` checks that a configured origin passes and the default is then
    refused.
  - The rota.yaml note and the web error hint use the new name.
- **`e2e/`:** a separate npm package whose only dependency is `@playwright/test` 1.65.1
  (VERSIONS.md). `web/package.json` is unchanged.
  - `playwright.config.ts`: Chromium only, 1440×900, one worker, 120 s per test (solves take
    3-30 s). It starts:
    - the API: `pwsh -NoProfile -File scripts/run-api.ps1 -SkipBuild -Port 8080`, waits for
      `/api/health`;
    - the web app: `npm run build && npm run start -- --port 3000` in `web/`, with
      `NEXT_PUBLIC_API_BASE=http://localhost:8080`.

    Both stop after the run. Without `CI`, servers already running are reused.
  - `tests/demo.spec.ts`:
    1. Solve mini: the grid renders, 14 `.entry-assigned` cells, no empty seat, score "Feasible"
       with hard 0.
    2. Solve tight: the panel shows "Unassigned (3)", each seat with at least one
       `E<n>: <reason>` line, and 3 empty seats in the grid.
    3. Sick repair: 2 highlighted cells, `S06#0` "was E2" in E3's cell and "now E3" in E2's cell,
       and the change list "Changes (1)" with `S06#0: E2 → E3 (reassigned)`. Then a full-page
       screenshot to `docs/demo/rota-demo.png`.
- **`scripts/verify_p08.ps1`:**
  - Checks layout, the exact `@playwright/test` pin against VERSIONS.md, node/npm/npx, and that
    ports 8080 and 3000 are free.
  - Then runs `scripts/build.ps1` (API jar), `npm install` in `web/` and in `e2e/`,
    `npx playwright install chromium`, and `npx playwright test` with `CI=1`.
  - Finally checks that the screenshot was written by this run.
  - The header notes that the first run downloads packages and Chromium. Registered as `p08`.
- **Not in scope:** DB, Docker, Kafka, other browsers, visual diffing, CI pipeline.

## What local must run

```powershell
git pull --rebase origin main        # after company Windows commits/pushes these files
# stop any API on 8080 / web app on 3000 first
pwsh -NoProfile -File scripts/verify.ps1 p06   # CorsTest + WebOriginPropertyTest
pwsh -NoProfile -File scripts/verify.ps1 p07
pwsh -NoProfile -File scripts/verify.ps1 p08   # first run downloads npm packages + Chromium
```

Then:

1. Record the exact outputs and exit codes in `.agent/STATE.md`.
2. Open `docs/demo/rota-demo.png`; it should show the sick repair with S06#0 highlighted. Commit
   it and `e2e/package-lock.json` from company Windows.
3. Failures:
   - Toolchain or plumbing problems (paths, flags, a Playwright option renamed in 1.65.1,
     webServer command quoting on Windows) are mechanical fixes for local opencode.
   - Changing test expectations, selectors' meaning or the solver goes back to Opus
     (QUESTIONS.md O5).
4. Mark P08 DONE only if `verify p08` exits 0 and p06/p07 stay green.

## Expected result (predicted, not observed)

| Check | Prediction |
| --- | --- |
| p06 | Green; `CorsTest` (3) and `WebOriginPropertyTest` (2) pass |
| p07 | Green; `web/package.json` unchanged |
| p08 layout, versions, tools, ports | OK (ports fail if an API or web app is still running) |
| p08 build, web-npm, e2e-npm, browser | OK; the first run downloads Chromium (about 150-200 MB) |
| p08 e2e | 3 passed; each test takes about 5-35 s, and the web build adds about 10-30 s at start |
| p08 screenshot | `docs/demo/rota-demo.png` written |

## Known risks

- **Playwright 1.65.1 options and Windows process cleanup are untried** (QUESTIONS.md N1/N2). If a
  `java` process survives on 8080, stop it by hand; the next port check reports it.
- **Test 3 assumes the solver's repair is exactly S06#0 E2 → E3** (as the P05/P06 tests assert).
- **Chromium download** needs network or proxy access (N3).
- **The property rename** breaks any private override that still uses `rota.cors.allowed-origins`
  (O3).

## Changed files

Modified:

- `bootstrap/src/main/java/dev/rota/bootstrap/WebCorsConfiguration.java`: property `rota.web.origin`
- `bootstrap/src/main/resources/application.properties`: `rota.web.origin`
- `web/lib/api.ts`: error hint names `rota.web.origin`
- `docs/contracts/openapi/rota.yaml`: CORS note names `rota.web.origin`
- `docs/plan/ROADMAP.md`: P08 row and section, with the reason for the change
- `scripts/verify.ps1`: register `p08`
- `.agent/STATE.md`, `.agent/HANDOFF.md`, `.agent/QUESTIONS.md`

New:

- `e2e/package.json`, `e2e/.gitignore`, `e2e/playwright.config.ts`, `e2e/tests/demo.spec.ts`
- `bootstrap/src/test/java/dev/rota/bootstrap/WebOriginPropertyTest.java`
- `scripts/verify_p08.ps1`

Generated by the local run (not written here): `docs/demo/rota-demo.png`, `e2e/package-lock.json`.

Not changed: domain, solver, application, the web UI components, `web/package.json`, fixtures,
VERSIONS.md, STACK.md, AGENTS.md, `run-api.ps1`, `verify_p06.ps1`, `verify_p07.ps1`.
