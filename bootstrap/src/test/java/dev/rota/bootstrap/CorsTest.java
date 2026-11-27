package dev.rota.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The P07 web demo origin may call the API from the browser; other origins may not. */
@SpringBootTest
class CorsTest {

    private static final String WEB_ORIGIN = "http://localhost:3000";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void preflightFromWebDemoOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/rosters/solve")
                        .param("fixture", "mini")
                        .header("Origin", WEB_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB_ORIGIN));
    }

    @Test
    void simpleRequestFromWebDemoOriginGetsAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/health").header("Origin", WEB_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB_ORIGIN));
    }

    @Test
    void otherOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/rosters/solve")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
