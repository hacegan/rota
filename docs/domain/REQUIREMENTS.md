# Rota Requirements

Scope: turn a set of employees, shifts, and availability into a published roster that
respects hard rules and minimizes penalties.

## Functional requirements

- **FR1 Ingest** employees, shifts, and availability from JSON/CSV fixtures.
- **FR2 Skills** match employees to shifts whose required skill they hold.
- **FR3 Assignment** fills every shift up to its required headcount.
- **FR4 Hard constraints** are enforced during assignment and validation.
- **FR5 Scoring** assigns a penalty to a roster for soft-constraint violations.
- **FR6 Search** improves a roster from a seedable starting point.
- **FR7 Publication** exports and publishes the roster (board, CSV/PDF/ICS, email/iCal).

## Non-functional requirements

- **NFR1 Determinism** — a fixed input and seed produce the same roster.
- **NFR2 Latency** — re-plan in seconds on the mini fixture; scale to 100k shifts.
- **NFR3 Offline core** — solving needs no network; publication is opt-in.
- **NFR4 Explainability** — violations are reported in human-readable form.
- **NFR5 Minimal dependencies** — prefer the standard library of the chosen stack.

## Constraint taxonomy

Hard constraints (a roster is invalid if any is violated):

| ID | Constraint | Rule |
| --- | --- | --- |
| H1 | Qualification | The assigned employee holds the skill the shift requires. |
| H2 | Availability | The shift lies inside an availability window for that employee. |
| H3 | No overlap | An employee has no two shifts that overlap in time. |
| H4 | Rest between shifts | At least 11 hours between the end of one shift and the start of the next. |
| H5 | Max hours | Weekly scheduled hours do not exceed the employee's `max_hours_per_week`. |
| H6 | Headcount | Every shift is staffed to its required headcount. |

Soft constraints (each violation adds a weighted penalty):

| ID | Constraint | Rule |
| --- | --- | --- |
| S1 | Preference match | Prefer shifts/days the employee asked for. |
| S2 | Fairness | Balance scheduled hours across employees. |
| S3 | Continuity | Prefer stable shift patterns week over week. |
| S4 | Cost | Minimize overtime and premium-pay hours. |

## Glossary mapping

The terms **shift**, **employee**, **skill**, **availability**, **roster**, **constraint**,
**penalty**, and **publication** are used as defined in `docs/domain/GLOSSARY.md`.
