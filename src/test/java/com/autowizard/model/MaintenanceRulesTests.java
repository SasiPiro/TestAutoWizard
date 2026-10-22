package com.autowizard.model;

import com.autowizard.exception.BusinessRuleException;
import com.autowizard.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules on tasks and mileage, without Spring or a database. */
class MaintenanceRulesTests {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    @Test
    void completingATaskRecordsTheWorkAndRaisesTheVehicleMileage() {
        MaintenanceTask task = pendingTaskOn(vehicleAt(58_000));

        task.complete(new BigDecimal("210.00"), 61_200, NOW.minusDays(1), NOW);

        assertThat(task.getStatus()).isEqualTo(MaintenanceTask.TaskStatus.COMPLETED);
        assertThat(task.getActualCost()).isEqualByComparingTo("210");
        assertThat(task.getCompletionMileage()).isEqualTo(61_200);
        assertThat(task.getCompletionDate()).isEqualTo(NOW.minusDays(1));
        assertThat(task.getVehicle().getMileage()).isEqualTo(61_200);
    }

    @Test
    void workRecordedAtALowerMileageDoesNotLowerTheVehicleMileage() {
        // An old invoice entered late
        MaintenanceTask task = pendingTaskOn(vehicleAt(70_000));
        task.complete(null, 61_200, NOW, NOW);
        assertThat(task.getVehicle().getMileage()).isEqualTo(70_000);
    }

    @Test
    void aTaskCannotBeCompletedTwiceOrInTheFuture() {
        MaintenanceTask task = pendingTaskOn(vehicleAt(1_000));
        assertThatThrownBy(() -> task.complete(null, 1_000, NOW.plusDays(1), NOW))
                .isInstanceOf(BusinessRuleException.class);

        task.complete(null, 1_000, NOW, NOW);
        assertThatThrownBy(() -> task.complete(null, 1_000, NOW, NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aTypedMileageCanOnlyGrow() {
        Vehicle vehicle = vehicleAt(50_000);
        vehicle.updateMileage(50_000);
        vehicle.updateMileage(50_001);
        assertThat(vehicle.getMileage()).isEqualTo(50_001);
        assertThatThrownBy(() -> vehicle.updateMileage(49_999)).isInstanceOf(BusinessRuleException.class);
    }

    static Vehicle vehicleAt(long mileage) {
        Vehicle vehicle = new Vehicle();
        vehicle.setMileage(mileage);
        return vehicle;
    }

    static MaintenanceTask pendingTaskOn(Vehicle vehicle) {
        MaintenanceTask task = new MaintenanceTask();
        task.setStatus(MaintenanceTask.TaskStatus.PENDING);
        task.setVehicle(vehicle);
        return task;
    }
}
