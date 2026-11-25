package dev.rota.solver;

import dev.rota.domain.Employee;
import dev.rota.domain.Preference;
import dev.rota.domain.PreviousAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.Shift;
import dev.rota.domain.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * End-to-end solver runs for P04 soft rules. Each problem is built in code so that only the rule
 * under test can tell solutions apart: contract hours are 0 (S4 off) unless S4 is under test, and
 * there are no preferences or previous roster unless those are under test. All employees are
 * cashiers with max 40 h. Assertions are properties of the result (who got what), plus exact soft
 * scores only where they follow from SoftWeights and our own formulas.
 */
class SoftSolveTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final List<TimeRange> ALWAYS =
            List.of(range("2026-11-23", "00:00", "2026-12-07", "00:00"));

    // --- S2n / S2w: balanced beats skewed ---

    @Test
    void nightFairness_solverSplitsNightsEvenly() {
        List<Shift> nights = List.of(
                shift("N1", "2026-11-23", "00:00", "06:00"), shift("N2", "2026-11-24", "00:00", "06:00"),
                shift("N3", "2026-11-25", "00:00", "06:00"), shift("N4", "2026-11-26", "00:00", "06:00"));
        SolveResult result = solve(List.of(employee("E1", 0, ALWAYS), employee("E2", 0, ALWAYS)),
                nights, List.of(), List.of());

        assertFeasibleAndFull(result);
        Map<String, Integer> perEmployee = countPerEmployee(result);
        // (2,2) = 200 beats (3,1) = 250 and (4,0) = 400.
        assertEquals(2, perEmployee.getOrDefault("E1", 0), "E1 nights\n" + result.toReport("nights", ZONE));
        assertEquals(2, perEmployee.getOrDefault("E2", 0), "E2 nights\n" + result.toReport("nights", ZONE));
    }

    @Test
    void weekendFairness_solverGivesEachEmployeeOneWholeWeekend() {
        List<Shift> weekends = List.of(
                shift("W1", "2026-11-28", "08:00", "14:00"), shift("W2", "2026-11-29", "08:00", "14:00"),
                shift("W3", "2026-12-05", "08:00", "14:00"), shift("W4", "2026-12-06", "08:00", "14:00"));
        SolveResult result = solve(List.of(employee("E1", 0, ALWAYS), employee("E2", 0, ALWAYS)),
                weekends, List.of(), List.of());

        assertFeasibleAndFull(result);
        Map<String, String> bySeat = bySeat(result);
        String report = result.toReport("weekends", ZONE);
        // Weekends touched (1,1) = 50 beats (2,0) = 100, (2,1) = 125 and (2,2) = 200.
        assertEquals(bySeat.get("W1#0"), bySeat.get("W2#0"), "first weekend kept whole\n" + report);
        assertEquals(bySeat.get("W3#0"), bySeat.get("W4#0"), "second weekend kept whole\n" + report);
        assertNotEquals(bySeat.get("W1#0"), bySeat.get("W3#0"), "weekends shared out\n" + report);
    }

    // --- S1: preferred beats avoided when hard rules allow ---

    @Test
    void preferredAssignmentBeatsAvoided() {
        List<Preference> preferences = List.of(
                new Preference("E1", Preference.Kind.AVOID, LocalDate.parse("2026-11-25")),
                new Preference("E2", Preference.Kind.PREFER, LocalDate.parse("2026-11-25")));
        SolveResult result = solve(List.of(employee("E1", 0, ALWAYS), employee("E2", 0, ALWAYS)),
                List.of(shift("P1", "2026-11-25", "10:00", "16:00")), preferences, List.of());

        assertFeasibleAndFull(result);
        assertEquals("E2", bySeat(result).get("P1#0"), result.toReport("prefer", ZONE));
        assertEquals(0, result.softScore(), result.toReport("prefer", ZONE));
    }

    @Test
    void avoidedEmployeeStillCoversWhenHardRulesLeaveNoOtherChoice() {
        List<Preference> preferences = List.of(
                new Preference("E1", Preference.Kind.AVOID, LocalDate.parse("2026-11-25")),
                new Preference("E2", Preference.Kind.PREFER, LocalDate.parse("2026-11-25")));
        // E2 has no availability, so only E1 can legally take the seat; coverage (medium) wins over S1.
        SolveResult result = solve(List.of(employee("E1", 0, ALWAYS), employee("E2", 0, List.of())),
                List.of(shift("P1", "2026-11-25", "10:00", "16:00")), preferences, List.of());

        assertFeasibleAndFull(result);
        assertEquals("E1", bySeat(result).get("P1#0"), result.toReport("avoid-forced", ZONE));
        // E1 avoided (50) + E2 preferred date missed (50).
        assertEquals(-100, result.softScore(), result.toReport("avoid-forced", ZONE));
    }

    // --- S4: contract-hours deviation is penalized ---

    @Test
    void contractDeviationIsPenalized() {
        // E1 contract 12 h, E2 no contract; one 6 h seat. E1 takes it: 360 min under contract.
        // E2 taking it would leave E1 with no seat: 720 min. So E1 takes it and soft = -360.
        SolveResult result = solve(List.of(employee("E1", 12, ALWAYS), employee("E2", 0, ALWAYS)),
                List.of(shift("C1", "2026-11-24", "08:00", "14:00")), List.of(), List.of());

        assertFeasibleAndFull(result);
        assertEquals("E1", bySeat(result).get("C1#0"), result.toReport("contract", ZONE));
        assertEquals(-360, result.softScore(), result.toReport("contract", ZONE));
    }

    // --- S3: changing the previous roster incurs a stability penalty ---

    @Test
    void stability_keepsPreviousEmployeeWhenPossible() {
        SolveResult result = solve(List.of(employee("E1", 0, ALWAYS), employee("E2", 0, ALWAYS)),
                List.of(shift("R1", "2026-11-24", "08:00", "14:00")), List.of(),
                List.of(new PreviousAssignment("R1", "E1", false)));

        assertFeasibleAndFull(result);
        assertEquals("E1", bySeat(result).get("R1#0"), result.toReport("stable", ZONE));
        assertEquals(0, result.softScore(), result.toReport("stable", ZONE));
    }

    @Test
    void stability_changeIsPenalizedWhenForced() {
        // E1 (previous) is unavailable, so the seat must move to E2: one stability match (1000).
        SolveResult result = solve(List.of(employee("E1", 0, List.of()), employee("E2", 0, ALWAYS)),
                List.of(shift("R1", "2026-11-24", "08:00", "14:00")), List.of(),
                List.of(new PreviousAssignment("R1", "E1", false)));

        assertFeasibleAndFull(result);
        assertEquals("E2", bySeat(result).get("R1#0"), result.toReport("moved", ZONE));
        assertEquals(-1000, result.softScore(), result.toReport("moved", ZONE));
    }

    // --- helpers ---

    private static SolveResult solve(List<Employee> employees, List<Shift> shifts, List<Preference> preferences,
                                     List<PreviousAssignment> previous) {
        return new RosterSolver().solve(new SchedulingProblem(ZONE, employees, shifts, preferences, previous));
    }

    private static void assertFeasibleAndFull(SolveResult result) {
        assertEquals(0, result.hardScore(), "hard\n" + result.toReport("p04", ZONE));
        assertEquals(0, result.unassigned().size(), "unassigned\n" + result.toReport("p04", ZONE));
    }

    private static Map<String, String> bySeat(SolveResult result) {
        Map<String, String> bySeat = new HashMap<>();
        for (SolveResult.SeatAssignment assignment : result.assignments()) {
            bySeat.put(assignment.seatId(), assignment.employeeId());
        }
        return bySeat;
    }

    private static Map<String, Integer> countPerEmployee(SolveResult result) {
        Map<String, Integer> counts = new HashMap<>();
        for (SolveResult.SeatAssignment assignment : result.assignments()) {
            counts.merge(assignment.employeeId(), 1, Integer::sum);
        }
        return counts;
    }

    private static Employee employee(String id, int contractHours, List<TimeRange> availability) {
        return new Employee(id, id, Set.of("cashier"), 40, contractHours, availability);
    }

    private static Shift shift(String id, String day, String start, String end) {
        LocalDate date = LocalDate.parse(day);
        LocalTime from = LocalTime.parse(start);
        LocalTime to = LocalTime.parse(end);
        LocalDate endDay = to.isAfter(from) ? date : date.plusDays(1);
        return new Shift(id, new TimeRange(at(date, from), at(endDay, to)), "cashier", 1);
    }

    private static TimeRange range(String fromDay, String fromTime, String toDay, String toTime) {
        return new TimeRange(at(LocalDate.parse(fromDay), LocalTime.parse(fromTime)),
                at(LocalDate.parse(toDay), LocalTime.parse(toTime)));
    }

    private static Instant at(LocalDate day, LocalTime time) {
        return day.atTime(time).atZone(ZONE).toInstant();
    }
}
