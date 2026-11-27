package dev.rota.bootstrap;

import dev.rota.application.RosterRepository;
import dev.rota.persistence.PostgresRosterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profile "postgres": the API stores rosters in a real PostgreSQL (Testcontainers) and reads them
 * back unchanged. Same endpoints and JSON as the in-memory profile (rota.yaml).
 */
@SpringBootTest
@ActiveProfiles("postgres")
class PostgresProfileApiTest {

    /** Must match VERSIONS.md and platform/compose/postgres.yml (checked by verify_p09). */
    static final String POSTGRES_IMAGE = "postgres:19.0";

    @SuppressWarnings("resource") // stopped by Testcontainers' Ryuk when the test JVM ends
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withEnv("POSTGRES_DB", "rota_api_test")
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
                () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/rota_api_test");
        registry.add("rota.datasource.username", () -> "rota");
        registry.add("rota.datasource.password", () -> "rota-test-only");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RosterRepository rosterRepository;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void rosterRepositoryIsPostgres() {
        assertInstanceOf(PostgresRosterRepository.class, rosterRepository);
    }

    @Test
    void solvedRosterIsStoredInPostgresAndReadBackUnchanged() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/rosters/solve").param("fixture", "mini"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score.hard").value(0))
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        String id = location.substring(location.lastIndexOf('/') + 1);

        assertEquals(14, countRows("select count(*) from assignment where roster_id = ?", id));
        assertEquals(1, countRows("select count(*) from roster where id = ?", id));

        MvcResult fetched = mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn();
        assertEquals(created.getResponse().getContentAsString(), fetched.getResponse().getContentAsString());
    }

    @Test
    void repairChangeListIsStored() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/rosters/repair").param("fixture", "sick"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.changes[0].seatId").value("S06#0"))
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        String id = location.substring(location.lastIndexOf('/') + 1);

        assertTrue(countRows("select count(*) from roster_change where roster_id = ?", id) >= 1);
        MvcResult fetched = mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn();
        assertEquals(created.getResponse().getContentAsString(), fetched.getResponse().getContentAsString());
    }

    private long countRows(String sql, String rosterId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rosterId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
