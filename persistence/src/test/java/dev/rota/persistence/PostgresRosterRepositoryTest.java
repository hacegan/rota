package dev.rota.persistence;

import dev.rota.application.Roster;
import dev.rota.application.RosterPlan;
import dev.rota.domain.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The persistence adapter against a real PostgreSQL in Docker (Testcontainers core
 * {@link GenericContainer}; QUESTIONS.md P09 P5). Flyway V1-V4 run on startup.
 * The image tag must match VERSIONS.md and platform/compose/postgres.yml (checked by verify_p09).
 */
@SpringJUnitConfig(PersistenceConfiguration.class)
@ActiveProfiles("postgres")
class PostgresRosterRepositoryTest {

    static final String POSTGRES_IMAGE = "postgres:19.0";

    @SuppressWarnings("resource") // stopped by Testcontainers' Ryuk when the test JVM ends
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withEnv("POSTGRES_DB", "rota_test")
            .withEnv("POSTGRES_USER", "rota")
            .withEnv("POSTGRES_PASSWORD", "rota-test-only")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("rota.datasource.url",
                () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/rota_test");
        registry.add("rota.datasource.username", () -> "rota");
        registry.add("rota.datasource.password", () -> "rota-test-only");
    }

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final TimeRange MON_EARLY = range("2026-11-23T05:00:00Z", "2026-11-23T11:00:00Z");
    private static final TimeRange MON_LATE = range("2026-11-23T11:00:00Z", "2026-11-23T17:00:00Z");
    private static final TimeRange SUN_LATE = range("2026-11-29T11:00:00Z", "2026-11-29T17:00:00Z");

    @Autowired
    private PostgresRosterRepository repository;

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatedTheFourTables() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "select table_name from information_schema.tables where table_schema = 'public'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        assertTrue(tables.containsAll(Set.of("roster", "assignment", "unassigned_seat", "roster_change")), tables::toString);
    }

    @Test
    void nextIdIsUniqueAndPrefixed() {
        String first = repository.nextId();
        String second = repository.nextId();
        assertTrue(first.matches("r\\d+"), first);
        assertTrue(second.matches("r\\d+"), second);
        assertNotEquals(first, second);
    }

    @Test
    void solveRosterRoundTripsUnchanged() {
        Roster roster = solveRoster(repository.nextId());
        repository.save(roster);

        assertEquals(Optional.of(roster), repository.findById(roster.id()));
    }

    @Test
    void repairRosterRoundTripsChangesPinnedSeatsAndOrder() {
        RosterPlan plan = new RosterPlan(new RosterPlan.ScoreSummary(0, -360, -1200),
                List.of(new RosterPlan.Assignment("S02#0", MON_LATE, "cashier", "E3"),
                        new RosterPlan.Assignment("S01#0", MON_EARLY, "barista", "E1")),
                List.of(new RosterPlan.UnassignedSeat("S14#0", SUN_LATE, "cashier",
                        List.of("E1: week total would be 42h > max 40h", "E2: not available for 360 of 360 min"))),
                List.of(new RosterPlan.Change("S02#0", "E2", "E3", RosterPlan.Change.Kind.REASSIGNED),
                        new RosterPlan.Change("S14#0", "E2", null, RosterPlan.Change.Kind.LEFT_EMPTY),
                        new RosterPlan.Change("S09#0", null, "E1", RosterPlan.Change.Kind.FILLED)),
                List.of("S01#0", "S00#1"));
        Roster roster = new Roster(repository.nextId(), "sick", Roster.Operation.REPAIR, ZONE, plan);
        repository.save(roster);

        Roster loaded = repository.findById(roster.id()).orElseThrow();
        assertEquals(roster, loaded);
        assertEquals(List.of("S02#0", "S01#0"),
                loaded.plan().assignments().stream().map(RosterPlan.Assignment::seatId).toList());
    }

    @Test
    void savingSameIdReplacesContent() {
        String id = repository.nextId();
        repository.save(solveRoster(id));
        Roster smaller = new Roster(id, "mini", Roster.Operation.SOLVE, ZONE,
                new RosterPlan(new RosterPlan.ScoreSummary(0, 0, -1),
                        List.of(new RosterPlan.Assignment("S01#0", MON_EARLY, "barista", "E2")),
                        List.of(), List.of(), List.of()));
        repository.save(smaller);

        assertEquals(Optional.of(smaller), repository.findById(id));
    }

    @Test
    void unknownIdIsEmpty() {
        assertFalse(repository.findById("r-does-not-exist").isPresent());
        assertFalse(repository.findById(null).isPresent());
    }

    @Test
    void multiLineReasonIsRejectedNotSplit() {
        Roster roster = new Roster(repository.nextId(), "tight", Roster.Operation.SOLVE, ZONE,
                new RosterPlan(new RosterPlan.ScoreSummary(0, -360, 0), List.of(),
                        List.of(new RosterPlan.UnassignedSeat("S14#0", SUN_LATE, "cashier", List.of("E1: a\nb"))),
                        List.of(), List.of()));

        assertThrows(IllegalArgumentException.class, () -> repository.save(roster));
        assertFalse(repository.findById(roster.id()).isPresent());
    }

    private static Roster solveRoster(String id) {
        RosterPlan plan = new RosterPlan(new RosterPlan.ScoreSummary(0, -360, -25),
                List.of(new RosterPlan.Assignment("S01#0", MON_EARLY, "barista", "E1"),
                        new RosterPlan.Assignment("S02#0", MON_LATE, "cashier", "E2")),
                List.of(new RosterPlan.UnassignedSeat("S14#0", SUN_LATE, "cashier",
                        List.of("E1: week total would be 42h > max 40h", "E3: rest 0h next to S13#0 (min 11h)"))),
                List.of(), List.of());
        return new Roster(id, "tight", Roster.Operation.SOLVE, ZONE, plan);
    }

    private static TimeRange range(String start, String end) {
        return new TimeRange(Instant.parse(start), Instant.parse(end));
    }
}
