package dev.rota.solver;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import dev.rota.domain.TimeRange;
import dev.rota.solver.fixture.FixtureLoader;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static dev.rota.solver.Seats.assign;
import static dev.rota.solver.Seats.employee;
import static dev.rota.solver.Seats.schedule;
import static dev.rota.solver.Seats.seat;

/**
 * One ConstraintVerifier test per P04 soft rule, plus a guard showing the good case. Match
 * weights are natural units (seats, minutes, scaled squared counts); the soft weight itself lives
 * in {@link SoftWeights} and is not part of {@code penalizesBy}.
 *
 * <p>mini (2026-W48, Europe/Istanbul): E1 max 40 h / contract 40 h, E2 30/30, E3 24/24.
 * preferences.json: E1 PREFER 2026-11-23, E2 AVOID 2026-11-27. previous-roster.json: S02→E2, S04→E2.
 *
 * <p>Fairness values are exact because they use our own formula (CONSTRAINT-MODEL §4), not
 * {@code loadBalance}: count² × 1000 / max hours, integer division.
 */
class SoftConstraintProviderTest {

    private static final ZoneId ZONE = FixtureLoader.DEFAULT_ZONE;

    private final ConstraintVerifier<RotaConstraintProvider, RotaSchedule> verifier =
            ConstraintVerifier.build(new RotaConstraintProvider(), RotaSchedule.class, ShiftSeat.class);

    // --- S1 preferences ---

    @Test
    void s1_penalizesAvoidedDate() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s10 = assign(mini, "S10", "E2"); // Fri 2026-11-27, E2 AVOID
        verifier.verifyThat(RotaConstraintProvider::avoidedDate).given(s10).penalizesBy(1);
    }

    @Test
    void s1_acceptsSeatOnOtherDate() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s08 = assign(mini, "S08", "E2"); // Thu
        verifier.verifyThat(RotaConstraintProvider::avoidedDate).given(s08).penalizesBy(0);
    }

    @Test
    void s1_penalizesMissedPreferredDate() {
        RotaSchedule mini = schedule("mini");
        PlanningPreference e1Monday = mini.getPreferences().get(0); // E1 PREFER 2026-11-23
        ShiftSeat s02 = assign(mini, "S02", "E2"); // Monday seat, not E1
        verifier.verifyThat(RotaConstraintProvider::preferredDateMissed).given(e1Monday, s02).penalizesBy(1);
    }

    @Test
    void s1_acceptsMetPreferredDate() {
        RotaSchedule mini = schedule("mini");
        PlanningPreference e1Monday = mini.getPreferences().get(0);
        ShiftSeat s02 = assign(mini, "S02", "E1");
        verifier.verifyThat(RotaConstraintProvider::preferredDateMissed).given(e1Monday, s02).penalizesBy(0);
    }

    // --- S2n night fairness ---

    @Test
    void s2n_balancedScoresBetterThanSkewed() {
        RotaSchedule mini = schedule("mini");
        PlanningEmployee e1 = employee(mini, "E1"); // max 40 h
        PlanningEmployee e2 = employee(mini, "E2"); // max 30 h

        // Skewed onto E1: 2² × 1000 / 40 = 100.
        verifier.verifyThat(RotaConstraintProvider::nightFairness)
                .given(night("N1", "2026-11-23", e1), night("N2", "2026-11-24", e1))
                .penalizesBy(100);
        // Skewed onto E2: 2² × 1000 / 30 = 133.
        verifier.verifyThat(RotaConstraintProvider::nightFairness)
                .given(night("N1", "2026-11-23", e2), night("N2", "2026-11-24", e2))
                .penalizesBy(133);
        // Balanced: 1000 / 40 + 1000 / 30 = 25 + 33 = 58, lower than both skewed rosters.
        verifier.verifyThat(RotaConstraintProvider::nightFairness)
                .given(night("N1", "2026-11-23", e1), night("N2", "2026-11-24", e2))
                .penalizesBy(58);
    }

    @Test
    void s2n_ignoresDaySeats() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s01 = assign(mini, "S01", "E1"); // 08:00-14:00
        verifier.verifyThat(RotaConstraintProvider::nightFairness).given(s01).penalizesBy(0);
    }

    // --- S2w weekend fairness ---

    @Test
    void s2w_countsWeekendsNotSeats() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s11 = assign(mini, "S11", "E3"); // Sat
        ShiftSeat s13 = assign(mini, "S13", "E3"); // Sun, same weekend -> 1 weekend
        verifier.verifyThat(RotaConstraintProvider::weekendFairness).given(s11, s13).penalizesBy(41); // 1000 / 24
    }

    @Test
    void s2w_splittingOneWeekendCostsMoreThanKeepingItWhole() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s11 = assign(mini, "S11", "E1");
        ShiftSeat s13 = assign(mini, "S13", "E3");
        verifier.verifyThat(RotaConstraintProvider::weekendFairness).given(s11, s13).penalizesBy(66); // 25 + 41
    }

    // --- S3 stability ---

    @Test
    void s3_penalizesChangeFromPreviousRoster() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s02 = assign(mini, "S02", "E1"); // previous: E2
        verifier.verifyThat(RotaConstraintProvider::stability).given(s02).penalizesBy(1);
    }

    @Test
    void s3_acceptsUnchangedSeat() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s02 = assign(mini, "S02", "E2");
        verifier.verifyThat(RotaConstraintProvider::stability).given(s02).penalizesBy(0);
    }

    @Test
    void s3_penalizesEmptiedSeat() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s04 = seat(mini, "S04"); // previous: E2, now empty
        verifier.verifyThat(RotaConstraintProvider::stability).given(s04).penalizesBy(1);
    }

    // --- S4 contract hours ---

    @Test
    void s4_penalizesUnderContract() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat[] cashier = {
                assign(mini, "S02", "E2"), assign(mini, "S04", "E2"),
                assign(mini, "S06", "E2"), assign(mini, "S08", "E2")
        };
        // 24 h vs contract 30 h -> 360 min.
        verifier.verifyThat(RotaConstraintProvider::contractDeviation).given((Object[]) cashier).penalizesBy(360);
    }

    @Test
    void s4_acceptsExactContract() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat[] cashier = {
                assign(mini, "S02", "E2"), assign(mini, "S04", "E2"), assign(mini, "S06", "E2"),
                assign(mini, "S08", "E2"), assign(mini, "S10", "E2")
        };
        verifier.verifyThat(RotaConstraintProvider::contractDeviation).given((Object[]) cashier).penalizesBy(0);
    }

    @Test
    void s4_penalizesOverContract() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat[] e3 = {
                assign(mini, "S09", "E3"), assign(mini, "S11", "E3"), assign(mini, "S12", "E3"),
                assign(mini, "S13", "E3"), assign(mini, "S14", "E3")
        };
        // 30 h vs contract 24 h -> 360 min (hard rules are not part of this check).
        verifier.verifyThat(RotaConstraintProvider::contractDeviation).given((Object[]) e3).penalizesBy(360);
    }

    @Test
    void s4_penalizesEmployeeWithNoSeat() {
        RotaSchedule mini = schedule("mini");
        // E3 contract 24 h, horizon 1 week, no seat -> 1440 min.
        verifier.verifyThat(RotaConstraintProvider::contractUnused).given(employee(mini, "E3")).penalizesBy(1440);
    }

    @Test
    void s4_acceptsEmployeeWithASeat() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s12 = assign(mini, "S12", "E3");
        verifier.verifyThat(RotaConstraintProvider::contractUnused).given(employee(mini, "E3"), s12).penalizesBy(0);
    }

    /** Night seat 00:00-06:00 local on {@code day}, assigned to {@code employee}. */
    private static ShiftSeat night(String shiftId, String day, PlanningEmployee employee) {
        LocalDate date = LocalDate.parse(day);
        TimeRange time = new TimeRange(date.atStartOfDay(ZONE).toInstant(),
                date.atTime(PlanningMapper.NIGHT_END).atZone(ZONE).toInstant());
        ShiftSeat seat = new ShiftSeat(shiftId, 0, time, "cashier", PlanningMapper.weekKey(date), date, true, 0, null);
        seat.setEmployee(employee);
        return seat;
    }
}
