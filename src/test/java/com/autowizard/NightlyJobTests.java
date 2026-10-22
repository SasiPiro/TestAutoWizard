package com.autowizard;

import com.autowizard.service.NightlyMaintenanceJob;
import com.autowizard.service.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    private String taskStatus(long taskId) {
        return jdbc.queryForObject("SELECT status FROM maintenance_tasks WHERE id = ?", String.class, taskId);
    }
}
