package dev.rota.solver;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

import java.util.Set;
import java.util.function.Function;

/**
 * Hard H1-H5, medium M6 coverage (P03), soft S1-S4 (P04).
 *
 * <p>API status (TIMEFOLD-VERIFIED.md + green P03 local run): {@code forEach},
 * {@code forEachIncludingUnassigned}, {@code forEachUniquePair}, {@code Joiners.equal(Function)},
 * {@code ConstraintCollectors.sum}, {@code filter}, {@code groupBy(Function, collector)},
 * Uni/Bi {@code penalize}, {@code asConstraint} all compiled and passed.
 *
 * <p>UNVERIFIED and isolated (see .agent/QUESTIONS.md P04 §D): {@code ifNotExists} with two-mapping
 * {@code Joiners.equal(Function, Function)} (only in {@link #preferredDateMissed} and
 * {@link #contractUnused}), {@code ConstraintCollectors.toSet} signature (only in
 * {@link #weekendFairness}), {@code forEach} over a problem fact class (only in {@link #contractUnused}
 * and {@link #preferredDateMissed}).
 *
 * <p>{@code ConstraintCollectors.loadBalance} is NOT used: its exact formula is UNVERIFIED
 * (TIMEFOLD-VERIFIED.md #4). Fairness uses the convex formula of CONSTRAINT-MODEL.md §4 instead.
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
                coverage(factory),
                avoidedDate(factory),
                preferredDateMissed(factory),
                nightFairness(factory),
                weekendFairness(factory),
                stability(factory),
                contractDeviation(factory),
                contractUnused(factory)
        };
    }

    // ---------------------------------------------------------------- hard (P03)

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

    // ---------------------------------------------------------------- medium (P03)

    /** M6: every empty seat costs its duration in minutes on the medium level (D2, ADR 0001). */
    Constraint coverage(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() == null)
                .penalize(Scores.ONE_MEDIUM, ShiftSeat::getDurationMinutes)
                .asConstraint("M6 coverage");
    }

    // ---------------------------------------------------------------- soft (P04)

    /** S1 AVOID: one match per assigned seat on a date its employee asked to avoid. */
    Constraint avoidedDate(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null && seat.getEmployee().avoids(seat.getLocalDate()))
                .penalize(SoftWeights.PREFERENCE)
                .asConstraint("S1 avoided date");
    }

    /** S1 PREFER: one match per preferred date on which the employee has no seat. UNVERIFIED API (ifNotExists). */
    Constraint preferredDateMissed(ConstraintFactory factory) {
        return factory.forEach(PlanningPreference.class)
                .ifNotExists(ShiftSeat.class,
                        Joiners.equal(PlanningPreference::getEmployee, ShiftSeat::getEmployee),
                        Joiners.equal(PlanningPreference::getDate, ShiftSeat::getLocalDate))
                .penalize(SoftWeights.PREFERENCE)
                .asConstraint("S1 preferred date missed");
    }

    /**
     * S2n: per employee, nights² × FAIRNESS_SCALE / max hours (CONSTRAINT-MODEL §4 without the
     * constant −A²/C term). Convex, so moving a night from a heavy to a light employee always helps.
     */
    Constraint nightFairness(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null && seat.isNight())
                .groupBy(ShiftSeat::getEmployee, ConstraintCollectors.sum(ShiftSeat::getNightUnits))
                .penalize(SoftWeights.FAIRNESS, RotaConstraintProvider::nightFairness)
                .asConstraint("S2n night fairness");
    }

    static long nightFairness(PlanningEmployee employee, Long nights) {
        return fairness(nights, employee);
    }

    /**
     * S2w: per employee, (distinct weekends worked)² × FAIRNESS_SCALE / max hours. Counting weekends,
     * not seats, keeps one person's weekend whole instead of breaking two (CONSTRAINT-MODEL §4.5).
     * UNVERIFIED API: {@code ConstraintCollectors.toSet} signature.
     */
    Constraint weekendFairness(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null && seat.getWeekendKey() != 0)
                .groupBy(ShiftSeat::getEmployee, ConstraintCollectors.toSet(ShiftSeat::getWeekendKey))
                .penalize(SoftWeights.FAIRNESS, RotaConstraintProvider::weekendFairness)
                .asConstraint("S2w weekend fairness");
    }

    static long weekendFairness(PlanningEmployee employee, Set<Integer> weekends) {
        return fairness(weekends.size(), employee);
    }

    static long fairness(long count, PlanningEmployee employee) {
        long maxHours = Math.max(1, employee.getMaxMinutesPerWeek() / 60);
        return Math.multiplyExact(Math.multiplyExact(count, count), SoftWeights.FAIRNESS_SCALE) / maxHours;
    }

    /** S3: one match per seat whose employee (or emptiness) differs from previous-roster.json. */
    Constraint stability(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(ShiftSeat.class)
                .filter(RotaConstraintProvider::changedFromPrevious)
                .penalize(SoftWeights.STABILITY)
                .asConstraint("S3 stability");
    }

    static boolean changedFromPrevious(ShiftSeat seat) {
        String previous = seat.getPreviousEmployeeId();
        return previous != null && (seat.getEmployee() == null || !previous.equals(seat.getEmployee().getId()));
    }

    /** S4: per (employee with a contract, ISO week) |assigned minutes − contract minutes|. */
    Constraint contractDeviation(ConstraintFactory factory) {
        return factory.forEach(ShiftSeat.class)
                .filter(seat -> seat.getEmployee() != null && seat.getEmployee().getContractMinutesPerWeek() > 0)
                .groupBy(seat -> new EmployeeWeek(seat.getEmployee(), seat.getWeekKey()),
                        ConstraintCollectors.sum(ShiftSeat::getDurationMinutes))
                .filter((employeeWeek, minutes) -> contractDeviation(employeeWeek, minutes) > 0)
                .penalize(SoftWeights.CONTRACT, RotaConstraintProvider::contractDeviation)
                .asConstraint("S4 contract hours deviation");
    }

    static long contractDeviation(EmployeeWeek employeeWeek, Long minutes) {
        return Math.abs(minutes - employeeWeek.employee().getContractMinutesPerWeek());
    }

    /**
     * S4 for an employee with a contract and no seat at all: the full contract over the horizon.
     * The grouped stream above cannot see employees with zero seats. UNVERIFIED API (ifNotExists).
     */
    Constraint contractUnused(ConstraintFactory factory) {
        return factory.forEach(PlanningEmployee.class)
                .filter(employee -> employee.getContractMinutesPerWeek() > 0)
                .ifNotExists(ShiftSeat.class, Joiners.equal(Function.identity(), ShiftSeat::getEmployee))
                .penalize(SoftWeights.CONTRACT, PlanningEmployee::getContractMinutesForHorizon)
                .asConstraint("S4 contract hours unused");
    }

    record EmployeeWeek(PlanningEmployee employee, int weekKey) {
    }
}
