package com.autowizard.repository;

import com.autowizard.model.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {
    List<MaintenanceTask> findByStatus(MaintenanceTask.TaskStatus status);
    List<MaintenanceTask> findByVehicleId(Long vehicleId);
    List<MaintenanceTask> findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus status, Long ownerId);
    List<MaintenanceTask> findByStatusAndVehicleId(MaintenanceTask.TaskStatus status, Long vehicleId);
    List<MaintenanceTask> findByVehicleOwnerId(Long ownerId);
} 