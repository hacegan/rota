package dev.rota.domain;

import java.util.Objects;

/**
 * A block of work: time, required skill and how many seats must be filled.
 */
public record Shift(String id, TimeRange time, String requiredSkill, int headcount) {

    public Shift {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(requiredSkill, "requiredSkill");
        if (requiredSkill.isBlank()) {
            throw new IllegalArgumentException("requiredSkill must not be blank");
        }
        if (headcount < 1) {
            throw new IllegalArgumentException("headcount must be >= 1: " + headcount);
        }
    }
}
