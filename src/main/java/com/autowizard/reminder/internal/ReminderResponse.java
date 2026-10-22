package com.autowizard.reminder.internal;


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
