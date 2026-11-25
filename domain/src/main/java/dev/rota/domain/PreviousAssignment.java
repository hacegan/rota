package dev.rota.domain;

import java.util.Objects;

/**
 * One assignment from the previously published roster (D4, S3 stability). Matched to the
 * current roster by shift id. {@code pinned} is carried but not used until incremental repair.
 */
public record PreviousAssignment(String shiftId, String employeeId, boolean pinned) {

    public PreviousAssignment {
        Objects.requireNonNull(shiftId, "shiftId");
        if (shiftId.isBlank()) {
            throw new IllegalArgumentException("shiftId must not be blank");
        }
        Objects.requireNonNull(employeeId, "employeeId");
        if (employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
    }
}
