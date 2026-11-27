package dev.rota.bootstrap;

import dev.rota.application.FixturePort;
import dev.rota.application.GetRoster;
import dev.rota.application.RepairSickCall;
import dev.rota.application.RosterRepository;
import dev.rota.application.SchedulingPort;
import dev.rota.application.SolveRoster;
import dev.rota.persistence.PersistenceConfiguration;
import dev.rota.solver.RosterRepairer;
import dev.rota.solver.RosterSolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * Wires the pure-Java use cases to their adapters. Spring stays in this module: application and
 * domain classes are plain constructors, no annotations.
 *
 * <p>Roster storage: default profile = {@link InMemoryRosterRepository} (P06-P08 behaviour, E2E);
 * profile {@code postgres} = the persistence module's PostgreSQL adapter
 * ({@link PersistenceConfiguration}, active only under that profile).
 *
 * <p>Properties (application.properties):
 * <ul>
 *   <li>{@code rota.fixtures-dir}: fixtures folder; blank = walk up from the working directory.</li>
 *   <li>{@code rota.solver.spent-limit}, {@code rota.solver.unimproved-spent-limit}: ISO-8601
 *       durations, defaults match {@code new RosterSolver()} (PT30S / PT3S).</li>
 *   <li>{@code rota.datasource.*}: profile {@code postgres} only (application-postgres.properties).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@Import(PersistenceConfiguration.class)
public class RotaConfiguration {

    @Bean
    FixtureFiles fixtureFiles(@Value("${rota.fixtures-dir:}") String fixturesDir) {
        return new FixtureFiles(FixtureFiles.locate(fixturesDir));
    }

    @Bean
    RosterSolver rosterSolver(@Value("${rota.solver.spent-limit:PT30S}") String spentLimit,
                              @Value("${rota.solver.unimproved-spent-limit:PT3S}") String unimprovedSpentLimit) {
        return new RosterSolver(Duration.parse(spentLimit), Duration.parse(unimprovedSpentLimit));
    }

    @Bean
    SolverSchedulingAdapter schedulingAdapter(RosterSolver rosterSolver) {
        return new SolverSchedulingAdapter(rosterSolver, new RosterRepairer(rosterSolver));
    }

    /** Default storage; replaced by the PostgreSQL adapter under profile {@code postgres}. */
    @Bean
    @Profile("!postgres")
    InMemoryRosterRepository rosterRepository() {
        return new InMemoryRosterRepository();
    }

    @Bean
    SolveRoster solveRoster(FixturePort fixtures, SchedulingPort scheduling, RosterRepository rosters) {
        return new SolveRoster(fixtures, scheduling, rosters);
    }

    @Bean
    RepairSickCall repairSickCall(FixturePort fixtures, SchedulingPort scheduling, RosterRepository rosters) {
        return new RepairSickCall(fixtures, scheduling, rosters);
    }

    @Bean
    GetRoster getRoster(RosterRepository rosters) {
        return new GetRoster(rosters);
    }
}
