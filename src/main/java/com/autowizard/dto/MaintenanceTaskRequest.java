package com.autowizard.dto;

import com.autowizard.model.MaintenanceTask;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What a client may set on a new task: status, completion and actual cost are not among them. */
public record MaintenanceTaskRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String description,
        @NotNull LocalDateTime scheduledDate,
        @NotNull MaintenanceTask.TaskPriority priority,
        @NotNull @PositiveOrZero Long scheduledMileage,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal estimatedCost,
        @Size(max = 255) String notes,
        @NotNull Long vehicleId) {
}
