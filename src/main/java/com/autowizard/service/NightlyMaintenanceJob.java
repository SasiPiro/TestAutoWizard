package com.autowizard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Every night, shortly after midnight in the users' zone: tasks past their date or mileage
 * become overdue, then every user's reminders are brought in line with their tasks. Each
 * step is one transaction, so a failure in a later step does not undo an earlier one.
 */
@Component
@RequiredArgsConstructor
public class NightlyMaintenanceJob {

    private final MaintenanceTaskService maintenanceTaskService;
    private final ReminderService reminderService;
    private final TimeService timeService;

    @Scheduled(cron = "${app.jobs.nightly.cron:0 1 0 * * *}", zone = "${app.time-zone:Europe/Rome}")
    public void run() {
        LocalDateTime now = timeService.now();
        maintenanceTaskService.markOverdueTasks(now);
        reminderService.refreshReminders(now);
    }
}
