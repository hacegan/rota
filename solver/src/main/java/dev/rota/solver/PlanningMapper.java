package dev.rota.solver;

import dev.rota.domain.Employee;
import dev.rota.domain.Preference;
import dev.rota.domain.PreviousAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.Shift;
import dev.rota.domain.TimeRange;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps the pure domain problem to the planning model. Calendar facts (ISO week, local date,
 * night, weekend) are computed here in the site zone so constraint streams never touch
 * {@link ZoneId}.
 */
public final class PlanningMapper {

    /** D3: night = any shift overlapping 00:00-06:00 local. */
    static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    private PlanningMapper() {
    }

    public static RotaSchedule toSchedule(SchedulingProblem problem) {
        ZoneId zone = problem.zone();

        Set<Integer> weeks = new HashSet<>();
        for (Shift shift : problem.shifts()) {
            weeks.add(weekKey(shift, zone));
        }
        int horizonWeeks = Math.max(1, weeks.size());

        Map<String, Set<LocalDate>> avoidDates = new HashMap<>();
        for (Preference preference : problem.preferences()) {
            if (preference.kind() == Preference.Kind.AVOID) {
                avoidDates.computeIfAbsent(preference.employeeId(), k -> new HashSet<>()).add(preference.date());
            }
        }

        Map<String, PlanningEmployee> employees = new LinkedHashMap<>();
        for (Employee employee : problem.employees()) {
            employees.put(employee.id(), new PlanningEmployee(employee.id(), employee.skills(),
                    employee.maxHoursPerWeek() * 60, employee.contractHoursPerWeek() * 60, horizonWeeks,
                    avoidDates.getOrDefault(employee.id(), Set.of()), employee.availability()));
        }

        List<PlanningPreference> preferences = new ArrayList<>();
        for (Preference preference : problem.preferences()) {
            if (preference.kind() == Preference.Kind.PREFER) {
                preferences.add(new PlanningPreference(employees.get(preference.employeeId()), preference.date()));
            }
        }

        // The i-th previous assignment of a shift id maps to seat i of that shift (file order).
        Map<String, List<String>> previous = new HashMap<>();
        for (PreviousAssignment assignment : problem.previousAssignments()) {
            previous.computeIfAbsent(assignment.shiftId(), k -> new ArrayList<>()).add(assignment.employeeId());
        }

        List<ShiftSeat> seats = new ArrayList<>();
        for (Shift shift : problem.shifts()) {
            int weekKey = weekKey(shift, zone);
            LocalDate localDate = shift.time().start().atZone(zone).toLocalDate();
            boolean night = isNight(shift.time(), zone);
            int weekendKey = weekendKey(shift.time(), zone);
            List<String> previousEmployees = previous.getOrDefault(shift.id(), List.of());
            for (int seat = 0; seat < shift.headcount(); seat++) {
                String previousEmployee = seat < previousEmployees.size() ? previousEmployees.get(seat) : null;
                seats.add(new ShiftSeat(shift.id(), seat, shift.time(), shift.requiredSkill(), weekKey,
                        localDate, night, weekendKey, previousEmployee));
            }
        }
        return new RotaSchedule(new ArrayList<>(employees.values()), seats, preferences);
    }

    /** ISO week of the shift start in the site zone; a shift crossing the week boundary counts in its start week. */
    static int weekKey(Shift shift, ZoneId zone) {
        return weekKey(shift.time().start().atZone(zone).toLocalDate());
    }

    static int weekKey(LocalDate date) {
        return date.get(IsoFields.WEEK_BASED_YEAR) * 100 + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    /** D3: true when the range overlaps [d 00:00, d 06:00) local for any day d it touches. */
    static boolean isNight(TimeRange time, ZoneId zone) {
        LocalDate day = time.start().atZone(zone).toLocalDate();
        LocalDate last = time.end().atZone(zone).toLocalDate();
        for (; !day.isAfter(last); day = day.plusDays(1)) {
            Instant from = day.atStartOfDay(zone).toInstant();
            Instant to = day.atTime(NIGHT_END).atZone(zone).toInstant();
            if (new TimeRange(from, to).overlaps(time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * D3: weekend = Saturday + Sunday local. Returns the ISO week key of the first Saturday or
     * Sunday the range touches (Saturday and Sunday share an ISO week), or 0 if none.
     */
    static int weekendKey(TimeRange time, ZoneId zone) {
        LocalDate day = time.start().atZone(zone).toLocalDate();
        LocalDate last = time.end().minusNanos(1).atZone(zone).toLocalDate();
        for (; !day.isAfter(last); day = day.plusDays(1)) {
            DayOfWeek dow = day.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                return weekKey(day);
            }
        }
        return 0;
    }
}
