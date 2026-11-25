package dev.rota.solver;

import dev.rota.domain.Employee;
import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.Shift;
import dev.rota.domain.SickCall;
import dev.rota.domain.TimeRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * P05 incremental sick-call repair (CONSTRAINT-MODEL §5, simplified). Batch repair: the published
 * roster is the warm start of one ordinary solve with the P03/P04 constraints. No live
 * {@code ProblemChange}.
 *
 * <p>Per seat:
 * <ul>
 *   <li><b>Pinned</b> (started before {@link SickCall#reportedAt()}): keeps its published employee;
 *       the solver never changes it.</li>
 *   <li><b>Freed</b> (the sick employee's seat overlapping the effective sick period): starts empty;
 *       no stability reference, because changing it is the point of the repair.</li>
 *   <li><b>Movable</b> (all other future seats): starts with its published employee; S3 stability
 *       penalizes every change from the published roster, so seats move only when that buys coverage
 *       or a hard-rule fix.</li>
 * </ul>
 * The sick employee loses availability for the effective sick period, so H2 keeps them off it.
 * The stability reference is the published roster, not {@code previous-roster.json}.
 */
public final class RosterRepairer {

    private final RosterSolver solver;

    public RosterRepairer() {
        this(new RosterSolver());
    }

    public RosterRepairer(RosterSolver solver) {
        this.solver = solver;
    }

    public RepairResult repair(SchedulingProblem problem, List<RosterAssignment> published, SickCall sickCall) {
        RotaSchedule prepared = prepare(problem, published, sickCall);
        RotaSchedule solved = solver.solveSchedule(prepared);
        return RepairResult.of(solved, publishedBySeat(published));
    }

    /**
     * The problem as the repair sees it: the sick employee without availability for the effective
     * sick period, and no previous roster (the published roster is the stability reference).
     */
    public static SchedulingProblem applySickCall(SchedulingProblem problem, SickCall sickCall) {
        Optional<TimeRange> sickPeriod = sickCall.effectivePeriod();
        boolean found = false;
        List<Employee> employees = new ArrayList<>();
        for (Employee employee : problem.employees()) {
            if (employee.id().equals(sickCall.employeeId())) {
                found = true;
                employees.add(sickPeriod.map(employee::withoutAvailability).orElse(employee));
            } else {
                employees.add(employee);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("sick call for unknown employee: " + sickCall.employeeId());
        }
        return new SchedulingProblem(problem.zone(), employees, problem.shifts(), problem.preferences(), List.of());
    }

    /** Builds the warm-start schedule: pinned, freed and movable seats as described on the class. */
    static RotaSchedule prepare(SchedulingProblem problem, List<RosterAssignment> published, SickCall sickCall) {
        validate(problem, published);
        RotaSchedule schedule = PlanningMapper.toSchedule(applySickCall(problem, sickCall));
        Map<String, PlanningEmployee> employees = new HashMap<>();
        for (PlanningEmployee employee : schedule.getEmployees()) {
            employees.put(employee.getId(), employee);
        }
        Map<String, String> publishedBySeat = publishedBySeat(published);
        Optional<TimeRange> sickPeriod = sickCall.effectivePeriod();

        for (ShiftSeat seat : schedule.getSeats()) {
            String publishedEmployee = publishedBySeat.get(seat.getSeatId());
            seat.setPreviousEmployeeId(publishedEmployee);
            seat.setEmployee(publishedEmployee == null ? null : employees.get(publishedEmployee));
            if (seat.getTime().start().isBefore(sickCall.reportedAt())) {
                seat.setPinned(true);
            } else if (sickCall.employeeId().equals(publishedEmployee)
                    && sickPeriod.map(period -> period.overlaps(seat.getTime())).orElse(false)) {
                seat.setEmployee(null);
                seat.setPreviousEmployeeId(null);
            }
        }
        return schedule;
    }

    static Map<String, String> publishedBySeat(List<RosterAssignment> published) {
        Map<String, String> bySeat = new LinkedHashMap<>();
        for (RosterAssignment assignment : published) {
            if (bySeat.put(assignment.seatId(), assignment.employeeId()) != null) {
                throw new IllegalArgumentException("seat published twice: " + assignment.seatId());
            }
        }
        return bySeat;
    }

    private static void validate(SchedulingProblem problem, List<RosterAssignment> published) {
        Set<String> employeeIds = new HashSet<>();
        for (Employee employee : problem.employees()) {
            employeeIds.add(employee.id());
        }
        Set<String> seatIds = new HashSet<>();
        for (Shift shift : problem.shifts()) {
            for (int seat = 0; seat < shift.headcount(); seat++) {
                seatIds.add(shift.id() + "#" + seat);
            }
        }
        for (RosterAssignment assignment : published) {
            if (!seatIds.contains(assignment.seatId())) {
                throw new IllegalArgumentException("published seat not in problem: " + assignment.seatId());
            }
            if (!employeeIds.contains(assignment.employeeId())) {
                throw new IllegalArgumentException("published employee not in problem: " + assignment.employeeId());
            }
        }
    }
}
