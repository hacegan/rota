package dev.rota.bootstrap;

import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fixture adapter reads the repository fixtures and never leaves the fixtures folder. */
class FixtureFilesTest {

    private final FixtureFiles fixtures = new FixtureFiles(FixtureFiles.locate(""));

    @Test
    void locatesRepositoryFixturesFolder() {
        assertTrue(Files.isDirectory(fixtures.fixturesDir().resolve("mini")));
        assertTrue(Files.isDirectory(fixtures.fixturesDir().resolve("tight")));
        assertTrue(Files.isDirectory(fixtures.fixturesDir().resolve("sick")));
    }

    @Test
    void loadsMiniAndTightProblems() {
        SchedulingProblem mini = fixtures.problem("mini");
        assertEquals(FixtureFiles.DEFAULT_ZONE, mini.zone());
        assertFalse(mini.employees().isEmpty());
        assertFalse(mini.shifts().isEmpty());

        SchedulingProblem tight = fixtures.problem("tight");
        assertEquals(FixtureFiles.DEFAULT_ZONE, tight.zone());
        assertFalse(tight.shifts().isEmpty());
    }

    @Test
    void loadsSickPublishedRosterAndSickCall() {
        List<RosterAssignment> published = fixtures.publishedRoster("sick");
        assertTrue(published.stream().anyMatch(a -> a.seatId().equals("S06#0") && a.employeeId().equals("E2")));

        SickCall sick = fixtures.sickCall("sick");
        assertEquals("E2", sick.employeeId());
    }

    @Test
    void rejectsUnsafeOrUnknownNames() {
        assertThrows(IllegalArgumentException.class, () -> fixtures.problem("../fixtures/mini"));
        assertThrows(IllegalArgumentException.class, () -> fixtures.problem("Mini"));
        assertThrows(IllegalArgumentException.class, () -> fixtures.problem(""));
        assertThrows(IllegalArgumentException.class, () -> fixtures.problem(null));
        assertThrows(IllegalArgumentException.class, () -> fixtures.problem("does-not-exist"));
    }

    @Test
    void configuredFolderMustExist() {
        assertThrows(IllegalStateException.class, () -> FixtureFiles.locate("no/such/folder/anywhere"));
        assertEquals(fixtures.fixturesDir(), FixtureFiles.locate(fixtures.fixturesDir().toString()));
    }
}
