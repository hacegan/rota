# fixtures/sick

P05 sick-call repair fixture. Derived from `fixtures/mini` (week 2026-W48, Europe/Istanbul).
Success criterion: the repair is fully assigned with 0 hard violations.

## Files

| File | Content |
| --- | --- |
| `employees.json`, `shifts.csv`, `preferences.json` | Same as `fixtures/mini`. |
| `availability.json` | `fixtures/mini` plus **one window: E3 Wednesday 2026-11-25 14:00-20:00**. |
| `published-roster.json` | The roster already published to staff (below). `{shift_id, seat, employee_id}`. |
| `sick-call.json` | E2 sick for the whole local day 2026-11-25, reported at 07:00 that day. `date` means the whole local day; `start`/`end` (ISO offset) can be used instead. `reported_at` is "now" for the repair. |

No `previous-roster.json`: during repair the published roster is the stability reference.

## Published roster (legal under mini and sick)

| Day | Barista 08-14 | Cashier 14-20 |
| --- | --- | --- |
| Mon 23 | S01 E1 | S02 E2 |
| Tue 24 | S03 E1 | S04 E2 |
| Wed 25 | S05 E1 | **S06 E2** |
| Thu 26 | S07 E1 | S08 E2 |
| Fri 27 | S09 E3 | S10 E2 |
| Sat 28 | S11 E1 | S12 E3 |
| Sun 29 | S13 E3 | S14 E1 |

Hours: E1 36 h (max 40), E2 30 h (max 30), E3 18 h (max 24). Tightest rest: E3 Sat 20:00 to
Sun 08:00 = 12 h (min 11 h).

## Why E3 gets a Wednesday window

Without it the repair is impossible. The sick call frees **S06** (Wed 14-20 cashier):

- **E2:** sick.
- **E1:** works S05 (Wed 08-14), so S06 would leave 0 h rest (H4). S05 cannot move, because only
  E1 is a barista with Wednesday availability.
- **E3:** in mini, not available on Wednesday (H2).

With E3 available Wed 14-20, E3 covers S06: 18 + 6 = 24 h, exactly the cap, and the rest
to S09 (Fri 08:00) is 36 h.

Expected repair: **one change, S06 E2 → E3**. S01-S04 are pinned (they started before the report
at Wed 07:00). Every other seat keeps its published employee, because each change would cost the S3
stability penalty (1000) and no single move gains that much in other soft rules.

## The infeasible twin

`fixtures/mini` with this same published roster and sick call is the "impossible repair" case:
S06 stays empty, hard 0, and the reasons say why (see `SickRepairTest`). `fixtures/tight` is unchanged.
