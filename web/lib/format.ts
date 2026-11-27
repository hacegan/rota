// Pure formatting helpers. No Date-with-local-timezone, no locale APIs: the API already writes
// times in the roster's zone, so the text prefix is the local date/time and the output is the
// same on server and browser.

import type { Change, ScoreSummary } from "./types";

const ISO_PREFIX = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/;
const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

function parts(iso: string): RegExpExecArray {
  const match = ISO_PREFIX.exec(iso);
  if (!match) {
    throw new Error(`not an ISO date-time: ${iso}`);
  }
  return match;
}

/** "2026-11-23T08:00+03:00" -> "2026-11-23" (local date in the roster's zone). */
export function localDate(iso: string): string {
  const m = parts(iso);
  return `${m[1]}-${m[2]}-${m[3]}`;
}

/** "2026-11-23T08:00+03:00" -> "08:00". */
export function localTime(iso: string): string {
  const m = parts(iso);
  return `${m[4]}:${m[5]}`;
}

/** "08:00–14:00", or "22:00–06:00 +1" when the seat ends on a later local date. */
export function timeRange(start: string, end: string): string {
  const label = `${localTime(start)}–${localTime(end)}`;
  return localDate(end) > localDate(start) ? `${label} +1` : label;
}

/** "2026-11-23" -> "2026-11-24" (calendar arithmetic in UTC, independent of the browser zone). */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split("-").map(Number);
  const next = new Date(Date.UTC(y, m - 1, d + days));
  const yyyy = String(next.getUTCFullYear()).padStart(4, "0");
  const mm = String(next.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(next.getUTCDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/** "2026-11-23" -> "Mon 23 Nov". */
export function dayLabel(date: string): string {
  const [y, m, d] = date.split("-").map(Number);
  const weekday = new Date(Date.UTC(y, m - 1, d)).getUTCDay();
  return `${WEEKDAYS[weekday]} ${d} ${MONTHS[m - 1]}`;
}

/** "0hard / -1080medium / -25soft". */
export function formatScore(score: ScoreSummary): string {
  return `${score.hard}hard / ${score.medium}medium / ${score.soft}soft`;
}

/** "S06#0" -> "S06". */
export function shiftId(seatId: string): string {
  const hash = seatId.indexOf("#");
  return hash < 0 ? seatId : seatId.slice(0, hash);
}

/** "S06#0: E2 → E3 (reassigned)"; null employee reads "empty". */
export function describeChange(change: Change): string {
  const before = change.before ?? "empty";
  const after = change.after ?? "empty";
  return `${change.seatId}: ${before} → ${after} (${change.kind.toLowerCase().replace("_", " ")})`;
}

/** Natural order for employee ids: E2 before E10. */
export function compareIds(a: string, b: string): number {
  return a.localeCompare(b, "en", { numeric: true });
}
