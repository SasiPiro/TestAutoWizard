package com.autowizard.reminder.internal;

import com.autowizard.maintenance.MaintenanceTaskService;
import com.autowizard.shared.TimeService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Every night, shortly after midnight in the users' zone: tasks past their date or mileage
 * become overdue, every user's reminders are brought in line with their tasks, and the
 * reminders that changed are emailed. Each step is one transaction, so a failure in a later
 * step does not undo an earlier one; emails go out after the reminders are committed.
 *
 * <p>
 * One run across all instances (ShedLock): the lock is held at least
 * {@code app.jobs.nightly.lock-at-least-for}, so an instance whose clock is a few seconds
 * behind does not run the job a second time, and at most 30 minutes if an instance dies
 * while running it.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class NightlyMaintenanceJob {

    private final MaintenanceTaskService maintenanceTaskService;
    private final ReminderService reminderService;
    private final ReminderMailer reminderMailer;
    private final TimeService timeService;

    @Scheduled(cron = "${app.jobs.nightly.cron:0 1 0 * * *}", zone = "${app.time-zone:Europe/Rome}")
    @SchedulerLock(name = "nightlyMaintenance", lockAtMostFor = "PT30M",
            lockAtLeastFor = "${app.jobs.nightly.lock-at-least-for:PT5M}")
    public void run() {
        LocalDateTime now = timeService.now();
        maintenanceTaskService.markOverdueTasks(now);
        reminderMailer.send(reminderService.refreshReminders(now));
    }
}
