package dev.rota.domain;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Input to scheduling: the site time zone (D3), employees and shifts. Ids are unique.
 */
public record SchedulingProblem(ZoneId zone, List<Employee> employees, List<Shift> shifts) {

    public SchedulingProblem {
        Objects.requireNonNull(zone, "zone");
        employees = List.copyOf(Objects.requireNonNull(employees, "employees"));
        shifts = List.copyOf(Objects.requireNonNull(shifts, "shifts"));
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
    }
}
