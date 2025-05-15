package com.autowizard.repository;

import com.autowizard.model.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {
    List<MaintenanceTask> findByVehicleId(Long vehicleId);
} 