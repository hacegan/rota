# fixtures/mini

Small hand-made fixture. D1 success criterion: every shift assigned, 0 hard violations.

## Change log

- 2026-11-25: added one availability window for E3 on Friday 2026-11-27 08:00-14:00
  (Europe/Istanbul). E3 already held the `barista` skill and was available on the weekend;
  without a weekday barista window, E1 had to cover all 5 weekday barista seats (30 h) plus 2
  weekend seats (12 h) = 42 h, exceeding its 40 h cap, so S14 (Sunday 14:00-20:00 cashier)
  could not be staffed. Giving E3 a single Friday barista window lets E3 take S09, reducing E1
  to 24 h on weekdays + 12 h at the weekend = 36 h, which fits under the 40 h cap. No
  constraint, weight, or hard rule was changed; only this availability entry. See
  `.agent/QUESTIONS.md` F1 (resolved).
