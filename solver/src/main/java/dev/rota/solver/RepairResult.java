package dev.rota.solver;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Outcome of a sick-call repair (P05): the repaired roster with score and unassigned seats plus
 * reasons ({@link SolveResult}), the change list against the published roster, and the seats that
 * were pinned as history.
 */
public record RepairResult(SolveResult result, List<SeatChange> changes, List<String> pinnedSeatIds) {

    public RepairResult {
        Objects.requireNonNull(result, "result");
        changes = List.copyOf(changes);
        pinnedSeatIds = List.copyOf(pinnedSeatIds);
    }

    /** One seat whose employee differs from the published roster. {@code before}/{@code after} may be null (empty). */
    public record SeatChange(String seatId, String before, String after, Kind kind) {

        public enum Kind {
            /** Published employee replaced by another employee. */
            REASSIGNED,
            /** Published employee removed; nobody could legally take the seat. */
            LEFT_EMPTY,
            /** Seat published empty, now filled. */
            FILLED
        }
    }

    static RepairResult of(RotaSchedule solved, Map<String, String> publishedBySeat) {
        List<SeatChange> changes = new ArrayList<>();
        List<String> pinned = new ArrayList<>();
        for (ShiftSeat seat : solved.getSeats()) {
            if (seat.isPinned()) {
                pinned.add(seat.getSeatId());
            }
            String before = publishedBySeat.get(seat.getSeatId());
            String after = seat.getEmployee() == null ? null : seat.getEmployee().getId();
            if (Objects.equals(before, after)) {
                continue;
            }
            SeatChange.Kind kind = after == null ? SeatChange.Kind.LEFT_EMPTY
                    : before == null ? SeatChange.Kind.FILLED : SeatChange.Kind.REASSIGNED;
            changes.add(new SeatChange(seat.getSeatId(), before, after, kind));
        }
        return new RepairResult(SolveResult.of(solved), changes, pinned);
    }

    public int changeCount() {
        return changes.size();
    }

    /** {@link SolveResult#toReport} plus the change list and pinned seats. */
    public String toReport(String fixture, ZoneId zone) {
        StringBuilder out = new StringBuilder(result.toReport(fixture, zone));
        out.append("changes: ").append(changes.size()).append('\n');
        for (SeatChange change : changes) {
            out.append("CHANGE ").append(change.seatId()).append(' ')
                    .append(change.before() == null ? "-" : change.before()).append(" -> ")
                    .append(change.after() == null ? "-" : change.after()).append(' ')
                    .append(change.kind()).append('\n');
        }
        out.append("pinned: ").append(String.join(", ", pinnedSeatIds)).append('\n');
        return out.toString();
    }
}
