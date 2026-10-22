package com.autowizard;

import com.autowizard.maintenance.MaintenanceTaskCompleted;
import com.autowizard.reminder.internal.ReminderService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Completing a task publishes MaintenanceTaskCompleted; the reminder module reacts after the
 * commit, and the publication is recorded until the listener has succeeded.
 */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class TaskEventsTests {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ReminderService reminderService;

    @Test
    void aCompletedTaskLeavesTheOpenRemindersWithoutWaitingForTheNightlyJob() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token, 10_000);
        long done = api.createTask(token, vehicleId, "2031-05-02T10:00:00", 90_000);
        long stillDue = api.createTask(token, vehicleId, "2031-05-03T10:00:00", 90_000);
        reminderService.refreshReminders(LocalDateTime.of(2031, 5, 1, 0, 1));
        assertThat(remindedTasks(token)).containsExactlyInAnyOrder(done, stillDue);

        mvc.perform(patch("/api/maintenance-tasks/" + done + "/complete").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completionMileage\":10000}"))
                .andExpect(status().isOk());

        await().atMost(WAIT).until(() -> remindedTasks(token).equals(List.of(stillDue)));
        await().atMost(WAIT).until(() -> completedPublications(done) == 1);
    }

    private List<Long> remindedTasks(String token) throws Exception {
        String body = mvc.perform(get("/api/reminders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[*].taskIds[*]");
        return ids.stream().map(Number::longValue).toList();
    }

    private int completedPublications(long taskId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_publication
                 WHERE event_type = ? AND serialized_event LIKE ? AND completion_date IS NOT NULL""",
                Integer.class, MaintenanceTaskCompleted.class.getName(), "%\"taskId\":" + taskId + ",%");
    }
}
