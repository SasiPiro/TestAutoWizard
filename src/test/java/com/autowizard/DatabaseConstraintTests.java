package com.autowizard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** What the services check is also guaranteed by the database, for concurrent requests. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DatabaseConstraintTests {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentRegistrationsOfTheSamePlateGiveOneVehicleAndOne409() throws Exception {
        TestApi api = new TestApi(mvc);
        List<String> tokens = List.of(api.newUser(), api.newUser());
        String vehicle = TestApi.vehicleJson(1_000);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> statuses = tokens.stream()
                    .map(token -> pool.submit(() -> {
                        start.await();
                        return mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON).content(vehicle))
                                .andReturn().getResponse().getStatus();
                    }))
                    .toList();
            start.countDown();
            assertThat(List.of(statuses.get(0).get(10, TimeUnit.SECONDS), statuses.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aPlateIsUniqueAmongActiveVehiclesOnly() throws Exception {
        TestApi api = new TestApi(mvc);
        long vehicleId = api.createVehicle(api.newUser());
        String plate = jdbc.queryForObject("SELECT license_plate FROM vehicles WHERE id = ?", String.class, vehicleId);

        assertThatThrownBy(() -> copyWithPlate(vehicleId, plate, "VINACTIVE0000001"))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update("UPDATE vehicles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", vehicleId);
        copyWithPlate(vehicleId, plate, "VINAFTERDELETE01");
    }

    @Test
    void aUserHasAtMostOneOpenReminderPerType() throws Exception {
        TestApi api = new TestApi(mvc);
        long vehicleId = api.createVehicle(api.newUser());
        long userId = jdbc.queryForObject("SELECT owner_id FROM vehicles WHERE id = ?", Long.class, vehicleId);

        reminder(userId, false);
        reminder(userId, true);  // notified ones are history: any number
        reminder(userId, true);
        assertThatThrownBy(() -> reminder(userId, false)).isInstanceOf(DuplicateKeyException.class);
    }

    private void copyWithPlate(long vehicleId, String plate, String vin) {
        jdbc.update("""
                INSERT INTO vehicles (owner_id, type, brand, model, license_plate, vin, mileage, registration_year, fuel)
                SELECT owner_id, type, brand, model, ?, ?, mileage, registration_year, fuel FROM vehicles WHERE id = ?""",
                plate, vin, vehicleId);
    }

    private void reminder(long userId, boolean notified) {
        jdbc.update("""
                INSERT INTO reminders (user_id, type, title, message, notification_date, notified)
                VALUES (?, 'OVERDUE_TASKS', 't', 'm', CURRENT_TIMESTAMP, ?)""", userId, notified);
    }
}
