package com.autowizard;

import com.autowizard.maintenance.MaintenanceTask;
import com.autowizard.reminder.internal.ReminderService;
import com.autowizard.vehicle.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entity equality is the identity of the row: it must not walk associations (generated
 * equals/hashCode over every field did), and an entity must equal a proxy of itself.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class EntityEqualityTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManagerFactory emf;
    @Autowired ReminderService reminderService;

    @Test
    void tasksCanBePutInASetAndRemindersGenerated() throws Exception {
        // Before: 500, the HashSet of tasks hashed vehicle -> owner -> collections
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        long taskId = api.createTask(token, vehicleId, "2030-01-01T10:00:00", 60_000);

        reminderService.refreshReminders(LocalDateTime.of(2029, 12, 31, 0, 1));

        mvc.perform(get("/api/reminders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskIds[0]").value(taskId));
    }

    @Test
    void anEntityEqualsAProxyOfItself() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        long otherId = api.createVehicle(token);
        Vehicle loaded = inNewContext(em -> em.find(Vehicle.class, vehicleId));
        Vehicle other = inNewContext(em -> em.find(Vehicle.class, otherId));

        try (EntityManager em = emf.createEntityManager()) {
            Vehicle proxy = em.getReference(Vehicle.class, vehicleId);
            assertThat(Hibernate.isInitialized(proxy)).as("a real, uninitialized proxy").isFalse();
            assertThat(proxy.getClass()).isNotEqualTo(loaded.getClass());

            assertThat(loaded).isEqualTo(proxy);
            assertThat(proxy).isEqualTo(loaded).isNotEqualTo(other);
            assertThat(proxy.hashCode()).isEqualTo(loaded.hashCode());
            assertThat(new HashSet<>(Set.of(loaded))).contains(proxy);
        }
    }

    @Test
    void transientEntitiesAreEqualOnlyToThemselves() {
        MaintenanceTask a = new MaintenanceTask();
        MaintenanceTask b = new MaintenanceTask();
        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }

    @Test
    void toStringDoesNotWalkAssociations() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        Vehicle vehicle = inNewContext(em -> em.find(Vehicle.class, vehicleId));

        // Outside the persistence context: touching a lazy collection here would throw
        assertThat(vehicle.toString()).isEqualTo("Vehicle(id=" + vehicleId + ")");
    }

    private <T> T inNewContext(Function<EntityManager, T> work) {
        try (EntityManager em = emf.createEntityManager()) {
            return work.apply(em);
        }
    }
}
