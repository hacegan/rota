package dev.rota.solver;

import dev.rota.domain.SchedulingProblem;
import dev.rota.solver.fixture.FixtureLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Solver run on fixtures/mini. D1 success criterion: every shift assigned, 0 hard violations.
 * The report is written before asserting so verify can print it either way.
 */
class MiniFixtureSolveTest {

    @Test
    void everySeatAssignedWithZeroHard() {
        SchedulingProblem problem = FixtureLoader.load("mini");
        SolveResult result = new RosterSolver().solve(problem);
        String report = result.toReport("mini", problem.zone());
        Reports.write("mini", report);

        assertEquals(0, result.hardScore(), "hard violations\n" + report);
        assertEquals(0, result.unassigned().size(), "unassigned seats\n" + report);
        assertEquals(0, result.mediumScore(), "medium\n" + report);
    }
}
