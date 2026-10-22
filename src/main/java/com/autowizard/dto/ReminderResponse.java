package com.autowizard.dto;

import com.autowizard.model.Reminder;

import java.time.LocalDateTime;
import java.util.Set;

public record ReminderResponse(
        Long id,
        String title,
        String message,
        LocalDateTime notificationDate,
        boolean notified,
        Set<Long> taskIds,
        Long userId,
        Reminder.ReminderType type,
        Set<Long> vehicleIds) {
}
