package dev.rota.domain;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Input to scheduling: the site time zone (D3), employees, shifts, preferences and the previous
 * roster (D4). Employee and shift ids are unique; preferences must name a known employee.
 */
public record SchedulingProblem(ZoneId zone, List<Employee> employees, List<Shift> shifts,
                                List<Preference> preferences, List<PreviousAssignment> previousAssignments) {

    public SchedulingProblem {
        Objects.requireNonNull(zone, "zone");
        employees = List.copyOf(Objects.requireNonNull(employees, "employees"));
        shifts = List.copyOf(Objects.requireNonNull(shifts, "shifts"));
        preferences = List.copyOf(Objects.requireNonNull(preferences, "preferences"));
        previousAssignments = List.copyOf(Objects.requireNonNull(previousAssignments, "previousAssignments"));
        Set<String> employeeIds = new HashSet<>();
        for (Employee employee : employees) {
            if (!employeeIds.add(employee.id())) {
                throw new IllegalArgumentException("duplicate employee id: " + employee.id());
            }
        }
        Set<String> shiftIds = new HashSet<>();
        for (Shift shift : shifts) {
            if (!shiftIds.add(shift.id())) {
                throw new IllegalArgumentException("duplicate shift id: " + shift.id());
            }
        }
        for (Preference preference : preferences) {
            if (!employeeIds.contains(preference.employeeId())) {
                throw new IllegalArgumentException("preference for unknown employee: " + preference.employeeId());
            }
        }
    }

    public SchedulingProblem(ZoneId zone, List<Employee> employees, List<Shift> shifts) {
        this(zone, employees, shifts, List.of(), List.of());
    }
}
