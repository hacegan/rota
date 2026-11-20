# ADR 0001 — Coverage (H6) as a medium level

## Status

Accepted (D2, 2026-11-20).

## Context

`REQUIREMENTS.md` originally listed H6 (headcount/coverage) as a **hard** constraint: every
shift must be fully staffed. The mini fixture cannot satisfy that: barista seats need 42 h of
barista time but E1's cap is 40 h, and weekend cashier seats cannot be covered without
breaking the 11 h rest rule. At least 3 seats (1080 min) must stay empty. If H6 is hard, the
solver cannot return a legal roster at all; it would trade a rest violation for coverage, or
leave hard > 0, which is unpublishable.

## Decision

H6 coverage is **medium**, not hard. Hard = legal/safety/skill/availability (H1-H5). Coverage
is best-effort; gaps are reported (FR8). Score = HardMediumSoft.

## Consequences

- A legal roster with empty seats is acceptable and publishable; medium counts the gap.
- The solver never breaks a hard rule to improve coverage.
- `verify` / reporting lists unassigned seats with reasons.
- P06 success criteria becomes "hard 0, medium -1080" on the (infeasible) tight fixture and
  "every shift assigned, 0 hard violations" on mini.

## Rejected alternative

H6 as hard: makes the mini fixture infeasible and the solver unable to produce a publishable
roster; also conflates legality with coverage, so a rest violation could be traded for an
empty seat.
