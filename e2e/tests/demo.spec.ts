import { expect, test, type Page } from "@playwright/test";
import * as path from "node:path";

// Browser smoke tests of the P07 calendar demo against the real API and solver (no mocks).
// Expected outcomes are the ones the solver/API tests already assert (P03, P05, P06):
//   mini  -> 14 seats, all assigned, hard 0
//   tight -> 3 empty seats, each with per-employee reasons
//   sick  -> one change, S06#0 E2 -> E3

const SCREENSHOT = path.resolve(__dirname, "..", "..", "docs", "demo", "rota-demo.png");

async function runScenario(page: Page, label: string): Promise<void> {
  await page.goto("/");
  await page.getByRole("button", { name: label, exact: true }).click();
  // The grid appears only once the solver result has arrived.
  await expect(page.locator("table.week")).toBeVisible();
  await expect(page.locator(".error")).toHaveCount(0);
}

test("solve mini: grid renders with 14 assigned cells and a feasible score", async ({ page }) => {
  await runScenario(page, "Solve mini");

  await expect(page.locator("table.week .entry-assigned")).toHaveCount(14);
  await expect(page.locator("table.week .entry-empty")).toHaveCount(0);

  const score = page.getByRole("region", { name: "Score" });
  await expect(score).toContainText("Feasible: no hard rule broken.");
  await expect(score.locator("dd").first()).toHaveText("0");

  await expect(page.getByRole("region", { name: "Unassigned seats" })).toContainText("Every seat is filled.");
});

test("solve tight: unassigned seats panel shows reasons", async ({ page }) => {
  await runScenario(page, "Solve tight");

  const unassigned = page.getByRole("region", { name: "Unassigned seats" });
  await expect(unassigned.getByRole("heading")).toHaveText("Unassigned (3)");

  const seats = unassigned.locator("ul.unassigned > li");
  await expect(seats).toHaveCount(3);
  for (let i = 0; i < 3; i++) {
    const reasons = seats.nth(i).locator("ul > li");
    await expect(reasons.first()).toBeVisible();
    // Reasons are "<employeeId>: <why not>", e.g. "E1: week total would be 42h > max 40h".
    await expect(reasons.first()).toHaveText(/^E\d+: .+/);
  }

  // The same seats sit in the grid's Unassigned column.
  await expect(page.locator("table.week .entry-empty")).toHaveCount(3);
});

test("sick repair: changed cells highlighted, change list visible, screenshot", async ({ page }) => {
  await runScenario(page, "Sick repair");

  // Both cells of the move are highlighted: E3 gained S06#0, E2 lost it.
  await expect(page.locator("table.week td.cell.changed")).toHaveCount(2);
  await expect(page.locator("table.week .entry-changed")).toHaveCount(1);
  await expect(page.locator("table.week .entry-changed")).toContainText("S06#0");
  await expect(page.locator("table.week .entry-changed")).toContainText("was E2");
  await expect(page.locator("table.week .entry-removed")).toContainText("now E3");

  const changes = page.getByRole("region", { name: "Changes" });
  await expect(changes).toBeVisible();
  await expect(changes.getByRole("heading")).toHaveText("Changes (1)");
  await expect(changes.getByRole("listitem")).toHaveText(["S06#0: E2 → E3 (reassigned)"]);

  await page.screenshot({ path: SCREENSHOT, fullPage: true });
});
