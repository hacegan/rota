package dev.rota.solver;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import org.junit.jupiter.api.Test;

import static dev.rota.solver.Seats.assign;
import static dev.rota.solver.Seats.employee;
import static dev.rota.solver.Seats.schedule;
import static dev.rota.solver.Seats.seat;

/**
 * One ConstraintVerifier test per P03 constraint (CONSTRAINT-MODEL.md §6), plus a guard showing
 * the legal case scores 0. Penalties are in minutes. Fixture week 2026-W48, zone Europe/Istanbul.
 *
 * <p>UNVERIFIED (see .agent/QUESTIONS.md, A5): {@code ConstraintVerifier.build}, {@code verifyThat},
 * {@code given}, {@code penalizesBy}. The class itself is CONFIRMED in core.
 */
class RotaConstraintProviderTest {

    private final ConstraintVerifier<RotaConstraintProvider, RotaSchedule> verifier =
            ConstraintVerifier.build(new RotaConstraintProvider(), RotaSchedule.class, ShiftSeat.class);

    // --- H1 qualification ---

    @Test
    void h1_penalizesUnqualifiedEmployee() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s01 = assign(mini, "S01", "E2"); // barista seat, E2 is cashier only
        verifier.verifyThat(RotaConstraintProvider::qualification).given(s01).penalizesBy(360);
    }

    @Test
    void h1_acceptsQualifiedEmployee() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s01 = assign(mini, "S01", "E1");
        verifier.verifyThat(RotaConstraintProvider::qualification).given(s01).penalizesBy(0);
    }

    // --- H2 availability ---

    @Test
    void h2_penalizesSeatOutsideAvailability() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s12 = assign(mini, "S12", "E2"); // Saturday, E2 has no Saturday window
        verifier.verifyThat(RotaConstraintProvider::availability).given(s12).penalizesBy(360);
    }

    @Test
    void h2_penalizesSeatOutsideAvailabilityInTightDayFormat() {
        RotaSchedule tight = schedule("tight"); // availability as day + HH:mm
        ShiftSeat s12 = assign(tight, "S12", "E2");
        verifier.verifyThat(RotaConstraintProvider::availability).given(s12).penalizesBy(360);
    }

    @Test
    void h2_acceptsSeatInsideAvailability() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s12 = assign(mini, "S12", "E3");
        verifier.verifyThat(RotaConstraintProvider::availability).given(s12).penalizesBy(0);
    }

    // --- H3 no overlap ---

    @Test
    void h3_penalizesOverlap() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s01 = assign(mini, "S01", "E1");
        // Variant built in code: S01 with headcount 2, both seats to E1.
        ShiftSeat s01Second = new ShiftSeat(s01.getShiftId(), 1, s01.getTime(), s01.getRequiredSkill(),
                s01.getWeekKey());
        s01Second.setEmployee(employee(mini, "E1"));
        verifier.verifyThat(RotaConstraintProvider::noOverlap).given(s01, s01Second).penalizesBy(360);
    }

    @Test
    void h3_acceptsBackToBack() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s11 = assign(mini, "S11", "E1");
        ShiftSeat s12 = assign(mini, "S12", "E1");
        verifier.verifyThat(RotaConstraintProvider::noOverlap).given(s11, s12).penalizesBy(0);
    }

    // --- H4 rest ---

    @Test
    void h4_penalizesBackToBackWithoutRest() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s11 = assign(mini, "S11", "E1"); // Sat 08-14
        ShiftSeat s12 = assign(mini, "S12", "E1"); // Sat 14-20, 0h rest
        verifier.verifyThat(RotaConstraintProvider::rest).given(s11, s12).penalizesBy(660);
    }

    @Test
    void h4_acceptsTwelveHoursRest() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat s12 = assign(mini, "S12", "E1"); // Sat 14-20
        ShiftSeat s13 = assign(mini, "S13", "E1"); // Sun 08-14, 12h rest
        verifier.verifyThat(RotaConstraintProvider::rest).given(s12, s13).penalizesBy(0);
    }

    // --- H5 max hours per week ---

    @Test
    void h5_penalizesWeeklyExcess() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat[] barista = {
                assign(mini, "S01", "E1"), assign(mini, "S03", "E1"), assign(mini, "S05", "E1"),
                assign(mini, "S07", "E1"), assign(mini, "S09", "E1"), assign(mini, "S11", "E1"),
                assign(mini, "S13", "E1")
        };
        // 7 x 6h = 42h > E1 max 40h in 2026-W48 -> 120 min.
        verifier.verifyThat(RotaConstraintProvider::maxHoursPerWeek).given((Object[]) barista).penalizesBy(120);
    }

    @Test
    void h5_acceptsExactlyMax() {
        RotaSchedule mini = schedule("mini");
        ShiftSeat[] cashier = {
                assign(mini, "S02", "E2"), assign(mini, "S04", "E2"), assign(mini, "S06", "E2"),
                assign(mini, "S08", "E2"), assign(mini, "S10", "E2")
        };
        // 5 x 6h = 30h = E2 max 30h.
        verifier.verifyThat(RotaConstraintProvider::maxHoursPerWeek).given((Object[]) cashier).penalizesBy(0);
    }

    // --- M6 coverage (medium) ---

    @Test
    void m6_penalizesEmptySeat() {
        RotaSchedule tight = schedule("tight");
        ShiftSeat s13 = assign(tight, "S13", "E1");
        ShiftSeat s14 = seat(tight, "S14"); // left empty
        verifier.verifyThat(RotaConstraintProvider::coverage).given(s13, s14).penalizesBy(360);
    }

    @Test
    void m6_acceptsFilledSeat() {
        RotaSchedule tight = schedule("tight");
        ShiftSeat s14 = assign(tight, "S14", "E1");
        verifier.verifyThat(RotaConstraintProvider::coverage).given(s14).penalizesBy(0);
    }
}
