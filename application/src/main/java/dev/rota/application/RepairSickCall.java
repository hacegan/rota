package dev.rota.application;

import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Use case: repair a fixture's published roster after its sick call (P05 repair) and store the
 * repaired roster with its change list.
 */
public final class RepairSickCall {

    /** Fixtures a repair accepts (P06 scope): they carry published-roster.json and sick-call.json. */
    public static final Set<String> FIXTURES = Set.of("sick");

    private final FixturePort fixtures;
    private final SchedulingPort scheduling;
    private final RosterRepository rosters;

    public RepairSickCall(FixturePort fixtures, SchedulingPort scheduling, RosterRepository rosters) {
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.scheduling = Objects.requireNonNull(scheduling, "scheduling");
        this.rosters = Objects.requireNonNull(rosters, "rosters");
    }

    public Roster repair(String fixture) {
        if (fixture == null || !FIXTURES.contains(fixture)) {
            throw new UnknownFixtureException(fixture, FIXTURES);
        }
        SchedulingProblem problem = fixtures.problem(fixture);
        List<RosterAssignment> published = fixtures.publishedRoster(fixture);
        SickCall sickCall = fixtures.sickCall(fixture);
        RosterPlan plan = scheduling.repair(problem, published, sickCall);
        Roster roster = new Roster(rosters.nextId(), fixture, Roster.Operation.REPAIR, problem.zone(), plan);
        rosters.save(roster);
        return roster;
    }
}
