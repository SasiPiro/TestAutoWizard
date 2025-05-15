package com.autowizard.dto;

import com.autowizard.model.MaintenanceTask;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MaintenanceTaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime scheduledDate;
    private MaintenanceTask.TaskStatus status;
    private MaintenanceTask.TaskPriority priority;
    private LocalDateTime completionDate;
    private String notes;
    private Double cost;
    private Long scheduledMileage;
    private Long completionMileage;
    private Long vehicleId;
} 