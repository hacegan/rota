package dev.rota.solver;

import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;
import dev.rota.domain.TimeRange;
import dev.rota.solver.fixture.FixtureLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P05 sick-call repair, end to end through the P03/P04 solver. fixtures/sick: E2 sick on Wed
 * 2026-11-25, reported Wed 07:00; expected repair is the single change S06 E2 -> E3
 * (fixtures/sick/README.md). fixtures/mini with the same roster and sick call is the impossible twin.
 */
class SickRepairTest {

    private static final ZoneId ZONE = FixtureLoader.DEFAULT_ZONE;

    @Test
    void sickEmployeeAssignmentsAreReassigned() {
        SickCall sick = FixtureLoader.sickCall("sick");
        RepairResult repair = repair("sick", sick);
        String report = repair.toReport("sick", ZONE);

        assertFeasibleAndFull(repair, report);
        Map<String, String> after = assignments(repair.result());
        assertEquals("E3", after.get("S06#0"), report);
        TimeRange sickPeriod = sick.effectivePeriod().orElseThrow();
        for (SolveResult.SeatAssignment assignment : repair.result().assignments()) {
            assertFalse("E2".equals(assignment.employeeId()) && sickPeriod.overlaps(assignment.time()),
                    "E2 still on " + assignment.seatId() + " during the sick period\n" + report);
        }
        assertTrue(repair.changes().contains(
                new RepairResult.SeatChange("S06#0", "E2", "E3", RepairResult.SeatChange.Kind.REASSIGNED)), report);
    }

    @Test
    void unaffectedAssignmentsStayUnchanged() {
        RepairResult repair = repair("sick", FixtureLoader.sickCall("sick"));
        String report = repair.toReport("sick", ZONE);

        Map<String, String> after = assignments(repair.result());
        for (RosterAssignment published : FixtureLoader.publishedRoster("sick")) {
            if (!published.seatId().equals("S06#0")) {
                assertEquals(published.employeeId(), after.get(published.seatId()), published.seatId() + "\n" + report);
            }
        }
        assertEquals(1, repair.changeCount(), report);
    }

    @Test
    void pastPinnedAssignmentsNeverMove() {
        // E2 sick Mon-Wed but reported Wed 07:00: E2's Mon/Tue seats are history and stay E2.
        SickCall reported = FixtureLoader.sickCall("sick");
        SickCall sick = new SickCall("E2",
                new TimeRange(LocalDate.parse("2026-11-23").atStartOfDay(ZONE).toInstant(),
                        LocalDate.parse("2026-11-26").atStartOfDay(ZONE).toInstant()),
                reported.reportedAt());
        RepairResult repair = repair("sick", sick);
        String report = repair.toReport("sick-past", ZONE);

        assertFeasibleAndFull(repair, report);
        assertEquals(List.of("S01#0", "S02#0", "S03#0", "S04#0"), repair.pinnedSeatIds(), report);
        Map<String, String> after = assignments(repair.result());
        assertEquals("E1", after.get("S01#0"), report);
        assertEquals("E2", after.get("S02#0"), report);
        assertEquals("E1", after.get("S03#0"), report);
        assertEquals("E2", after.get("S04#0"), report);
        for (RepairResult.SeatChange change : repair.changes()) {
            assertFalse(repair.pinnedSeatIds().contains(change.seatId()), "pinned seat changed: " + change + "\n" + report);
        }
    }

    @Test
    void repairChangesNoMoreThanFullResolve() {
        SchedulingProblem problem = FixtureLoader.load("sick");
        List<RosterAssignment> published = FixtureLoader.publishedRoster("sick");
        SickCall sick = FixtureLoader.sickCall("sick");

        RepairResult repair = new RosterRepairer().repair(problem, published, sick);
        SolveResult full = new RosterSolver().solve(RosterRepairer.applySickCall(problem, sick));

        int fullChanges = changesAgainst(published, full);
        assertTrue(repair.changeCount() <= fullChanges,
                "repair " + repair.changeCount() + " > full re-solve " + fullChanges + "\n"
                        + repair.toReport("repair", ZONE) + full.toReport("full", ZONE));
    }

    @Test
    void impossibleRepairReportsUnassignedSeatsWithReasons() {
        // mini has no E3 Wednesday window, so nobody can legally take S06.
        RepairResult repair = new RosterRepairer().repair(FixtureLoader.load("mini"),
                FixtureLoader.publishedRoster("sick"), FixtureLoader.sickCall("sick"));
        String report = repair.toReport("mini-sick", ZONE);

        assertEquals(0, repair.result().hardScore(), report);
        assertEquals(-360, repair.result().mediumScore(), report);
        assertEquals(1, repair.result().unassigned().size(), report);
        SolveResult.UnassignedSeat s06 = repair.result().unassigned().get(0);
        assertEquals("S06#0", s06.seatId(), report);
        assertFalse(s06.reasons().isEmpty(), report);
        assertTrue(s06.reasons().stream().anyMatch(reason -> reason.startsWith("E2: ") && reason.contains("not available")),
                report);
        assertTrue(repair.changes().contains(
                new RepairResult.SeatChange("S06#0", "E2", null, RepairResult.SeatChange.Kind.LEFT_EMPTY)), report);
    }

    /**
     * UNVERIFIED (TIMEFOLD-VERIFIED.md #5, QUESTIONS.md P05 G3): termination is wall-clock based,
     * so identical results across runs are not confirmed. Enable once determinism is CONFIRMED.
     */
    @Disabled("UNVERIFIED: run-to-run determinism of the solver is not confirmed (TIMEFOLD-VERIFIED.md #5)")
    @Test
    void repeatedRepairIsDeterministic() {
        RepairResult first = repair("sick", FixtureLoader.sickCall("sick"));
        RepairResult second = repair("sick", FixtureLoader.sickCall("sick"));
        assertEquals(assignments(first.result()), assignments(second.result()));
        assertEquals(first.changes(), second.changes());
    }

    // --- helpers ---

    private static RepairResult repair(String fixture, SickCall sick) {
        return new RosterRepairer().repair(FixtureLoader.load(fixture), FixtureLoader.publishedRoster(fixture), sick);
    }

    private static void assertFeasibleAndFull(RepairResult repair, String report) {
        assertEquals(0, repair.result().hardScore(), "hard\n" + report);
        assertEquals(0, repair.result().unassigned().size(), "unassigned\n" + report);
    }

    private static Map<String, String> assignments(SolveResult result) {
        Map<String, String> bySeat = new HashMap<>();
        for (SolveResult.SeatAssignment assignment : result.assignments()) {
            bySeat.put(assignment.seatId(), assignment.employeeId());
        }
        return bySeat;
    }

    private static int changesAgainst(List<RosterAssignment> published, SolveResult result) {
        Map<String, String> after = assignments(result);
        int changes = 0;
        for (RosterAssignment assignment : published) {
            if (!Objects.equals(assignment.employeeId(), after.get(assignment.seatId()))) {
                changes++;
            }
        }
        return changes;
    }
}
