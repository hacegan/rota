package dev.rota.application;

import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;
import dev.rota.domain.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SolveRoster, RepairSickCall and GetRoster against in-test fakes (no solver, no files, no Spring). */
class UseCasesTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final TimeRange MONDAY_EARLY = new TimeRange(
            Instant.parse("2026-11-23T04:00:00Z"), Instant.parse("2026-11-23T12:00:00Z"));

    private final FakeFixtures fixtures = new FakeFixtures();
    private final FakeScheduling scheduling = new FakeScheduling();
    private final FakeRepository rosters = new FakeRepository();

    @Test
    void solveLoadsFixtureSolvesAndStoresRoster() {
        Roster roster = new SolveRoster(fixtures, scheduling, rosters).solve("mini");

        assertEquals(List.of("problem:mini"), fixtures.calls);
        assertEquals(List.of("solve"), scheduling.calls);
        assertEquals("R1", roster.id());
        assertEquals("mini", roster.fixture());
        assertEquals(Roster.Operation.SOLVE, roster.operation());
        assertEquals(ZONE, roster.zone());
        assertSame(FakeScheduling.SOLVED, roster.plan());
        assertSame(roster, rosters.stored.get("R1"));
    }

    @Test
    void solveAcceptsTight() {
        assertEquals("tight", new SolveRoster(fixtures, scheduling, rosters).solve("tight").fixture());
    }

    @Test
    void solveRejectsUnknownFixtureBeforeLoadingAnything() {
        SolveRoster solve = new SolveRoster(fixtures, scheduling, rosters);

        assertThrows(UnknownFixtureException.class, () -> solve.solve("sick"));
        assertThrows(UnknownFixtureException.class, () -> solve.solve("../etc"));
        assertThrows(UnknownFixtureException.class, () -> solve.solve(null));
        assertTrue(fixtures.calls.isEmpty());
        assertTrue(scheduling.calls.isEmpty());
        assertTrue(rosters.stored.isEmpty());
    }

    @Test
    void repairPassesPublishedRosterAndSickCallToSolverAndStoresChanges() {
        Roster roster = new RepairSickCall(fixtures, scheduling, rosters).repair("sick");

        assertEquals(List.of("problem:sick", "published:sick", "sickCall:sick"), fixtures.calls);
        assertEquals(List.of("repair:1:E2"), scheduling.calls);
        assertEquals(Roster.Operation.REPAIR, roster.operation());
        assertEquals("sick", roster.fixture());
        assertEquals(List.of(new RosterPlan.Change("S06#0", "E2", "E3", RosterPlan.Change.Kind.REASSIGNED)),
                roster.plan().changes());
        assertSame(roster, rosters.stored.get(roster.id()));
    }

    @Test
    void repairRejectsFixtureWithoutSickCall() {
        RepairSickCall repair = new RepairSickCall(fixtures, scheduling, rosters);

        assertThrows(UnknownFixtureException.class, () -> repair.repair("mini"));
        assertTrue(scheduling.calls.isEmpty());
        assertTrue(rosters.stored.isEmpty());
    }

    @Test
    void getReturnsStoredRosterAndFailsForUnknownId() {
        Roster stored = new SolveRoster(fixtures, scheduling, rosters).solve("mini");
        GetRoster get = new GetRoster(rosters);

        assertSame(stored, get.get(stored.id()));
        assertThrows(RosterNotFoundException.class, () -> get.get("nope"));
        assertThrows(RosterNotFoundException.class, () -> get.get(null));
    }

    @Test
    void eachStoredRosterGetsItsOwnId() {
        SolveRoster solve = new SolveRoster(fixtures, scheduling, rosters);
        Roster first = solve.solve("mini");
        Roster second = solve.solve("mini");

        assertFalse(first.id().equals(second.id()));
        assertEquals(2, rosters.stored.size());
    }

    @Test
    void feasibleMeansNoHardPenalty() {
        assertTrue(new RosterPlan.ScoreSummary(0, -1080, -5).feasible());
        assertFalse(new RosterPlan.ScoreSummary(-1, 0, 0).feasible());
    }

    // --- fakes ---

    private static final class FakeFixtures implements FixturePort {

        final List<String> calls = new ArrayList<>();

        @Override
        public SchedulingProblem problem(String fixture) {
            calls.add("problem:" + fixture);
            return new SchedulingProblem(ZONE, List.of(), List.of());
        }

        @Override
        public List<RosterAssignment> publishedRoster(String fixture) {
            calls.add("published:" + fixture);
            return List.of(new RosterAssignment("S06", 0, "E2"));
        }

        @Override
        public SickCall sickCall(String fixture) {
            calls.add("sickCall:" + fixture);
            return new SickCall("E2", MONDAY_EARLY, MONDAY_EARLY.start());
        }
    }

    private static final class FakeScheduling implements SchedulingPort {

        static final RosterPlan SOLVED = new RosterPlan(new RosterPlan.ScoreSummary(0, 0, -10),
                List.of(new RosterPlan.Assignment("S01#0", MONDAY_EARLY, "cashier", "E1")),
                List.of(), List.of(), List.of());

        final List<String> calls = new ArrayList<>();

        @Override
        public RosterPlan solve(SchedulingProblem problem) {
            calls.add("solve");
            return SOLVED;
        }

        @Override
        public RosterPlan repair(SchedulingProblem problem, List<RosterAssignment> published, SickCall sickCall) {
            calls.add("repair:" + published.size() + ":" + sickCall.employeeId());
            return new RosterPlan(new RosterPlan.ScoreSummary(0, 0, -1000), List.of(), List.of(),
                    List.of(new RosterPlan.Change("S06#0", "E2", "E3", RosterPlan.Change.Kind.REASSIGNED)),
                    List.of());
        }
    }

    private static final class FakeRepository implements RosterRepository {

        final Map<String, Roster> stored = new HashMap<>();
        private int next = 1;

        @Override
        public String nextId() {
            return "R" + next++;
        }

        @Override
        public void save(Roster roster) {
            stored.put(roster.id(), roster);
        }

        @Override
        public Optional<Roster> findById(String id) {
            return Optional.ofNullable(stored.get(id));
        }
    }
}
