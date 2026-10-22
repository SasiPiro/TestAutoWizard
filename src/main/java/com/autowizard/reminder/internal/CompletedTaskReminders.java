package com.autowizard.reminder.internal;

import com.autowizard.maintenance.MaintenanceTaskCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * A completed task leaves the user's open reminders at once, instead of being reminded about
 * until the next nightly run.
 *
 * <p>
 * {@link ApplicationModuleListener}: asynchronously, after the completion commits, in a
 * transaction of its own, so a failure here never rolls back the completed task. The event
 * publication registry stores the event with the completion and keeps it until this listener
 * succeeds, so a failed or interrupted delivery can be resubmitted.
 * </p>
 */
@Component
@RequiredArgsConstructor
class CompletedTaskReminders {

    private final ReminderService reminderService;

    @ApplicationModuleListener
    void on(MaintenanceTaskCompleted event) {
        reminderService.forgetCompletedTask(event.taskId(), event.userId());
    }
}
