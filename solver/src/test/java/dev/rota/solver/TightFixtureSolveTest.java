package dev.rota.solver;

import dev.rota.domain.SchedulingProblem;
import dev.rota.solver.fixture.FixtureLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Solver run on fixtures/tight (infeasible by design, D1). Criterion: 0 hard violations and every
 * unassigned seat listed with reasons. ADR 0001: optimum leaves 3 seats (1080 min) empty.
 */
class TightFixtureSolveTest {

    @Test
    void zeroHardAndEveryGapExplained() {
        SchedulingProblem problem = FixtureLoader.load("tight");
        SolveResult result = new RosterSolver().solve(problem);
        String report = result.toReport("tight", problem.zone());
        Reports.write("tight", report);

        assertEquals(0, result.hardScore(), "hard violations\n" + report);
        assertFalse(result.unassigned().isEmpty(), "tight must report gaps\n" + report);
        for (SolveResult.UnassignedSeat seat : result.unassigned()) {
            assertFalse(seat.reasons().isEmpty(), "no reasons for " + seat.seatId() + "\n" + report);
        }
        assertEquals(3, result.unassigned().size(), "unassigned seats\n" + report);
        assertEquals(-1080, result.mediumScore(), "medium\n" + report);
    }
}
