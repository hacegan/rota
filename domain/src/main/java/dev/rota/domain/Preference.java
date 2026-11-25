package dev.rota.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An employee's wish about a calendar date in the site zone (D4, S1).
 * PREFER: the employee wants to work that date. AVOID: the employee does not want to.
 */
public record Preference(String employeeId, Kind kind, LocalDate date) {

    public enum Kind {
        PREFER,
        AVOID
    }

    public Preference {
        Objects.requireNonNull(employeeId, "employeeId");
        if (employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(date, "date");
    }
}
