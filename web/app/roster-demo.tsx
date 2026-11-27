"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { ACTIONS, apiBase, runAction, type DemoAction } from "../lib/api";
import { dayLabel, describeChange, formatScore, localDate, timeRange } from "../lib/format";
import { buildWeekGrid, cellKey, isChangedCell, type CellEntry, type WeekGrid } from "../lib/grid";
import type { Roster, UnassignedSeat } from "../lib/types";

export default function RosterDemo() {
  const [roster, setRoster] = useState<Roster | null>(null);
  const [pending, setPending] = useState<DemoAction | null>(null);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef<AbortController | null>(null);

  useEffect(() => () => inFlight.current?.abort(), []);

  const grid = useMemo(() => (roster ? buildWeekGrid(roster) : null), [roster]);

  async function run(action: DemoAction): Promise<void> {
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;
    setPending(action);
    setError(null);
    setRoster(null);
    try {
      setRoster(await runAction(action, controller.signal));
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === "AbortError") {
        return;
      }
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      if (inFlight.current === controller) {
        inFlight.current = null;
        setPending(null);
      }
    }
  }

  return (
    <main className="page">
      <header className="header">
        <h1>Rota — calendar demo</h1>
        <p className="muted">
          API: <code>{apiBase()}</code>. Every result is computed by the solver; nothing is mocked.
        </p>
      </header>

      <div className="actions" role="group" aria-label="Scenarios">
        {ACTIONS.map(({ action, label }) => (
          <button key={action} type="button" onClick={() => void run(action)} disabled={pending !== null}>
            {pending === action ? `${label}…` : label}
          </button>
        ))}
      </div>

      {pending !== null && (
        <p className="status" role="status" aria-live="polite">
          Solving… the solver runs for a few seconds (up to 30 s).
        </p>
      )}

      {error !== null && (
        <div className="error" role="alert">
          <strong>Request failed.</strong> {error}
        </div>
      )}

      {roster === null && pending === null && error === null && (
        <p className="muted">Pick a scenario above.</p>
      )}

      {roster !== null && grid !== null && (
        <div className="layout">
          <section className="grid-section" aria-label="Week grid">
            <p className="muted">
              Roster <code>{roster.id}</code> · fixture <strong>{roster.fixture}</strong> ·{" "}
              {roster.operation === "REPAIR" ? "sick repair" : "solve"} · times in {roster.zone}
              {roster.operation === "REPAIR" && ` · ${grid.changedCellCount} changed cell(s) highlighted`}
            </p>
            <WeekTable grid={grid} />
          </section>
          <aside className="panel">
            <ScorePanel roster={roster} />
            {roster.operation === "REPAIR" && <ChangeList roster={roster} />}
            <UnassignedList seats={roster.unassigned} />
          </aside>
        </div>
      )}
    </main>
  );
}

function WeekTable({ grid }: { grid: WeekGrid }) {
  return (
    <div className="table-wrap">
      <table className="week">
        <thead>
          <tr>
            <th scope="col">Day</th>
            {grid.employees.map((employee) => (
              <th scope="col" key={employee}>
                {employee}
              </th>
            ))}
            <th scope="col">Unassigned</th>
          </tr>
        </thead>
        <tbody>
          {grid.days.map((day) => (
            <tr key={day}>
              <th scope="row">{dayLabel(day)}</th>
              {grid.employees.map((employee) => {
                const entries = grid.cells[cellKey(day, employee)];
                return (
                  <td key={employee} className={isChangedCell(entries) ? "cell changed" : "cell"}>
                    {entries?.map((entry) => <Entry key={`${entry.seatId}-${entry.kind}`} entry={entry} />)}
                  </td>
                );
              })}
              <td className="cell">
                {grid.unassignedByDay[day]?.map((seat) => (
                  <div key={seat.seatId} className="entry entry-empty">
                    <span className="time">{timeRange(seat.start, seat.end)}</span>
                    <span>{seat.skill}</span>
                    <span className="seat">{seat.seatId}</span>
                  </div>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Entry({ entry }: { entry: CellEntry }) {
  return (
    <div className={`entry entry-${entry.kind}`} title={entry.pinned ? "Pinned: started before the sick call" : undefined}>
      <span className="time">{timeRange(entry.start, entry.end)}</span>
      <span>{entry.skill}</span>
      <span className="seat">
        {entry.seatId}
        {entry.pinned && " · pinned"}
      </span>
      {entry.note !== null && <span className="note">{entry.note}</span>}
    </div>
  );
}

function ScorePanel({ roster }: { roster: Roster }) {
  const { score } = roster;
  return (
    <section aria-label="Score">
      <h2>Score</h2>
      <dl className="score">
        <dt>Hard</dt>
        <dd className={score.hard < 0 ? "bad" : undefined}>{score.hard}</dd>
        <dt>Medium</dt>
        <dd className={score.medium < 0 ? "warn" : undefined}>{score.medium}</dd>
        <dt>Soft</dt>
        <dd>{score.soft}</dd>
      </dl>
      <p className={score.feasible ? "ok" : "bad"}>
        {score.feasible ? "Feasible: no hard rule broken." : "Infeasible: hard rules broken."}
      </p>
      <p className="muted small">{formatScore(score)}</p>
    </section>
  );
}

function ChangeList({ roster }: { roster: Roster }) {
  return (
    <section aria-label="Changes">
      <h2>Changes ({roster.changes.length})</h2>
      {roster.changes.length === 0 ? (
        <p className="muted">No seat changed.</p>
      ) : (
        <ul className="changes">
          {roster.changes.map((change) => (
            <li key={change.seatId}>{describeChange(change)}</li>
          ))}
        </ul>
      )}
      {roster.pinnedSeatIds.length > 0 && (
        <p className="muted small">{roster.pinnedSeatIds.length} seat(s) pinned (started before the sick call).</p>
      )}
    </section>
  );
}

function UnassignedList({ seats }: { seats: UnassignedSeat[] }) {
  return (
    <section aria-label="Unassigned seats">
      <h2>Unassigned ({seats.length})</h2>
      {seats.length === 0 ? (
        <p className="muted">Every seat is filled.</p>
      ) : (
        <ul className="unassigned">
          {seats.map((seat) => (
            <li key={seat.seatId}>
              <strong>{seat.seatId}</strong> {dayLabel(localDate(seat.start))} {timeRange(seat.start, seat.end)}{" "}
              {seat.skill}
              <ul>
                {seat.reasons.map((reason) => (
                  <li key={reason}>{reason}</li>
                ))}
              </ul>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
