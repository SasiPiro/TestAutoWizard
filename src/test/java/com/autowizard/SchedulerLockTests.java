package com.autowizard;

import com.autowizard.reminder.internal.NightlyMaintenanceJob;
import com.autowizard.shared.TimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** With the production lock settings, a second trigger in the lock window does nothing. */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class SchedulerLockTests {

    @Autowired MockMvc mvc;
    @Autowired NightlyMaintenanceJob job;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean TimeService timeService;

    @Test
    void anotherTriggerWhileTheLockIsHeldIsSkipped() throws Exception {
        when(timeService.now()).thenReturn(LocalDateTime.of(2026, 10, 21, 0, 1));
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token, 60_000);

        job.run();
        // Another instance (or the same one) triggers again within lockAtLeastFor
        long taskId = api.createTask(token, vehicleId, "2026-10-01T10:00:00", 90_000);
        job.run();

        assertThat(jdbc.queryForObject("SELECT status FROM maintenance_tasks WHERE id = ?", String.class, taskId))
                .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT lock_until > locked_at FROM shedlock WHERE name = 'nightlyMaintenance'",
                Boolean.class)).isTrue();
    }
}
