package dev.rota.domain;

import java.util.Objects;

/**
 * One seat of a published roster: seat {@code seatIndex} of shift {@code shiftId} is held by
 * {@code employeeId} (P05). Seats with no entry were published empty.
 */
public record RosterAssignment(String shiftId, int seatIndex, String employeeId) {

    public RosterAssignment {
        Objects.requireNonNull(shiftId, "shiftId");
        if (shiftId.isBlank()) {
            throw new IllegalArgumentException("shiftId must not be blank");
        }
        if (seatIndex < 0) {
            throw new IllegalArgumentException("seatIndex must be >= 0: " + seatIndex);
        }
        Objects.requireNonNull(employeeId, "employeeId");
        if (employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
    }

    /** Same id format as the solver seats: {@code shiftId#seatIndex}. */
    public String seatId() {
        return shiftId + "#" + seatIndex;
    }
}
