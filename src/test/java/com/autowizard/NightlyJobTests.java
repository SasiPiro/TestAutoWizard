package com.autowizard;

import com.autowizard.service.NightlyMaintenanceJob;
import com.autowizard.service.TimeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Automatic triggers off: the test runs the job itself, with a fixed clock
@SpringBootTest(properties = "app.scheduling.enabled=false")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class NightlyJobTests {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 0, 1);

    @Autowired MockMvc mvc;
    @Autowired NightlyMaintenanceJob job;
    @Autowired JdbcTemplate jdbc;

    @MockBean TimeService timeService;

    TestApi api;
    String token;

    @BeforeEach
    void fixedClock() throws Exception {
        when(timeService.now()).thenReturn(NOW);
        api = new TestApi(mvc);
        token = api.newUser();
    }

    @Test
    void pendingTasksPastTheirDateOrMileageBecomeOverdue() throws Exception {
        long vehicleId = api.createVehicle(token, 60_000);
        long pastDate = api.createTask(token, vehicleId, "2026-10-20T10:00:00", 90_000);
        long mileageReached = api.createTask(token, vehicleId, "2027-06-01T10:00:00", 60_000);
        long notYet = api.createTask(token, vehicleId, "2026-10-21T10:00:00", 60_001);

        job.run();

        assertThat(taskStatus(pastDate)).isEqualTo("OVERDUE");
        assertThat(taskStatus(mileageReached)).isEqualTo("OVERDUE");
        assertThat(taskStatus(notYet)).isEqualTo("PENDING");
    }

    @Test
    void completedTasksAndTasksOfDeletedVehiclesAreLeftAlone() throws Exception {
        long vehicleId = api.createVehicle(token, 60_000);
        long completed = api.createTask(token, vehicleId, "2026-01-01T10:00:00", 1);
        jdbc.update("UPDATE maintenance_tasks SET status = 'COMPLETED' WHERE id = ?", completed);
        long deletedVehicle = api.createVehicle(token, 60_000);
        long ofDeletedVehicle = api.createTask(token, deletedVehicle, "2026-01-01T10:00:00", 1);
        mvc.perform(delete("/api/vehicles/" + deletedVehicle).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        job.run();

        assertThat(taskStatus(completed)).isEqualTo("COMPLETED");
        assertThat(taskStatus(ofDeletedVehicle)).isEqualTo("PENDING");
    }

    @Test
    void runningTheJobTwiceCreatesOneReminder() throws Exception {
        long vehicleId = api.createVehicle(token, 60_000);
        long overdue = api.createTask(token, vehicleId, "2026-10-01T10:00:00", 90_000);

        job.run();
        job.run();

        assertThat(openReminders("OVERDUE_TASKS")).hasSize(1);
        mvc.perform(get("/api/reminders").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("OVERDUE_TASKS"))
                .andExpect(jsonPath("$[0].taskIds[0]").value(overdue))
                .andExpect(jsonPath("$[0].message").value("Hai 1 manutenzioni che non hai eseguito nei tempi consigliati"));
    }

    @Test
    void theOpenReminderFollowsTheTasksAndDisappearsWhenTheyAreDone() throws Exception {
        long vehicleId = api.createVehicle(token, 60_000);
        long first = api.createTask(token, vehicleId, "2026-10-01T10:00:00", 90_000);
        job.run();
        List<Long> before = openReminders("OVERDUE_TASKS");

        long second = api.createTask(token, vehicleId, "2026-10-02T10:00:00", 90_000);
        job.run();
        assertThat(openReminders("OVERDUE_TASKS")).isEqualTo(before); // same reminder, updated
        assertThat(remindedTasks(before.get(0))).containsExactlyInAnyOrder(first, second);

        for (long taskId : List.of(first, second)) {
            mvc.perform(patch("/api/maintenance-tasks/" + taskId + "/complete").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"completionMileage\":60000}"))
                    .andExpect(status().isOk());
        }
        job.run();
        assertThat(openReminders("OVERDUE_TASKS")).isEmpty();
    }

    @Test
    void aNotifiedReminderIsNotRepeatedForTheSameTasks() throws Exception {
        long vehicleId = api.createVehicle(token, 60_000);
        api.createTask(token, vehicleId, "2026-10-01T10:00:00", 90_000);
        job.run();
        long reminderId = openReminders("OVERDUE_TASKS").get(0);
        mvc.perform(put("/api/reminders/" + reminderId + "/notified").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        job.run();

        assertThat(openReminders("OVERDUE_TASKS")).isEmpty();
    }

    @Test
    void onlyPendingTasksDueSoonGetAnUpcomingReminder() throws Exception {
        long vehicleId = api.createVehicle(token, 10_000);
        long inThreeDays = api.createTask(token, vehicleId, "2026-10-24T10:00:00", 90_000);
        api.createTask(token, vehicleId, "2027-03-01T10:00:00", 90_000);

        job.run();

        List<Long> upcoming = openReminders("PENDING_TASKS");
        assertThat(upcoming).hasSize(1);
        assertThat(remindedTasks(upcoming.get(0))).containsExactly(inThreeDays);
    }

    private List<Long> openReminders(String type) throws Exception {
        String body = mvc.perform(get("/api/reminders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[?(@.type == '" + type + "')].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private List<Long> remindedTasks(long reminderId) {
        return jdbc.queryForList("SELECT task_id FROM reminder_tasks WHERE reminder_id = ?", Long.class, reminderId);
    }

    private String taskStatus(long taskId) {
        return jdbc.queryForObject("SELECT status FROM maintenance_tasks WHERE id = ?", String.class, taskId);
    }
}
