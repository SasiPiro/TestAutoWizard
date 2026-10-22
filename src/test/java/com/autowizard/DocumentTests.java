package com.autowizard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.autowizard.SqlCapture")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DocumentTests {

    static final byte[] PDF = "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    @Autowired MockMvc mvc;

    String token;
    long vehicleId;

    @BeforeEach
    void userWithAVehicle() throws Exception {
        TestApi api = new TestApi(mvc);
        token = api.newUser();
        vehicleId = api.createVehicle(token);
    }

    @Test
    void listingDocumentsDoesNotReadTheFiles() throws Exception {
        upload("polizza.pdf", "application/pdf", PDF).andExpect(status().isOk());
        upload("bollo.png", "image/png", PNG).andExpect(status().isOk());

        SqlCapture.clear();
        mvc.perform(get("/api/documents/vehicle/" + vehicleId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").doesNotExist());

        assertThat(SqlCapture.statements()).filteredOn(sql -> sql.contains("documents")).isNotEmpty()
                .noneMatch(sql -> sql.matches("(?s).*\\.content\\b.*"));
    }

    @Test
    void theStoredTypeIsDetectedFromTheContentNotTakenFromTheClient() throws Exception {
        long id = id(upload("scan.pdf", "application/pdf", PNG).andExpect(status().isOk()));

        mvc.perform(get("/api/documents/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void onlyPdfJpegAndPngAreAccepted() throws Exception {
        upload("invoice.pdf", "application/pdf", "<html><script>alert(1)</script>".getBytes())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("https://autowizard.example/errors/unsupported-file"));
        upload("empty.pdf", "application/pdf", new byte[0]).andExpect(status().isBadRequest());
    }

    @Test
    void theFileNameIsStrippedOfPathsAndSentBackEncoded() throws Exception {
        long id = id(upload("C:\\Users\\mario\\Documenti\\libretto è.pdf", "application/pdf", PDF)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("libretto è.pdf")));

        String disposition = mvc.perform(get("/api/documents/" + id).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getHeader("Content-Disposition");
        assertThat(disposition).startsWith("attachment;").contains("filename*=UTF-8''libretto%20%C3%A8.pdf");
    }

    @Test
    void aDeletedDocumentIsGoneAndOnlyItsOwnerCanDeleteIt() throws Exception {
        long id = id(upload("polizza.pdf", "application/pdf", PDF));
        String stranger = new TestApi(mvc).newUser();

        mvc.perform(delete("/api/documents/" + id).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/documents/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/documents/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private ResultActions upload(String name, String declaredType, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/documents/upload")
                .file(new MockMultipartFile("file", name, declaredType, bytes))
                .param("type", "ASSICURAZIONE").param("vehicleId", String.valueOf(vehicleId))
                .header("Authorization", "Bearer " + token));
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
