package com.autowizard;

import com.autowizard.ai.internal.AIService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** An unexpected failure is a 500 that tells the client nothing about the internals. */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class ServerErrorTests {

    @Autowired MockMvc mvc;

    @MockBean AIService aiService;

    @Test
    void anUnexpectedExceptionIsA500WithoutItsMessage() throws Exception {
        when(aiService.getAIResponse(any()))
                .thenThrow(new IllegalStateException("connection refused: db-internal.example:5432 user=admin"));

        String body = mvc.perform(post("/api/ai/query").header("Authorization", "Bearer " + new TestApi(mvc).newUser())
                        .contentType(MediaType.TEXT_PLAIN).content("when is my insurance due?"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("db-internal").doesNotContain("IllegalStateException")
                .contains("An unexpected error occurred");
    }
}
