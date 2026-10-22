package com.autowizard.dto;

import com.autowizard.model.MaintenanceTask;

import java.time.LocalDateTime;

public record MaintenanceTaskResponse(
        Long id,
        String title,
        String description,
        LocalDateTime scheduledDate,
        MaintenanceTask.TaskStatus status,
        MaintenanceTask.TaskPriority priority,
        LocalDateTime completionDate,
        String notes,
        Double estimatedCost,
        Double actualCost,
        Long scheduledMileage,
        Long completionMileage,
        Long vehicleId) {
}
