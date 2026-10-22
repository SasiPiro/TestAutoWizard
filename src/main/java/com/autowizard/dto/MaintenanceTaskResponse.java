package com.autowizard.dto;

import com.autowizard.model.MaintenanceTask;

import java.math.BigDecimal;
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
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        Long scheduledMileage,
        Long completionMileage,
        Long vehicleId) {
}
