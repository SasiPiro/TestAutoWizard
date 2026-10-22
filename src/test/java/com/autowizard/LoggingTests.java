package com.autowizard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Log lines are part of the contract with whoever operates the application:
 * one consistent [EVENT] key=value format, the right level, no duplicates, no secrets.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LoggingTests {

    @Autowired MockMvc mvc;

    @Test
    void failedLoginIsOneWarnWithTheEmailAndNoPassword(CapturedOutput output) throws Exception {
        String email = TestApi.randomEmail("lg");
        new TestApi(mvc).register(email);
        int from = output.getOut().length();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"S3cret-typed-by-user\"}"))
                .andExpect(status().isUnauthorized());

        String logs = output.getOut().substring(from);
        assertThat(logs).containsPattern(" WARN .*\\[AUTH_LOGIN] outcome=failure email=" + Pattern.quote(email)
                + " reason=BadCredentialsException");
        assertThat(logs).doesNotContain("S3cret-typed-by-user");
        assertThat(count(logs, "[AUTH_LOGIN]")).isEqualTo(1);
        assertThat(logs).doesNotContain("[HTTP_SERVER_ERROR]").doesNotContain(" ERROR ");
    }

    @Test
    void duplicateRegistrationIsAnInfoAndNotAServerError(CapturedOutput output) throws Exception {
        String email = TestApi.randomEmail("dup");
        new TestApi(mvc).register(email);
        int from = output.getOut().length();

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-456\"}"))
                .andExpect(status().isConflict());

        String logs = output.getOut().substring(from);
        assertThat(logs).contains("[AUTH_REGISTER] outcome=rejected email=" + email + " reason=duplicate_email");
        assertThat(logs).doesNotContain("[HTTP_SERVER_ERROR]").doesNotContain(" ERROR ");
    }

    @Test
    void aRejectedRequestIsLoggedOnceAtInfo(CapturedOutput output) throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token, 50_000);
        int from = output.getOut().length();

        mvc.perform(patch("/api/vehicles/" + vehicleId + "/mileage").param("newMileage", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        String logs = output.getOut().substring(from);
        assertThat(logs).containsPattern(" INFO .*\\[HTTP_CLIENT_ERROR] status=400 method=PATCH .*exception=BusinessRuleException");
        assertThat(count(logs, "[HTTP_CLIENT_ERROR]")).isEqualTo(1);
        assertThat(logs).doesNotContain(" ERROR ").doesNotContain(" WARN ");
    }

    @Test
    void anUploadIsOneEventLineWithIdsAndNoFileName(CapturedOutput output) throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        int from = output.getOut().length();

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "polizza-mario-rossi.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("type", "ASSICURAZIONE").param("vehicleId", String.valueOf(vehicleId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String logs = output.getOut().substring(from);
        assertThat(logs).containsPattern("\\[DOCUMENT_UPLOAD] outcome=success documentId=\\d+ vehicleId=" + vehicleId
                + " type=ASSICURAZIONE contentType=application/pdf size=8");
        // File names often contain personal data
        assertThat(logs).doesNotContain("mario-rossi");
    }

    private static int count(String text, String token) {
        return text.split(Pattern.quote(token), -1).length - 1;
    }
}
