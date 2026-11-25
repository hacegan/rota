package dev.rota.solver;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

/**
 * P03 constraints: hard H1-H5 and medium M6 coverage. No soft constraints yet.
 *
 * <p>CONFIRMED in TIMEFOLD-VERIFIED.md: {@code forEach}, {@code forEachIncludingUnassigned},
 * {@code forEachUniquePair}, {@code Joiners.equal}, {@code ConstraintCollectors.sum}.
 * UNVERIFIED and isolated here (see .agent/QUESTIONS.md, A1-A3): the {@code ConstraintProvider}
 * interface itself, {@code filter}, {@code groupBy}, {@code penalize}, {@code asConstraint}.
 *
 * <p>Every hard constraint filters out empty seats itself, so results are correct whether or not
 * {@code forEach} already skips unassigned entities (UNVERIFIED #3, probed by
 * {@code TimefoldBehaviourTest}).
 */
public final class RotaConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                qualification(factory),
                availability(factory),
                noOverlap(factory),
                rest(factory),
                maxHoursPerWeek(factory),
                coverage(factory)
        };
    }

    Constraint qualification(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null && HardRules.qualificationPenalty(seat) > 0)
                .penalize(Scores.ONE_HARD, HardRules::qualificationPenalty)
                .asConstraint("H1 qualification");
    }

    Constraint availability(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null && HardRules.availabilityPenalty(seat) > 0)
                .penalize(Scores.ONE_HARD, HardRules::availabilityPenalty)
                .asConstraint("H2 availability");
    }

    Constraint noOverlap(ConstraintFactory factory) {
        return factory.forEachUniquePair(ShiftSeat.class, Joiners.equal(ShiftSeat::getEmployee))
                .filter((a, b) -> a.getEmployee() != null && HardRules.overlapMinutes(a, b) > 0)
                .penalize(Scores.ONE_HARD, HardRules::overlapMinutes)
                .asConstraint("H3 no overlap");
    }

    Constraint rest(ConstraintFactory factory) {
        return factory.forEachUniquePair(ShiftSeat.class, Joiners.equal(ShiftSeat::getEmployee))
                .filter((a, b) -> a.getEmployee() != null && HardRules.restDeficitMinutes(a, b) > 0)
                .penalize(Scores.ONE_HARD, HardRules::restDeficitMinutes)
                .asConstraint("H4 rest");
    }

    /** H5: per (employee, ISO week of seat start) assigned minutes must not exceed the cap. */
    Constraint maxHoursPerWeek(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null)
                .groupBy(seat -> new EmployeeWeek(seat.getEmployee(), seat.getWeekKey()),
                        ConstraintCollectors.sum(ShiftSeat::getDurationMinutes))
                .filter((employeeWeek, minutes) -> excessMinutes(employeeWeek, minutes) > 0)
                .penalize(Scores.ONE_HARD, RotaConstraintProvider::excessMinutes)
                .asConstraint("H5 max hours per week");
    }

    /**
     * H5 weigher: assigned minutes above the weekly cap. Types proven by the 2.8.0 compiler (local
     * verify 2026-11-25): {@code groupBy(..., sum(...))} yields {@code Long}, and
     * {@code BiConstraintStream.penalize(Score, ToLongBiFunction)} takes a long weight. No narrowing.
     */
    static long excessMinutes(EmployeeWeek employeeWeek, Long minutes) {
        return minutes - employeeWeek.employee().getMaxMinutesPerWeek();
    }

    /** M6: every empty seat costs its duration in minutes on the medium level (D2, ADR 0001). */
    Constraint coverage(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() == null)
                .penalize(Scores.ONE_MEDIUM, ShiftSeat::getDurationMinutes)
                .asConstraint("M6 coverage");
    }

    record EmployeeWeek(PlanningEmployee employee, int weekKey) {
    }
}
