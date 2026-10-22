package com.autowizard.maintenance.internal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What was actually done: cost and mileage of the work; the date defaults to now. */
public record CompleteTaskRequest(
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal actualCost,
        @NotNull @PositiveOrZero Long completionMileage,
        LocalDateTime completionDate) {
}
