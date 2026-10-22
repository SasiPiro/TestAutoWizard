package com.autowizard;

import com.autowizard.reminder.internal.NightlyMaintenanceJob;
import com.autowizard.shared.TimeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Automatic triggers off: the test runs the job itself, with a fixed clock
@SpringBootTest(properties = {"app.scheduling.enabled=false", "app.mail.enabled=true"})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class NightlyJobTests {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 0, 1);

    @Autowired MockMvc mvc;
    @Autowired NightlyMaintenanceJob job;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean TimeService timeService;
    @MockitoBean JavaMailSender mailSender;

    TestApi api;
    String email;
    String token;

    @BeforeEach
    void fixedClock() throws Exception {
        when(timeService.now()).thenReturn(NOW);
        api = new TestApi(mvc);
        email = TestApi.randomEmail("job");
        token = api.register(email);
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
    void aReminderIsEmailedOnceWhenCreatedAndAgainOnlyWhenItChanges() throws Exception {
        long vehicleId = api.createVehicle(token, 60_000);
        api.createTask(token, vehicleId, "2026-10-01T10:00:00", 90_000);

        job.run();
        job.run();
        assertThat(emailsTo(email)).hasSize(1);
        assertThat(emailsTo(email).get(0).getSubject()).isEqualTo("AutoWizard: Manutenzioni scadute");

        api.createTask(token, vehicleId, "2026-10-02T10:00:00", 90_000);
        job.run();
        assertThat(emailsTo(email)).hasSize(2);
    }

    @Test
    void aFailingMailServerDoesNotLoseTheReminder() throws Exception {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        api.createTask(token, api.createVehicle(token, 60_000), "2026-10-01T10:00:00", 90_000);

        job.run();

        assertThat(openReminders("OVERDUE_TASKS")).hasSize(1);
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
        // Completing publishes MaintenanceTaskCompleted: let its listener finish first
        await().atMost(Duration.ofSeconds(10)).until(() -> List.of(first, second).stream().allMatch(this::completionHandled));
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

    private List<SimpleMailMessage> emailsTo(String recipient) {
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(sent.capture());
        return sent.getAllValues().stream()
                .filter(message -> message.getTo() != null && List.of(message.getTo()).contains(recipient))
                .toList();
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

    private boolean completionHandled(long taskId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_publication
                 WHERE serialized_event LIKE ? AND completion_date IS NOT NULL""",
                Integer.class, "%\"taskId\":" + taskId + ",%") == 1;
    }

    private String taskStatus(long taskId) {
        return jdbc.queryForObject("SELECT status FROM maintenance_tasks WHERE id = ?", String.class, taskId);
    }
}
