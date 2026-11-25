package dev.rota.application;

import dev.rota.domain.SchedulingProblem;

import java.util.Objects;
import java.util.Set;

/** Use case: solve a fixture from scratch and store the roster. */
public final class SolveRoster {

    /** Fixtures a solve accepts (P06 scope). */
    public static final Set<String> FIXTURES = Set.of("mini", "tight");

    private final FixturePort fixtures;
    private final SchedulingPort scheduling;
    private final RosterRepository rosters;

    public SolveRoster(FixturePort fixtures, SchedulingPort scheduling, RosterRepository rosters) {
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.scheduling = Objects.requireNonNull(scheduling, "scheduling");
        this.rosters = Objects.requireNonNull(rosters, "rosters");
    }

    public Roster solve(String fixture) {
        if (fixture == null || !FIXTURES.contains(fixture)) {
            throw new UnknownFixtureException(fixture, FIXTURES);
        }
        SchedulingProblem problem = fixtures.problem(fixture);
        RosterPlan plan = scheduling.solve(problem);
        Roster roster = new Roster(rosters.nextId(), fixture, Roster.Operation.SOLVE, problem.zone(), plan);
        rosters.save(roster);
        return roster;
    }
}
