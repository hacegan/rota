package dev.rota.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchedulingProblemTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    @Test
    void rejectsPreferenceForUnknownEmployee() {
        List<Employee> employees = List.of(new Employee("E1", "One"));
        List<Preference> preferences = List.of(
                new Preference("E9", Preference.Kind.AVOID, LocalDate.parse("2026-11-27")));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulingProblem(ZONE, employees, List.of(), preferences, List.of()));
    }

    @Test
    void threeArgConstructorHasNoPreferencesOrPreviousRoster() {
        SchedulingProblem problem = new SchedulingProblem(ZONE, List.of(), List.of());
        assertEquals(List.of(), problem.preferences());
        assertEquals(List.of(), problem.previousAssignments());
    }

    @Test
    void employeeRejectsNegativeContractHours() {
        assertThrows(IllegalArgumentException.class,
                () -> new Employee("E1", "One", Set.of(), 40, -1, List.of()));
    }
}
