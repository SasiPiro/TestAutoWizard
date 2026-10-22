package com.autowizard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards against hidden queries: authenticating a request costs none, and a listing costs
 * one statement however many rows it returns.
 */
@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.autowizard.SqlCapture")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class QueryCountTests {

    @Autowired MockMvc mvc;

    @Test
    void listingVehiclesIsOneQuery() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        for (int i = 0; i < 3; i++) {
            api.createVehicle(token);
        }

        List<String> statements = statementsOf(token, "/api/vehicles", 3);

        // Before: the user loaded by the JWT filter, then vehicles with their owner joined
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0)).startsWith("select").contains("vehicles").doesNotContain("users");
    }

    @Test
    void listingTasksIsOneQueryWhateverTheNumberOfVehicles() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        for (int i = 0; i < 3; i++) {
            api.createTask(token, api.createVehicle(token), "2030-01-01T10:00:00", 60_000);
        }

        assertThat(statementsOf(token, "/api/maintenance-tasks/user", 3)).hasSize(1);
    }

    private List<String> statementsOf(String token, String path, int expectedItems) throws Exception {
        SqlCapture.clear();
        mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expectedItems));
        return SqlCapture.statements();
    }
}
