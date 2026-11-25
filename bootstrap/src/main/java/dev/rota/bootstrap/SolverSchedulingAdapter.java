package dev.rota.bootstrap;

import dev.rota.application.RosterPlan;
import dev.rota.application.SchedulingPort;
import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;
import dev.rota.solver.RepairResult;
import dev.rota.solver.RosterRepairer;
import dev.rota.solver.RosterSolver;
import dev.rota.solver.SolveResult;

import java.util.List;
import java.util.Objects;

/**
 * {@link SchedulingPort} over the solver module ({@link RosterSolver}, {@link RosterRepairer}).
 * Only maps results; the solver's behaviour is unchanged. No Timefold type crosses this class:
 * it uses the solver module's own result records.
 */
public final class SolverSchedulingAdapter implements SchedulingPort {

    private final RosterSolver solver;
    private final RosterRepairer repairer;

    public SolverSchedulingAdapter(RosterSolver solver, RosterRepairer repairer) {
        this.solver = Objects.requireNonNull(solver, "solver");
        this.repairer = Objects.requireNonNull(repairer, "repairer");
    }

    @Override
    public RosterPlan solve(SchedulingProblem problem) {
        return plan(solver.solve(problem), List.of(), List.of());
    }

    @Override
    public RosterPlan repair(SchedulingProblem problem, List<RosterAssignment> published, SickCall sickCall) {
        RepairResult repair = repairer.repair(problem, published, sickCall);
        List<RosterPlan.Change> changes = repair.changes().stream()
                .map(change -> new RosterPlan.Change(change.seatId(), change.before(), change.after(),
                        RosterPlan.Change.Kind.valueOf(change.kind().name())))
                .toList();
        return plan(repair.result(), changes, repair.pinnedSeatIds());
    }

    static RosterPlan plan(SolveResult result, List<RosterPlan.Change> changes, List<String> pinnedSeatIds) {
        List<RosterPlan.Assignment> assignments = result.assignments().stream()
                .map(a -> new RosterPlan.Assignment(a.seatId(), a.time(), a.skill(), a.employeeId()))
                .toList();
        List<RosterPlan.UnassignedSeat> unassigned = result.unassigned().stream()
                .map(u -> new RosterPlan.UnassignedSeat(u.seatId(), u.time(), u.skill(), u.reasons()))
                .toList();
        return new RosterPlan(new RosterPlan.ScoreSummary(result.hardScore(), result.mediumScore(), result.softScore()),
                assignments, unassigned, changes, pinnedSeatIds);
    }
}
