// JSON shapes of the Rota API (docs/contracts/openapi/rota.yaml). Keep in step with the contract.

export type Operation = "SOLVE" | "REPAIR";

export interface ScoreSummary {
  hard: number;
  medium: number;
  soft: number;
  feasible: boolean;
}

/** Times are ISO-8601 offset date-times in the roster's zone, e.g. "2026-11-23T08:00+03:00". */
export interface Assignment {
  seatId: string;
  start: string;
  end: string;
  skill: string;
  employeeId: string;
}

export interface UnassignedSeat {
  seatId: string;
  start: string;
  end: string;
  skill: string;
  reasons: string[];
}

export type ChangeKind = "REASSIGNED" | "LEFT_EMPTY" | "FILLED";

/** before/after are null for an empty seat. */
export interface Change {
  seatId: string;
  before: string | null;
  after: string | null;
  kind: ChangeKind;
}

export interface Roster {
  id: string;
  fixture: string;
  operation: Operation;
  zone: string;
  score: ScoreSummary;
  assignments: Assignment[];
  unassigned: UnassignedSeat[];
  changes: Change[];
  pinnedSeatIds: string[];
}

export interface ApiErrorBody {
  error: string;
  message: string;
}
