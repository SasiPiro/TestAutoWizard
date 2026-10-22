package com.autowizard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every error is an RFC 7807 problem detail with the status that matches its meaning,
 * and never reveals more than the caller is entitled to know.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class ErrorModelTests {

    @Autowired MockMvc mvc;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(mvc);
    }

    // --- 401 ---

    @Test
    void unknownEmailAndWrongPasswordGetTheSameGeneric401() throws Exception {
        String email = TestApi.randomEmail("login");
        api.register(email);

        String unknownEmail = login(TestApi.randomEmail("nobody"), TestApi.PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(detail(unknownEmail)).isEqualTo(detail(wrongPassword)).isEqualTo("Invalid email or password");
    }

    // --- 409 ---

    @Test
    void duplicateEmailIs409() throws Exception {
        String email = TestApi.randomEmail("dup");
        api.register(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"another-password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://autowizard.example/errors/conflict"));
    }

    @Test
    void duplicateLicensePlateIs409() throws Exception {
        String token = api.newUser();
        String vehicle = TestApi.vehicleJson(10_000);
        mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(vehicle)).andExpect(status().isOk());
        mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + api.newUser())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicle))
                .andExpect(status().isConflict());
    }

    // --- 404 ---

    @Test
    void unknownRoutesAre404NotServerErrors() throws Exception {
        // In the Postman collection, never implemented
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + api.newUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void someoneElsesDocumentIsIndistinguishableFromAMissingOne() throws Exception {
        String owner = api.newUser();
        long vehicleId = api.createVehicle(owner);
        String upload = mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "polizza.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("type", "ASSICURAZIONE").param("vehicleId", String.valueOf(vehicleId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long documentId = ((Number) JsonPath.read(upload, "$.id")).longValue();

        String stranger = api.newUser();
        String foreign = mvc.perform(get("/api/documents/" + documentId).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(get("/api/documents/" + Long.MAX_VALUE).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        assertThat(detail(foreign)).isEqualTo(detail(missing));
    }

    @Test
    void aReminderThatIsNotTheCallersIs404() throws Exception {
        mvc.perform(put("/api/reminders/" + Long.MAX_VALUE + "/notify").header("Authorization", "Bearer " + api.newUser()))
                .andExpect(status().isNotFound());
    }

    // --- 400 ---

    @Test
    void malformedBodyAndWrongParameterTypeAre400() throws Exception {
        String token = api.newUser();
        mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/api/vehicles/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aBrokenBusinessRuleIs400WithItsReason() throws Exception {
        String token = api.newUser();
        long vehicleId = api.createVehicle(token, 50_000);
        mvc.perform(patch("/api/vehicles/" + vehicleId + "/mileage").param("newMileage", "49000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://autowizard.example/errors/business-rule"))
                .andExpect(jsonPath("$.detail").value("New mileage cannot be less than current mileage"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private static String detail(String problemJson) {
        return JsonPath.read(problemJson, "$.detail");
    }
}
