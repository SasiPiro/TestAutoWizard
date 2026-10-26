package com.autowizard;

import com.autowizard.document.internal.DocumentRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documents are metadata in PostgreSQL and files in an S3-compatible object store (both in
 * Docker): the tests check both sides, through the API and directly.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DocumentTests {

    static final byte[] PDF = "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired S3Client s3;
    @MockitoSpyBean DocumentRepository documentRepository;

    String token;
    long vehicleId;

    @BeforeEach
    void userWithAVehicle() throws Exception {
        TestApi api = new TestApi(mvc);
        token = api.newUser();
        vehicleId = api.createVehicle(token);
    }

    @Test
    void theDatabaseKeepsOnlyTheMetadata() {
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = current_schema() AND table_name = 'documents'""", String.class))
                .contains("storage_key", "size")
                .doesNotContain("content");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = current_schema() AND data_type = 'bytea'""", Integer.class))
                .isZero();
    }

    @Test
    void aFileMakesTheRoundTripThroughObjectStorage() throws Exception {
        long id = id(upload("polizza.pdf", "application/pdf", PDF).andExpect(status().isOk()));
        String key = jdbc.queryForObject("SELECT storage_key FROM documents WHERE id = ?", String.class, id);

        assertThat(key).matches("documents/[0-9a-f-]{36}").doesNotContain("polizza");
        assertThat(storedFile(key)).isEqualTo(PDF);
        mvc.perform(get("/api/documents/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().longValue("Content-Length", PDF.length))
                .andExpect(content().bytes(PDF));

        mvc.perform(delete("/api/documents/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThatThrownBy(() -> storedFile(key)).isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void aFailedInsertLeavesNoFileBehind() throws Exception {
        int filesBefore = storedFileCount();
        doThrow(new IllegalStateException("database unavailable")).when(documentRepository).save(any());

        upload("polizza.pdf", "application/pdf", PDF).andExpect(status().isInternalServerError());

        assertThat(storedFileCount()).isEqualTo(filesBefore);
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

    private byte[] storedFile(String key) {
        ResponseBytes<?> file = s3.getObjectAsBytes(request -> request.bucket(ObjectStorageContainer.BUCKET).key(key));
        return file.asByteArray();
    }

    private int storedFileCount() {
        return s3.listObjectsV2(request -> request.bucket(ObjectStorageContainer.BUCKET).prefix("documents/")).keyCount();
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
