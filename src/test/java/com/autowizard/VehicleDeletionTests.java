package com.autowizard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting a vehicle is a soft delete: it disappears from the API, its history stays in the
 * database and nothing is left pointing to a row that does not exist.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class VehicleDeletionTests {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void aVehicleWithTasksReferencedByRemindersCanBeDeleted() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        long taskId = api.createTask(token, vehicleId, "2030-01-01T10:00:00", 60_000);
        long documentId = upload(token, vehicleId);
        remindAbout(vehicleId, taskId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reminder_tasks WHERE task_id = ?", Integer.class, taskId))
                .isEqualTo(1);

        mvc.perform(delete("/api/vehicles/" + vehicleId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Gone from the API...
        mvc.perform(get("/api/vehicles/" + vehicleId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        assertThat(ids(get("/api/vehicles"), token)).doesNotContain(vehicleId);
        assertThat(ids(get("/api/maintenance-tasks/user"), token)).doesNotContain(taskId);
        assertThat(ids(get("/api/maintenance-tasks/pending/user"), token)).doesNotContain(taskId);
        mvc.perform(get("/api/documents/" + documentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // ...kept in the database, with no reference to a missing row
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE id = ? AND deleted_at IS NOT NULL",
                Integer.class, vehicleId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM reminder_tasks rt
                 WHERE NOT EXISTS (SELECT 1 FROM maintenance_tasks t WHERE t.id = rt.task_id)""", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM maintenance_tasks t
                 WHERE NOT EXISTS (SELECT 1 FROM vehicles v WHERE v.id = t.vehicle_id)""", Integer.class)).isZero();
    }

    @Test
    void theSamePlateCanBeRegisteredAgainAfterDeletion() throws Exception {
        TestApi api = new TestApi(mvc);
        String seller = api.newUser();
        String vehicle = TestApi.vehicleJson(80_000);
        long vehicleId = create(seller, vehicle);

        mvc.perform(delete("/api/vehicles/" + vehicleId).header("Authorization", "Bearer " + seller))
                .andExpect(status().isOk());

        assertThat(create(api.newUser(), vehicle)).isNotEqualTo(vehicleId);
    }

    /** A reminder linked to the task, written directly: the reminder API is not under test here. */
    private void remindAbout(long vehicleId, long taskId) {
        Long userId = jdbc.queryForObject("SELECT owner_id FROM vehicles WHERE id = ?", Long.class, vehicleId);
        jdbc.update("""
                INSERT INTO reminders (title, message, notification_date, notified, user_id, type)
                VALUES ('Manutenzioni', '1 task', CURRENT_TIMESTAMP, FALSE, ?, 'PENDING_TASKS')""", userId);
        Long reminderId = jdbc.queryForObject("SELECT MAX(id) FROM reminders WHERE user_id = ?", Long.class, userId);
        jdbc.update("INSERT INTO reminder_tasks (reminder_id, task_id) VALUES (?, ?)", reminderId, taskId);
    }

    private long create(String token, String vehicleJson) throws Exception {
        String body = mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleJson))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long upload(String token, long vehicleId) throws Exception {
        String body = mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "bollo.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("type", "BOLLO").param("vehicleId", String.valueOf(vehicleId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private List<Long> ids(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                           String token) throws Exception {
        String body = mvc.perform(request.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }
}
