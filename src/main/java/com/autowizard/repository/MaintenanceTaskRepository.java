package com.autowizard.repository;

import com.autowizard.model.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {
    List<MaintenanceTask> findByStatus(MaintenanceTask.TaskStatus status);
    List<MaintenanceTask> findByVehicleId(Long vehicleId);
    List<MaintenanceTask> findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus status, Long ownerId);
    List<MaintenanceTask> findByStatusAndVehicleId(MaintenanceTask.TaskStatus status, Long vehicleId);
    List<MaintenanceTask> findByVehicleOwnerId(Long ownerId);

    /**
     * One statement for all users: pending tasks of active vehicles whose date has passed or
     * whose mileage the vehicle has reached become overdue.
     */
    @Modifying
    @Query("""
            UPDATE MaintenanceTask t SET t.status = :overdue
             WHERE t.status = :pending
               AND EXISTS (SELECT v.id FROM Vehicle v
                            WHERE v.id = t.vehicle.id AND v.deletedAt IS NULL
                              AND (t.scheduledDate < :now OR t.scheduledMileage <= v.mileage))""")
    int markOverdue(LocalDateTime now, MaintenanceTask.TaskStatus pending, MaintenanceTask.TaskStatus overdue);

    default int markOverdue(LocalDateTime now) {
        return markOverdue(now, MaintenanceTask.TaskStatus.PENDING, MaintenanceTask.TaskStatus.OVERDUE);
    }

    /** The task with its vehicle, if the vehicle belongs to the owner (and is not deleted). */
    @Query("SELECT t FROM MaintenanceTask t JOIN FETCH t.vehicle v WHERE t.id = :id AND v.owner.id = :ownerId")
    Optional<MaintenanceTask> findOwned(Long id, Long ownerId);
} 