package com.autowizard.dto;

import com.autowizard.model.Reminder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ReminderDTO {
    private Long id;
    private String title;
    private String message;
    private LocalDateTime notificationDate;
    private boolean notified;
    private Set<Long> taskIds;
    private Long userId;
    private Reminder.ReminderType type;
    private Set<Long> vehicleIds;
} 