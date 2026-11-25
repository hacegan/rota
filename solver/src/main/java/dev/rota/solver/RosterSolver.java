package dev.rota.solver;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import dev.rota.domain.SchedulingProblem;

import java.time.Duration;

/**
 * Builds and runs the Timefold solver for a {@link SchedulingProblem}.
 *
 * <p>UNVERIFIED and isolated here (see .agent/QUESTIONS.md, A6-A7): {@code SolverFactory},
 * {@code Solver}, the {@code SolverConfig} builder methods and the {@code TerminationConfig}
 * limit methods. Termination is wall-clock based, so the stopping point is not strictly
 * reproducible (NFR1 risk, A7).
 */
public final class RosterSolver {

    private final SolverFactory<RotaSchedule> solverFactory;

    public RosterSolver() {
        this(Duration.ofSeconds(30), Duration.ofSeconds(3));
    }

    public RosterSolver(Duration spentLimit, Duration unimprovedSpentLimit) {
        SolverConfig config = new SolverConfig()
                .withSolutionClass(RotaSchedule.class)
                .withEntityClasses(ShiftSeat.class)
                .withConstraintProviderClass(RotaConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withSpentLimit(spentLimit)
                        .withUnimprovedSpentLimit(unimprovedSpentLimit));
        this.solverFactory = SolverFactory.create(config);
    }

    public SolveResult solve(SchedulingProblem problem) {
        Solver<RotaSchedule> solver = solverFactory.buildSolver();
        RotaSchedule solved = solver.solve(PlanningMapper.toSchedule(problem));
        return SolveResult.of(solved);
    }
}
