// Pure: turns a roster from the API into a week grid (days as rows, employees as columns) and
// marks the repair diff. No React, no fetch, so it can be unit-tested on its own.

import { addDays, compareIds, localDate } from "./format";
import type { Change, Roster, UnassignedSeat } from "./types";

/**
 * One shift seat shown in a cell.
 * - "assigned": unchanged seat.
 * - "changed":  the employee got this seat in the repair (highlighted).
 * - "removed":  the employee lost this seat in the repair (shown struck through in their column).
 */
export type EntryKind = "assigned" | "changed" | "removed";

export interface CellEntry {
  seatId: string;
  start: string;
  end: string;
  skill: string;
  kind: EntryKind;
  pinned: boolean;
  /** For changed/removed: "was E2", "now E3", "now empty". */
  note: string | null;
}

export interface WeekGrid {
  /** Every local date from the first to the last seat, ascending, gaps filled. */
  days: string[];
  /** Employee ids in natural order: everyone with a seat, plus everyone who lost one. */
  employees: string[];
  /** Key: cellKey(day, employeeId). Entries sorted by start time. */
  cells: Record<string, CellEntry[]>;
  /** Empty seats per local date, sorted by start time. */
  unassignedByDay: Record<string, UnassignedSeat[]>;
  /** Number of cells that contain a changed or removed entry. */
  changedCellCount: number;
}

export function cellKey(day: string, employeeId: string): string {
  return `${day}|${employeeId}`;
}

interface SeatTime {
  start: string;
  end: string;
  skill: string;
}

export function buildWeekGrid(roster: Roster): WeekGrid {
  const changes = new Map<string, Change>();
  for (const change of roster.changes) {
    changes.set(change.seatId, change);
  }
  const pinned = new Set(roster.pinnedSeatIds);
  const seatTimes = new Map<string, SeatTime>();
  const cells: Record<string, CellEntry[]> = {};
  const employees = new Set<string>();
  const dates = new Set<string>();

  const add = (day: string, employeeId: string, entry: CellEntry): void => {
    const key = cellKey(day, employeeId);
    (cells[key] ??= []).push(entry);
  };

  for (const a of roster.assignments) {
    seatTimes.set(a.seatId, { start: a.start, end: a.end, skill: a.skill });
    const day = localDate(a.start);
    dates.add(day);
    employees.add(a.employeeId);
    const change = changes.get(a.seatId);
    const changed = change !== undefined && change.before !== a.employeeId;
    add(day, a.employeeId, {
      seatId: a.seatId,
      start: a.start,
      end: a.end,
      skill: a.skill,
      kind: changed ? "changed" : "assigned",
      pinned: pinned.has(a.seatId),
      note: changed ? `was ${change.before ?? "empty"}` : null,
    });
  }

  const unassignedByDay: Record<string, UnassignedSeat[]> = {};
  for (const u of roster.unassigned) {
    seatTimes.set(u.seatId, { start: u.start, end: u.end, skill: u.skill });
    const day = localDate(u.start);
    dates.add(day);
    (unassignedByDay[day] ??= []).push(u);
  }

  for (const change of roster.changes) {
    if (change.before === null || change.before === change.after) {
      continue;
    }
    const seat = seatTimes.get(change.seatId);
    if (seat === undefined) {
      continue; // the seat is neither assigned nor unassigned: nothing to place it on
    }
    const day = localDate(seat.start);
    employees.add(change.before);
    add(day, change.before, {
      seatId: change.seatId,
      start: seat.start,
      end: seat.end,
      skill: seat.skill,
      kind: "removed",
      pinned: pinned.has(change.seatId),
      note: `now ${change.after ?? "empty"}`,
    });
  }

  const byStart = (x: { start: string }, y: { start: string }): number =>
    x.start < y.start ? -1 : x.start > y.start ? 1 : 0;
  for (const entries of Object.values(cells)) {
    entries.sort(byStart);
  }
  for (const seats of Object.values(unassignedByDay)) {
    seats.sort(byStart);
  }

  const changedCellCount = Object.values(cells).filter((entries) =>
    entries.some((e) => e.kind !== "assigned"),
  ).length;

  return {
    days: fillDays([...dates]),
    employees: [...employees].sort(compareIds),
    cells,
    unassignedByDay,
    changedCellCount,
  };
}

/** Sorted dates from the earliest to the latest, including days with no seats. */
export function fillDays(dates: string[]): string[] {
  if (dates.length === 0) {
    return [];
  }
  const sorted = [...dates].sort();
  const last = sorted[sorted.length - 1];
  const days: string[] = [];
  for (let day = sorted[0]; day <= last; day = addDays(day, 1)) {
    days.push(day);
  }
  return days;
}

/** True when a cell holds a changed or removed entry (used to highlight it). */
export function isChangedCell(entries: CellEntry[] | undefined): boolean {
  return entries !== undefined && entries.some((e) => e.kind !== "assigned");
}
