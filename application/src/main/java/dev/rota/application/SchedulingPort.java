package dev.rota.application;

import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;

import java.util.List;

/** Solver port. Implemented outside the application module (bootstrap adapter over the Timefold solver). */
public interface SchedulingPort {

    /** Full solve of a problem; the plan has no changes and no pinned seats. */
    RosterPlan solve(SchedulingProblem problem);

    /** Sick-call repair of a published roster; the plan carries changes against {@code published}. */
    RosterPlan repair(SchedulingProblem problem, List<RosterAssignment> published, SickCall sickCall);
}
