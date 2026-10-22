package com.autowizard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RequestIdTests {

    @Autowired MockMvc mvc;

    @Test
    void everyResponseCarriesAGeneratedRequestId() throws Exception {
        String id = mvc.perform(get("/api/vehicles"))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(id).matches("[0-9a-f-]{36}");
    }

    @Test
    void incomingIdIsPropagatedToTheResponseTheErrorBodyAndTheLogs(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/auth/login").header("X-Request-Id", "login-test-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.it\",\"password\":\"wrong-password\"}"))
                .andExpect(header().string("X-Request-Id", "login-test-42"))
                .andExpect(jsonPath("$.requestId").value("login-test-42"));

        assertThat(output.getOut()).containsPattern("\\[login-test-42] .*\\[AUTH_LOGIN] outcome=failure");
    }

    @Test
    void unsafeIncomingIdIsReplacedToPreventLogInjection() throws Exception {
        String id = mvc.perform(get("/api/vehicles").header("X-Request-Id", "x INFO [AUTH_LOGIN] outcome=success"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(id).matches("[0-9a-f-]{36}");
    }
}
