package dev.rota.solver;

import dev.rota.domain.Employee;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;
import dev.rota.solver.fixture.FixtureLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.rota.solver.Seats.employee;
import static dev.rota.solver.Seats.seat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P05 warm-start preparation, without running the solver. fixtures/sick: E2 sick on Wed 2026-11-25,
 * reported Wed 07:00, so Mon/Tue seats (S01-S04) are history.
 */
class RepairPreparationTest {

    private static RotaSchedule prepared() {
        return RosterRepairer.prepare(FixtureLoader.load("sick"), FixtureLoader.publishedRoster("sick"),
                FixtureLoader.sickCall("sick"));
    }

    @Test
    void seatsBeforeTheReportArePinnedWithTheirPublishedEmployee() {
        RotaSchedule schedule = prepared();
        String[][] expected = {{"S01", "E1"}, {"S02", "E2"}, {"S03", "E1"}, {"S04", "E2"}};
        for (String[] pair : expected) {
            ShiftSeat seat = seat(schedule, pair[0]);
            assertTrue(seat.isPinned(), pair[0] + " pinned");
            assertEquals(pair[1], seat.getEmployee().getId(), pair[0] + " employee");
        }
    }

    @Test
    void sickEmployeesSeatInTheSickPeriodIsFreedWithoutStabilityReference() {
        ShiftSeat s06 = seat(prepared(), "S06");
        assertNull(s06.getEmployee());
        assertNull(s06.getPreviousEmployeeId());
        assertFalse(s06.isPinned());
    }

    @Test
    void otherFutureSeatsStartWithPublishedEmployeeAndStabilityReference() {
        RotaSchedule schedule = prepared();
        String[][] expected = {{"S05", "E1"}, {"S08", "E2"}, {"S10", "E2"}, {"S13", "E3"}, {"S14", "E1"}};
        for (String[] pair : expected) {
            ShiftSeat seat = seat(schedule, pair[0]);
            assertFalse(seat.isPinned(), pair[0] + " not pinned");
            assertEquals(pair[1], seat.getEmployee().getId(), pair[0] + " employee");
            assertEquals(pair[1], seat.getPreviousEmployeeId(), pair[0] + " stability reference");
        }
    }

    @Test
    void sickEmployeeLosesAvailabilityOnlyForTheEffectivePeriod() {
        RotaSchedule schedule = prepared();
        PlanningEmployee e2 = employee(schedule, "E2");
        assertEquals(360, e2.unavailableMinutes(seat(schedule, "S06").getTime())); // Wed: sick
        assertEquals(0, e2.unavailableMinutes(seat(schedule, "S08").getTime()));   // Thu: fine
        assertEquals(0, e2.unavailableMinutes(seat(schedule, "S02").getTime()));   // Mon: history
    }

    @Test
    void repairProblemDropsPreviousRosterAndRejectsUnknownEmployee() {
        SchedulingProblem mini = FixtureLoader.load("mini"); // has previous-roster.json
        SickCall sick = FixtureLoader.sickCall("sick");
        assertEquals(List.of(), RosterRepairer.applySickCall(mini, sick).previousAssignments());

        SickCall unknown = new SickCall("E9", sick.period(), sick.reportedAt());
        assertThrows(IllegalArgumentException.class, () -> RosterRepairer.applySickCall(mini, unknown));
    }

    @Test
    void sickEmployeeKeepsEverythingButTheSickPeriod() {
        SchedulingProblem repaired = RosterRepairer.applySickCall(FixtureLoader.load("sick"), FixtureLoader.sickCall("sick"));
        Employee e2 = repaired.employees().stream().filter(e -> e.id().equals("E2")).findFirst().orElseThrow();
        assertEquals(4, e2.availability().size()); // Mon, Tue, Thu, Fri; Wed removed
    }
}
