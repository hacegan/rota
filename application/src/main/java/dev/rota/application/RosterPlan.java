package dev.rota.application;

import dev.rota.domain.TimeRange;

import java.util.List;
import java.util.Objects;

/**
 * What a solve or repair produced, independent of the solver library: score levels, every filled
 * seat, every empty seat with per-employee reasons (FR8), and for a repair the change list against
 * the published roster plus the pinned seats. Solve results have no changes and no pinned seats.
 */
public record RosterPlan(ScoreSummary score, List<Assignment> assignments, List<UnassignedSeat> unassigned,
                         List<Change> changes, List<String> pinnedSeatIds) {

    public RosterPlan {
        Objects.requireNonNull(score, "score");
        assignments = List.copyOf(assignments);
        unassigned = List.copyOf(unassigned);
        changes = List.copyOf(changes);
        pinnedSeatIds = List.copyOf(pinnedSeatIds);
    }

    /** Hard / medium / soft score levels (HardMediumSoftScore in the solver). */
    public record ScoreSummary(long hard, long medium, long soft) {

        /** No hard rule broken. Empty seats (medium) do not make a roster infeasible. */
        public boolean feasible() {
            return hard == 0;
        }
    }

    public record Assignment(String seatId, TimeRange time, String skill, String employeeId) {

        public Assignment {
            Objects.requireNonNull(seatId, "seatId");
            Objects.requireNonNull(time, "time");
            Objects.requireNonNull(employeeId, "employeeId");
        }
    }

    public record UnassignedSeat(String seatId, TimeRange time, String skill, List<String> reasons) {

        public UnassignedSeat {
            Objects.requireNonNull(seatId, "seatId");
            Objects.requireNonNull(time, "time");
            reasons = List.copyOf(reasons);
        }
    }

    /** One seat whose employee differs from the published roster; {@code before}/{@code after} null = empty. */
    public record Change(String seatId, String before, String after, Kind kind) {

        public Change {
            Objects.requireNonNull(seatId, "seatId");
            Objects.requireNonNull(kind, "kind");
        }

        public enum Kind {
            REASSIGNED,
            LEFT_EMPTY,
            FILLED
        }
    }
}
