package dev.rota.solver;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import dev.rota.domain.TimeRange;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of a solve: score levels, every seat with its employee, and every empty seat with
 * per-employee reasons (FR8). Never silently drops an empty seat.
 */
public record SolveResult(long hardScore, long mediumScore, long softScore,
                          List<SeatAssignment> assignments, List<UnassignedSeat> unassigned) {

    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public SolveResult {
        assignments = List.copyOf(assignments);
        unassigned = List.copyOf(unassigned);
    }

    public record SeatAssignment(String seatId, TimeRange time, String skill, String employeeId) {
    }

    public record UnassignedSeat(String seatId, TimeRange time, String skill, List<String> reasons) {
        public UnassignedSeat {
            reasons = List.copyOf(reasons);
        }
    }

    static SolveResult of(RotaSchedule solved) {
        HardMediumSoftScore score = solved.getScore();
        List<SeatAssignment> assignments = new ArrayList<>();
        List<UnassignedSeat> unassigned = new ArrayList<>();
        for (ShiftSeat seat : solved.getSeats()) {
            if (seat.getEmployee() == null) {
                unassigned.add(new UnassignedSeat(seat.getSeatId(), seat.getTime(), seat.getRequiredSkill(),
                        UnassignedSeatExplainer.explain(seat, solved)));
            } else {
                assignments.add(new SeatAssignment(seat.getSeatId(), seat.getTime(), seat.getRequiredSkill(),
                        seat.getEmployee().getId()));
            }
        }
        // Record component accessors of HardMediumSoftScore (javap: fields hardScore/mediumScore/softScore).
        return new SolveResult(score.hardScore(), score.mediumScore(), score.softScore(), assignments, unassigned);
    }

    /**
     * Plain-text report parsed by scripts/verify_p03.ps1. Keep the {@code key: value} lines and the
     * {@code UNASSIGNED ... | reasons} format stable.
     */
    public String toReport(String fixture, ZoneId zone) {
        StringBuilder out = new StringBuilder();
        out.append("fixture: ").append(fixture).append('\n');
        out.append("zone: ").append(zone).append('\n');
        out.append("score: ").append(hardScore).append("hard/").append(mediumScore).append("medium/")
                .append(softScore).append("soft\n");
        out.append("hard: ").append(hardScore).append('\n');
        out.append("medium: ").append(mediumScore).append('\n');
        out.append("seats: ").append(assignments.size() + unassigned.size()).append('\n');
        out.append("assigned: ").append(assignments.size()).append('\n');
        out.append("unassigned: ").append(unassigned.size()).append('\n');
        for (SeatAssignment a : assignments) {
            out.append("ASSIGNED ").append(a.seatId()).append(' ').append(format(a.time(), zone)).append(' ')
                    .append(a.skill()).append(" -> ").append(a.employeeId()).append('\n');
        }
        for (UnassignedSeat u : unassigned) {
            out.append("UNASSIGNED ").append(u.seatId()).append(' ').append(format(u.time(), zone)).append(' ')
                    .append(u.skill()).append(" | ").append(String.join("; ", u.reasons())).append('\n');
        }
        return out.toString();
    }

    private static String format(TimeRange time, ZoneId zone) {
        return DAY_TIME.format(time.start().atZone(zone)) + "-" + TIME.format(time.end().atZone(zone));
    }
}
