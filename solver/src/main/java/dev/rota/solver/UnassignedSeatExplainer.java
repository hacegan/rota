package dev.rota.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * FR8 / NFR4: for an empty seat, says per employee which hard rule would break if that employee
 * took it, given the rest of the solved roster. Uses {@link HardRules}, the same arithmetic as
 * the constraints.
 */
final class UnassignedSeatExplainer {

    private UnassignedSeatExplainer() {
    }

    static List<String> explain(ShiftSeat empty, RotaSchedule schedule) {
        List<String> reasons = new ArrayList<>();
        for (PlanningEmployee employee : schedule.getEmployees()) {
            List<String> blockers = new ArrayList<>();
            if (HardRules.qualificationMinutes(employee, empty) > 0) {
                blockers.add("lacks skill " + empty.getRequiredSkill());
            }
            int unavailable = HardRules.unavailableMinutes(employee, empty);
            if (unavailable > 0) {
                blockers.add("not available for " + unavailable + " of " + empty.getDurationMinutes() + " min");
            }
            int weekMinutes = empty.getDurationMinutes();
            for (ShiftSeat other : schedule.getSeats()) {
                if (other == empty || other.getEmployee() != employee) {
                    continue;
                }
                if (HardRules.overlapMinutes(empty, other) > 0) {
                    blockers.add("overlaps " + other.getSeatId());
                }
                int deficit = HardRules.restDeficitMinutes(empty, other);
                if (deficit > 0) {
                    int rest = HardRules.MIN_REST_MINUTES - deficit;
                    blockers.add("rest " + hours(rest) + " next to " + other.getSeatId() + " (min 11h)");
                }
                if (other.getWeekKey() == empty.getWeekKey()) {
                    weekMinutes += other.getDurationMinutes();
                }
            }
            if (weekMinutes > employee.getMaxMinutesPerWeek()) {
                blockers.add("week total would be " + hours(weekMinutes) + " > max "
                        + hours(employee.getMaxMinutesPerWeek()));
            }
            if (blockers.isEmpty()) {
                blockers.add("no hard blocker found (left empty by search)");
            }
            reasons.add(employee.getId() + ": " + String.join(", ", blockers));
        }
        return reasons;
    }

    private static String hours(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + "h" : h + "h" + m + "m";
    }
}
