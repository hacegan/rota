package dev.rota.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints of docs/contracts/openapi/rota.yaml against the full Spring context, the real solver
 * and the repository fixtures. MockMvc is built from the WebApplicationContext (spring-test) rather
 * than {@code @AutoConfigureMockMvc}, whose package moved in Boot 4 (QUESTIONS.md P06 J3).
 *
 * <p>Expected solver outcomes are the ones the solver module's own tests already assert:
 * mini fully assigned with hard 0 (P03), tight 3 empty seats with reasons and medium -1080 (P03),
 * sick repair = exactly one change S06#0 E2 -> E3 (P05).
 */
@SpringBootTest
class RosterApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void solveMiniReturnsFeasibleFullRoster() throws Exception {
        mockMvc.perform(post("/api/rosters/solve").param("fixture", "mini"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/rosters/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fixture").value("mini"))
                .andExpect(jsonPath("$.operation").value("SOLVE"))
                .andExpect(jsonPath("$.zone").value("Europe/Istanbul"))
                .andExpect(jsonPath("$.score.hard").value(0))
                .andExpect(jsonPath("$.score.medium").value(0))
                .andExpect(jsonPath("$.score.feasible").value(true))
                .andExpect(jsonPath("$.assignments", not(empty())))
                .andExpect(jsonPath("$.assignments[0].seatId").isNotEmpty())
                .andExpect(jsonPath("$.assignments[0].employeeId").isNotEmpty())
                .andExpect(jsonPath("$.assignments[0].start", endsWith("+03:00")))
                .andExpect(jsonPath("$.unassigned", hasSize(0)))
                .andExpect(jsonPath("$.changes", hasSize(0)))
                .andExpect(jsonPath("$.pinnedSeatIds", hasSize(0)));
    }

    @Test
    void solveTightReportsUnassignedSeatsWithReasons() throws Exception {
        mockMvc.perform(post("/api/rosters/solve").param("fixture", "tight"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fixture").value("tight"))
                .andExpect(jsonPath("$.score.hard").value(0))
                .andExpect(jsonPath("$.score.medium").value(-1080))
                .andExpect(jsonPath("$.score.feasible").value(true))
                .andExpect(jsonPath("$.unassigned", hasSize(3)))
                .andExpect(jsonPath("$.unassigned[0].seatId").isNotEmpty())
                .andExpect(jsonPath("$.unassigned[0].skill").isNotEmpty())
                .andExpect(jsonPath("$.unassigned[0].reasons", not(empty())))
                .andExpect(jsonPath("$.unassigned[1].reasons", not(empty())))
                .andExpect(jsonPath("$.unassigned[2].reasons", not(empty())));
    }

    @Test
    void repairSickReturnsChangeList() throws Exception {
        mockMvc.perform(post("/api/rosters/repair").param("fixture", "sick"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/rosters/")))
                .andExpect(jsonPath("$.fixture").value("sick"))
                .andExpect(jsonPath("$.operation").value("REPAIR"))
                .andExpect(jsonPath("$.score.hard").value(0))
                .andExpect(jsonPath("$.unassigned", hasSize(0)))
                .andExpect(jsonPath("$.changes", hasSize(1)))
                .andExpect(jsonPath("$.changes[0].seatId").value("S06#0"))
                .andExpect(jsonPath("$.changes[0].before").value("E2"))
                .andExpect(jsonPath("$.changes[0].after").value("E3"))
                .andExpect(jsonPath("$.changes[0].kind").value("REASSIGNED"));
    }

    @Test
    void storedRosterCanBeReadBackById() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/rosters/solve").param("fixture", "mini"))
                .andExpect(status().isCreated())
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);

        MvcResult fetched = mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixture").value("mini"))
                .andExpect(jsonPath("$.operation").value("SOLVE"))
                .andReturn();
        assertEquals(created.getResponse().getContentAsString(), fetched.getResponse().getContentAsString());
    }

    @Test
    void unknownRosterIdIsNotFound() throws Exception {
        mockMvc.perform(get("/api/rosters/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("roster_not_found"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unknownFixtureIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/rosters/solve").param("fixture", "sick"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unknown_fixture"));
        mockMvc.perform(post("/api/rosters/solve").param("fixture", "../fixtures/mini"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unknown_fixture"));
        mockMvc.perform(post("/api/rosters/repair").param("fixture", "mini"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unknown_fixture"));
    }

    @Test
    void missingFixtureParameterIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/rosters/solve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("missing_parameter"));
        mockMvc.perform(post("/api/rosters/repair"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("missing_parameter"));
    }
}
