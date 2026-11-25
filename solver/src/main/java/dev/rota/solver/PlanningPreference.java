package dev.rota.solver;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Problem fact: a PREFER preference (S1). AVOID dates live on {@link PlanningEmployee} instead,
 * because an avoided date can be checked from the seat alone. Plain final class, not a record
 * (TIMEFOLD-VERIFIED.md UNVERIFIED #2). Equality is identity.
 */
public final class PlanningPreference {

    private final PlanningEmployee employee;
    private final LocalDate date;

    public PlanningPreference(PlanningEmployee employee, LocalDate date) {
        this.employee = Objects.requireNonNull(employee, "employee");
        this.date = Objects.requireNonNull(date, "date");
    }

    public PlanningEmployee getEmployee() {
        return employee;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        return employee.getId() + " PREFER " + date;
    }
}
