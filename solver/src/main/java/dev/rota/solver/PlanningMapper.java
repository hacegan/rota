package dev.rota.solver;

import dev.rota.domain.Employee;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.Shift;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps the pure domain problem to the planning model. Calendar facts (ISO week) are computed
 * here in the site zone so constraint streams never touch {@link ZoneId}.
 */
public final class PlanningMapper {

    private PlanningMapper() {
    }

    public static RotaSchedule toSchedule(SchedulingProblem problem) {
        List<PlanningEmployee> employees = new ArrayList<>();
        for (Employee employee : problem.employees()) {
            employees.add(new PlanningEmployee(employee.id(), employee.skills(),
                    employee.maxHoursPerWeek() * 60, employee.availability()));
        }
        List<ShiftSeat> seats = new ArrayList<>();
        for (Shift shift : problem.shifts()) {
            int weekKey = weekKey(shift, problem.zone());
            for (int seat = 0; seat < shift.headcount(); seat++) {
                seats.add(new ShiftSeat(shift.id(), seat, shift.time(), shift.requiredSkill(), weekKey));
            }
        }
        return new RotaSchedule(employees, seats);
    }

    /** ISO week of the shift start in the site zone; a shift crossing the week boundary counts in its start week. */
    static int weekKey(Shift shift, ZoneId zone) {
        ZonedDateTime start = shift.time().start().atZone(zone);
        return start.get(IsoFields.WEEK_BASED_YEAR) * 100 + start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
