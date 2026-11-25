package dev.rota.solver;

/**
 * Plain-Java hard-rule arithmetic, shared by the constraints and the unassigned-seat
 * explanations so both always agree. All penalties are in minutes (CONSTRAINT-MODEL.md §2).
 */
final class HardRules {

    static final int MIN_REST_MINUTES = 11 * 60;

    private HardRules() {
    }

    /** H1: minutes penalized when the employee lacks the seat's skill. */
    static int qualificationMinutes(PlanningEmployee employee, ShiftSeat seat) {
        return employee.hasSkill(seat.getRequiredSkill()) ? 0 : seat.getDurationMinutes();
    }

    /** H2: minutes of the seat outside the employee's availability. */
    static int unavailableMinutes(PlanningEmployee employee, ShiftSeat seat) {
        return employee.unavailableMinutes(seat.getTime());
    }

    /** H1 weigher for an assigned seat (exact method reference avoids overload ambiguity in penalize). */
    static int qualificationPenalty(ShiftSeat seat) {
        return qualificationMinutes(seat.getEmployee(), seat);
    }

    /** H2 weigher for an assigned seat. */
    static int availabilityPenalty(ShiftSeat seat) {
        return unavailableMinutes(seat.getEmployee(), seat);
    }

    /** H3: minutes two seats overlap. */
    static int overlapMinutes(ShiftSeat a, ShiftSeat b) {
        return (int) a.getTime().overlapMinutes(b.getTime());
    }

    /**
     * H4: minutes short of 11 h between two non-overlapping seats. Overlapping seats return 0
     * because H3 already penalizes them.
     */
    static int restDeficitMinutes(ShiftSeat a, ShiftSeat b) {
        if (a.getTime().overlaps(b.getTime())) {
            return 0;
        }
        ShiftSeat first = a.getTime().start().isAfter(b.getTime().start()) ? b : a;
        ShiftSeat second = first == a ? b : a;
        long gap = first.getTime().gapMinutesTo(second.getTime());
        return gap >= MIN_REST_MINUTES ? 0 : (int) (MIN_REST_MINUTES - gap);
    }
}
