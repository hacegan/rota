import { defineConfig, devices } from "@playwright/test";
import * as path from "node:path";

// Starts the real API and the real web app, then runs the browser smoke tests against them.
//   API: scripts/run-api.ps1 -SkipBuild  (needs the bootstrap jar: scripts/build.ps1 first;
//        verify_p08.ps1 does that) on http://localhost:8080
//   Web: next build + next start in web/ on http://localhost:3000 (the API's default
//        rota.web.origin, so CORS allows it)
// Locally an already running API/web on those ports is reused; with CI set (verify_p08.ps1 sets
// it) Playwright starts its own and fails if a port is taken.

const repoRoot = path.resolve(__dirname, "..");
const apiBase = "http://localhost:8080";
const webBase = "http://localhost:3000";
const reuse = !process.env.CI;

export default defineConfig({
  testDir: "./tests",
  // One shared API and a CPU-heavy solver: run tests one after another.
  fullyParallel: false,
  workers: 1,
  retries: 0,
  // Each solve takes 3-30 s (solver termination), plus page load.
  timeout: 120_000,
  expect: { timeout: 60_000 },
  reporter: [["list"]],
  outputDir: "test-results",
  use: {
    baseURL: webBase,
    viewport: { width: 1440, height: 900 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } } }],
  webServer: [
    {
      command: "pwsh -NoProfile -File scripts/run-api.ps1 -SkipBuild -Port 8080",
      cwd: repoRoot,
      url: `${apiBase}/api/health`,
      timeout: 180_000,
      reuseExistingServer: reuse,
      stdout: "ignore",
      stderr: "pipe",
    },
    {
      command: "npm run build && npm run start -- --port 3000",
      cwd: path.join(repoRoot, "web"),
      url: webBase,
      timeout: 300_000,
      reuseExistingServer: reuse,
      env: { NEXT_PUBLIC_API_BASE: apiBase },
      stdout: "ignore",
      stderr: "pipe",
    },
  ],
});
