package com.autowizard.repository;

import com.autowizard.model.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {
    List<MaintenanceTask> findByStatus(MaintenanceTask.TaskStatus status);
    List<MaintenanceTask> findByVehicleId(Long vehicleId);
    List<MaintenanceTask> findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus status, Long ownerId);
    List<MaintenanceTask> findByStatusAndVehicleId(MaintenanceTask.TaskStatus status, Long vehicleId);
    List<MaintenanceTask> findByVehicleOwnerId(Long ownerId);

    /** The task with its vehicle, if the vehicle belongs to the owner (and is not deleted). */
    @Query("SELECT t FROM MaintenanceTask t JOIN FETCH t.vehicle v WHERE t.id = :id AND v.owner.id = :ownerId")
    Optional<MaintenanceTask> findOwned(Long id, Long ownerId);
} 