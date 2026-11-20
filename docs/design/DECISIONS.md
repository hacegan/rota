# Design Decisions (Samet, 2026-11-20)

Recorded verbatim from the user's instructions. These resolve the open questions in
`docs/design/CONSTRAINT-MODEL.md` §7.

## D1 — Mini must be feasible

- Add E3 (barista skill, weekend availability).
- Keep the current infeasible version as `fixtures/tight/` unchanged.
- Success criteria: mini = every shift assigned, 0 hard violations.
- tight = 0 hard violations + unassigned shifts listed with reasons. Never silently fail.

## D2 — H6 coverage is MEDIUM, not hard

- Hard = legal, safety, skill, availability.
- Coverage is best-effort; gaps are reported.
- Score = HardMediumSoft.
- Update REQUIREMENTS.

## D3 — Definitions

- night = any shift overlapping 00:00-06:00 local.
- weekend = Sat+Sun.
- Timezone = site configuration, Europe/Istanbul in fixtures.
- Availability = datetime intervals that may cross midnight, not time-of-day.

## D4 — New inputs

- preferences.json (PREFER/AVOID)
- previous-roster.json (pinning + stability)
- contractHoursPerWeek on each employee.
- Pay rates and cost are out of scope.

## D5 — New module name is `solver`

- Consistent with domain/application/bootstrap.
- Timefold never enters domain.
- Extend the ArchUnit rule.
- Update STACK.md.

## D6 — Timefold API verification

- Every Timefold API must be verified against the real jar. Guessing is forbidden.
- Cannot verify -> leave UNVERIFIED and add to `.agent/QUESTIONS.md`.
